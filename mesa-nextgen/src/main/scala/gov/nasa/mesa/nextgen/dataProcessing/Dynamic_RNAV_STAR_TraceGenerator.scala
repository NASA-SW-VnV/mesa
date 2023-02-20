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

/**
  * This class represents a MESA actor that generates traces composed of a
  * sequence of waypoints which belong to the STAR assigned to the flight and
  * are "visited" by the flight with a lateral deviation specified in the given
  * config (using the key "deviation"). The traces also include StarChange
  * and FlightCompleted events. Note that the STAR assigned to the flight is
  * obtained on-the-fly from the flight plan captured by FlightTrack
  * instances.
  */
class Dynamic_RNAV_STAR_TraceGenerator(val config: Config) extends MesaActor {

  /** Lateral deviation specified in config. */
  val dev: Length = NauticalMiles(config.getDoubleOrElse("deviation", 1.0))

  /** Receives BusEvent objects representing the message events FlightInfo,
    * StarChanged, FlightCompleted.
    *
    * For FlightInfo(FlightState, FlightTrack) objects, if the flight position
    * is within the given radius from one of the STAR's waypoints, it publishes
    * the event WaypointVisit to the specified channel. For StarChanged and
    * FlightCompleted, it just publishes the message event on
    * the channel.
    *
    * @return a partial function with the Dynamic_RNAV_STAR_TraceGenerator
    *         actor logic.
    */
  override def handleMessage: Receive = {
    case BusEvent(_, finfo@FlightInfo(state@FlightState(_, _, _, _, _, _, _, _),
    ft@FlightTrack(_, _, _, _, _, _)), _) =>
      // if the star is not rnav, the message is ignored
      if (Airport.isRnavStar(ft.arrivalPoint,
        ft.getArrivalProcedure.fold("")(_.name))) {
        Geo.getWaypointInProximity(state, dev, getWaypointList(ft)) foreach
          { wp => publish(WaypointVisit(finfo, wp))}
      }
    case BusEvent(_, msg@StarChanged(_), _) => publish(msg)
    case BusEvent(_, msg@FlightCompleted(_, _), _) => publish(msg)
  }

  /** Obtains the waypoints of the star for the given FlightTrack.
    *
    * @param ft an object storing the flight track information
    * @return a list of the waypoints specified in the given FlightTrack arrival
    *         procedure.
    */
  def getWaypointList(ft: FlightTrack): Seq[Waypoint] = {

    val star = ft.getArrivalProcedure flatMap {
      arr => Airport.getRnavStar(ft.arrivalPoint, arr.name)
    }

    val wpList = star.fold(Seq.empty[Waypoint]) {
      _.waypoints
    }

    if (wpList.isEmpty && ft.getArrivalProcedure.isDefined)
      println(s"${Console.MAGENTA}WARNING: ${ft.cs} re-assigned to unknown " +
        s"STAR ${ft.getArrivalProcedure}")

    wpList
  }
}