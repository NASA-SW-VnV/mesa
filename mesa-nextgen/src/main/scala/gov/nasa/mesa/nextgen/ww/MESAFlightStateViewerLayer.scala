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
package gov.nasa.mesa.nextgen.ww

import akka.actor.Actor.Receive
import com.typesafe.config.Config
import gov.nasa.mesa.nextgen.core._
import gov.nasa.race.core.BusEvent
import gov.nasa.race.track.TrackTerminationMessage
import gov.nasa.race.ww.RaceViewer
import gov.nasa.race.ww.air.SfdpsTracksLayer

/** This class represents a WorldWind layer that displays flights.
  *
  * @param raceViewer a viewer state facade implemented in RACE which is
  *                   executing in the UI thread
  * @param config the configuration
  */
class MESAFlightStateViewerLayer(override val raceViewer: RaceViewer,
                                 override val config: Config)
  extends SfdpsTracksLayer(raceViewer: RaceViewer, config: Config) {

  /** Handles the input trace events.
    *
    * @return a partial function with the handleSFDPSMessage logic
    */
  override def handleSFDPSMessage: Receive = {
    case BusEvent(_,
    FlightInfo(state@FlightState(_, _, _, _, _, _, _, _),
    FlightTrack(_, _, _, _, _, _, _)), _) =>
      handleTrack(state)
    case BusEvent(_, state@FlightState(_, _, _, _, _, _, _, _), _) =>
      handleTrack(state)
    case BusEvent(_, msg: FlightTrack, _) => // ignore
    case BusEvent(_, msg: StarChanged, _) => // ignore
    case BusEvent(_, msg: FlightCompleted, _) => // ignore
    case BusEvent(_, msg: TrackTerminationMessage, _) =>
      handleTermination(msg)
  }
}
