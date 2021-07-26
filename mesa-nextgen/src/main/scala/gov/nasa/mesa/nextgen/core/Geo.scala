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
package gov.nasa.mesa.nextgen.core

import gov.nasa.race.geo.Datum
import gov.nasa.race.uom.Area.√
import gov.nasa.race.uom.Length.Meters
import gov.nasa.race.uom.{Angle, Length}

import scala.collection.mutable.ListBuffer

/** Supports geospatial computation.
  */
object Geo {

  /** Returns a list of the waypoints within the given lateral proximity.
    *
    * @param state an event that captures the state of the flight
    * @param proximity a lateral proximity around the flight position
    * @param waypoints a list of waypoints
    * @return a list of waypoints from the given list which are within the
    *         given proximity from the flight position.
    */
  def getWaypointsInProximity(state: FlightState, proximity: Length,
                            waypoints: Seq[Waypoint]): ListBuffer[Waypoint] = {

    var proximitylist = new ListBuffer[Waypoint]()

    for (w <- waypoints)
      if (isInProximity(state, w, proximity)) proximitylist += w

    proximitylist
  }

  /** Returns the closest waypoint, from the given list of waypoints, which in
    * the proximity of the given position.
    *
    * @param state an event that captures the state of the flight
    * @param proximity a lateral proximity around the flight position
    * @param waypoints a list of waypoints
    * @return Some[Waypoint] capturing a waypoint, from the given list of
    *         waypoints and within the given lateral proximity of the flight,
    *         which is closest to the flight position, or None if there is no
    *         waypoint within the given proximity.
    */
  def getWaypointInProximity(state: FlightState, proximity: Length,
                             waypoints: Seq[Waypoint]): Option[Waypoint] = {

    var minDev = Meters(Integer.MAX_VALUE)
    var minDevWaypoint: Option[Waypoint] = None

    for (w <- waypoints) {
      val distance = getEuclideanDistance(state.position.φ, state.position.λ,
        Meters(0), w.position.φ, w.position.λ, Meters(0))

      if (distance <= proximity && distance < minDev) {
        minDevWaypoint = Some(w)
      }
    }

    minDevWaypoint
  }

  /** Checks if the given waypoint is in the given lateral proximity of the
    * specified flight position.
    *
    * @param state an event that captures the state of the flight
    * @param wp a waypoint
    * @param proximity a lateral proximity around the flight position
    * @return true if the given waypoint is within the given lateral proximity
    *         of the flight position, otherwise, returns false.
    */
  def isInProximity(state: FlightState, wp: Waypoint, proximity: Length)
  : Boolean = {
    val alt1 = Meters(0)
    val alt2 = Meters(0)
    getEuclideanDistance(state.position.φ, state.position.λ, alt1,
      wp.position.φ, wp.position.λ, alt2) <= proximity
  }

  /** Returns the Euclidean distance between the two given geographic
    * coordinates.
    */
  def getEuclideanDistance(φ1: Angle, λ1: Angle, alt1: Length, φ2: Angle,
                           λ2: Angle, alt2: Length): Length = {
    val p1 = Datum.wgs84ToECEF(φ1, λ1, alt1)
    val p2 = Datum.wgs84ToECEF(φ2, λ2, alt2)
    √((p1.x - p2.x).`²` + (p1.y - p2.y).`²` + (p1.z - p2.z).`²`)
  }
}