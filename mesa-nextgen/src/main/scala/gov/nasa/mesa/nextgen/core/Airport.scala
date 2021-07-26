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
package gov.nasa.mesa.nextgen.core

import gov.nasa.mesa.nextgen.core.rnav.{AirportRNavStars, KSFORNavStars,
  RnavStar}
import gov.nasa.race.geo.{GeoPosition, GeoPositioned}

/** Used to store Airport instances.
  *
  * TODO: to be merged with air.Airport in RACE
  */
object Airport {

  val KSFO = new Airport("KSFO", "San Francisco International", "San Francisco",
    GeoPosition.fromDegreesAndFeet(37.618889, -122.375, 13), true, true,
    KSFORNavStars)

  /** the list of airports defined in MESA. */
  val airportList = Seq(KSFO)

  /** Gets the Airport instance with the given airportId.
    *
    * @param id the ICAO airport code
    * @return Some[Airport] including the Airport instance with the given
    *         ICAO code, or None if there is no airport with the given ICAO
    *         code.
    */
  def getAirport(id: String): Option[Airport] =
    airportList.find(airport => airport.id.equalsIgnoreCase(id))

  /** Returns the specified RNAV STAR instance.
    *
    * @param airportId the ICAO airport code
    * @param starName the name of a RNAV STAR procedure
    * @return Some[RnavStar] object including the specified RnavStar instance,
    *         or None if a RNAV STAR with the given name does not exist.
    */
  def getRnavStar(airportId: String, starName: String): Option[RnavStar] =
    getAirport(airportId) flatMap {
      _.getRnavStar(starName)
    }

  /** Checks if the specified RNAV STAR is valid.
    *
    * @param airportId the ICAO airport code
    * @param starName the name of a RNAV STAR procedure
    * @return true if the airport instance with the given id has a RNAV STAR
    *         with a given name, otherwise returns false.
    */
  def isRnavStar(airportId: String, starName: String): Boolean = {
    getAirport(airportId).fold(false) { a => a.isRnavStar(starName) }
  }

  /** Gets the airport for the specified RNAV STAR procedure.
    *
    * @param starName the name of a RNAV STAR procedure
    * @return Some[Airport] object including an Aiport instance to which the
    *         given RNAV STAR belongs, or None if there is no airport that
    *         includes the specified RNAV STAR.
    */
  def getStarAirport(starName: String): Option[Airport] =
    airportList.find(airport => airport.stars.getRnavStar(starName).isDefined)

  final val NoAirport = new Airport("<none>", "", "",
    GeoPosition.fromDegreesAndFeet(0, 0, 0), false, false, null)
}

/** Represents objects that capture airports.
  *
  * @param id the ICAO airport code
  * @param name the name of the airport
  * @param city the city for the airport
  * @param position the position of the airport
  * @param hasAsdex indicates if the airport is equipped with ASDE-X
  *                 surveillance system
  * @param hasStar indicates if the airport has STAR procedures
  * @param stars the list of RNAV STAR procedures that belong to the airport
  */
case class Airport(id: String,
                   name: String,
                   city: String,
                   position: GeoPosition,
                   hasAsdex: Boolean,
                   hasStar: Boolean,
                   stars: AirportRNavStars) extends GeoPositioned {

  /** Returns the airport RNAV STAR procedure with the given name.
    *
    * @param starName the name of a RNAV STAR
    * @return Some[RnavStar] object including the specified RnavStar
    *         instance, or None if a RNAV STAR with the given name does not
    *         exist.
    */
  def getRnavStar(starName: String): Option[RnavStar] =
    stars.getRnavStar(starName)

  /** Checks if the specified RNAV STAR is valid.
    *
    * @param starName  a RNAV STAR name
    * @return true if the RNAV STAR with a given name belongs to the airport,
    *         otherwise, returns false.
    */
  def isRnavStar(starName: String): Boolean = stars.isRnavStar(starName)
}

