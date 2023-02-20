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

import gov.nasa.race.air.SfdpsTrack
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.uom._

//-- events and messages for NextGen

/** A message that encapsulates dynamic and static information about the flight.
  *
  * @param flightState an event capturing the state of the flight
  * @param flightTrack the track information of the flight
  */
case class FlightInfo(flightState: FlightState, flightTrack: FlightTrack)

/** This event occurs if the fight crosses inside a specified (e.g. 1.0 NM)
  * radius around the waypoint.
  *
  * @param flightInfo a message in`cluding flight information
  * @param waypoint a waypoint
  */
case class WaypointVisit(flightInfo: FlightInfo, waypoint: Waypoint)

/** This event occurs if the flight is completed.
  *
  * @param cs the call sign of the flight
  * @param star the RNAV STAR procedure assigned to the flight with the given
  *             call sign
  */
case class FlightCompleted(cs: String, star: Procedure)

/** A message indicating the change of RNAV STAR.
  *
  * @param flightTrack the new track information assigned to the flight
  */
case class StarChanged(flightTrack: FlightTrack)

/** Captures the state of an operating flight.
  *
  * @param id the flight identifier //changes over the course of the flight
  * @param cs the flight call sign
  * @param position the flight position
  * @param speed the flight speed
  * @param heading the heading of the flight
  * @param vr the vertical speed or the rate of climb
  * @param date the timestamp for this message
  * @param status the flight status
  * @param src originating ARTCC
  * @param departurePoint the departure airport
  * @param departureDate the time of departure
  * @param arrivalPoint the arrival airport
  * @param arrivalDate the time of arrival
  */
class FlightState(id: String,
                  cs: String,
                  position: GeoPosition,
                  speed: Speed,
                  heading: Angle,
                  vr: Speed,
                  date: DateTime,
                  status: Int,
                  /* SFDPS specific */
                  src: String = "?",
                  departurePoint: String = "?",
                  departureDate: DateTime = DateTime.UndefinedDateTime,
                  arrivalPoint: String = "?",
                  arrivalDate: DateTime = DateTime.UndefinedDateTime)
  extends SfdpsTrack(id, cs, position, speed, heading, vr, date, status, src,
    departurePoint, departureDate, arrivalPoint, arrivalDate) {

  override def toString = {
    var s1, s2 = ""

    s1 = s"FlightState($id,$cs,$position,${speed.toKnots.toInt}kn," +
      s"${heading.toNormalizedDegrees.toInt}°,$date, 0x${status.toHexString}"

    if(src!="?" && departurePoint!="?" && departurePoint!="?") {
      s2 = s"$src, DeparturePoint: $departurePoint, " +
        s"DepartureDate: $departureDate, " + s"ArrivalPoint: $arrivalPoint, " +
        s"ArrivalDate: $arrivalDate"
    }

    s1+s2+")"
  }

}

/** The companion extractor object for FlightState. */
object FlightState {

  /** A constructor which takes relevant arguments and creates a FlightState
    * object.
    *
    * @param id the flight identifier // changes over the course of the flight
    * @param cs the flight call sign
    * @param position the flight position
    * @param speed the flight speed
    * @param heading the heading of the flight
    * @param vr the vertical speed or the rate of climb
    * @param date the timestamp for this message
    * @param status the flight status
    * @param src originating ARTCC
    * @param departurePoint the departure airport
    * @param departureDate the time of departure
    * @param arrivalPoint the arrival airport
    * @param arrivalDate the time of arrival
    * @return an instance of the class FlightObject.
    */
  def apply(id: String, cs: String, position: GeoPosition,
            speed: Speed, heading: Angle, vr: Speed,
            date: DateTime, status: Int, src: String,
            departurePoint: String = "?",
            departureDate: DateTime = DateTime.UndefinedDateTime,
            arrivalPoint: String = "?",
            arrivalDate: DateTime = DateTime.UndefinedDateTime): FlightState = {
    new FlightState(id, cs, position, speed, heading, vr, date, status, src,
      departurePoint, departureDate, arrivalPoint, arrivalDate)
  }

