/**
  * Copyright © 2023 United States Government as represented by the
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
import gov.nasa.race.uom.{Angle, DateTime, Speed}

/** This class extends SfdpsTrack with additional information about the route and
  * the equipments used in the plane.
  *
  * @param id the flight identifier, which could change over the course of the flight
  * @param cs the flight call sign
  * @param position the flight position
  * @param speed the flight speed
  * @param heading the heading of the flight
  * @param vr the vertical speed or the rate of climb
  * @param date the timestamp for this message
  * @param status the flight status
  * @param src originating Air Route Traffic Control Center (ARTCC) which provides air traffic services to aircraft operating on IFR
  * @param departurePoint the departure airport
  * @param departureDate the time of departure
  * @param arrivalPoint the arrival airport
  * @param arrivalDate the time of arrival
  * @param route the flight plan specified in the ICAO format
  * @param flightRules flight rules which is either IFR or VFR
  * @param equipmentQualifier the equipment qualifier
  */
class ExtendedSfdpsTrack(id: String,
                         cs: String,
                         position: GeoPosition,
                         speed: Speed,
                         heading: Angle,
                         vr: Speed,
                         date: DateTime,
                         status: Int,
                         //--- SFDPS specific
                         src: String, // originating ARTCC
                         departurePoint: String = "?",
                         departureDate: DateTime = DateTime.UndefinedDateTime, // actual
                         arrivalPoint: String = "?",
                         arrivalDate: DateTime = DateTime.UndefinedDateTime, // actual or estimate
                         //--- Extended part
                         val route: String,
                         val flightRules: String,
                         val equipmentQualifier: String)
  extends SfdpsTrack(id, cs, position, speed, heading, vr, date, status, src,
    departurePoint, departureDate, arrivalPoint, arrivalDate) {

  override def toString: String = f"ExtendedSfdpsTrack($id%s, $cs%s, (${position.toGenericString3D}), " +
    f"${speed.d}%.3fm/s, ${heading.toNormalizedDegrees}%.3f°," +
    f"${vr.d}%.3fm/s, $date, $status, $src, $departurePoint, $departureDate, $arrivalPoint, $arrivalDate, " +
    f"$route, $flightRules, $equipmentQualifier)"
}

/** The companion extractor object for ExtendedSfdpsTrack. */
object ExtendedSfdpsTrack {

  /** A constructor which takes relevant arguments and creates an ExtendedSfdpsTrack object
    * object.
    *
    * @param id                 the flight identifier, which could change over the course of the flight
    * @param cs                 the flight call sign
    * @param position           the flight position
    * @param speed              the flight speed
    * @param heading            the heading of the flight
    * @param vr                 the vertical speed or the rate of climb
    * @param date               the timestamp for this message
    * @param status             the flight status
    * @param src                originating Air Route Traffic Control Center (ARTCC) which provides air traffic services to aircraft operating on IFR
    * @param departurePoint     the departure airport
    * @param departureDate      the time of departure
    * @param arrivalPoint       the arrival airport
    * @param arrivalDate        the time of arrival
    * @param route              the flight plan specified in the ICAO format
    * @param flightRules        flight rules which is either IFR or VFR
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of the class ExtendedSfdpsTrack.
    */
  def apply(id: String, cs: String, position: GeoPosition,
            speed: Speed, heading: Angle, vr: Speed,
            date: DateTime, status: Int, src: String,
            departurePoint: String = "?",
            departureDate: DateTime = DateTime.UndefinedDateTime,
            arrivalPoint: String = "?",
            arrivalDate: DateTime = DateTime.UndefinedDateTime,
            route: String,
            flightRules: String,
            equipmentQualifier: String): ExtendedSfdpsTrack = {
    new ExtendedSfdpsTrack(id, cs, position, speed, heading, vr, date, status,
      src, departurePoint, departureDate, arrivalPoint, arrivalDate, route,
      flightRules, equipmentQualifier)
  }

  /** Takes an ExtendedSfdpsTrack object and gives back the arguments which is
    * mostly used for pattern matching in actor receive methods.
    *
    * @param est an object capturing the SFDPS flight message
    * @return an Option object storing all the arguments extracted from the
    *         given ExtendedSfdpsTrack.
    */
  def unapply(est: ExtendedSfdpsTrack): Option[(String, String, GeoPosition, Speed,
    Angle, Speed, DateTime, Int, String, String, DateTime, String, DateTime, String,
    String, String)] = {
    Some(est.id, est.cs, est.position, est.speed, est.heading, est.vr, est.date,
      est.status, est.src, est.departurePoint, est.departureDate, est.arrivalPoint,
      est.arrivalDate, est.route, est.flightRules, est.equipmentQualifier)
  }
}
