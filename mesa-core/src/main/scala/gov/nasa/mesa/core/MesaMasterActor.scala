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

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.actor.ActorRef
import com.typesafe.config.{Config, ConfigException}
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.{MasterActor, RaceActorSystem,
  RaceInitializeException, error, info, warning}
import gov.nasa.race.util.ThreadUtils.waitInterruptibleUpTo

import scala.language.postfixOps

/** This class represents a master actor for a MesaActorSystem instance. The
  * master actor is a special actor built into the RACE infrastructure. It is
  * responsible to create the top-level actors defined in the configuration
  * file, and it guarantees synchronized execution of all lifetime phases for
  * each actor in order, from instantiation to termination.
  *
  * @param ras the actor system to which this master belongs to
  */
class MesaMasterActor(ras: RaceActorSystem) extends MasterActor(ras)  {

  /** Using the given Config object, generates an actor within this MESA
    * process.
    *
    * The reason for overriding this method from RACE is to be able to set the
    * actor mailbox to a non-default one.
    *
    * @param actorConfig the configuration for the actor to be generated
    * @return a Some[ActorRef] object including a new actor created with the
    *         given configuration.
    */
  override protected def instantiateLocalActor(actorConfig: Config)
  : Option[ActorRef] = {
    try {
      val actorName = actorConfig.getString("name")
      val clsName = actorConfig.getClassName("class")
      val actorCls = ras.classLoader.loadClass(clsName)
      val createActor = getActorCtor(actorCls, actorConfig)

      info(s"creating $actorName ..")
      val sync = new LinkedBlockingQueue[Either[Throwable,ActorRef]](1)
      val props = getActorProps(createActor,sync)

      // note this executes the constructor in another thread.
      val aref = context.actorOf(
        if(actorConfig.getBooleanOrElse("stats.mailbox", fallback = false))
          props.withMailbox("stats-collector-mailbox") else props,
        actorName)

      // using timeout to block and see if the actor is successfully created.
      val createTimeout = actorConfig.getFiniteDurationOrElse("create-timeout",
        ras.defaultActorTimeout)

      def _timedOut: Option[ActorRef] = {
        if (isOptionalActor(actorConfig)) {
          warning(s"optional actor construction timed out $actorName")
          None
        } else {
          error(s"non-optional actor construction timed out $actorName, " +
            s"escalating")
          throw new RaceInitializeException(s"failed to create actor " +
            s"$actorName")
        }
      }

      waitInterruptibleUpTo(createTimeout, _timedOut) { dur =>
        sync.poll(dur.toMillis, TimeUnit.MILLISECONDS) match {
          case null => _timedOut
          case Left(t) => // ctor bombed
            if (isOptionalActor(actorConfig)) {
              warning(s"constructor of optional actor $actorName caused " +
                s"exception $t")
              None
            } else {
              error(s"constructor of non-optional actor $actorName caused " +
                s"exception $t, escalating")
              throw new RaceInitializeException(s"failed to create actor " +
                s"$actorName")
            }
          case Right(ar) => // success, we could check actorRef equality here
            info(s"actor $actorName created")
            Some(aref)
        }
      }
    } catch { // those are exceptions that happened in this thread
      case cnfx: ClassNotFoundException =>
        if (isOptionalActor(actorConfig)) {
          warning(s"optional actor class not found: ${cnfx.getMessage}")
          None
        } else {
          error(s"unknown actor class: ${cnfx.getMessage}, escalating")
          throw new RaceInitializeException(s"no actor class: ${cnfx
            .getMessage}")
        }
      case cx: ConfigException =>
        error(s"invalid actor config: ${cx.getMessage}, escalating")
        throw new RaceInitializeException(s"missing actor config: ${cx
          .getMessage}")
    }
  }
}