  /** Takes a FlightState object and gives back the arguments which is mostly
    * used for pattern matching in actor receive methods.
    *
    * @param fs an event capturing the state of the flight
    * @return an Option object storing all the arguments extracted from the
    *         given FlightState.
    */
  def unapply(fs: FlightState): Option[(String, String, GeoPosition, Speed,
    Angle, Speed, DateTime, Int)] = {
    Some(fs.id, fs.cs, fs.position, fs.speed, fs.heading, fs.vr, fs.date,
      fs.status)
  }
}

/** Captures the state of a completed flight.
  *
  * @param id the flight identifier //changes over the course of the flight
  * @param cs the flight call sign
  * @param position the flight position
  * @param speed the flight speed
  * @param heading the heading of the flight
  * @param vr the vertical speed or the rate of climb
  * @param date the timestamp for this message
  * @param status the flight status
  * @param src originating ARTCC
  * @param departurePoint the departure airport
  * @param departureDate the time of departure
  * @param arrivalPoint the arrival airport
  * @param arrivalDate the time of arrival
  */
class FlightStateCompleted(id: String,
                           cs: String,
                           position: GeoPosition,
                           speed: Speed,
                           heading: Angle,
                           vr: Speed,
                           date: DateTime,
                           status: Int,
                           /* SFDPS specific */
                           src: String,
                           departurePoint: String = "?",
                           departureDate: DateTime = DateTime.UndefinedDateTime,
                           arrivalPoint: String = "?",
                           arrivalDate: DateTime = DateTime.UndefinedDateTime)
  extends FlightState(id, cs, position, speed, heading, vr, date, status, src,
    departurePoint, departureDate, arrivalPoint, arrivalDate) {

  override def toString =
    super.toString.replace("FlightState", "FlightStateCompleted")
}

/** The companion extractor object for FlightStateCompleted. */
object FlightStateCompleted {

  /** A constructor which takes relevant arguments and creates a
    * FlightStateCompleted object.
    *
    * @param id the flight identifier
    * @param cs the flight call sign
    * @param position the flight position
    * @param speed the flight speed
    * @param heading the heading of the flight
    * @param vr the vertical speed or the rate of climb
    * @param date the timestamp for this message
    * @param status the flight status
    * @param src originating ARTCC
    * @param departurePoint the departure airport
    * @param departureDate the time of departure
    * @param arrivalPoint the arrival airport
    * @param arrivalDate the time of arrival
    * @return an instance of the class FlightStateCompleted.
    */
  def apply(id: String, cs: String, position: GeoPosition,
            speed: Speed, heading: Angle, vr: Speed,
            date: DateTime, status: Int, src: String,
            departurePoint: String = "?",
            departureDate: DateTime = DateTime.UndefinedDateTime,
            arrivalPoint: String = "?",
            arrivalDate: DateTime = DateTime.UndefinedDateTime): FlightState = {
    new FlightStateCompleted(id, cs, position, speed, heading, vr, date, status,
      src, departurePoint, departureDate, arrivalPoint, arrivalDate)
  }

  /** Takes a FlightStateCompleted object and gives back the arguments which is
    * mostly used for pattern matching in actor receive methods.
    *
    * @param fs an event capturing the state of the flight
    * @return an Option object storing all the arguments extracted from the
    *         given FlightStateCompleted.
    */
  def unapply(fs: FlightStateCompleted): Option[(String, String, GeoPosition,
    Speed, Angle, Speed, DateTime, Int)] = {
    Some(fs.id, fs.cs, fs.position, fs.speed, fs.heading, fs.vr, fs.date,
      fs.status)
  }
}

/** This class encapsulates data obtained from parsers directly. It captures
  * the track information of a flight.
  *
  * @param id the flight identifier //changes over the course of the flight
  * @param cs the flight call sign
  * @param position the flight position
  * @param speed the flight speed
  * @param heading the heading of the flight
  * @param vr the vertical speed or the rate of climb
  * @param date the timestamp for this message
  * @param status the flight status
  * @param src originating ARTCC
  * @param departurePoint the departure airport
  * @param departureDate the time of departure
  * @param arrivalPoint the arrival airport
  * @param arrivalDate the time of arrival
  * @param fplan the flight plan information
  * @param equipmentQualifier the equipment qualifier
  */
