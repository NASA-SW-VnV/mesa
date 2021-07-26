import sbt.Keys._
import Dependencies._

name := "mesa"

scalacOptions in (Compile, doc) += "-no-java-comments"

// factor out common settings across the sub-projects
lazy val commonSettings = Seq(
  organization := "gov.nasa",
  version := "1.0",
  scalaVersion := "2.13.2",
  mainClass in Compile := Some("gov.nasa.mesa.core.MesaMain"),
  scalacOptions in (Compile, doc) += "-no-java-comments",
  // uncomment to get the full stack trace for failed tests
  //testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
  libraryDependencies ++=
    Seq(
      typesafeConfig,
      akkaActor,
      akkaTestkit,
      scalaTest,
      flexmarkAll,
      scalaTestPlus,
      scalaCheck,
      jodaTime,
      raceCore,
      raceNetJms,
      raceAir,
      raceWw,
      raceWwAir,
      raceTestKit
    ),
  commands ++= Commands.stagingCmds
)

lazy val subProjectSettings = commonSettings ++ Seq(
  unmanagedBase := (unmanagedBase in LocalRootProject).value
)

lazy val dautProject =
  RootProject(uri(sys.props.getOrElse("daut_uri", daut)))
lazy val tracecontractProject =
  RootProject(uri(sys.props.getOrElse("tracecontract_uri", tracecontract)))

// root project aggregates the sub-projects
lazy val root = Project("mesa", file("."))
  .aggregate(mesaCore, mesaNextgen)
  .dependsOn(mesaCore, mesaNextgen)
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings)

lazy val mesaCore = Project("mesa-core", file("mesa-core"))
  .dependsOn(dautProject, tracecontractProject)
  .settings(subProjectSettings)

// mesa-nas provides runtime verification for national airspace system properties
lazy val mesaNextgen = Project("mesa-nextgen", file("mesa-nextgen"))
  .dependsOn(mesaCore)
  .settings(subProjectSettings)
