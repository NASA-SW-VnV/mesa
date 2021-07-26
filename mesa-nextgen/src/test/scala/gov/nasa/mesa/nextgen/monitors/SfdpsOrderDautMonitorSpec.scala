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
import gov.nasa.mesa.nextgen.core.FlightState
import gov.nasa.mesa.nextgen.verification.monitors.daut.{FlightMsgOrder_Monitor, SingleFlightMsgOrder_Monitor}
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.uom.{Angle, DateTime, Speed}
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * A test suite for gov.nasa.mesa.nextgen.verification.monitors.daut.
  * FlightMsgOrder_Monitor/SingleFlightMsgOrder_Monitor
  */
class SfdpsOrderDautMonitorSpec extends RaceActorSpec with AnyWordSpecLike {

  val emptyConfig = ConfigFactory.empty

  val cs = "ABC123"

  val t1 = DateTime.now
  Thread.sleep(10)
  val t2 = DateTime.now
  Thread.sleep(10)
  val t3 = DateTime.now

  val id = "123"

  val fpos1 = FlightState(id, cs, GeoPosition.undefinedPos, Speed
    .UndefinedSpeed,
    Angle.UndefinedAngle, Speed.UndefinedSpeed, t1, 0, "?", "?", DateTime.Date0,
    "?", DateTime.Date0)
  val fpos2 = FlightState(id, cs, GeoPosition.undefinedPos, Speed
    .UndefinedSpeed,
    Angle.UndefinedAngle, Speed.UndefinedSpeed, t2, 0, "?", "?", DateTime.Date0,
    "?", DateTime.Date0)
  val fpos3 = FlightState(id, cs, GeoPosition.undefinedPos, Speed
    .UndefinedSpeed,
    Angle.UndefinedAngle, Speed.UndefinedSpeed, t3, 0, "?", "?", DateTime.Date0,
    "?", DateTime.Date0)

  "AllFlightSfdpsOrderDautMonitor" must {
    "check if sfdps messages for a flight are received " +
      "in the right order" in {
      val monitor = new FlightMsgOrder_Monitor(emptyConfig)

      monitor.verify(fpos1)
      monitor.verify(fpos2)
      monitor.verify(fpos3)

      monitor.getErrorCount should be(0)
    }
  }

  "AllFlightSfdpsOrderDautMonitor" must {
    "report an error when sfdps messages for a flight are not " +
      "received in the right order" in {
      val monitor = new FlightMsgOrder_Monitor(emptyConfig)

      monitor.verify(fpos2)
      monitor.verify(fpos3)
      monitor.verify(fpos1)

      monitor.getErrorCount should be(1)
    }
  }

  val config: Config = ConfigFactory.parseString(
    s"""{
      cs = "$cs"
       }""")

  "SingleFlightMsgOrder_Monitor" must {
    "check if sfdps messages for a flight are received " +
      "in the right order" in {
      val monitor = new SingleFlightMsgOrder_Monitor(config)

      monitor.verify(fpos1)
      monitor.verify(fpos2)
      monitor.verify(fpos3)

      monitor.getErrorCount should be(0)
    }
  }

  "SingleFlightMsgOrder_Monitor" must {
    "report an error when sfdps messages for a flight are not " +
      "received in the right order" in {
      val monitor = new SingleFlightMsgOrder_Monitor(config)

      monitor.verify(fpos2)
      monitor.verify(fpos3)
      monitor.verify(fpos1)

      monitor.getErrorCount should be(1)
    }
  }
}
