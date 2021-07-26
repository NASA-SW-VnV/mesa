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
package gov.nasa.mesa.nextgen

import com.typesafe.config.ConfigFactory
import gov.nasa.mesa.nextgen.core.{FlightPlan, FlightTrack}
import gov.nasa.mesa.nextgen.dataProcessing.filters.FlightTrackFilter
import gov.nasa.race.test.RaceActorSpec
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * A test suite for gov.nasa.mesa.nextgen.dataProcessing.filters.
  * FlightTrackFilter.
  */
class FlightTrackFilterSpec extends RaceActorSpec with AnyWordSpecLike {

  // FlightTrack definition
  val id = "253"
  val cs = "SWA3651"
  val route = "KBWI.TERPZ6.MAULS.Q40.AEX.WAPPL4.KHOU/0209"
  val departureAirport = "KBWI"
  val arrivalAirport = "KHOU"
  val departure = FlightPlan.getDepartureProcedure(route)
  val arrival = FlightPlan.getArrivalProcedure(route)
  val flightRules = "IFR"
  val equipmentQualifier = "I"
  val ti = FlightTrack(id, cs, departureAirport, arrivalAirport,
    new FlightPlan(cs, route, departure, arrival), flightRules,
    equipmentQualifier)

  "FlightTrackFilter" must {
    "match FlightTracks using the given setting in the config" in {
      val config = ConfigFactory.parseString(
        """
      arrival-point = ["KHOU"]
    """)
      val filter = new FlightTrackFilter(config)
      assert(filter.pass(ti))
    }
  }

  "FlightTrackFilter" must {
    "filter out FlightTracks that do not match the given setting in the " +
      "config" in {
      val config1 = ConfigFactory.parseString(
        """
      equipment-qualifier = ["L","G","Z"]
    """)
      val filter1 = new FlightTrackFilter(config1)
      println(s"arrival-point: ${config1}")
      assert(!filter1.pass(ti))

      val config2 = ConfigFactory.parseString(
        """
      arrival-point = ["KHOU"]
      equipment-qualifier = ["L","G","Z"]
    """)
      val filter2 = new FlightTrackFilter(config2)
      println(s"arrival-point: ${config2}")
      assert(!filter2.pass(ti))
    }
  }
}
