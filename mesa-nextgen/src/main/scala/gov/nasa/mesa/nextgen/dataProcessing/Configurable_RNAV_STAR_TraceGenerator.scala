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
package gov.nasa.mesa.nextgen.dataProcessing

import com.typesafe.config.Config
import gov.nasa.mesa.core.MesaActor
import gov.nasa.mesa.nextgen.core._
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.BusEvent
import gov.nasa.race.uom.Length
import gov.nasa.race.uom.Length.NauticalMiles

/** This class represents a MESA actor that generates traces composed of a
  * sequence of waypoints which belong to the STARs specified in the given
  * config (using the key "star") and are "visited" by the flight with a lateral
  * deviation specified in the given config (using the key "deviation").
  *
  * @param config the actor configuration
  */
class Configurable_RNAV_STAR_TraceGenerator(val config: Config)
  extends MesaActor {

  // Airport airportId
  val airportId: String = config.getString("airport")

  // List of RNAV STARs
  val stars: Seq[String] = config.getStringListOrElse("star", Seq.empty[String])

  // Lateral deviation specified in config
  val dev: Length = NauticalMiles(config.getDoubleOrElse("deviation", 1.0))

  // A list of waypoints that belong to 'stars'
  var waypointList: Seq[Waypoint] = getWaypointList

  /** Receives BusEvent objects including FlightState events, and if the
    * flight position is within the given radius from one of the specified
    * waypoints, it publishes a pair of (FlightState, Waypoint) to the
    * specified channel.
    *
    * @return a partial function with the Configurable_RNAV_STAR_TraceGenerator
    *         actor logic.
    */
  override def handleMessage: Receive = {
    case BusEvent(_, state: ExtendedFlightState, _) =>
      val proximityWaypoint = Geo.getWaypointInProximity(state, dev,
        waypointList)
      if (proximityWaypoint.isDefined) publish((state, proximityWaypoint.get))
  }

  /** Returns a list of all waypoints that belong to the RNAR STARs specified in
    * the given config.
    *
    * @return a list of all waypoints that belong to the RNAR STARs specified in
    *         the config.
    */
  def getWaypointList: Seq[Waypoint] = {
    var wpList = Seq[Waypoint]()
    if (stars.isEmpty)
      wpList = Airport.getAirport(airportId).get.stars.waypointList
    else stars.foreach(
      starName => {
        val star = Airport.getRnavStar(airportId, starName)
        if (star.isDefined) wpList ++= star.get.waypoints
        else sys.error(s"The STAR $starName for the airport $airportId is not" +
          s" supported.")
      })
    wpList
  }
}