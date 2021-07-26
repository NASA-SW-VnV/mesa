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

import gov.nasa.mesa.nextgen.core.FlightPlan
import gov.nasa.race.test.RaceActorSpec
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * A test suite for gov.nasa.mesa.nextgen.core.FlightPlan.
  */
class FlightPlanSpec extends RaceActorSpec with AnyWordSpecLike {

  "FlightPlan" must {
    "retrieve the departure procedure and the initial transition from the " +
      "ICAO route" in {
      val route1 = "KBWI.TERPZ6.MAULS.Q40.AEX.WAPPL4.KHOU/0209"
      val departure1 = FlightPlan.getDepartureProcedure(route1)

      assert(departure1.isDefined)
      assert(departure1.get.transition == "MAULS")
      assert(departure1.get.name == "TERPZ6")

      val route2 = "KFLG.FLG1.OATES..NAVHO..DRK..MAIER.BRUSR1.KPHX/0148"
      val departure2 = FlightPlan.getDepartureProcedure(route2)

      assert(departure2.isDefined)
      assert(departure2.get.transition == "OATES")
      assert(departure2.get.name == "FLG1")

      val route3 = "KFLG.FLG1.OATES..NAVHO..DRK..MAIER.BRUSR1.KPHX"
      val departure3 = FlightPlan.getDepartureProcedure(route3)

      assert(departure3.isDefined)
      assert(departure3.get.transition == "OATES")
      assert(departure3.get.name == "FLG1")
    }
  }

  "FlightPlan" must {
    "handle the ICAO flight plan with no departure procedure assigned" in {
      val route = "KBWI..Q40.AEX.WAPPL4.KHOU/0209"
      val departure = FlightPlan.getDepartureProcedure(route)
      assert(departure.isEmpty)
    }
  }

  "FlightPlan" must {
    "retrieve the arrival procedure and the initial transition from the " +
      "ICAO route" in {
      val route1 = "KBWI.TERPZ6.MAULS.Q40.AEX.WAPPL4.KHOU/0209"
      val arrival1 = FlightPlan.getArrivalProcedure(route1)

      assert(arrival1.isDefined)
      assert(arrival1.get.transition == "AEX")
      assert(arrival1.get.name == "WAPPL4")

      val route2 = "KFLG.FLG1.OATES..NAVHO..DRK..MAIER.BRUSR1.KPHX/0148"
      val arrival2 = FlightPlan.getArrivalProcedure(route2)

      assert(arrival2.isDefined)
      assert(arrival2.get.transition == "MAIER")
      assert(arrival2.get.name == "BRUSR1")
    }
  }

  "FlightPlan" must {
    "handle the ICAO flight plan with no arrival procedure assigned" in {
      val route = "KBWI.TERPZ6.MAULS.Q40..KHOU/0209"
      val arrival = FlightPlan.getArrivalProcedure(route)
      assert(arrival.isEmpty)
    }
  }
}
