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
import gov.nasa.mesa.monitoring.dispatchers.IndexingDispatcher
import gov.nasa.mesa.nextgen.core.{FlightState, FlightStateCompleted}
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler
import gov.nasa.race.core.BusEvent

/** This class extends the indexing dispatcher towards the NextGen applications
  * It is a dispatcher actor that creates monitor objects on-the-fly. For each
  * flight call sign, creates a monitor object, and maintains a map from unique
  * call signs to monitor objects.
  *
  * It uses flight call signs as slicing criteria. For each received FlightState
  * message, it invokes verifyEvent on the associated monitor object with the
  * same call sign as the one stored in the message. For each received
  * FlightCompleted message, it removes the corresponding
  * monitor object from the map.
  *
  * @param config the dispatcher actor configuration
  */
class FlightIndexingDispatcher(val config: Config) extends IndexingDispatcher {

  /** A config object for the monitors */
  val monitorConfig: Config = config.getConfig("monitor")

  /** Implements the indexing dispatcher actor for flight messages. It creates
    * monitor objects on-the-fly, and it supports indexing at the dispatcher
    * level by storing monitor instances in a hash map. It also updates the
    * GlobalStatsProfiler object as it processes the input trace.
    *
    * @return a partial function with the FlightIndexingDispatcher logic.
    */
  override def handleMessage: Receive = {
    case BusEvent(_, state@FlightState(_, cs, _, _, _, _, _, _), _) =>
      // Process the message, only if the termination condition has not been
      // reached.
      if (!GlobalStatsProfiler.dispatchedAllMsgs) {
        GlobalStatsProfiler.incDispatchedMsgCount
        if (!monitors.contains(cs)) createMonitor(monitorConfig, cs)
        monitors(cs).verifyEvent(state)
      }
    case BusEvent(_, t@FlightStateCompleted(_, cs, _, _, _, _, _, _), _) =>
      if (!GlobalStatsProfiler.dispatchedAllMsgs) {
        GlobalStatsProfiler.incDispatchedMsgCount
        if (monitors.contains(cs)) {
          val monitor = removeMonitor(cs)
          monitor.verifyEvent(t)
        }
        // dumping the msg
        else GlobalStatsProfiler.incMsgCounter
      }
  }
}
