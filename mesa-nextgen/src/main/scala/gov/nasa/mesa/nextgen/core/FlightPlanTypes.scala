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

import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.uom.Length.Meters
import gov.nasa.race.uom.Speed.MetersPerSecond
import gov.nasa.race.uom.{Length, Speed}

/**
  * This class encapsulates the flight plan information.
  *
  * Retrieving flight plans in the right format is in progress. The goal is to
  * build flight plans in the Arinc format (see the Arinc 702A-3 document)
  *
  * @param cs the flight call sign
  * @param route the flight plan specified in the ICAO format
  * @param departure the departure transition
  * @param arrival the arrival transition
  */
case class FlightPlan(val cs: String,
                      val route: String,
                      val departure: Option[Procedure],
                      val arrival: Option[Procedure]) {

  override def toString =
    s"FlightPlan(cs: $cs, " +
      s"route: $route, " +
      s"departure: ${departure.getOrElse(None)}, " +
      s"arrival: ${arrival.getOrElse(None)})"

  /** Checks if the flight is assigned with an arrival procedure.
    *
    * @return true, if the flight is assigned with an arrival procedure,
    *         otherwise, returns false
    */
  def hasArrivalProcedure: Boolean = arrival.isDefined

  /** Checks if the flight is assigned with an departure procedure.
    *
    * @return true, if the flight is assigned with an departure procedure,
    *         otherwise, returns false
    */
  def hasDepartureProcedure: Boolean = departure.isDefined
}

object FlightPlan {

  /**
    * A regular expression that captures the departure transition and procedure
    * in the flight plan specified in the ICAO format
    */
  val departureRegex =
    """K[A-Z]{3}+\.([A-Z0-9]+)\.([A-Z0-9]+)\.(?:\.)?.+""".r

  /**
    * A regular expression that captures the arrival transition and procedure
    * in the flight plan specified in the ICAO format
    */
  val arrivalRegex =
    """.+\.(?:\.)?([A-Z0-9]+)\.([A-Z0-9]+)\.K[A-Z]{3}+(?:/[0-9]+)?""".r

  /** Extracts the departure procedure from the given ICAO route.
    *
    * @param route a flight route in the ICAO format.
    * @return Some[Procedure] capturing the departure procedure from the given
    *         ICAO route, if any, otherwise it returns None
    */
  def getDepartureProcedure(route: String): Option[Procedure] = {
    route match {
      case departureRegex(procedure, transition) =>
        Some(new Procedure(transition, procedure))
      case _ => None
    }
  }

  /** Extracts the arrival procedure from the given ICAO route.
    *
    * @param route a flight route in the ICAO format.
    * @return Some[Procedure] capturing the arrival procedure from the given
    *         ICAO route, if any, otherwise it returns None
    */
  def getArrivalProcedure(route: String): Option[Procedure] = {
    route match {
      case arrivalRegex(transition, procedure) =>
        Some(new Procedure(transition, procedure))
      case _ => None
    }
  }
}

/** This type is used to encapsulate the departure and arrival procedures.
  */
case class Procedure(val transition: String, val name: String) {
  override def toString = s"$transition.$name"
}

object Procedure {
  final val NotAssigned = Procedure("<none>", "<unassigned>")
}

/** Encapsulates information of the waypoint.
  *
  * @param id the waypoint name
  * @param position the waypoint position
  * @param minAlt minimum permitted altitude at the waypoint
  * @param maxAlt maximum permitted altitude at the waypoint
  * @param speed maximum permitted speed at the waypoint
  */
case class Waypoint(val id: String,
                    val position: GeoPosition,
                    val minAlt: Length,
                    val maxAlt: Length,
                    val speed: Speed)

object Waypoint {
  final val NoWaypoint =
    Waypoint("<none>", GeoPosition.fromDegrees(0, 0), Meters(0), Meters(0),
      MetersPerSecond(0))
}
