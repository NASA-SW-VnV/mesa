import sbt._

object Dependencies {
  lazy val latestVersion = "latest.release"
  lazy val raceVersion = "1.8.0"

  val typesafeConfig = "com.typesafe" % "config" % "1.4.1"

  val akkaVersion = "2.6.4" //"2.6.9 in race
  val akkaOrg = "com.typesafe.akka"

  val akkaActor = akkaOrg %% "akka-actor" % akkaVersion
  val akkaTestkit = akkaOrg %% "akka-testkit" % akkaVersion

  //--- scalaTest
  val scalaTest = "org.scalatest" %% "scalatest" % "3.3.0-SNAP2" //(?) % "test" ?? % Test //"3.1.0-SNAP13"
  val flexmarkAll = "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" // should be a scalaTest dependency but 3.1.0-SNAP13 is missing it
  val scalaTestPlus = "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" //"1.0.0-SNAP8"

  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3" % Test

  val jodaTime = "joda-time" % "joda-time" % "2.10.1"

  //--- dependencies to RACE sub-projects
  val raceCore = "gov.nasa.race" %% "race-core" % raceVersion
  val raceNetJms = "gov.nasa.race" %% "race-net-jms" % raceVersion
  val raceAir = "gov.nasa.race" %% "race-air" % raceVersion
  val raceWwAir = "gov.nasa.race" %% "race-ww-air" % raceVersion
  val raceWw = "gov.nasa.race" %% "race-ww" % raceVersion
  val raceTestKit = "gov.nasa.race" %% "race-testkit" % raceVersion

  //--- dependencies to DSLs used for specification
  val daut = "git://github.com/havelund/daut.git#master"
  val tracecontract = "git://github.com/havelund/tracecontract.git#master"
}