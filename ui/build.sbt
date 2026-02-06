name := """ui"""
organization := "com.ecommmerce"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.18"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.ecommmerce.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.ecommmerce.binders._"
