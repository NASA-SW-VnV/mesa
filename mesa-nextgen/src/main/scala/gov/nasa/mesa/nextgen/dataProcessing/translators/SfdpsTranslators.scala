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
package gov.nasa.mesa.nextgen.dataProcessing.translators

import gov.nasa.mesa.nextgen.core.{FlightPlan, FlightState,
  FlightStateCompleted, FlightTrack}
import gov.nasa.race.air.SfdpsTrack
import gov.nasa.race.common._
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.track.TrackedObject
import gov.nasa.race.uom.Angle._
import gov.nasa.race.uom._

/** This class represents a RACE ConfigurableTranslator that translates SFDPS
  * MessageCollection messages obtained from SWIM to FlightState and
  * FlightStateCompleted events.
  */
class Sfdps2FlightStateTranslator extends SfdpsParser {

  /** Creates an instance of a FlightState or FlightStateCompleted object.
    *
    * @param id  the flight id
    * @param cs  the flight call sign
    * @param lat latitude for the flight position
    * @param lon longitude for the flight position
    * @param vx  the ordinate coordinate
    * @param vy the abscissa coordinate
    * @param alt altitude for the flight position
    * @param spd the speed of the flight
    * @param vr the vertical speed which is the climb rate
    * @param date the timestamp of the message
    * @param arrivalPoint the arrival airport
    * @param departurePoint the departure airport
    * @param arrivalDate the arrival time
    * @param departureDate the departure time
    * @param status the statue of the flight
    * @param src originating ARTCC
    * @param route the route
    * @param flightRules flight rules
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of a FlightState or FlightStateCompleted object.
    */
  override def createSfdpsObject(id: String, cs: String, lat: Double,
                                 lon: Double, vx: Double, vy: Double,
                                 alt: Length, spd: Speed, vr: Speed,
                                 date: DateTime, arrivalPoint: String,
                                 departurePoint: String, arrivalDate: DateTime,
                                 departureDate: DateTime, status: Int,
                                 src: String, route: String, flightRules: String,
                                 equipmentQualifier: String): SfdpsTrack = {
    if ((status & TrackedObject.CompletedFlag) != 0) {
      FlightStateCompleted(id, cs, GeoPosition(Degrees(lat), Degrees(lon), alt),
        spd, Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
        departurePoint, departureDate, arrivalPoint, arrivalDate)
    } else {
      if (lat.isDefined && lon.isDefined && date.isDefined &&
        vx.isDefined && vy.isDefined && spd.isDefined && alt.isDefined) {
        FlightState(id, cs, GeoPosition(Degrees(lat), Degrees(lon), alt), spd,
          Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
          departurePoint, departureDate, arrivalPoint, arrivalDate)
      } else null
    }
  }
}

/** This class represents a translator that translates SFDPS MessageCollection
  * messages obtained from SWIM to FlightTrack events.
  */
class Sfdps2FlightTrackTranslator extends SfdpsParser {

  /** Creates an instance of a FlightTrack object.
    *
    * @param id  the flight id
    * @param cs  the flight call sign
    * @param lat latitude for the flight position
    * @param lon longitude for the flight position
    * @param vx  the ordinate coordinate
    * @param vy the abscissa coordinate
    * @param alt altitude for the flight position
    * @param spd the speed of the flight
    * @param vr the vertical speed which is the climb rate
    * @param date the timestamp of the message
    * @param arrivalPoint the arrival airport
    * @param departurePoint the departure airport
    * @param arrivalDate the arrival time
    * @param departureDate the departure time
    * @param status the statue of the flight
    * @param src originating ARTCC
    * @param route the route
    * @param flightRules flight rules
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of a FlightTrack object.
    */
  override def createSfdpsObject(id: String, cs: String, lat: Double,
                                 lon: Double, vx: Double, vy: Double,
                                 alt: Length, spd: Speed,vr: Speed,
                                 date: DateTime, arrivalPoint: String,
                                 departurePoint: String, arrivalDate: DateTime,
                                 departureDate: DateTime, status: Int,
                                 src: String, route: String, flightRules: String,
                                 equipmentQualifier: String): SfdpsTrack = {
    if (route != "?") {
      val departureProcedure = FlightPlan.getDepartureProcedure(route)
      val arrivalProcedure = FlightPlan.getArrivalProcedure(route)
      val plan = new FlightPlan(cs, route, departureProcedure, arrivalProcedure, flightRules)

      FlightTrack(id, cs, GeoPosition(Degrees(lat),Degrees(lon),alt), spd,
        Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
        departurePoint, departureDate, arrivalPoint, arrivalDate, plan,
        equipmentQualifier)
    } else null
  }
}

/** This class represents a translator that translates SFDPS MessageCollection
  * messages obtained from SWIM to the NextGen event objects of types
  * FlightState, FlightStateCompleted, and FlightTrack.
  */
class SfdpsFullTranslator extends SfdpsParser {

  /** Creates an instance of a FlightState, FlightStateCompleted, or
    * FlightTrack object.
    *
    * @param id  the flight id
    * @param cs  the flight call sign
    * @param lat latitude for the flight position
    * @param lon longitude for the flight position
    * @param vx  the ordinate coordinate
    * @param vy the abscissa coordinate
    * @param alt altitude for the flight position
    * @param spd the speed of the flight
    * @param vr the vertical speed which is the climb rate
    * @param date the timestamp of the message
    * @param arrivalPoint the arrival airport
    * @param departurePoint the departure airport
    * @param arrivalDate the arrival time
    * @param departureDate the departure time
    * @param status the statue of the flight
    * @param src originating ARTCC
    * @param route the route
    * @param flightRules flight rules
    * @param equipmentQualifier the equipment qualifier
    * @return an instance of a a FlightState, FlightStateCompleted, or
    *         FlightTrack object.
    */
  override def createSfdpsObject(id: String, cs: String, lat: Double,
                                 lon: Double, vx: Double, vy: Double,
                                 alt: Length, spd: Speed,vr: Speed,
                                 date: DateTime, arrivalPoint: String,
                                 departurePoint: String, arrivalDate: DateTime,
                                 departureDate: DateTime, status: Int,
                                 src: String, route: String, flightRules: String,
                                 equipmentQualifier: String): SfdpsTrack = {
    if ((status & TrackedObject.CompletedFlag) != 0) {
      FlightStateCompleted(id, cs, GeoPosition(Degrees(lat),Degrees(lon),alt),
        spd, Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
        departurePoint, departureDate, arrivalPoint, arrivalDate)
    } else {
      if (lat.isDefined && lon.isDefined && date.isDefined &&
        vx.isDefined && vy.isDefined && spd.isDefined && alt.isDefined) {
        FlightState(id, cs, GeoPosition(Degrees(lat),Degrees(lon),alt), spd,
          Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
          departurePoint, departureDate, arrivalPoint, arrivalDate)
      }
      else if(route != "?") {
        val departureProcedure = FlightPlan.getDepartureProcedure(route)
        val arrivalProcedure = FlightPlan.getArrivalProcedure(route)
        val plan = new FlightPlan(cs, route, departureProcedure, arrivalProcedure, flightRules)
        FlightTrack(id, cs, GeoPosition(Degrees(lat),Degrees(lon),alt), spd,
          Degrees(Math.atan2(vx, vy).toDegrees), vr, date, status, src,
          departurePoint, departureDate, arrivalPoint, arrivalDate, plan,
          equipmentQualifier)
      }
      else null
    }
  }
}
