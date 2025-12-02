// File: build.sbt (project root)

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.crawler"

val pekkoVersion = "1.0.2"
val kafkaVersion = "3.6.1"
val circeVersion = "0.14.6"
val slf4jVersion = "2.0.9"
val logbackVersion = "1.4.14"

lazy val commonDependencies = Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.1",
  "org.jsoup" % "jsoup" % "1.17.2",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.google.guava" % "guava" % "32.1.3-jre",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

lazy val root = (project in file("."))
  .aggregate(core, master, worker, api)
  .settings(
    name := "distributed-crawler"
  )

lazy val core = (project in file("crawler-core"))
  .settings(
    name := "crawler-core",
    libraryDependencies ++= commonDependencies
  )

lazy val master = (project in file("crawler-master"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, worker)
  .settings(
    name := "crawler-master",
    libraryDependencies ++= commonDependencies,
    Compile / mainClass := Some("com.crawler.master.CrawlerLauncher"),
    Universal / javaOptions := Seq("-J-Xmx2g", "-J-Xms512m")  // -J prefix for JVM opts
  )

lazy val worker = (project in file("crawler-worker"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, api)
  .settings(
    name := "crawler-worker",
    libraryDependencies ++= commonDependencies,
    Compile / mainClass := Some("com.crawler.worker.WorkerMain")
  )

lazy val api = (project in file("crawler-api"))
  .dependsOn(core)
  .settings(
    name := "crawler-api",
    libraryDependencies ++= commonDependencies
  )