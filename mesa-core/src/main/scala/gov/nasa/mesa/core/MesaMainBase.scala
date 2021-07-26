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

import java.io.File

import akka.actor.{Actor, ActorRef}
import com.typesafe.config.Config
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler
import gov.nasa.race.core.RaceTerminateRequest
import gov.nasa.race.core.RaceActorSystem
import gov.nasa.race.main.ConsoleMainBase
import gov.nasa.race.util.{ConsoleIO, ThreadUtils}

import scala.collection.Seq

/** This is a singleton object that provides an entry point to MESA execution.
  */
object MesaMain extends ConsoleMainBase {

  /** Overriding this so it instantiates MesaActorSystem for RaceActorSystem
    * instances.
    *
    * @param configFiles the actor system(s) configuration file(s)
    * @param logLevel the log level which specifies the level information about
    *                 the actor system execution printed on the console. It
    *                 can be set to "info", "warning", or "off"
    * @return a list of MESA actor system(s) corresponding to the list of
    *         configuration file(s).
    */
  override def instantiateRaceActorSystems(configFiles: Seq[File],
                                           logLevel: Option[String])
  : Seq[RaceActorSystem] = {
    try {
      getUniverseConfigs(configFiles, logLevel).map(new MesaActorSystem(_))
    } catch {
      case t: Throwable =>
        ConsoleIO.printlnErr(t.getMessage)
        Seq.empty[MesaActorSystem]
    }
  }
}

/** An instance of this class represents an Akka actor system which is extended
  * by RACE.
  *
  * Inherited from RACE, The MESA actor system includes one master actor that
  * oversees all other actors in the system.
  *
  * @param config the actor system configuration
  */
class MesaActorSystem(override val config: Config) extends
  RaceActorSystem(config: Config) {

  // initializing Profiler which keeps tracks of number of messages, and
  // monitors.
  GlobalStatsProfiler.initialize(config, () => {
    master ! RaceTerminateRequest
  })

  /** Returns the type for the master actor.
    *
    * @return an instance of Class presenting the type MesaMasterActor.
    */
  override def getMasterClass: Class[_ <: Actor] = classOf[MesaMasterActor]

  /** Sends a request to terminate the entire actor system. Note that Unlike
    * RACE, in MESA, any actor can send termination request to the master actor.
    *
    * @param actorRef the reference of an actor that is sending the
    *                 termination request
    */
  override def requestTermination(actorRef: ActorRef): Unit = {
    if (!isTerminating) // avoid recursive termination
      if (allowSelfTermination ||
        (allowRemoteTermination && isRemoteActor(actorRef))) {
        // make sure we don't hang if this is called from a receive method
        ThreadUtils.execAsync(terminateActors)
      }
  }

  /** Terminates all the actors in the actor system. First, it tries graceful
    * shutdown, and if that fails, if enforces shutdown by terminating the
    * currently running JVM.
    *
    * @return always true indicating a successful shutdown, since it uses
    *         forced shutdown.
    */
  override def terminateActors: Boolean = {
    val status = super.terminateActors
    // force shutdown
    if (!status) System.exit(1)
    true
  }
}
