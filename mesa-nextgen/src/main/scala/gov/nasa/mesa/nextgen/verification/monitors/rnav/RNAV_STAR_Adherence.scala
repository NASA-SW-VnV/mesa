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
package gov.nasa.mesa.nextgen.verification.monitors.rnav

import com.typesafe.config.Config
import gov.nasa.mesa.core.DautMonitor
import gov.nasa.mesa.nextgen.core._
import gov.nasa.mesa.nextgen.core.rnav.RnavStar

import scala.collection.immutable.HashMap

/** This class implements a finite state machine which is specified using
  * the Daut DLS and captures the RNAV STAR Adherence property using a finite
  * state machine. Given a RNAV STAR procedure, it checks whether the flights
  * assigned to this STAR adhere to the STAR laterally.
  *
  * We informally define the RNAV STAR lateral adherence property as follows:
  * "A fight shall cross inside a certain radius (e.g. 1.0 NM) around each
  * waypoint in the assigned RNAV STAR route, in order"
  *
  * @param config the configuration
  */
class RNAV_STAR_Adherence(config: Config) extends DautMonitor(config) {

  /**
    * The airport including the RNAV STAR procedure to be monitored.
    */
  val airport: Airport = Airport.getAirport(config.getString("airport")).get

  /**
    * The RNAV STAR procedure to be monitored.
    */
  val rnavStar: RnavStar = airport.getRnavStar(config.getString("star")).orNull

  /**
    * The final waypoint of the RNAV STAR procedure to be monitored.
    */
  val finalWaypoint: Waypoint = rnavStar.getFinalWaypoint

  /**
    * This represents a list that is used to keep track of call signs of the
    * flights that successfully completed the procedure and reached
    * 'finalWaypoint'.
    */
  var completedFlights = Seq.empty[String]

  /** Updates the list of completed flights by adding the given call sign to
    * the list.
    *
    * @param cs a flight call sign
    */
  def flightCompleted(cs: String): Unit = {
    completedFlights = completedFlights :+ cs
    println(s"STAR Completed - Stopped Monitoring: $cs")
  }

  /** Checks it the flight has completed by looking into the list of
    * 'completedFlights'
    *
    * @param cs a flight call sign
    * @return true if the flight is completed, otherwise return false.
    */
  def isCompleted(cs: String): Boolean = completedFlights.contains(cs)

  /** This method overrides the method daut.Monitor.verifyBeforeEvent which is
    * always invoked before invoking 'verify' on the message.
    *
    * @param msg a message
    */
  override def verifyBeforeEvent(msg: Any): Unit = msg match {
    case WaypointVisit(FlightInfo(state: FlightState, ti: FlightTrack),
    wp: Waypoint) =>
      if (!isCompleted(state.cs) && rnavStar.has(wp))
        println(s"** ${state.cs} at ${wp.id}")
    case StarChanged(ft: FlightTrack) => // ignore
    case FlightCompleted(_, _) => // ignore

  }

  /**
    * A map from flight call signs to traces including a sequence of waypoints
    * in 'rnavStar' that has been visited by the flight so far.
    */
  var csMap: Map[String, String] = HashMap.empty[String, String]

  /** Checks if the flight with the given call sign is being monitored.
    *
    * @param cs a flight call sign
    * @return true if the flight is already being monitored, otherwise returns
    *         false.
    */
  def monitored(cs: String): Boolean = csMap.contains(cs)

  /** Terminates and drops the analysis for the flight with the given call sign.
    * This is called when the RNAV STAR procedure of the flight is changed to
    * a different one.
    *
    * @param cs a flight call sign
    */
  def unmonitor(cs: String): Unit = csMap = csMap - cs

  /** Stars tracking the flight with the given call by adding it to 'csMap'
    *
    * @param cs a flight call sign
    */
  def monitor(cs: String): Unit = csMap += (cs -> "start")

  /** Checks if the flight is transitioning into the arrival procedure at the
    * assigned initial waypoint.
    *
    * @param ft an object including the flight track information
    * @param wp a waypoint at which the flight transitioned into the procedure
    * @return true if wp is the initial point in the assigned arrival procedure.
    */
  def atInitialTransition(ft: FlightTrack, wp: Waypoint): Boolean = {
    if (ft.fplan.hasArrivalProcedure)
      ft.getArrivalProcedure.get.transition.equals(wp.id)
    else
      false
  }

  /** Adds the given waypoint to the trace traveled by the flight with the
    * given call sign.
    *
    * @param cs a call sign
    * @param wp a waypoint
    */
  def extendTrace(cs: String, wp: Waypoint): Unit = {
    val trace = csMap(cs) + s" -> ${wp.id}"
    csMap += (cs -> trace)
  }

  /** Generates an error message indicating the flight did not adhere to the
    * assigned RNAV STAR procedure.
    *
    * @param ft an object including the flight track information
    * @param curr the current waypoint
    * @param error the waypoint at which the error occurred, that is, the
    *              waypoint from which the flight deviated
    * @return an error message
    */
  def generateErrorMsg(ft: FlightTrack, curr: Waypoint, error: Waypoint)
  : String = {
    extendTrace(ft.cs, error)

    s"${Console.RED}ERROR: ${ft.cs} didn't adhere to " +
      s"${ft.getArrivalProcedure.get}\n      " +
      s"${csMap(ft.cs)}${Console.RESET}"
  }

