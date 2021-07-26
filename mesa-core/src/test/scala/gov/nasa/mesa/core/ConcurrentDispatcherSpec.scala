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

import akka.actor.ActorRef
import akka.event.Logging
import com.typesafe.config.Config
import gov.nasa.mesa.monitoring.dispatchers.ConcurrentDispatcher
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.uom.DateTime
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * A test suite for gov.nasa.mesa.monitoring.dispatchers.ConcurrentDispatcher
  */
class ConcurrentDispatcherSpec extends RaceActorSpec with AnyWordSpecLike {

  val conf: Config = createConfig(
    """
      |name = "monitor"
      |class = "gov.nasa.mesa.core.MonitorActorTest"
      |monitor.class = "gov.nasa.mesa.core.TestMonitor"
    """.stripMargin)

  val id = "id"

  "ConcurrentDispatcher" must {
    "receive Reply from monitor actors" in {
      runRaceActorSystem(Logging.WarningLevel) {
        val dispatcherObj = addTestActor[TestActorDispatcher]("dispatcher",
          conf)

        printTestActors
        initializeTestActors
        startTestActors(DateTime.now)

        val actorRef = dispatcherObj.createMonitorActor(conf, id)
        assert(actorRef.isInstanceOf[ActorRef])

        val result = dispatcherObj.askMonitorActor(actorRef, Question, Reply)
        assert(result)

        terminateTestActors
      }
    }
  }

  "ConcurrentDispatcher" must {
    "create monitor actors" in {
      runRaceActorSystem(Logging.WarningLevel) {
        val dispatcherObj = addTestActor[TestActorDispatcher]("dispatcher",
          conf)

        printTestActors
        initializeTestActors
        startTestActors(DateTime.now)

        val result = expectResponse(dispatcherObj.self, CreateMonitor(conf, id),
          12 seconds) {
          case actorRef: ActorRef => actorRef
          case _ => None
        }

        assert(result.isInstanceOf[ActorRef])
        assertResult(result)(dispatcherObj.monitorActors(id))

        terminateTestActors
      }
    }
  }

  "ConcurrentDispatcher" must {
    "terminate monitor actors" in {
      runRaceActorSystem(Logging.WarningLevel) {
        val dispatcherObj = addTestActor[TestActorDispatcher]("dispatcher",
          conf)
        val dispatcherRef = dispatcherObj.self

        printTestActors
        initializeTestActors
        startTestActors(DateTime.now)

        val actorRef = expectResponse(dispatcherRef, CreateMonitor(conf, id),
          12 seconds) {
          case actorRef: ActorRef => actorRef
          case _ => None
        }

        assert(dispatcherObj.monitorActors.contains(id))

        val result = expectResponse(dispatcherRef, TerminateMonitor(actorRef
          .asInstanceOf[ActorRef]), 12 seconds) {
          case Reply => true
          case _ => false
        }

        assert(result == true)
        assert(!dispatcherObj.monitorActors.contains(id))

        terminateTestActors
      }
    }
  }
}

case class Question()

case class Reply()

case class CreateMonitor(conf: Config, id: String)

case class TerminateMonitor(actorRef: ActorRef)

class TestActorDispatcher(val config: Config) extends ConcurrentDispatcher {

  override val monitorConfig: Config = null

  override def handleMessage: Receive = {
    case CreateMonitor(conf, id) =>
      val actorRef = createMonitorActor(conf, id)
      sender ! actorRef
    case TerminateMonitor(actorRef: ActorRef) =>
      terminateMonitorActor(actorRef)
      sender ! Reply
  }
}

class MonitorActorTest(val config: Config) extends MesaActor {
  override def handleMessage: Receive = {
    case Question => sender ! Reply
  }
}