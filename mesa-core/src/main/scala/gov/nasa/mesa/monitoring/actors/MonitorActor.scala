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
package gov.nasa.mesa.monitoring.actors

import com.typesafe.config.Config
import gov.nasa.mesa.core.{MesaActor, MesaMonitor}
import gov.nasa.race.core.BusEvent

/** This is one of the key classes in MESA and it represents monitoring actors.
  *
  * It has a field of type MesaMonitor which is set to a monitor object during
  * the actor initialization. The concrete type for the underlying monitor
  * object is specified in the given config object.
  *
  * For each incoming message, this actor invokes the method verifyEvent on its
  * underlying monitor object to verify the event against the properties
  * captured by the monitor.
  *
  * @param config the actor configuration
  */
class MonitorActor(val config: Config) extends MesaActor {

  /** Stores the underlying monitor object. */
  val monitor: MesaMonitor = createMonitor(config)

  /** Creates an instance of a monitor using a concrete type specified in the
    * given config object (by "monitor.class")
    *
    * @param config the configuration
    * @return a new instance of MesaMonitor.
    */
  def createMonitor (config: Config): MesaMonitor = {
    val monitor = newInstance[MesaMonitor](
      config.getString("monitor.class"), Array(classOf[Config]), Array(config)).get
    info(s"instantiated monitor $name")
    monitor
  }

  /** Invokes the verifyEvent on the underlying monitor object which checks
    * the property against the incoming trace.
    *
    * @return a partial function with the actor logic.
    */
  override def handleMessage: Receive = {
    case BusEvent(_,msg,_) =>
      monitor.verifyEvent(msg)
      publish(msg)
  }
}