class FlightTrack(id: String,
                  cs: String,
                  position: GeoPosition,
                  speed: Speed,
                  heading: Angle,
                  vr: Speed,
                  date: DateTime,
                  status: Int,
                  /* SFDPS specific */
                  src: String, // originating ARTCC
                  departurePoint: String = "?",
                  departureDate: DateTime = DateTime.UndefinedDateTime,
                  arrivalPoint: String = "?",
                  arrivalDate: DateTime = DateTime.UndefinedDateTime,
                  val fplan: FlightPlan,
                  val equipmentQualifier: String)
  extends FlightState(id, cs, position, speed, heading, vr, date, status, src,
    departurePoint, departureDate, arrivalPoint, arrivalDate) {

  /** Returns the departure procedure of the flight. */
  def getDepartureProcedure: Option[Procedure] = fplan.departure

  /** Returns the arrival procedure of the flight. */
  def getArrivalProcedure: Option[Procedure] = fplan.arrival
}

/** The companion extractor object for FlightTrack. */
object FlightTrack {

  /** A constructor which takes relevant arguments and creates a FlightTrack
    * object.
    *
    * @param id the flight identifier
    * @param cs the flight call sign
    * @param position the flight position
    * @param speed the flight speed
    * @param heading the heading of the flight
    * @param vr the vertical speed or the rate of climb
    * @param date the timestamp for this message
    * @param status the flight status
    * @param src originating ARTCC
    * @param departurePoint the departure airport
    * @param departureDate the time of departure
    * @param arrivalPoint the arrival airport
    * @param arrivalDate the time of arrival
    * @param fplan the flight plan information
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of the class FlightTrack.
    */
  def apply(id: String, cs: String, position: GeoPosition,
            speed: Speed, heading: Angle, vr: Speed,
            date: DateTime, status: Int, src: String,
            departurePoint: String = "?",
            departureDate: DateTime = DateTime.UndefinedDateTime,
            arrivalPoint: String = "?",
            arrivalDate: DateTime = DateTime.UndefinedDateTime,
            fplan: FlightPlan, equipmentQualifier: String): FlightState = {
    new FlightTrack(id, cs, position, speed, heading, vr, date, status, src,
      departurePoint, departureDate, arrivalPoint, arrivalDate, fplan,
      equipmentQualifier)
  }

  /** A constructor which takes relevant arguments and creates a FlightTrack
    * object.
    *
    * @param id the flight identifier
    * @param cs the flight call sign
    * @param departurePoint the departure airport
    * @param arrivalPoint the arrival airport
    * @param fplan the flight plan information
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of the class FlightTrack.
    */
  def apply(id: String, cs: String,
            departurePoint: String, arrivalPoint: String,
            fplan: FlightPlan, equipmentQualifier: String): FlightTrack = {
    new FlightTrack(id, cs, GeoPosition.undefinedPos, Speed.UndefinedSpeed,
      Angle.UndefinedAngle, Speed.UndefinedSpeed, DateTime.UndefinedDateTime,
      0, "?", departurePoint, DateTime.UndefinedDateTime, arrivalPoint,
      DateTime.UndefinedDateTime, fplan, equipmentQualifier)
  }

  /** Takes a FlightTrack object and gives back the arguments which is mostly
    * used for pattern matching in actor receive methods.
    *
    * @param ft the flight track information
    * @return an Option object storing all the arguments extracted from the
    *         given FlightTrack.
    */
  def unapply(ft: FlightTrack): Option[(String, String, String, String,
    FlightPlan, String)] = {
    Some(ft.id, ft.cs, ft.departurePoint, ft.arrivalPoint, ft.fplan,
      ft.equipmentQualifier)
  }
}
