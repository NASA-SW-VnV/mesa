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
package gov.nasa.mesa.nextgen.dataProcessing

import akka.actor.ActorRef
import com.typesafe.config.Config
import gov.nasa.mesa.core.MesaActor
import gov.nasa.mesa.nextgen.core.{FlightState, FlightStateCompleted}
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler
import gov.nasa.race.core.BusEvent

import scala.collection.immutable.List

/** This class represents a MESA actor which simply counts the number of
  * different call signs in the incoming trace composed of FlightState and
  * FlightStateCompleted objects.
  *
  * @param config the actor configuration
  */
class SfdpsCSCounterActor(val config: Config) extends MesaActor {

  var count = List.empty[String]
  val printFrequency = 200

  /** Receives FlightState and FlightStateCompleted messages and counts the
    * number of different call sign, stored in 'count'.
    *
    * @return a partial function with the Dynamic_RNAV_STAR_TraceGenerator
    *         actor logic.
    */
  override def handleMessage: Receive = {
    case e@BusEvent(_, FlightState(_, cs, _, _, _, _, _, _), _) =>
      if (!GlobalStatsProfiler.processedAllMsgs) {
        GlobalStatsProfiler.incMsgCounter
        if (!count.contains(cs)) {
          count = cs :: count
          if (count.size % printFrequency == 0)
            println(s"cs count: ${count.size}")
        }
      }
    case e@BusEvent(_, FlightStateCompleted(_, cs, _, _, _, _, _, _), _) =>
      if (!GlobalStatsProfiler.processedAllMsgs) {
        GlobalStatsProfiler.incMsgCounter
      }
  }

  /** Called when terminating this actor.
    *
    * @param originator the actor sending the termination request
    * @return true if the termination is successful, otherwise returns false.
    */
  override def onTerminateRaceActor(originator: ActorRef): Boolean = {
    println(s"number of call signs: ${count.size}")
    super.onTerminateRaceActor(originator)
  }
}