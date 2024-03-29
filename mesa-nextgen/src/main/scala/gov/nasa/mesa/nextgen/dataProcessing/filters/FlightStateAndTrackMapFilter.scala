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
package gov.nasa.mesa.nextgen.dataProcessing.filters

import com.typesafe.config.Config
import gov.nasa.mesa.nextgen.core.{FlightState, ExtendedFlightState}
import gov.nasa.race.config.ConfigurableFilter

import scala.collection.mutable.HashSet

/** This class represents a filter object used to filter FlightState objects
  * by mapping them into to the list of received ExtendedFlightState objects. It
  * maintains a list of flight call signs that their track has been received,
  * and filters out any FlightState whose call sign does not match the call
  * sign of any ExtendedFlightState object in the list.
  *
  * @param config the actor configuration
  */
class FlightStateAndTrackMapFilter(val config: Config)
  extends ConfigurableFilter {

  val csList = new HashSet[String]

  /** Filters out those FlightStates whose track info has not been received
    * so far.
    *
    * @param o a message
    * @return true if the message is of type of FlightState and its ExtendedFlightState
    *         objects has been received, otherwise, returns false.
    */
  override def pass(o: Any): Boolean = {
    o match {
      case extendedState: ExtendedFlightState =>
        if (csList.add(extendedState.cs)) {
        }
        false
      case state: FlightState =>
        if (csList.contains(state.cs)) {
          true
        } else false
      case other => false
    }
  }
}
