import sbt._

object Version {
  final val scala = "2.12.18"
  final val akka = "2.6.20"
  final val akkaHttp = "10.2.10"
  final val akkaHttpCirce = "1.39.2"
  final val akkaPersistenceCassandra = "1.0.6"
  final val akkaStreamKafka = "2.1.1"
  final val scalaTest = "3.2.17"
  final val commonsIO = "2.15.1"
  final val logbackClassic = "1.4.14"
  final val circe = "0.14.6"
  final val leveldb = "0.12"
  final val leveldbJni = "1.8"
  final val jodaTime = "2.12.5"
  final val cats = "2.10.0"
  final val slick = "3.4.1"
  final val slf4jNop = "2.0.9"
  final val wixMysql = "4.6.2"
  final val mysqlDriver = "8.2.0"
  final val betterFiles = "3.9.2"
  final val scalatags = "0.12.0"
  final val squeryl = "0.9.17"
  final val catsEffect = "2.5.5"
  final val unboundid = "6.0.11"
  final val dsiLdap = "0.4.1"
  final val zio = "2.0.21"
  final val zioJdbc = "0.1.2"
}

object Library {
  val akkaActor                = "com.typesafe.akka"          %%  "akka-actor"                      % Version.akka
  val akkaStream               = "com.typesafe.akka"          %%  "akka-stream"                     % Version.akka
  val akkaPersistence          = "com.typesafe.akka"          %%  "akka-persistence"                % Version.akka
  val akkaCluster              = "com.typesafe.akka"          %%  "akka-cluster"                    % Version.akka
  val akkaClusterTools         = "com.typesafe.akka"          %%  "akka-cluster-tools"              % Version.akka
  val akkaClusterSharding      = "com.typesafe.akka"          %%  "akka-cluster-sharding"           % Version.akka
  val akkaSlf4j                = "com.typesafe.akka"          %%  "akka-slf4j"                      % Version.akka
  val akkaPersistenceCassandra = "com.typesafe.akka"          %%  "akka-persistence-cassandra"      % Version.akkaPersistenceCassandra
  val akkaStreamKafka          = "com.typesafe.akka"          %%  "akka-stream-kafka"               % Version.akkaStreamKafka
  val commonsIO                = "commons-io"                 %   "commons-io"                      % Version.commonsIO
  val logbackClassic           = "ch.qos.logback"             %   "logback-classic"                 % Version.logbackClassic
  val akkaHttp                 = "com.typesafe.akka"          %%  "akka-http"                       % Version.akkaHttp
  val akkaHttpCirce            = "de.heikoseeberger"          %%  "akka-http-circe"                 % Version.akkaHttpCirce
  val circeCore                = "io.circe"                   %%  "circe-core"                      % Version.circe
  val circeGeneric             = "io.circe"                   %%  "circe-generic"                   % Version.circe
  val circeParser              = "io.circe"                   %%  "circe-parser"                    % Version.circe
  val jodaTime                 = "joda-time"                  %   "joda-time"                       % Version.jodaTime
  val catsCore                 = "org.typelevel"              %%  "cats-core"                       % Version.cats

  val akkaTestKit              = "com.typesafe.akka"          %%  "akka-testkit"                    % Version.akka
  val akkaMultiNodeTestkit     = "com.typesafe.akka"          %%  "akka-multi-node-testkit"         % Version.akka
  val scalaTest                = "org.scalatest"              %%  "scalatest"                       % Version.scalaTest

  val leveldb                  = "org.iq80.leveldb"           %   "leveldb"                         % Version.leveldb
  val leveldbJni               = "org.fusesource.leveldbjni"  %   "leveldbjni-all"                  % Version.leveldbJni

  val slick                    = "com.typesafe.slick"         %% "slick"                            % Version.slick
  val slf4jNop                 = "org.slf4j"                  %  "slf4j-nop"                        % Version.slf4jNop
  val slickHikaricp            = "com.typesafe.slick"         %% "slick-hikaricp"                   % Version.slick
  val mysqlDriver              = "com.mysql"                  %  "mysql-connector-j"                % Version.mysqlDriver

  val wixMysql                 = "com.wix"                    %  "wix-embedded-mysql"               % Version.wixMysql
  val betterFiles              = "com.github.pathikrit"       %% "better-files"                     % Version.betterFiles
  val scalatags                = "com.lihaoyi"                %% "scalatags"                        % Version.scalatags
  val scalaCompiler            = "org.scala-lang"             %  "scala-compiler"                   % Version.scala
  val squeryl                  = "org.squeryl"                %% "squeryl"                          % Version.squeryl
  val h2database               = "com.h2database"             %  "h2"                               % "2.2.224"
  val catsEffect               = "org.typelevel"              %% "cats-effect"                      % Version.catsEffect
  val unboundid                = "com.unboundid"              %  "unboundid-ldapsdk"                % Version.unboundid
  val dsiLdap                  = "pt.tecnico.dsi"             %% "ldap"                              % Version.dsiLdap
  val zio                      = "dev.zio"                    %% "zio"                              % Version.zio
  val zioJdbc                  = "dev.zio"                    %% "zio-jdbc"                         % Version.zioJdbc
  val postgresql               = "org.postgresql"             %  "postgresql"                       % "42.7.1"
}

object Groupings {
  val akkaBasics = Seq(
    Library.akkaActor,
    Library.akkaStream,
    Library.akkaSlf4j,
    Library.commonsIO,
    Library.logbackClassic,
    Library.akkaTestKit % "test"
  )

  val akkaPersistence = Seq(
    Library.akkaPersistence,
    Library.leveldb,
    Library.leveldbJni
  )

  val akkaCluster = Seq(
    Library.akkaCluster,
    Library.akkaClusterTools,
    Library.akkaClusterSharding,
    Library.akkaMultiNodeTestkit
  )

  val akkaHttp = Seq(
    Library.akkaHttp,
    Library.akkaHttpCirce
  )

  val circe = Seq(
    Library.circeCore,
    Library.circeGeneric,
    Library.circeParser
  )

  val slick = Seq(
    Library.slick,
    Library.slickHikaricp,
    Library.slf4jNop,
    Library.mysqlDriver
  )
}