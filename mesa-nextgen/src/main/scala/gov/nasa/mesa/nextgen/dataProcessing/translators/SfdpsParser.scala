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

import gov.nasa.mesa.nextgen.core.FlightPlan
import gov.nasa.race.air.SfdpsTrack
import gov.nasa.race.air.translator.MessageCollectionParser
import gov.nasa.race.common._
import gov.nasa.race.track.TrackedObject
import gov.nasa.race.uom.Length._
import gov.nasa.race.uom.Speed._
import gov.nasa.race.uom.{DateTime, _}

/** An optimized translator for SWIM SFDPS MessageCollection (and legacy
  * NasFlight) messages.
  *
  * This extends MessageCollectionParser in RACE to obtain addition information
  * about the flight, such as route, IFR status, and equipment qualifier.
  *
  * The StringMatchGenerator in RACE is implemented to automatically generate
  * the matching part using the script "race/script/smg" in RACE.
  */
abstract class SfdpsParser extends MessageCollectionParser {

  //--- added to RACE
  val _route = ConstAsciiSlice("route")
  val _agreed = ConstAsciiSlice("agreed")
  val _nasRouteText = ConstAsciiSlice("nasRouteText")
  val _initialFlightRules = ConstAsciiSlice("initialFlightRules")
  val _aircraftDescription = ConstAsciiSlice("aircraftDescription")
  val _equipmentQualifier = ConstAsciiSlice("equipmentQualifier")

