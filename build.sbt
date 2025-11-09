ThisBuild / scalaVersion := "2.13.14"

lazy val commonSettings = Seq(
    organization := "io.github.stanleyli",
    version := "0.1.0",
    libraryDependencies ++= Seq(
        "org.apache.pekko" %% "pekko-actor-typed" % "1.1.2",
        "org.apache.pekko" %% "pekko-stream" % "1.1.2",
        "ch.qos.logback" % "logback-classic" % "1.5.20"
    )
)

lazy val core = (project in file("crawler-core"))
    .settings(commonSettings)
    .settings(
        name := "crawler-core"
    )

lazy val master = (project in file("crawler-master"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
        name := "crawler-master"
    )

lazy val worker = (project in file("crawler-worker"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
        name := "crawler-worker"
    )

lazy val root = (project in file("."))
    .aggregate(core, master, worker)
    .settings(
        name := "distributed-crawler",
        publish / skip := true
    )