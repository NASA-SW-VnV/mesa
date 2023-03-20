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
package gov.nasa.mesa.nextgen.core.rnav

import gov.nasa.mesa.nextgen.core.{FlightState, ExtendedFlightState, Geo, Waypoint}
import gov.nasa.race.uom.Length

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.language.{existentials, implicitConversions}

/** Represents a single RNAV STAR procedure.
  *
  * @param name the name of the RNAV STAR procedure
  * @param graph the graph representation of the RNAV STAR procedure
  */
case class RnavStar(name: String, graph: StarGraph) {

  def waypoints: List[Waypoint] = graph.waypoints

  /** Returns the final waypoint for this STAR provided that the STAR has only
    * one final waypoint.
    *
    * @return the final waypoint for the RNAV STAR captured by this instance.
    */
  def getFinalWaypoint: Waypoint = graph.finalWayPoint

  /** Returns the list of all waypoints in this RNAV STAR within the given
    * proximity.
    *
    * @param state an event capturing the state of the flight
    * @param proximity proximity from the aircraft position
    * @return the list of all waypoints within the given proximity from the
    *         aircraft.
    */
  def getWaypointsInProximity(state: FlightState, proximity: Length)
  : mutable.ListBuffer[Waypoint] =
    Geo.getWaypointsInProximity(state, proximity, waypoints)

  /** Returns the closest waypoint within the given proximity from the aircraft.
    *
    * @param state an event capturing the state of the flight
    * @param proximity proximity from the aircraft position
    * @return Some[Waypoint] representing the closest RNAV STAR waypoint within
    *         the given proximity, or None if there is not any waypoints in
    *         the specified proximity.
    */
  def getWaypointInProximity(state: ExtendedFlightState, proximity: Length)
  : Option[Waypoint] =
    Geo.getWaypointInProximity(state, proximity, waypoints)

  /** Checks if the given waypoint belongs to this RNAV STAR
    *
    * @param waypoint a waypoint
    * @return true if the given waypoint belongs to this RNAV STAR, otherwise,
    *         false.
    */
  def has(waypoint: Waypoint): Boolean = waypoints.contains(waypoint)

  override def toString: String = {
    s"STAR $name: ${waypoints.foreach(e => s"${e.id} ")}"
  }

  /** Gets the direct successor of the given waypoint in this RNAV STAR.
    *
    * @param waypoint a waypoint
    * @return a waypoint which is the direct successor of the given waypoint, or
    *         Waypoint.NoWayPoint if the waypoint is the final waypoint of this
    *         RNAV STAR.
    */
  def getNextWaypoint(waypoint: Waypoint): Waypoint = graph.next(waypoint)

  /** Checks if the given waypoint is part of the RNAV STAR route assigned to
    * the aircraft.
    *
    * @param initial a waypoint that represents the initial point in the
    *                assigned route
    * @param wp a waypoint
    * @return true if the given waypoint belongs to the RNAV STAR route that
    *         starts with the point 'initial'.
    */
  def belongsToRoute(initial: Waypoint, wp: Waypoint): Boolean =
    graph.belongsToPath(initial, wp)

  /** Gets the waypoint with the given name.
    *
    * @param name the name of a waypoint
    * @return Some[Waypoint] representing the waypoint with the given name, or
    *         None if there is no waypoint with a specified name in this RNAV
    *         STAR.
    */
  def getWaypoint(name: String): Option[Waypoint] = {
    graph.waypoints.find(_.id == name)
  }
}

/** This class represents the edges of the directed graph that captures a RNAV
  * STAR.
  *
  * @param tail the end node of the edge
  * @param restrictions the restrictions, such as altitude and speed limits,
  *                     for the part of the RNAV STAR that corresponds to
  *                     this edge.
  */
case class Edge(tail: Waypoint, restrictions: String)

/** A directed graph implementation used to represent RNAV STAR procedures.
  *
  * @param links a map from waypoints corresponding to head nodes to the edges
  *              including the direct successor of the waypoint
  */
class StarGraph(val links: HashMap[Waypoint, Edge]) {

  /** Finds the final waypoint for the RNAV STAR by looking into 'links' and
    * finding a edge with no tail.
    *
    * @return the final waypoint for this RNAV STAR provided that the STAR
    *         has only one final waypoint.
    */
  def finalWayPoint: Waypoint = {
    val e = links.values.find(edge => !links.contains(edge.tail)).get
    e.tail
  }

  /** A list of waypoints that belong to this STAR. */
  val waypoints: List[Waypoint] = links.keySet.toList.appended(finalWayPoint)

  /** Gets the next waypoint in the RNAV STAR.
    *
    * @param wp a waypoint corresponding to a node
    * @return the direct successor of the given wayppoint
    */
  def next(wp: Waypoint): Waypoint = {
    val e = links.get(wp)
    if (e.isDefined) e.get.tail else Waypoint.NoWaypoint
  }

  /** Checks if the given waypoint, 'waypoint', belongs to a path that begins
    * with 'initial'.
    *
    * @param initial the initial point of the route
    * @param wp a waypoint
    * @return true if the given waypoint is part of the route starting from the
    *         given initial waypoint, otherwise returns false.
    */
  def belongsToPath(initial: Waypoint, wp: Waypoint): Boolean = {
    waypointsInPath(initial).contains(wp)
  }

  /** Returns a list of all waypoints in the path that begins with 'initial'.
    *
    * @param initial the initial point of the route
    * @return the list of all waypoints in the specified route
    */
  def waypointsInPath(initial: Waypoint): mutable.Seq[Waypoint] = {
    var wp = initial
    var list = mutable.ListBuffer.empty[Waypoint]
    while (wp != Waypoint.NoWaypoint) {
      list += wp
      wp = next(wp)
    }
    list
  }
}

/** Companion object for the graph implementation used to represent RNAV STARs.
  */
object StarGraph {

  /** Creates a new StarGraph object.
    *
    * @param tuples tuples that specify the head and tail of the edges
    * @return a new StarGraph instance.
    */
  def apply(tuples: (Waypoint, Waypoint)*): StarGraph = {
    var tmp: HashMap[Waypoint, Edge] = HashMap.empty[Waypoint, Edge]
    for (t <- tuples) tmp = tmp + (t._1 -> Edge(t._2, ""))
    new StarGraph(tmp)
  }
}