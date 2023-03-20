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
package gov.nasa.mesa.nextgen.monitors

import com.typesafe.config.{Config, ConfigFactory}
import gov.nasa.mesa.nextgen.core._
import gov.nasa.mesa.nextgen.core.rnav.KSFORNavStars._
import gov.nasa.mesa.nextgen.verification.monitors.rnav.RNAV_STAR_Adherence
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.uom.{Angle, DateTime, Speed}
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * A test suite for gov.nasa.mesa.nextgen.verification.monitors.rnav.
  * RNAV_STAR_Adherence
  */
class RNAV_STAR_AdherenceSpec extends RaceActorSpec with AnyWordSpecLike {

  val config: Config = ConfigFactory.parseString(
    """{
      airport = "KSFO"
      star = "BDEGA3"
      }""")

  // ExtendedFlightState definition
  val id = "253"
  val cs = "SWA3651"
  val route = "KSEA.HAROB6.FEPOT.Q3.FOWND..MLBEC.BDEGA2.KSFO/0055"
  val departureAirport = "KBWI"
  val arrivalAirport = "KHOU"
  val departure: Option[Procedure] = FlightPlan.getDepartureProcedure(route)
  val arrival: Option[Procedure] = FlightPlan.getArrivalProcedure(route)
  val flightRules = "IFR"
  val equipmentQualifier = "I"

  // FlightState definition
  val FlightState: ExtendedFlightState = new ExtendedFlightState(id, cs, GeoPosition.undefinedPos,
    Speed.UndefinedSpeed, Angle.UndefinedAngle, Speed.UndefinedSpeed,
    DateTime.Date0, 0, "?", departureAirport, DateTime.Date0, arrivalAirport,
    DateTime.Date0, new FlightPlan(cs, route, departure, arrival, flightRules),
    equipmentQualifier)

  "RNAV_STAR_Adherence" must {
    "report error if the flight does not adhere to the STAR assigned to it" in {
      val monitor = new RNAV_STAR_Adherence(config)

      monitor.verify(WaypointVisit(FlightState, MLBEC))
      monitor.verify(WaypointVisit(FlightState, JONNE))
      monitor.verify(WaypointVisit(FlightState, LOZIT))

      monitor.getErrorCount should be (1)
    }
  }

  "RNAV_STAR_Adherence" must {
    "check if the flight adheres to the STAR assigned to it" in {
      val monitor = new RNAV_STAR_Adherence(config)

      monitor.verify(WaypointVisit(FlightState, MLBEC))
      monitor.verify(WaypointVisit(FlightState, JONNE))
      monitor.verify(WaypointVisit(FlightState, BGGLO))

      monitor.getErrorCount should be (0)
    }
  }

  "RNAV_STAR_Adherence" must {
    "handle traces with repeated waypoints" in {
      val monitor = new RNAV_STAR_Adherence(config)

      monitor.verify(WaypointVisit(FlightState, MLBEC))
      monitor.verify(WaypointVisit(FlightState, JONNE))
      monitor.verify(WaypointVisit(FlightState, JONNE))
      monitor.verify(WaypointVisit(FlightState, BGGLO))

      monitor.getErrorCount should be(0)
    }
  }

  "RNAV_STAR_Adherence" must {
    "stop monitoring the flight after it reaches the final waypoint" in {
      val monitor = new RNAV_STAR_Adherence(config)

      monitor.verify(WaypointVisit(FlightState, MLBEC))
      monitor.verify(WaypointVisit(FlightState, JONNE))
      monitor.verify(WaypointVisit(FlightState, BGGLO))
      monitor.verify(WaypointVisit(FlightState, LOZIT))
      monitor.verify(WaypointVisit(FlightState, BDEGA))
      monitor.verify(WaypointVisit(FlightState, CORKK))
      monitor.verify(WaypointVisit(FlightState, BRIXX))
      // since BRIXX is the final waypoint, it should stop monitoring here
      monitor.verify(WaypointVisit(FlightState, Waypoint.NoWaypoint))

      monitor.getErrorCount should be(0)
    }
  }

  "RNAV_STAR_Adherence" must {
    "ensure that the flight transitions into the route at an specified " +
      "initial waypoint" in {
      val monitor = new RNAV_STAR_Adherence(config)

      monitor.verify(WaypointVisit(FlightState, BDEGA))
      monitor.verify(WaypointVisit(FlightState, CORKK))
      monitor.verify(WaypointVisit(FlightState, BRIXX))

      monitor.getErrorCount should be(1)
    }
  }
}
