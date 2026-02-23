lazy val ecommerce =
  project
    .in(file("."))
    .aggregate(customers, fulfillment, inventory, `order-tracking`, payment, `product-catalog`,
      receiving, shipping, shoppingcart, orchestrator)


lazy val commonSettings = Seq(
  scalaVersion := Version.scala,
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaHttp ++
    Groupings.circe
)

lazy val common = project.in(file("common")).settings(commonSettings)

lazy val orchestratorSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.orchestrator.Boot"),
  assembly / assemblyJarName := "orchestrator.jar",
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Seq(
      Library.scalaTest % "test",
      Library.jodaTime,
      Library.catsCore,
      Library.betterFiles,
      Library.scalatags
    )
)

lazy val orchestrator = project.in(file("orchestrator")).settings(orchestratorSettings).dependsOn(common)

lazy val customersSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.customers.Boot"),
  assembly / assemblyJarName := "customers.jar",
  libraryDependencies ++= Seq(
    Library.akkaActor,
    Library.akkaHttp,
    Library.akkaHttpCirce
  )
)

lazy val customers = project.in(file("customers")).settings(customersSettings)


lazy val fulfillmentSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.fulfillment.Boot"),
  assembly / assemblyJarName := "fulfillment.jar",
  libraryDependencies ++= Seq(
    Library.akkaActor,
    Library.akkaHttp,
    Library.akkaHttpCirce
  )
)

lazy val fulfillment = project.in(file("fulfillment")).settings(fulfillmentSettings)


lazy val inventorySettings = Seq(
  scalaVersion := Version.scala,
  assembly / assemblyJarName := "inventory.jar",
  resolvers ++= Seq("Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
    "Sonatype snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots/"),
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaPersistence ++
    Groupings.akkaCluster ++
    Groupings.akkaHttp ++
    Groupings.circe  ++
    Seq(
      Library.scalaTest % "test",
      Library.betterFiles,
      Library.mysqlDriver
    )
)

lazy val inventory = project.in(file("inventory")).settings(inventorySettings).dependsOn(common)


lazy val orderTrackingSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.ordertracking.Boot"),
  assembly / assemblyJarName := "ordertracking.jar",
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaCluster ++
    Groupings.akkaPersistence ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Seq(
      Library.dsiLdap,
      Library.catsEffect
    )
)

lazy val `order-tracking` = project.in(file("order-tracking")).settings(orderTrackingSettings)


lazy val paymentSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.payment.Boot"),
  assembly / assemblyJarName := "payment.jar",
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Seq(
      Library.zio,
      Library.zioJdbc,
      Library.postgresql,
      Library.scalaCompiler,
      Library.scalatags
    )
)

lazy val payment = project.in(file("payment")).settings(paymentSettings)


lazy val productcatalogSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.productcatalog.Boot"),
  assembly / assemblyJarName := "productcatalog.jar",
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  run / fork := true,
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Groupings.slick ++
    Seq(
      Library.scalaTest % "test",
      Library.wixMysql  % "test",
      Library.postgresql,
      Library.scalaCompiler
    )
)

lazy val `product-catalog` = project.in(file("product-catalog")).settings(productcatalogSettings).dependsOn(common)


lazy val receivingSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.receiving.Boot"),
  assembly / assemblyJarName := "receiving.jar",
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaPersistence ++
    Groupings.akkaCluster ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Seq(
      Library.dsiLdap,
      Library.catsEffect,
      Library.scalatags
    )
)


lazy val receiving = project.in(file("receiving")).settings(receivingSettings).dependsOn(common)


lazy val shippingSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.shipping.Boot"),
  assembly / assemblyJarName := "shipping.jar",
  libraryDependencies ++=
    Groupings.akkaBasics ++
      Groupings.akkaPersistence ++
      Groupings.akkaHttp ++
      Groupings.circe
)

lazy val shipping = project.in(file("shipping")).settings(shippingSettings)


lazy val shoppingcartSettings = Seq(
  scalaVersion := Version.scala,
  Global / mainClass := Some("com.ecommerce.shoppingcart.Boot"),
  assembly / assemblyJarName := "shoppingcart.jar",
  resolvers ++= Seq("Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
    "Sonatype snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots/"),
  libraryDependencies ++=
    Groupings.akkaBasics ++
    Groupings.akkaCluster ++
    Groupings.akkaPersistence ++
    Groupings.akkaHttp ++
    Groupings.circe ++
    Seq(
      Library.scalaTest % "test",
      Library.akkaTestKit % "test",
      Library.scalatags
    )
)

lazy val shoppingcart = project.in(file("shoppingcart")).settings(shoppingcartSettings).dependsOn(common)


lazy val ui = project.in(file("ui")).enablePlugins(PlayScala)
