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
package gov.nasa.mesa.core

import akka.event.Logging
import com.typesafe.config.Config
import gov.nasa.mesa.core.MesaActor
import gov.nasa.mesa.reporting.stats.MsgProcessingStats
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.uom.DateTime
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * A test suite for gov.nasa.mesa.reporting.stats.InstrumentedReceive
  */
class StatsCollectorMonitorActorSpec extends RaceActorSpec
  with AnyWordSpecLike {

  val conf: Config = createConfig(
    """
      |name = "service-stats-actor"
      |class = "gov.nasa.mesa.core.TestServiceStatsActor"
      |stats {
      |  service = true
      |}
    """.stripMargin)

  "StatsCollectorActorReceive" must {
    "measure the service time for each message processed by the actor" in {
      runRaceActorSystem(Logging.WarningLevel) {
        val actor = addTestActor[TestServiceStatsActor]("service-stats-actor",
          conf)
        val actorRef = actor.self

        val statCollector = addTestActor[MesaTestProbe]("stat-collector",
          createConfig(""))
        val statCollectorRef = statCollector.self
        system.eventStream.subscribe(statCollectorRef,
          classOf[MsgProcessingStats])

        printTestActors
        initializeTestActors
        startTestActors(DateTime.now)

        // sending the TestServiceStatsActor a message
        actorRef ! TestMessage

        terminateTestActors
      }
    }
  }
}

class MesaTestProbe(val config: Config) extends MesaActor {
  override def handleMessage: Receive = {
    case MsgProcessingStats(_, entry, exit) => {
      assert(exit - entry >= 800)
      println(s"TestProbe received - elapsed time: ${exit - entry}")
    }
  }
}

case class TestMessage()

class TestServiceStatsActor(val config: Config) extends MesaActor {
  override def handleMessage: Receive = {
    case TestMessage => Thread.sleep(800)
  }
}