  override protected def parseFlight: Unit = {
    var id, cs: String = null
    var src: String = null
    var lat, lon, vx, vy: Double = UndefinedDouble
    var alt: Length = UndefinedLength
    var spd: Speed = UndefinedSpeed
    var vr: Speed = UndefinedSpeed // there is no vertical rate in FIXM_v3_2
    var date = DateTime.UndefinedDateTime
    var arrivalPoint: String = "?"
    var arrivalDate: DateTime = DateTime.UndefinedDateTime
    var departurePoint: String = "?"
    var departureDate: DateTime = DateTime.UndefinedDateTime
    var status: Int = 0

    //--- added to RACE
    var route: String = "?"
    var flightRules: String = "?"
    var equipmentQualifier: String = "?"
    //---

    var loc: Int = LOC_UNDEFINED

    def flight(data: Array[Byte], off: Int, len: Int): Unit = {
      if (isStartTag) {
        def readSpeed: Speed = {
          val u = if (parseAttr(_uom)) attrValue else ConstUtf8Slice.EmptySlice

          if (parseSingleContentString) {
            val v = contentString.toDouble
            if (u == _mph) UsMilesPerHour(v)
            else if (u == _kmh) KilometersPerHour(v)
            else Knots(v)

          } else UndefinedSpeed
        }

        def readAltitude: Length = {
          val u = if (parseAttr(_uom)) attrValue else ConstUtf8Slice.EmptySlice

          if (parseSingleContentString) {
            val v = contentString.toDouble
            if (u == _meters) Meters(v) else Feet(v)
          } else UndefinedLength
        }

        @inline def process_flightIdentification = {
          while ((id == null || cs == null) && parseNextAttr) {
            val off = attrName.off
            val len = attrName.len

            @inline def match_computerId = {
              len == 10 && data(off) == 99 && data(off + 1) == 111 &&
                data(off + 2) == 109 && data(off + 3) == 112 &&
                data(off + 4) == 117 && data(off + 5) == 116 &&
                data(off + 6) == 101 && data(off + 7) == 114 &&
                data(off + 8) == 73 && data(off + 9) == 100
            }

            @inline def match_aircraftIdentification = {
              len == 22 && data(off) == 97 && data(off + 1) == 105 &&
                data(off + 2) == 114 && data(off + 3) == 99 &&
                data(off + 4) == 114 && data(off + 5) == 97 &&
                data(off + 6) == 102 && data(off + 7) == 116 &&
                data(off + 8) == 73 && data(off + 9) == 100 &&
                data(off + 10) == 101 && data(off + 11) == 110 &&
                data(off + 12) == 116 && data(off + 13) == 105 &&
                data(off + 14) == 102 && data(off + 15) == 105 &&
                data(off + 16) == 99 && data(off + 17) == 97 &&
                data(off + 18) == 116 && data(off + 19) == 105 &&
                data(off + 20) == 111 && data(off + 21) == 110
            }

            if (match_computerId) {
              id = attrValue.intern
            } else if (match_aircraftIdentification) {
              cs = attrValue.intern
            }
          }
        }

        @inline def process_pos = {
          if (tagHasParent(_location)) {
            if (parseSingleContentString) {
              slicer.setSource(contentString)
              if (slicer.hasNext) lat = slicer.next(valueSlice).toDouble
              if (slicer.hasNext) lon = slicer.next(valueSlice).toDouble
            }
          }
        }

        @inline def process_position = {
          if (tagHasParent(_enRoute)) {
            if (parseAttr(_positionTime)) date = DateTime.parseYMDT(attrValue)
          }
        }

        @inline def process_x = vx = readDoubleContent

        @inline def process_y = vy = readDoubleContent

        @inline def process_surveillance =
          if (tagHasParent(_actualSpeed)) spd = readSpeed

        @inline def process_altitude = alt = readAltitude

        @inline def process_arrival = {
          if (parseAttr(_arrivalPoint)) arrivalPoint = attrValue.intern
          loc = LOC_ARRIVAL
        }

        @inline def process_departure = {
          if (parseAttr(_departurePoint)) departurePoint = attrValue.intern
          loc = LOC_DEPARTURE
        }

        def process_actual = {
          if (tagHasParent(_runwayTime)) {
            if (parseAttr(_time)) {
              val d = DateTime.parseYMDT(attrValue)
              if (loc == LOC_DEPARTURE) {
                departureDate = d
              } else if (loc == LOC_ARRIVAL) {
                arrivalDate = d
                status |= TrackedObject.CompletedFlag
              }
            }
          }
        }

        def process_estimated = {
          if (tagHasParent(_runwayTime)) {
            if (loc == LOC_ARRIVAL) {
              if (parseAttr(_time)) arrivalDate = DateTime.parseYMDT(attrValue)
            }
          }
        }

        //--- Added to RACE
        @inline def process_route = {
          while ((route == "?" || flightRules == "?") && parseNextAttr) {
            val off = attrName.off
            val len = attrName.len

            @inline def match_nasRouteText = {
              len == 12 && data(off) == 110 && data(off + 1) == 97 &&
                data(off + 2) == 115 && data(off + 3) == 82 &&
                data(off + 4) == 111 && data(off + 5) == 117 &&
                data(off + 6) == 116 && data(off + 7) == 101 &&
                data(off + 8) == 84 && data(off + 9) == 101 &&
                data(off + 10) == 120 && data(off + 11) == 116
            }

            @inline def match_initialFlightRules = {
              len == 18 && data(off) == 105 && data(off + 1) == 110 &&
                data(off + 2) == 105 && data(off + 3) == 116 &&
                data(off + 4) == 105 && data(off + 5) == 97 &&
                data(off + 6) == 108 && data(off + 7) == 70 &&
                data(off + 8) == 108 && data(off + 9) == 105 &&
                data(off + 10) == 103 && data(off + 11) == 104 &&
                data(off + 12) == 116 && data(off + 13) == 82 &&
                data(off + 14) == 117 && data(off + 15) == 108 &&
                data(off + 16) == 101 && data(off + 17) == 115
            }

            if (match_nasRouteText) {
              route = attrValue.intern
            } else if (match_initialFlightRules) {
              flightRules = attrValue.intern
            }
          }
        }

        @inline def process_aircraftDescription = {
          while (equipmentQualifier == "?" && parseNextAttr) {
            val off = attrName.off
            val len = attrName.len

            @inline def match_equipmentQualifier = {
              len == 18 && data(off) == 101 && data(off + 1) == 113 &&
                data(off + 2) == 117 && data(off + 3) == 105 &&
                data(off + 4) == 112 && data(off + 5) == 109 &&
                data(off + 6) == 101 && data(off + 7) == 110 &&
                data(off + 8) == 116 && data(off + 9) == 81 &&
                data(off + 10) == 117 && data(off + 11) == 97 &&
                data(off + 12) == 108 && data(off + 13) == 105 &&
                data(off + 14) == 102 && data(off + 15) == 105 &&
                data(off + 16) == 101 && data(off + 17) == 114
            }

            if (match_equipmentQualifier) {
              equipmentQualifier = attrValue.intern
            }
          }
        }
        //---

        @inline def match_flightIdentification = {
          len == 20 && data(off) == 102 && data(off + 1) == 108 &&
            data(off + 2) == 105 && data(off + 3) == 103 &&
            data(off + 4) == 104 && data(off + 5) == 116 &&
            data(off + 6) == 73 && data(off + 7) == 100 &&
            data(off + 8) == 101 && data(off + 9) == 110 &&
            data(off + 10) == 116 && data(off + 11) == 105 &&
            data(off + 12) == 102 && data(off + 13) == 105 &&
            data(off + 14) == 99 && data(off + 15) == 97 &&
            data(off + 16) == 116 && data(off + 17) == 105 &&
            data(off + 18) == 111 && data(off + 19) == 110
        }

        @inline def match_pos = {
          len >= 3 && data(off) == 112 && data(off + 1) == 111 &&
            data(off + 2) == 115
        }

        @inline def match_pos_len = {
          len == 3
        }

        @inline def match_position = {
          len == 8 && data(off + 3) == 105 && data(off + 4) == 116 &&
            data(off + 5) == 105 && data(off + 6) == 111 && data(off + 7) == 110
        }

        @inline def match_x = {
          len == 1 && data(off) == 120
        }

        @inline def match_y = {
          len == 1 && data(off) == 121
        }

        @inline def match_surveillance = {
          len == 12 && data(off) == 115 && data(off + 1) == 117 &&
            data(off + 2) == 114 && data(off + 3) == 118 &&
            data(off + 4) == 101 && data(off + 5) == 105 &&
            data(off + 6) == 108 && data(off + 7) == 108 &&
            data(off + 8) == 97 && data(off + 9) == 110 &&
            data(off + 10) == 99 && data(off + 11) == 101
        }

        @inline def match_a = {
          len >= 1 && data(off) == 97
        }

        @inline def match_altitude = {
          len == 8 && data(off + 1) == 108 && data(off + 2) == 116 &&
            data(off + 3) == 105 && data(off + 4) == 116 &&
            data(off + 5) == 117 && data(off + 6) == 100 &&
            data(off + 7) == 101
        }

        @inline def match_arrival = {
          len == 7 && data(off + 1) == 114 && data(off + 2) == 114 &&
            data(off + 3) == 105 && data(off + 4) == 118 &&
            data(off + 5) == 97 && data(off + 6) == 108
        }

        @inline def match_actual = {
          len == 6 && data(off + 1) == 99 && data(off + 2) == 116 &&
            data(off + 3) == 117 && data(off + 4) == 97 &&
            data(off + 5) == 108
        }

        @inline def match_departure = {
          len == 9 && data(off) == 100 && data(off + 1) == 101 &&
            data(off + 2) == 112 && data(off + 3) == 97 &&
            data(off + 4) == 114 && data(off + 5) == 116 &&
            data(off + 6) == 117 && data(off + 7) == 114 &&
            data(off + 8) == 101
        }

        @inline def match_estimated = {
          len == 9 && data(off) == 101 && data(off + 1) == 115 &&
            data(off + 2) == 116 && data(off + 3) == 105 &&
            data(off + 4) == 109 && data(off + 5) == 97 &&
            data(off + 6) == 116 && data(off + 7) == 101 &&
            data(off + 8) == 100
        }

        //--- added to RACE
        @inline def match_route = {
          len == 5 && data(off) == 114 && data(off + 1) == 111 &&
            data(off + 2) == 117 && data(off + 3) == 116 &&
            data(off + 4) == 101
        }

        @inline def match_aircraftDescription = {
          len == 19 && data(off) == 97 && data(off + 1) == 105 &&
            data(off + 2) == 114 && data(off + 3) == 99 &&
            data(off + 4) == 114 && data(off + 5) == 97 &&
            data(off + 6) == 102 && data(off + 7) == 116 &&
            data(off + 8) == 68 && data(off + 9) == 101 &&
            data(off + 10) == 115 && data(off + 11) == 99 &&
            data(off + 12) == 114 && data(off + 13) == 105 &&
            data(off + 14) == 112 && data(off + 15) == 116 &&
            data(off + 16) == 105 && data(off + 17) == 111 &&
            data(off + 18) == 110
        }
        //---

        if (match_flightIdentification) {
          process_flightIdentification
        } else if (match_pos) {
          if (match_pos_len) {
            process_pos
          } else if (match_position) {
            process_position
          }
        } else if (match_x) {
          process_x
        } else if (match_y) {
          process_y
        } else if (match_surveillance) {
          process_surveillance
        } else if (match_a) {
          if (match_altitude) {
            process_altitude
          } else if (match_arrival) {
            process_arrival
          } else if (match_actual) {
            process_actual
          }
          //--- added to RACE
          else if (match_aircraftDescription) {
            process_aircraftDescription
          }
          //---
        } else if (match_departure) {
          process_departure
        } else if (match_estimated) {
          process_estimated
        }
        //--- added to RACE
        else if (match_route) {
          if (tagHasParent(_agreed)) {
            process_route
          }
        }
        //---
      }
    }

    if (parseAttr(_centre)) src = attrValue.intern
    if (artccId == null) { // first src spec in this MessageCollection
      if (filterSrc(src)) return // bail out - not a relevant source

      artccId = src
      elements.src = src
    } else {
      if (artccId != src) throw new RuntimeException(s"conflicting 'centre' " +
        s"attributes: $artccId - $src")
    }

    parseElement(flight)

    if (cs != null) {
      val track = createSfdpsObject(id, cs, lat, lon,
        vx, vy, alt, spd, vr, date, arrivalPoint, departurePoint, arrivalDate,
        departureDate, status, src, route, flightRules, equipmentQualifier)

      if (!filterSrc(src) && (track != null && !filterTrack(track)))
        elements += track
    }
  }

  def createSfdpsObject(id: String, cs: String, lat: Double,
                        lon: Double, vx: Double, vy: Double, alt: Length,
                        spd: Speed, vr: Speed, date: DateTime,
                        arrivalPoint: String, departurePoint: String,
                        arrivalDate: DateTime, departureDate: DateTime,
                        status: Int, src: String, route: String,
                        flightRules: String, equipmentQualifier: String): SfdpsTrack
}

