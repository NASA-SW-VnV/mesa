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
package gov.nasa.mesa.monitoring.dispatchers

import akka.actor.ActorRef
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigValueFactory}
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core._

import scala.collection.mutable

/** Dispatchers in MESA are actors that are used to specify how the monitoring
  * step of the runtime verification is distributed.
  *
  * This trait implements a core functionality for a dispatcher, referred to as
  * concurrent dispatcher, which is used to create monitor actors on-the-fly. It
  * supports indexing at the dispatcher level by storing references of monitor
  * actors in a hash map. It slices the trace up into sub-traces according to
  * selected data in the trace events, and feeds each resulting sub-trace to
  * the associated monitor actor.
  *
  * This dispatcher can be used in two settings:
  * 1) Unbounded - in the unbounded setting, one monitor actor is generated for
  * each value of the selected data used as the slicing criteria.
  * 2) Bounded - in the bounded setting, the number of monitor actors are
  * limited to a number specified in the dispatcher's config (using the key
  * "actor-monitor-count"). Therefore, one monitor actor is associated with a
  * set of values of the parameter used as the slicing criteria instead of one
  * value.
  *
  * The type for the monitors created by the dispatcher is specified in the
  * dispatcher actor's config using the key "monitor".
  *
  * To use this functionality, one needs to extend the trait with the
  * handleMessage method implementation which is a partial function that
  * specifies how the actor handles incoming messages.
  */
trait ConcurrentDispatcher extends DispatcherActor {

  val monitorConfig: Config

  /** Map from unique ids to monitor actors */
  val monitorActors: mutable.Map[Any, ActorRef] = mutable.Map[Any, ActorRef]()

  /** maximum number of monitor actors, set for the bounded setting */
  val monitorCount: Int = config.getIntOrElse("actor-monitor-count", -1)

  /** Returns true in the bounded setting
    *
    * @return true for the bounded setting and false for unbounded.
    */
  def isBounded: Boolean = monitorCount > 0

  /** Used to keep track of the current group for the bounded setting */
  var groupId: Int = 0

  /** groups, which is used in the bounded setting, is a map from group id to
    * actor reference.
    */
  val groups: mutable.Map[Int, ActorRef] = mutable.Map[Int, ActorRef]()

  /** It checks if the number of monitors created reached the specified limit
    * in the bounded setting.
    *
    * @return true if the number of monitors created reached the specified
    *         limit otherwise returns false.
    */
  def boundReached: Boolean = groups.size == monitorCount

  /** Records the actor reference and its corresponding group.
    *
    * @param actorRef the actor reference
    */
  def addToGroup(actorRef: ActorRef): Unit = {
    groups += (groupId -> actorRef)
  }

  /** Updates the current group id to the next group.
    *
    * @return the current group id.
    */
  def incGroupId: Int = {
    groupId = groupId + 1
    if (groupId == monitorCount) groupId = 0
    groupId
  }

  /** Records the id and the corresponding monitor actor that is monitoring
    * the events with the given id
    *
    * Note that in the case of the bounded setting multiple id could be assigned
    * to the same monitor actor. However, in the unbounded setting, there is one
    * monitor actor per id.
    *
    * @param id the actor id
    * @param actorRef the monitor actor reference
    */
  def addMonitorActor(id: Any, actorRef: ActorRef): Unit = {
    monitorActors += (id -> actorRef)
  }

  /** Removes the monitor actor associated to the given id from the
    * 'monitorActors' map.
    *
    * Note that in the case of the bounded setting multiple id could be assigned
    * to the same monitor actor. However, in the unbounded setting, there is one
    * monitor actor per id.
    *
    * @param id the id associated to the monitor
    * @return the reference to the actor removed from the map.
    */
  def removeMonitorActor(id: Any): ActorRef = {
    monitorActors.remove(id).get
  }

