resolvers += Classpaths.typesafeReleases

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.21")

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

