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
package gov.nasa.mesa.nextgen.verification.dispatchers

import com.typesafe.config.Config
import gov.nasa.mesa.monitoring.actors.MonitorActor
import gov.nasa.mesa.nextgen.core.{FlightState, FlightStateCompleted}
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler
import gov.nasa.race.core.BusEvent

/** This class represents a MESA monitor actor which uses one monitor object
  * for all the flights in the input event trace. In a way, the monitoring step
  * of this configuration is equivalent to directly using the Daut (or
  * TraceContract) tool to process the trace sequentially.
  *
  * @param config the actor configuration
  */
class FlightMonitorIndexing(config: Config) extends MonitorActor(config) {

  GlobalStatsProfiler.incMonitorCounter

  /** Implements a MESA monitor actor for flight messages. It also updates
    * the GlobalStatsProfiler object as it processes the input trace.
    *
    * @return a partial function.
    */
  override def handleMessage: Receive = {
    case BusEvent(_, msg@FlightState(_, cs, _, _, _, _, _, _), _) =>
      if (!GlobalStatsProfiler.dispatchedAllMsgs) {
        GlobalStatsProfiler.incDispatchedMsgCount
        monitor.verifyEvent(msg)
        // publish(msg)
      }
    case BusEvent(_, msg@FlightStateCompleted(_, cs, _, _, _, _, _, _), _) =>
      if (!GlobalStatsProfiler.dispatchedAllMsgs) {
        GlobalStatsProfiler.incDispatchedMsgCount
        monitor.verifyEvent(msg)
        // publish(msg)
      }
  }
}