  /** Verifies if the given event 'e' is a relevant event.
    *
    * It verifies the following conditions:
    *   1. The flight is not being monitored already.
    *   2. The waypoint (stored in 'e') visited by the flight belongs to
    * 'rnavStar'.
    *   3. The waypoint belongs to the route starting at the initial waypoint.
    *
    * @param e a WaypointVisit event
    * @return true, if the event is relevant to this RNAV STAR, otherwise
    *         return false.
    */
  def isDefined(e: WaypointVisit): Boolean = {
    val flightTrack = e.flightInfo.flightTrack
    val wp = e.waypoint
    val init = rnavStar.getWaypoint(flightTrack.getArrivalProcedure.get
      .transition).getOrElse(Waypoint.NoWaypoint)
    !monitored(flightTrack.cs) && rnavStar.has(wp) &&
      rnavStar.belongsToRoute(init, wp)
  }

  /** Verifies if the given event 'e' is a relevant event.
    *
    * It verifies the following conditions:
    *   1. The flight is not being monitored already.
    *   2. The flight is assigned to 'rnavStar'.
    *
    * @param event a FlightCompleted event
    * @return true, if the event is relevant to this RNAV STAR, otherwise
    *         return false.
    */
  def isDefined(event: FlightCompleted): Boolean = {
    !monitored(event.cs) && event.star.name == rnavStar.name
  }

  /** This is the definition of the finite state machine capturing the RNAV
    * STAR Adherence property.
    */
  always {
    case e@WaypointVisit(fi@FlightInfo(_, ft: FlightTrack), wp: Waypoint)
      // Checks if the event is relevant and if a new finite state machine
      // should be generated for the flight.
      if (isDefined(e)) =>
      monitor(ft.cs)
      if (atInitialTransition(ft, wp)) {
        println(s"${Console.UNDERLINED}Started Monitoring: ${ft.cs}   " +
          s"STAR: ${ft.getArrivalProcedure.get}${Console.RESET}")
        nextState(wp, ft.cs)
      }
      else {
        errorState(s"${Console.MAGENTA}ERROR: ${ft.cs} is transitioning to " +
          s"${ft.getArrivalProcedure.get} at " + s"${wp.id}${Console.RESET}")
      }
    // Checks if the event is relevant
    case e@FlightCompleted(cs, star)
      if (isDefined(e)) =>
      errorState(s"${Console.MAGENTA}ERROR: $cs, COMPLETED, without " +
        s"visiting any waypoints in  $star ${Console.RESET}")
    case _ => //
  }

  /** Takes the finite state machine to the next state.
    *
    * @param wp a waypoint corresponding to the current state in the finite
    *           state machine.
    * @param cs the flight call
    * @return the next state
    */
  def nextState(wp: Waypoint, cs: String): state = {
    extendTrace(cs, wp)
    val next = rnavStar.getNextWaypoint(wp)
    watch {
      // flight visits the current waypoint and stays in the same sate
      case WaypointVisit(FlightInfo(FlightState(_, `cs`, _, _, _, _, _, _), _),
      `wp`) =>
        nextState(wp, cs)
      // flight visit the next waypoint in the procedure
      case WaypointVisit(FlightInfo(FlightState(_, `cs`, _, _, _, _, _, _),
      ft: FlightTrack), `next`) =>
        // if it's a final waypoint it moves to the accept state
        if (next == finalWaypoint) acceptState(finalWaypoint, ft)
        else nextState(next, cs)
      // if it visits any waypoints other than the current and the next one it
      // moves to the error state
      case WaypointVisit(FlightInfo(FlightState(_, `cs`, _, _, _, _, _, _),
      ft: FlightTrack), w: Waypoint) =>
        errorState(generateErrorMsg(ft, w, wp))
      // if we get here, it means that the flight is completed without visiting
      // the final waypoint, therefore an error is issued
      case FlightCompleted(`cs`, star: Procedure) =>
        extendTrace(cs, Waypoint.NoWaypoint)
        errorState(s"${Console.RED}ERROR: $cs, COMPLETED, didn't adhere to " +
          s"$star ${Console.RESET}")
      // if the flight is assigned to different procedure we need to drop the
      // analysis
      case StarChanged(ti@FlightTrack(_, `cs`, _, _, _, _)) => dropState(cs)
    }
  }

  /** Represents the error state.
    *
    * @param errMsg an error message
    * @return the error state.
    */
  def errorState(errMsg: String): state = {
    error(errMsg)
  }

  /** Represents the accept state.
    *
    * @param wp an accept state
    * @param ti an object storing the flight track info
    * @return the state 'ok' representing the accept state.
    */
  def acceptState(wp: Waypoint, ti: FlightTrack): state = {
    extendTrace(ti.cs, wp)
    println(s"${Console.GREEN}SUCCESS: ${ti.cs} adhered to " +
      s"${ti.getArrivalProcedure.get}\n ${csMap(ti.cs)} " +
      s"${Console.RESET}")
    flightCompleted(ti.cs)
    ok
  }

  /** Terminates the analysis for the flight with the given call sign.
    *
    * @param cs the flight call sign
    * @return the state 'ok' representing the accept state.
    */
  def dropState(cs: String): state = {
    unmonitor(cs)
    println(s"${Console.MAGENTA}WARNING: dismissed Monitoring $cs due to " +
      s"STAR change${Console.RESET}")
    ok
  }
}