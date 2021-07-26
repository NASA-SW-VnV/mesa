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
import com.typesafe.config.{Config, ConfigValueFactory}
import gov.nasa.mesa.core.MesaMonitor
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler

import scala.collection.mutable

/** Dispatchers in MESA are actors that are used to specify how the monitoring
  * step of the runtime verification is distributed.
  *
  * This trait implements a core functionality for a dispatcher referred to as
  * indexing dispatcher which is used to create monitor objects on-the-fly. It
  * supports indexing at the dispatcher level by storing monitor instances in a
  * hash map. It slices the trace up into sub-traces according to selected data
  * in the trace events, and feeds each resulting sub-trace to the associated
  * monitor object.
  *
  * The type for the monitors created by the dispatcher is specified in the
  * dispatcher actor's config using the key "monitor".
  *
  * To use this functionality, one needs to extend the trait with the
  * handleMessage method implementation which is a partial function that
  * specifies how the actor handles incoming messages.
  */
trait IndexingDispatcher extends DispatcherActor {

  /** A map from unique ids (slicing criteria) to monitor objects generated
    * on-the-fly.
    */
  val monitors: mutable.Map[Any, MesaMonitor] = mutable.Map[Any, MesaMonitor]()

  /** Adds a monitor and its associated id to the map.
    *
    * @param id an id used as a slicing criteria
    * @param monitor a mesa monitor object
    */
  def addMonitor(id: Any, monitor: MesaMonitor): Unit = {
    monitors += (id -> monitor)
  }

  /** Removes the entry with given key from the map
    *
    * @param id an id associated to a mesa monitor
    * @return a mesa monitor with the given id removed from the map.
    */
  def removeMonitor(id: Any): MesaMonitor = {
    monitors.remove(id).get
  }

  /** Instantiates a new monitor by invoking 'instantiateMonitor' and adds it
    * to the map
    *
    * @param config the configuration
    * @param id an id associated to the monitor, used as a slicing criteria
    *           as well
    * @return a new instance of a MesaMonitor
    */
  def createMonitor(config: Config, id: Any): MesaMonitor = {
    val actorName = s"${config.getString("name")}-${
      GlobalStatsProfiler
        .incMonitorCounter
    }"
    // A configuration based on the given config, but the "name" is set to
    // "actorName"
    val actorConf = config.withValue("name",
      ConfigValueFactory.fromAnyRef(actorName))
    info(s"instantiating monitor object $actorName")
    val monitor = instantiateMonitor(actorConf)
    addMonitor(id, monitor)
    monitor
  }

  /** Instantiates a new monitor within the actor system
    *
    * @param config the configuration
    * @return a new instance of a MesaMonitor
    */
  def instantiateMonitor(config: Config): MesaMonitor = {
    val monitor = newInstance[MesaMonitor](config.getString("monitor.class"),
      Array(classOf[Config]), Array(config)).get
    info(s"instantiated monitor $name")
    monitor
  }

  /** Called when terminating this dispatcher actor.
    *
    * @param originator the actor sending the termination request
    * @return true if the termination is successful, otherwise returns false.
    */
  override def onTerminateRaceActor(originator: ActorRef): Boolean = {
    super.onTerminateRaceActor(originator)
  }
}