  /** Terminates the monitor actor with the given reference dispatched from
    * this dispatcher. It sends a termination request to the monitor actor
    * (without checking the status of the termination) and then removes the
    * monitor actor from the 'monitorActors' map.
    *
    * @param actorRef the reference to a monitor actor
    */
  def terminateMonitorActor(actorRef: ActorRef): Unit = {
    val id = monitorActors.find(_._2 == actorRef).get._1
    askMonitorActor(actorRef, TerminateRaceActor(self), RaceActorTerminated())
    removeMonitorActor(id)
  }

  /** Creates an instance of the specified monitor actor.
    *
    * @param config the actor configuration
    * @param id the id associated to the monitor actor
    * @return the reference to the new monitor actor.
    */
  def createMonitorActor(config: Config, id: Any): ActorRef = {
    var actorRef: ActorRef = null
    // if the maximum number of monitors reached, new ids are added to the
    // existing groups of monitors.
    if (isBounded && boundReached) {
      actorRef = groups(incGroupId)
    } else {
      val actorName = s"${config.getString("name")}-${GlobalStatsProfiler
        .incMonitorCounter}"
      val actorConf = config.withValue("name", ConfigValueFactory.fromAnyRef
      (actorName))
      actorRef = instantiateMonitorActor(actorName, actorConf)
      if (isBounded) {
        incGroupId
        addToGroup(actorRef)
      }
    }
    addMonitorActor(id, actorRef)
    actorRef
  }

  /** Instantiates a monitor actor with the given name and configuration.
    *
    * @param name the name of the monitor actor
    * @param config the actor configuration
    * @return the reference to the new monitor actor.
    */
  def instantiateMonitorActor(name: String, config: Config): ActorRef = {
    info(s"instantiating monitor actor $name")
    val actorRef = instantiateActor(name, config)
    askMonitorActor(actorRef, InitializeRaceActor(raceContext, config),
      RaceActorInitialized(capabilities))
    askMonitorActor(actorRef, StartRaceActor(self), RaceActorStarted())
    actorRef
  }

  /** Called when terminating this dispatcher actor.
    *
    * @param originator the actor sending the termination request
    * @return true if the termination is successful, otherwise returns false.
    */
  override def onTerminateRaceActor(originator: ActorRef) = {
    if (terminateMonitorActors) {
      super.onTerminateRaceActor(originator)
    } else false
  }

  /** Sends a termination request to all monitors actor dispatched by this
    * dispatcher.
    *
    * @return true if all monitor actors acknowledge the successful termination
    *         by sending back the reply 'RaceActorTerminated, otherwise returns
    *         false.
    */
  def terminateMonitorActors: Boolean =
    askMonitorActors(TerminateRaceActor(self), RaceActorTerminated())

  /** Sends the message 'question' to the specified monitor actor and waits to
    * receive the message 'answer'.
    *
    * @param actorRef the reference to monitor actor
    * @param question the message sent to the monitor actor
    * @param answer the message that the monitor actor is expected to send
    *               back
    * @return true if the expected message, 'answer', is received from the
    *         monitor actor, and false if times out.
    */
  def askMonitorActor(actorRef: ActorRef, question: Any, answer: Any)
  : Boolean = {
    askForResult(actorRef ? question) {
      case `answer` => true
      case TimedOut => {
        warning(s"dependent actor timed out: ${actorRef.path.name}");
        false
      }
    }
  }

  /** Sends the 'question' to all the monitor actors and expects to receive the
    * 'answer' from every monitor actor, otherwise it returns false.
    *
    * @param question the message sent to all monitor actors
    * @param answer the message that all monitor actors are expected to send
    *               back
    * @return true if the expected message, 'answer', is received from all
    *         the monitor actors, and false if one or more monitor times out.
    */
  def askMonitorActors(question: Any, answer: Any): Boolean = {
    var results = true
    for (m <- monitorActors.iterator)
      if (!askMonitorActor(m._2, question, answer)) results = false
    results
  }
}
