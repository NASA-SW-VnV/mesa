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
package gov.nasa.mesa.nextgen.verification.monitors.daut

import com.typesafe.config.Config
import gov.nasa.mesa.core.DautMonitor
import gov.nasa.mesa.nextgen.core.{FlightState, FlightStateCompleted}
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler

/** This class represents a Daut monitor that captures a property that checks
  * if messages for the flight with the call sign 'cs' are received in a
  * chronological order.
  *
  * @param config the configuration
  */
class SingleFlightMsgOrder_Monitor(config: Config) extends DautMonitor(config){

  val cs = config.getString("cs")

  always {
    case FlightState(_, `cs`, _, _, _, _, date1, _) =>
      watch {
      case FlightState(_, `cs`, _, _, _, _, date2, _) => {
        date2.isAfter(date1)
      }
    }
  }
}

/** This class maintains a list of SingleFlightMsgOrder_Monitor instances as its
  * sub-monitors.
  *
  * It uses a feature of Daut that allows for defining sub-monitors within
  * one monitor object. This is meant to be used in our empirical studies to
  * mitigate issues associated with micro-benchmarking.
  *
  * The number of sub-monitors is specified in the monitor config using the key
  * "sub-monitor-count".
  *
  * @param config the configuration
  */
class SingleFlightMsgOrder_MultiMonitor(config: Config) extends DautMonitor(config) {
  val size = config.getInt("sub-monitor-count") - 1
  val cs = config.getString("cs");

  val m = for(i <- 0 to size) yield new SingleFlightMsgOrder_Monitor(config)
  monitor(m: _*)

  /** This method overrides the method daut.Monitor.verifyBeforeEvent which is
    * always invoked before invoking 'verify' on the message.
    *
    * @param msg a message
    */
  override def verifyAfterEvent(msg: Any): Unit = {
    msg match {
      case FlightState(_, `cs`, _, _, _, _, _, _) => GlobalStatsProfiler.incMsgCounter
      case FlightStateCompleted(_,`cs`,_,_,_,_,_,_) => {
        GlobalStatsProfiler.incMsgCounter
      }
      case _ => // ignore
    }
  }
}

