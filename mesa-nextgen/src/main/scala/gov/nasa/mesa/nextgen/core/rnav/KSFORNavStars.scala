/**
  * Copyright © 2020 United States Government as represented by the
  * Administrator of the National Aeronautics and Space Administration.  All
  * Rights Reserved.
  *
  * No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY
  * WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING,
  * BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM
  * TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
  * A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT THE
  * SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT DOCUMENTATION,
  * IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE. THIS AGREEMENT DOES
  * NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY
  * PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE
  * PRODUCTS OR ANY OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT
  * SOFTWARE.  FURTHER, GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND
  * LIABILITIES REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE ORIGINAL
  * SOFTWARE, AND DISTRIBUTES IT "AS IS."
  *
  * Waiver and Indemnity:  RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS
  * AGAINST THE UNITED STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS,
  * AS WELL AS ANY PRIOR RECIPIENT.  IF RECIPIENT'S USE OF THE SUBJECT
  * SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES, EXPENSES OR LOSSES
  * ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM PRODUCTS BASED ON, OR
  * RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT SOFTWARE, RECIPIENT SHALL
  * INDEMNIFY AND HOLD HARMLESS THE UNITED STATES GOVERNMENT, ITS CONTRACTORS
  * AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE EXTENT
  * PERMITTED BY LAW.  RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE
  * THE IMMEDIATE, UNILATERAL TERMINATION OF THIS AGREEMENT.
  */
package gov.nasa.mesa.nextgen.core.rnav

import gov.nasa.mesa.nextgen.core.Waypoint
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.uom.Length.Meters
import gov.nasa.race.uom.Speed.MetersPerSecond

/** This object includes information about the RNAV STARs for San Francisco
  * International Airport.
  *
  * TODO: connect MESA to the RACE waypoint database to obtain waypoints on-the-fly.
  */
object KSFORNavStars extends AirportRNavStars {

  //-- waypoints
  val PEENO = new Waypoint("PEENO",
    GeoPosition.fromDegrees(38.57025833, -123.6155333), Meters(0), Meters(0),
    MetersPerSecond(0))

  val AMAKR = new Waypoint("AMAKR",
    GeoPosition.fromDegrees(39, -123.75), Meters(0), Meters(0),
    MetersPerSecond(0))

  val MLBEC = new Waypoint("MLBEC",
    GeoPosition.fromDegrees(38.87477222, -122.9589889), Meters(0), Meters(0),
    MetersPerSecond(0))

  val MRRLO = new Waypoint("MRRLO",
    GeoPosition.fromDegrees(38.89754722, -122.5782333), Meters(0), Meters(0),
    MetersPerSecond(0))

  val LEGGS = new Waypoint("LEGGS",
    GeoPosition.fromDegrees(39.336625, -121.0957639), Meters(0), Meters(0),
    MetersPerSecond(0))

  val DEEAN = new Waypoint("DEEAN",
    GeoPosition.fromDegrees(38.34916389, -123.3022889), Meters(0), Meters(0),
    MetersPerSecond(0))

  val QUINN = new Waypoint("QUINN",
    GeoPosition.fromDegrees(38.47995278, -123.08815), Meters(0), Meters(0),
    MetersPerSecond(0))

  val JONNE = new Waypoint("JONNE",
    GeoPosition.fromDegrees(38.55104167, -122.863275), Meters(0), Meters(0),
    MetersPerSecond(0))

  val MSCAT = new Waypoint("MSCAT",
    GeoPosition.fromDegrees(38.56669722, -122.6716667), Meters(0), Meters(0),
    MetersPerSecond(0))

  val GEEHH = new Waypoint("GEEHH",
    GeoPosition.fromDegrees(38.45333333, -122.42865), Meters(0), Meters(0),
    MetersPerSecond(0))

  // added in BDEGA3 replacing GEEHH
  val PYLLE = new Waypoint("PYLLE",
    GeoPosition.fromDegrees(38.45333333, -122.42865), Meters(0), Meters(0),
    MetersPerSecond(0))

  val BGGLO = new Waypoint("BGGLO",
    GeoPosition.fromDegrees(38.22458889, -122.7675056), Meters(0), Meters(0),
    MetersPerSecond(0))

  val LOZIT = new Waypoint("LOZIT",
    GeoPosition.fromDegrees(37.899325, -122.6731944), Meters(0), Meters(0),
    MetersPerSecond(0))

  val BDEGA = new Waypoint("BDEGA",
    GeoPosition.fromDegrees(37.823025, -122.5922), Meters(0), Meters(0),
    MetersPerSecond(0))

  val CORKK = new Waypoint("CORKK",
    GeoPosition.fromDegrees(37.73358889, -122.49755), Meters(0), Meters(0),
    MetersPerSecond(0))

  val BRIXX = new Waypoint("BRIXX",
    GeoPosition.fromDegrees(37.61784444, -122.3745278), Meters(0), Meters(0),
    MetersPerSecond(0))

  val YOSEM = new Waypoint("YOSEM",
    GeoPosition.fromDegrees(37.76274167, -118.7666111), Meters(0), Meters(0),
    MetersPerSecond(0))

