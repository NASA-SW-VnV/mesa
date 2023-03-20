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
package gov.nasa.mesa.nextgen.dataProcessing.filters

import com.typesafe.config.Config
import gov.nasa.mesa.core.MesaActor
import gov.nasa.mesa.nextgen.core._
import gov.nasa.race.config.ConfigurableFilter
import gov.nasa.race.core.BusEvent

import scala.collection.immutable.HashMap

/** This class represents a MESA actor used to filter out flight state objects
  * including ExtendedFlightState and FlightStateCompleted. It received a trace
  * including these objects and publishes ExtendedFlightState and FlightCompleted
  * objects.
  *
  * It includes a built-in filter of type FlightTrackFilter which filters out
  * data based on the given track information specified in the actor
  * configuration.
  *
  * @param config the actor configuration
  */
class FilteredFlightStateGenerator(val config: Config) extends MesaActor {

  var tiList = HashMap.empty[String, ExtendedFlightState]

  val flightTrackFilter: ConfigurableFilter = new FlightTrackFilter(config)

  /** This defines the actor behavior specified as a partial function with the
    * FilteredFlightStateGenerator actor logic.
    *
    * @return a partial function with the FilteredFlightStateGenerator actor
    *         logic.
    */
  override def handleMessage: Receive = {
    case BusEvent(_, state@ExtendedFlightState(_, cs, _, _, _, _, _, _, _, _, _, _), _) =>
      val tInfo = tiList.get(cs)
      if (tInfo.isEmpty) {
        if (state.hasflightPlan && flightTrackFilter.pass(state)) {
          // started monitoring cs
          tiList += (cs -> state)
          publish(state)
        }
      } else if(state.hasflightPlan) {
        // check to see if the arrival procedure has changed
        val arr1 = tInfo.get.getArrivalProcedure.getOrElse(None)
        val arr2 = state.getArrivalProcedure.getOrElse(None)
        if (!arr1.equals(arr2)) {
          println(s"${Console.YELLOW}$cs STAR CHANGED: ${arr1} -> " +
            s"${arr2}${Console.RESET}")
          publish(StarChanged(state))

          if (flightTrackFilter.pass(state)) {
            // updating
            tiList += (cs -> state)
            publish(state)
          } else {
            println(s"${Console.MAGENTA}WARNING: new $cs STAR is out of scope:" +
              s"\n ${tInfo.get.fplan.route} -> ${state.fplan.route}${Console.RESET}")
            // updating
            tiList -= cs
          }
        } else {
          publish(state)
        }
      } else { // !tInfo.isEmpty && !ft.hasflightPlan
        // as long as the last recorded flight plan is in the list, we still publish
        publish(ExtendedFlightState(state.id, state.cs, state.position, state.speed, state.heading, state.vr, state.date, state.status, state.src,
          state.departurePoint, state.departureDate, state.arrivalPoint, state.arrivalDate, tInfo.get.fplan,
          state.equipmentQualifier))
      }
    case BusEvent(_, FlightStateCompleted(_, cs, _, _, _, _, _, _), _) =>
      if (tiList.contains(cs)) {
        // remove the flight from the list
        val ft = tiList.get(cs)
        tiList -= cs
        publish(FlightCompleted(cs,
          ft.get.getArrivalProcedure.getOrElse(Procedure.NotAssigned)))
      }
  }
}

