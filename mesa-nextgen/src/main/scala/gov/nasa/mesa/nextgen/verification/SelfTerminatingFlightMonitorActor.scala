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
package gov.nasa.mesa.nextgen.verification

import com.typesafe.config.Config
import gov.nasa.mesa.monitoring.actors.MonitorActor
import gov.nasa.mesa.monitoring.dispatchers.TerminateMonitorChildActor
import gov.nasa.mesa.nextgen.core.FlightStateCompleted
import gov.nasa.race.core.BusEvent

/** A monitor actor that terminates itself upon receiving the TrackCompleted
  * message.
  *
  * TODO: turn this into a generic monitor actor that terminates upon receiving
  * a specified message.
  *
  * @param config the monitor actor configuration
  */
class SelfTerminatingFlightMonitorActor(override val config: Config)
  extends MonitorActor(config) {

  /** Implements the main behavior of SelfTerminatingFlightMonitorActor.
    *
    * @return a partial function
    */
  override def handleMessage: Receive = {
    case BusEvent(_, msg, _) =>
      monitor.verifyEvent(msg)
      publish(msg)
      checkForTermination(msg)
  }

  /** Checks if the given message is of type of FlightStateCompleted, and if
    * so it terminates itself by having the parent actor (master) send it
    * the TerminateMonitorChildActor message.
    *
    * @param msg a message
    */
  def checkForTermination(msg: Any): Unit = {
    msg match {
      case msg: FlightStateCompleted =>
        context.parent ! TerminateMonitorChildActor(self)
      case _ => //ignore
    }
  }
}
