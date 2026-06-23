# How to build (and run) this project

A complete, battle-tested guide for the **ecommerce** multi-module Akka microservices
project. The single most important thing is the **JDK version**; on this machine the
build is **verified green on JDK 21**. If anything goes wrong, jump to
[Troubleshooting](#troubleshooting).

> Unlike the older `scala-http4s-realworld-example-app` (which needs JDK 11), this
> project uses a **newer toolchain (sbt 1.9.7 + Scala 2.12.18)** that compiles cleanly on
> **JDK 21**. There are **no `-Xfatal-warnings`** here, so deprecation/feature warnings do
> **not** fail the build.

## Toolchain at a glance

| Component | Version | Pinned in |
| --- | --- | --- |
| Scala | 2.12.18 | `project/Dependencies.scala` (`Version.scala`) |
| sbt | 1.9.7 | `project/build.properties` |
| JDK to build | **21 (verified)**; 11/17 also expected to work | not pinned, you set `JAVA_HOME` |
| Compiler flags | default (NO `-Xfatal-warnings`; warnings are non-fatal) | per-module in `build.sbt` |
| Build plugins | `sbt-assembly` 2.1.5, Play `sbt-plugin` 2.8.21 | `project/plugins.sbt` |

This is a **multi-module aggregate build**. Modules (see `build.sbt`):

```
common          shared views / identity / client-actors (HTTP + Kafka)
orchestrator    API gateway (Akka HTTP); aggregates Shopping/Receiving/Admin routes
customers       Akka HTTP service (skeleton)
fulfillment     Akka HTTP service (skeleton)
inventory       Akka HTTP + persistence/cluster + MySQL
order-tracking  Akka HTTP + LDAP (dsiLdap) + cats-effect
payment         Akka HTTP + ZIO + zio-jdbc + PostgreSQL
product-catalog Akka HTTP + Slick + MySQL
receiving       Akka HTTP + LDAP (dsiLdap) + cats-effect
shipping        Akka HTTP + persistence (skeleton)
shoppingcart    Akka HTTP + persistence/cluster
ui              Play application (enablePlugins(PlayScala))
```

---

## TL;DR

1. Have a **JDK 21** (or 11/17) reachable via `JAVA_HOME`.
2. `sbt compile` (whole build) or `sbt "<module>/compile"` (one module).

```powershell
# PowerShell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version          # verified: openjdk 21.0.10 (Temurin)
sbt -batch clean compile
```

```bash
# bash / git-bash / Linux / macOS
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
sbt -batch clean compile
```

A clean build ends with `[success] Total time: ...`. The only output you will see is
deprecation/feature/unused warnings (e.g. in `ShardSupport.scala`, akka-http 10.2.0
deprecations). **These are expected and harmless** — there is no `-Xfatal-warnings`.

---

## Why the JDK matters (and what is verified)

The Scala 2.12.18 + sbt 1.9.7 toolchain here is modern enough for current JDKs.

| JDK | Result | Notes |
| --- | --- | --- |
| **21** | **Compiles (verified on this machine)** | `Eclipse Adoptium Temurin 21.0.10`; `sbt clean compile` -> `[success]` in ~37s |
| **17** | Expected to work | sbt 1.9.x supports it; not separately re-verified here |
| **11** | Expected to work | LTS, safe baseline for Scala 2.12 |
| **8** | May work for compile | older; prefer 11+ |

You do **not** need to uninstall other JDKs — just override `JAVA_HOME` for this project's
terminal session if your default is something else. Always confirm with `java -version` in
the **same** shell you run sbt in.

`sbt` is pinned to `1.9.7` via `project/build.properties`; the `sbt` launcher on your PATH
bootstraps that automatically, so the launcher version does not matter.

### Getting a JDK (portable, no admin) — example: Temurin 21 on Windows

```powershell
$dir = "$env:USERPROFILE\jdk21"
New-Item -ItemType Directory -Force $dir | Out-Null
$url = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.10%2B7/OpenJDK21U-jdk_x64_windows_hotspot_21.0.10_7.zip"
Invoke-WebRequest -Uri $url -OutFile "$dir\jdk21.zip"
Expand-Archive -Path "$dir\jdk21.zip" -DestinationPath $dir -Force
$env:JAVA_HOME = "$dir\jdk-21.0.10+7"   # the extracted folder name
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

System installs: `winget install EclipseAdoptium.Temurin.21.JDK` /
`choco install temurin21` / `sudo apt-get install -y openjdk-21-jdk` /
`brew install temurin@21`.

### Installing the sbt launcher (if needed)

```powershell
winget install sbt.sbt      # or: choco install sbt
```
```bash
# Debian/Ubuntu
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo tee /etc/apt/trusted.gpg.d/sbt.asc
sudo apt-get update && sudo apt-get install sbt
```

---

## Prerequisites checklist

- [ ] **JDK 21** (or 11/17) reachable via `JAVA_HOME` (`java -version` confirms it in the build shell).
- [ ] **sbt launcher** on PATH (`sbt -version`; it bootstraps sbt 1.9.7).
- [ ] **Internet** for the first build (downloads compiler + deps into the Coursier cache).
- [ ] **Docker** (Docker Desktop / engine + compose) only if you want to **run** the services against LDAP / PostgreSQL / MySQL. **Not needed for `sbt compile`.**
- [ ] Run `sbt` from the **repo root** (where `build.sbt` lives).

---

## Common commands

```bash
sbt -batch compile                      # compile ALL modules (aggregate)
sbt -batch "orchestrator/compile"       # compile a single module (fast feedback)
sbt -batch "payment/compile"
sbt -batch clean compile                # full clean rebuild (use to verify green)
sbt -batch "inventory/Test/compile"     # compile a module's tests
sbt -batch "orchestrator/test"          # run a module's tests
sbt -batch update                       # resolve/download deps only
sbt -batch "show orchestrator/scalacOptions"   # inspect active compiler flags
sbt -batch "orchestrator/assembly"      # build the fat jar (sbt-assembly)
```

`-batch` disables interactive prompts (good for CI / one-shot). Drop it to use the shell.

### Fastest edit loop: the sbt shell

Each `sbt <task>` boots a fresh JVM (slow). Open the shell once and stay in it:

```
sbt
> orchestrator/compile
> ~orchestrator/compile     # watch mode: recompile on save (best feedback loop)
> exit
```

### Per-module compile is your friend

The aggregate `compile` builds everything. When you only touched one module, compile just
that module (`sbt "payment/compile"`) — it is much faster and the error output is scoped.

---

## How dependencies are resolved

- Libraries are declared in **`project/Dependencies.scala`** — three objects:
  - `Version` — version constants.
  - `Library` — individual `ModuleID`s (e.g. `auth0Jwt`, `akkaHttpSession`, `postgresql`, `mysqlDriver`, `dsiLdap`, `zio`, ...).
  - `Groupings` — convenience `Seq`s (`akkaBasics`, `akkaHttp`, `circe`, `akkaCluster`, `akkaPersistence`, `slick`).
- Each module's `libraryDependencies` in **`build.sbt`** pulls from those. To add a dep:
  1. add a `val` to `Library` (and a constant to `Version` if you want),
  2. add `Library.<name>` to the **target module's** `Seq` in `build.sbt`,
  3. `sbt update` (or just compile).
  Example already done here: `auth0Jwt` and `akkaHttpSession` were added to the
  **orchestrator** module only.
- Cached in the **Coursier cache**:
  - Windows: `%LOCALAPPDATA%\Coursier\Cache\v1\` (`C:\Users\<you>\AppData\Local\Coursier\Cache\v1\`)
  - Linux: `~/.cache/coursier/v1/`
  - macOS: `~/Library/Caches/Coursier/v1/`
- **Scala-binary-version matters**: a Scala lib appears in the cache as `<artifact>_2.12`
  (this project) vs `_2.13` / `_3`. A `_2.13`/`_3` jar will **not** resolve for a 2.12
  module. Pure-Java libs (declared with a single `%`, e.g. `com.auth0:java-jwt`) have no
  suffix and work on any Scala version.
- To recover from a corrupt download: delete the offending artifact folder under the cache
  and rerun `sbt update`.

### Inspecting a library's real API with javap

When you need the exact constructor/method signature of a dependency:

```bash
# 1. locate the jar in the cache (Windows path shown)
ls "$LOCALAPPDATA/Coursier/Cache/v1/https/repo1.maven.org/maven2/<group/as/path>/<artifact>/<version>/"*.jar
# 2. dump the public API
"$JAVA_HOME/bin/javap" -cp "<path-to.jar>" fully.qualified.ClassName
```

For a Scala case class, the `javap` accessor names equal the `apply(...)`/`copy(...)`
parameter names — the reliable way to get named-argument names right (used here to confirm
`com.softwaremill.session.CookieConfig(name, domain, path, secure, httpOnly, sameSite)`).

---

## Running the services (needs Docker backends)

`sbt compile` does **not** need any database/LDAP. **Running** the persistence-backed
services does. A `docker-compose.yml` ships all backends:

```bash
docker compose up -d          # start LDAP + Postgres + MySQL (+ phpLDAPadmin)
docker compose ps             # check status
docker compose down           # stop (add -v to wipe volumes)
```

| Service | Container | Host port -> container | Credentials / DB |
| --- | --- | --- | --- |
| OpenLDAP | `ecommerce-ldap` | `2389 -> 389`, `2636 -> 636` | domain `ecommerce.com`, admin pw `admin` |
| phpLDAPadmin | `ecommerce-ldap-admin` | `8081 -> 80` | web UI for the LDAP above |
| PostgreSQL | `ecommerce-postgres` | `5433 -> 5432` | user `postgres` / pw `postgres`, DB `payments` |
| MySQL | `ecommerce-mysql` | `3307 -> 3306` | root pw `rootpassword`; DB `productcatalog`, user `admin`/`password1` |

Which module needs which backend:

| Module | Backend |
| --- | --- |
| payment | PostgreSQL `localhost:5433` DB `payments` (see `PaymentService` `ZConnectionPool.postgres`) |
| product-catalog | MySQL (Slick + mysql-connector-j) |
| inventory | MySQL (mysql-connector-j) |
| order-tracking, receiving | LDAP (dsiLdap / unboundid) at `localhost:2389` |
| orchestrator, shoppingcart, others | Akka HTTP / in-memory persistence; no external DB to start |

Init SQL/LDIF lives in `postgres-init/`, `mysql-init/`, `ldap-init/` (mounted into the
containers on first start).

### Launch a service

Each microservice has its own `Boot` main class (declared as `Global / mainClass` in
`build.sbt`). Run one with:

```bash
sbt -batch "orchestrator/run"
# or explicitly:
sbt -batch "orchestrator/runMain com.ecommerce.orchestrator.Boot"
sbt -batch "payment/runMain com.ecommerce.payment.Boot"
```

The `ui` module is a Play app: `sbt -batch "ui/run"` (serves on Play's default `:9000`).

Build distributable fat jars with sbt-assembly: `sbt "orchestrator/assembly"` (jar name set
per module, e.g. `orchestrator.jar`).

---

## Windows-specific notes

- git-bash uses POSIX paths (`/c/Users/...`); PowerShell uses `C:\Users\...`. Both work; be
  consistent within one command.
- Quote paths containing spaces: `"C:\Program Files\..."`.
- `git add` may warn `LF will be replaced by CRLF` — harmless line-ending normalization.
- The `Bash` shell available here is **Git Bash (POSIX sh)** — use `/dev/null`, forward
  slashes, `$VAR`. PowerShell here is **Windows PowerShell 5.1** — no `&&` chaining; use `;`
  or `if ($?) { ... }`.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `sbt` won't launch / `ClassCastException ... xsbti.FullReload` | Very old sbt on a new JDK (not expected here; sbt is pinned 1.9.7) | Ensure the pinned launcher is used; set `JAVA_HOME` to JDK 21/11 |
| Build uses the wrong Java | `JAVA_HOME` unset / PATH order | `java -version` in the same shell; put `$JAVA_HOME\bin` first |
| `unresolved dependency` / download errors | No internet / proxy / corrupt cache | Check connectivity/proxy; delete the artifact folder in the Coursier cache; `sbt update` |
| A Scala lib won't resolve though the jar "is there" | Wrong Scala binary (`_2.13`/`_3` present, `_2.12` missing) | Use a version published for `_2.12`, or a pure-Java lib; check the `_2.12` cache dir |
| `not found: value <param>` on `.copy(...)`/`apply(...)` | Wrong named param for that lib version | `javap` the class to get the real param names |
| `object X is not a member of package Y` | Missing dep or wrong import | Add the dep to `Library` + the module's `Seq` in `build.sbt`; reload |
| First build hangs / very slow | Downloading compiler + deps | Wait once; later builds are cached/offline |
| `OutOfMemoryError` / `Metaspace` | Heap too small | `JAVA_TOOL_OPTIONS="-Xmx2g"` then rebuild |
| sbt stuck on stale incremental state | Zinc cache out of date | `sbt clean compile` |
| Test sources fail to compile but main is fine | Tests out of sync with code | Does **not** block `sbt compile` of main; fix or ignore tests as needed |
| Deprecation / feature / unused warnings | Normal — **no `-Xfatal-warnings`** here | Ignore; they do not fail the build |
| `Connection refused` / auth failure at **run** (payment) | Postgres not up / wrong port | `docker compose up -d postgres`; it maps host **5433** |
| LDAP errors at **run** (order-tracking/receiving) | OpenLDAP not up | `docker compose up -d openldap`; host port **2389** |
| MySQL errors at **run** (product-catalog/inventory) | MySQL not up | `docker compose up -d mysql`; host port **3307** |
| Port already in use on `docker compose up` | 2389/2636/8081/5433/3307 taken | Stop the conflicting process or remap ports in `docker-compose.yml` |

### Still stuck? Gather this before asking for help

```bash
java -version
echo "$JAVA_HOME"        # PowerShell: echo $env:JAVA_HOME
sbt -version
```

Run `sbt -batch clean compile` and copy the **first** `[error]` line — errors cascade, so
the first one is the real cause — plus the three commands above.