  val SNORA = new Waypoint("SNORA",
    GeoPosition.fromDegrees(37.64555556, -119.8062944), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ARCHI = new Waypoint("ARCHI",
    GeoPosition.fromDegrees(37.49079722, -121.8755417), Meters(0), Meters(0),
    MetersPerSecond(0))

  val FRIGG = new Waypoint("FRIGG",
    GeoPosition.fromDegrees(37.46550833, -121.2572889), Meters(0), Meters(0),
    MetersPerSecond(0))

  val FAITH = new Waypoint("FAITH",
    GeoPosition.fromDegrees(37.40121667, -121.8619), Meters(0), Meters(0),
    MetersPerSecond(0))

  val NARWL = new Waypoint("NARWL",
    GeoPosition.fromDegrees(37.27478056, -122.0792944), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ORRCA = new Waypoint("ORRCA",
    GeoPosition.fromDegrees(38.44365833, -121.5516222), Meters(0), Meters(0),
    MetersPerSecond(0))

  val MVRKK = new Waypoint("MVRKK",
    GeoPosition.fromDegrees(37.73697222, -122.45445), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ZOMER = new Waypoint("ZOMER",
    GeoPosition.fromDegrees(37.54534722, -120.6314417), Meters(0), Meters(0),
    MetersPerSecond(0))

  val FLOWZ = new Waypoint("FLOWZ",
    GeoPosition.fromDegrees(37.59251944, -121.26475), Meters(0), Meters(0),
    MetersPerSecond(0))

  val NRRLI = new Waypoint("NRRLI",
    GeoPosition.fromDegrees(36.4956, -121.6994), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ADDMM = new Waypoint("ADDMM",
    GeoPosition.fromDegrees(37.77102778, -121.7085861), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ALWYS = new Waypoint("ALWYS",
    GeoPosition.fromDegrees(37.63341944, -120.9594306), Meters(0), Meters(0),
    MetersPerSecond(0))

  val ARRTU = new Waypoint("ARRTU",
    GeoPosition.fromDegrees(37.73362222, -121.5029889), Meters(0), Meters(0),
    MetersPerSecond(0))

  val OOWEN = new Waypoint("OOWEN",
    GeoPosition.fromDegrees(37.70699167, -121.2747806), Meters(0), Meters(0),
    MetersPerSecond(0))

  val TIPRE = new Waypoint("TIPRE",
    GeoPosition.fromDegrees(38.20583333, -121.0358333), Meters(0), Meters(0),
    MetersPerSecond(0))

  val SERFR = new Waypoint("SERFR",
    GeoPosition.fromDegrees(36.06830556, -121.3646639), Meters(0), Meters(0),
    MetersPerSecond(0))

  val LAANE = new Waypoint("LAANE",
    GeoPosition.fromDegrees(37.65898333, -120.7473917), Meters(0), Meters(0),
    MetersPerSecond(0))

  val WWAVS = new Waypoint("WWAVS",
    GeoPosition.fromDegrees(36.74153056, -121.8942333), Meters(0), Meters(0),
    MetersPerSecond(0))

  val FRELY = new Waypoint("FRELY",
    GeoPosition.fromDegrees(37.510575, -121.7931528), Meters(0), Meters(0),
    MetersPerSecond(0))

  val RISTI = new Waypoint("RISTI",
    GeoPosition.fromDegrees(37.608125, -121.5332778), Meters(0), Meters(0),
    MetersPerSecond(0))

  val BERKS = new Waypoint("BERKS",
    GeoPosition.fromDegrees(37.86086111, -122.2116778), Meters(0), Meters(0),
    MetersPerSecond(0))

  val WESLA = new Waypoint("WESLA",
    GeoPosition.fromDegrees(37.66437222, -122.4802917), Meters(0), Meters(0),
    MetersPerSecond(0))

  val WPOUT = new Waypoint("WPOUT",
    GeoPosition.fromDegrees(37.11948611, -122.2927417), Meters(0), Meters(0),
    MetersPerSecond(0))

  val HEFLY = new Waypoint("HEFLY",
    GeoPosition.fromDegrees(37.68385278, -121.2310306), Meters(0), Meters(0),
    MetersPerSecond(0))

  val DYAMD = new Waypoint("DYAMD",
    GeoPosition.fromDegrees(37.69916111, -120.4044278), Meters(0), Meters(0),
    MetersPerSecond(0))

  val RUSME = new Waypoint("RUSME",
    GeoPosition.fromDegrees(37.49416667, -117.52), Meters(0), Meters(0),
    MetersPerSecond(0))

  val SOOIE = new Waypoint("SOOIE",
    GeoPosition.fromDegrees(37.42857222, -121.6076389), Meters(0), Meters(0),
    MetersPerSecond(0))

  val MOVDD = new Waypoint("MOVDD",
    GeoPosition.fromDegrees(37.66135556, -121.4482028), Meters(0), Meters(0),
    MetersPerSecond(0))

  val BYRON = new Waypoint("BYRON",
    GeoPosition.fromDegrees(37.82286389, -121.4693694), Meters(0), Meters(0),
    MetersPerSecond(0))

  val EPICK = new Waypoint("EPICK",
    GeoPosition.fromDegrees(36.95082222, -121.9526722), Meters(0), Meters(0),
    MetersPerSecond(0))

  val THEEZ = new Waypoint("THEEZ",
    GeoPosition.fromDegrees(37.50346944, -122.4247528), Meters(0), Meters(0),
    MetersPerSecond(0))

  val INYOE = new Waypoint("INYOE",
    GeoPosition.fromDegrees(37.89562222, -118.7649917), Meters(0), Meters(0),
    MetersPerSecond(0))

  val CEDES = new Waypoint("CEDES",
    GeoPosition.fromDegrees(37.55082222, -121.6245861), Meters(0), Meters(0),
    MetersPerSecond(0))

  val TRACY = new Waypoint("TRACY",
    GeoPosition.fromDegrees(37.73143889, -121.4593028), Meters(0), Meters(0),
    MetersPerSecond(0))

  val STLER = new Waypoint("STLER",
    GeoPosition.fromDegrees(37.70136389, -122.7110639), Meters(0), Meters(0),
    MetersPerSecond(0))

  val HAIRE = new Waypoint("HAIRE",
    GeoPosition.fromDegrees(37.90713611, -121.4803778), Meters(0), Meters(0),
    MetersPerSecond(0))

  val EDDYY = new Waypoint("EDDYY",
    GeoPosition.fromDegrees(37.37490278, -122.11875), Meters(0), Meters(0),
    MetersPerSecond(0))

  val COGGR = new Waypoint("COGGR",
    GeoPosition.fromDegrees(37.81898056, -121.9761444), Meters(0), Meters(0),
    MetersPerSecond(0))

  val waypointList = Seq(PEENO, AMAKR, MLBEC, MRRLO, LEGGS, DEEAN, QUINN, JONNE,
    MSCAT, GEEHH, PYLLE, BGGLO, LOZIT, BDEGA, CORKK, BRIXX, YOSEM, SNORA, ARCHI,
    FRIGG, FAITH, NARWL, ORRCA, MVRKK, ZOMER, FLOWZ, NRRLI, ADDMM, ALWYS, ARRTU,
    OOWEN, TIPRE, SERFR, LAANE, WWAVS, FRELY, RISTI, BERKS, WESLA, WPOUT, HEFLY,
    DYAMD, RUSME, SOOIE, MOVDD, BYRON, EPICK, THEEZ, INYOE, CEDES, TRACY, STLER,
    HAIRE, EDDYY, COGGR)

  //-- Definition of RNAV STAR procedures
  override val rnavStarList = List(
    //-- BDEGA TWO ARRIVAL
    RnavStar("BDEGA2", StarGraph(
      (PEENO, DEEAN),
      (DEEAN, LOZIT),
      (LOZIT, BDEGA),
      (BDEGA, CORKK),
      (CORKK, BRIXX),
      (AMAKR, QUINN),
      (QUINN, BGGLO),
      (BGGLO, LOZIT),
      (MLBEC, JONNE),
      (JONNE, BGGLO),
      (MRRLO, MSCAT),
      (MSCAT, BGGLO),
      (LEGGS, GEEHH),
      (GEEHH, BGGLO)
    )),
    //-- BDEGA THREE ARRIVAL
    RnavStar("BDEGA3", StarGraph(
      (PEENO, DEEAN),
      (DEEAN, LOZIT),
      (LOZIT, BDEGA),
      (BDEGA, CORKK),
      (CORKK, BRIXX),
      (AMAKR, QUINN),
      (QUINN, BGGLO),
      (BGGLO, LOZIT),
      (MLBEC, JONNE),
      (JONNE, BGGLO),
      (MRRLO, MSCAT),
      (MSCAT, BGGLO),
      (LEGGS, PYLLE),
      (PYLLE, BGGLO)
    )),
    //-- SERFR THREE ARRIVAL
    RnavStar("SERFR3", StarGraph(
      (SERFR, NRRLI),
      (NRRLI, WWAVS),
      (WWAVS, EPICK),
      (EPICK, NARWL),
      (NARWL, EDDYY)
    )),
    //-- DYAMD THREE ARRIVAL
    RnavStar("DYAMD3", StarGraph(
      (RUSME, DYAMD),
      (INYOE, DYAMD),
      (DYAMD, LAANE),
      (LAANE, ALWYS),
      (ALWYS, FLOWZ),
      (FLOWZ, CEDES),
      (CEDES, FRELY),
      (FRELY, ARCHI)
    )),
    //-- DYAMD FOUR ARRIVAL
    RnavStar("DYAMD4", StarGraph(
      (RUSME, DYAMD),
      (INYOE, DYAMD),
      (DYAMD, LAANE),
      (LAANE, ALWYS),
      (ALWYS, FLOWZ),
      (FLOWZ, CEDES),
      (CEDES, FRELY),
      (FRELY, ARCHI)
    ))
  )

  override val rnavStarNames = Seq("ALWYS2", "BDEGA2", "BDEGA3", "DYAMD3",
    "DYAMD4", "RISTI1", "SERFR3", "STLER3", "WWAVS1", "YOSEM3")
}
