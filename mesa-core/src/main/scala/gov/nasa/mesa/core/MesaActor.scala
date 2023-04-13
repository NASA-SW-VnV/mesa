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

import akka.actor.{ActorRef, Props}
import com.typesafe.config.Config
import gov.nasa.mesa.reporting.stats.{InstrumentedReceive, MsgProcessingStats}
import gov.nasa.race.common.Status.Initialized
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.{RaceActorInitializeFailed,
  RaceActorInitialized}
import gov.nasa.race.core.{PublishingRaceActor, RaceActor, RaceContext,
  SubscribingRaceActor}

/** This trait represents an abstract base type for all MESA specific actors.
  *
  * Its subtypes, 'SubscribingRaceActor and 'PublishingRaceActor' are RACE
  * actors that can subscribe and publish to the event bus of this actor
  * system, respectively.
  */
trait MesaActor extends SubscribingRaceActor with PublishingRaceActor {

  /** Implements the main behavior of the actor and its responses to incoming
    * messages.
    *
    * @return a partial function with the actor logic.
    */
  def handleMessage: Receive

  /** Called when terminating this actor.
    *
    * @param originator the actor sending the termination request
    * @return true if the termination is successful, otherwise returns false.
    */
  override def onTerminateRaceActor(originator: ActorRef): Boolean = {
    super.onTerminateRaceActor(originator)
  }

  /** Sets the message handler to 'receiveLive', or 'InstrumentedReceive' if
    * the stats.service key for actor in config file is set to true.
    *
    * @param rctx the actor system data including the master reference and
    *             the communication bus
    * @param actorConf the actor configuration
    */
  override def handleInitializeRaceActor(rctx: RaceContext,
                                         actorConf: Config): Unit = {
    info("got InitializeRaceActor")
    try {
      raceContext = rctx

      if (actorConf.getBooleanOrElse("stats.service", fallback = false))
        context.become(new InstrumentedReceive(receiveLive, this))
      else
        context.become(receiveLive)

      if (onInitializeRaceActor(rctx, actorConf)) {
        info("initialized")
        status = Initialized
        sender ! RaceActorInitialized(capabilities)
      } else {
        warning("initialize rejected")
        sender ! RaceActorInitializeFailed("rejected")
      }
    } catch {
      case ex: Throwable => sender ! RaceActorInitializeFailed(ex.getMessage)
    }
  }

  /** In the case that 'stats.service' is set to true, this method is used to
    * forward the stats collected about the actor to the event bus of this
    * actor system.
    *
    * @param stats the statistics collected about the service time of a message
    */
  def forwardStats(stats: MsgProcessingStats): Unit = {
    context.system.eventStream.publish(stats)
  }

  /** Generates a new actor and returns the reference to the actor.
    *
    * @param actorName the name of the actor to be generated
    * @param actorConfig the actor configuration
    * @return a reference to a new actor.
    */
  override def instantiateActor(actorName: String, actorConfig: Config)
  : ActorRef = {
    val clsName = actorConfig.getClassName("class")
    val actorCls = loadClass(clsName, classOf[RaceActor])
    try {
      actorCls.getConstructor(classOf[Config])
      getActorRef(Props(actorCls, actorConfig), actorName, actorConfig)
    } catch {
      case _: java.lang.NoSuchMethodException =>
        actorCls.getConstructor()
        getActorRef(Props(actorCls), actorName, actorConfig)
    }
  }

  /** Instantiates an actor with the given name and configuration. If the key
    * 'stats.mailbox' in the 'actorConfig' is set to true, then instead of
    * the Akka default mailbox implementation, the actor mailbox is set to
    * 'StatsMailboxType'.
    *
    * @param props a thread-safe configuration object
    * @param actorName the name of the actor to be generated
    * @param actorConfig the actor configuration
    * @return a reference to the new actor.
    */
  def getActorRef(props: Props, actorName: String, actorConfig: Config)
  : ActorRef = {
    context.actorOf(if (actorConfig.getBooleanOrElse("stats.mailbox",
      fallback = false))
      props.withMailbox("stats-collector-mailbox") else props,
      actorName)
  }
}
