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

import com.typesafe.config.Config
import gov.nasa.race.archive.{ArchiveEntry, ArchiveReader, ArchiveWriter}
import gov.nasa.race.common.ConfigurableStreamCreator.{configuredPathName, createInputStream, createOutputStream}
import gov.nasa.race.config.ConfigUtils.ConfigWrapper
import gov.nasa.race.geo.GeoPosition
import gov.nasa.race.track.TrackedObject
import gov.nasa.race.uom.Angle.Degrees
import gov.nasa.race.uom.DateTime
import gov.nasa.race.uom.Length.Feet
import gov.nasa.race.uom.Speed.{FeetPerMinute, UsMilesPerHour}
import gov.nasa.race.util.InputStreamLineTokenizer

import java.io.{InputStream, OutputStream, PrintStream}

/** An ExtendedSfdpsTrack archiver that writes/parses ExtendedSfdpsTrack objects to text lines.
  * This class is used with ArchiveActor - the messages read by the actor are archived by
  * ExtendedSfdpsTrackArchiveWriter.
  *
  * To see the usage, check out: mesa/config/sfdps-archive-writer.conf
  *
  * @param oStream an output stream that accepts bytes
  * @param pathName a path for the generated archive including ExtendedSfdpsTrack fields
  * @param manipulateData a parameter that if set to true in the config file, part of the data
  *                       obtained from SFDPS messages is modified to conceal the original data,
  *                       including date, id, and callsign fields. The default value is false.
  *
  * @param excludeInvalidDates a parameter that if set to true in the config file, the messages
  *                            with undefined date are ignored. Note that by activating this
  *                            option, messages for completed flights are all skipped. The default
  *                            value is "false".
  */
class ExtendedSfdpsTrackArchiveWriter (val oStream: OutputStream, val pathName: String="<unknown>",
                                       val manipulateData: Boolean, val excludeInvalidDates: Boolean) extends ArchiveWriter {
  def this(conf: Config) = this(createOutputStream(conf), configuredPathName(conf),
    conf.getBooleanOrElse("manipulateData", false), conf.getBooleanOrElse("excludeInvalidDates", false))


  val ps = new PrintStream (oStream)
  override def close(): Unit = ps.close

  /** Modifies the given date if the manipulateData parameter is set to true in the config file.
    *
    * @param date a date
    * @return a modified date if manipulateData is set to true and date is defined, otherwise,
    *         returns the given date.
    */
  def dateModifier(date: DateTime): Long = {
    if(manipulateData && date.isDefined)
      date.toEpochMillis + 5*320*24*60*60*1000L
    else
      date.toEpochMillis
  }

  /** Modifies the given callsign if the manipulateData parameter is set to true in the config file.
    *
    * @param cs a flight callsign
    * @return a modified callsign if manipulateData is set to true and the callsign length is more than
    *         2, otherwise, returns the given callsign.
    */
  def callsignModifier(cs: String): String = {
    if(manipulateData && cs.length>2) {
      cs.substring(0,cs.length-2) + (cs.charAt(cs.length-1).asDigit*10 + cs.charAt(cs.length-2).asDigit + 5)
    } else
      cs
  }

  /** Modifies the given id if the manipulateData parameter is set to true in the config file.
    *
    * @param id a flight id
    * @return a modified id if manipulateData is set to true and the id length is more than
    *         2, otherwise, returns the given id.
    */
  def idModifier(id: String): String = {
    if (manipulateData && id.length>2)
      (id.charAt(2).asDigit*100 + id.charAt(1).asDigit*10 + id.charAt(0).asDigit + 4).toString
    else id
  }

  /** It writes the fields of ExtendedSfdpsTrack objects to the output stream
    *
    * @param track an object capturing a SFDPS message
    */
  protected def writeExtendedSfdpsTrack(track: ExtendedSfdpsTrack): Unit = {
    ps.print(idModifier(track.id)); ps.print(',')
    ps.print(callsignModifier(track.cs)); ps.print(',')

    val pos = track.position
    ps.print(pos.φ.toDegrees); ps.print(',')
    ps.print(pos.λ.toDegrees); ps.print(',')
    ps.print(pos.altitude.toFeet); ps.print(',')

    ps.print(track.speed.toUsMilesPerHour); ps.print(',')
    ps.print(track.heading.toDegrees); ps.print(',')
    ps.print(track.vr.toFeetPerMinute); ps.print(',')
    ps.print(dateModifier(track.date)); ps.print(',')
    ps.print(track.status); ps.print(',')

    ps.print(track.src); ps.print(',')
    ps.print(track.departurePoint); ps.print(',')
    ps.print(dateModifier(track.departureDate)); ps.print(',')
    ps.print(track.arrivalPoint); ps.print(',')
    ps.print(dateModifier(track.arrivalDate)); ps.print(',')

    ps.print(track.route); ps.print(',')
    ps.print(track.flightRules); ps.print(',')
    ps.print(track.equipmentQualifier);
  }

  override def write(date: DateTime, obj: Any): Boolean = {
    obj match {
      case track: ExtendedSfdpsTrack =>
        if(excludeInvalidDates && track.date.isUndefined)
          false
        else {
          ps.print(date.toEpochMillis)
          ps.print(',')
          writeExtendedSfdpsTrack(track)
          ps.println()
          true
        }
      case _ => false
    }
  }
}

/** Reads ExtendedSfdpsTrack fields from an archive and returns ExtendedFlightState and
  * FlighStateCompleted.
  *
  * To see usage check out: "mesa/config/sfdps-archive-reader.conf"
  *
  * @param iStream an input stream where it reads the ExtendedSfdpsTrack files from
  * @param pathName a path for the archive where this class reads from
  */
class FlightStateArchiveReader (val iStream: InputStream, val pathName: String="<unknown>")
  extends ArchiveReader with InputStreamLineTokenizer {
  def this(conf: Config) = this(createInputStream(conf), configuredPathName(conf))

  def hasMoreArchivedData = iStream.available > 0
  def close(): Unit = iStream.close

  /** Reads the next entry from the archive.
    *
    * @return an Option of ArchiveEntry which encapsulate an ExtendedFlightState object.
    */
  override def readNextEntry(): Option[ArchiveEntry] = {
    var fs = getLineFields(iStream)

    if (fs.size == 19) {
      try {
        val recDt = fs.head.toLong; fs = fs.tail

        val id = fs.head.intern; fs = fs.tail
        val cs = fs.head.intern; fs = fs.tail

        val phi = fs.head.toDouble; fs = fs.tail
        val lambda = fs.head.toDouble; fs = fs.tail
        val alt = fs.head.toDouble; fs = fs.tail
        val speed = fs.head.toDouble; fs = fs.tail
        val heading = fs.head.toDouble; fs = fs.tail
        val vr = fs.head.toDouble; fs = fs.tail

        val date = getDate(DateTime.ofEpochMillis(fs.head.toLong)); fs = fs.tail
        val status = fs.head.toInt; fs = fs.tail

        val src = fs.head.intern; fs = fs.tail
        val departurePoint = fs.head.intern; fs = fs.tail
        val departureDate = getDate(DateTime.ofEpochMillis(fs.head.toLong)); fs = fs.tail
        val arrivalPoint = fs.head.intern; fs = fs.tail
        val arrivalDate = getDate(DateTime.ofEpochMillis(fs.head.toLong)); fs = fs.tail

        val route = fs.head.intern; fs = fs.tail
        val flightRules = fs.head.intern; fs = fs.tail
        val equipmentQualifier = fs.head.intern; fs = fs.tail

        val departureProcedure = FlightPlan.getDepartureProcedure(route)
        val arrivalProcedure = FlightPlan.getArrivalProcedure(route)
        val flightPlan = new FlightPlan(cs, route, departureProcedure, arrivalProcedure, flightRules)

        if((status & TrackedObject.CompletedFlag)!=0)
          archiveEntry(DateTime.ofEpochMillis(recDt), new FlightStateCompleted(id, cs, GeoPosition(Degrees(phi), Degrees(lambda), Feet(alt)),
            UsMilesPerHour(speed), Degrees(heading), FeetPerMinute(vr), date, status, src, departurePoint, departureDate,
            arrivalPoint, arrivalDate))
        else
          archiveEntry(DateTime.ofEpochMillis(recDt), new ExtendedFlightState(id, cs, GeoPosition(Degrees(phi), Degrees(lambda), Feet(alt)),
          UsMilesPerHour(speed), Degrees(heading), FeetPerMinute(vr), date, status, src, departurePoint, departureDate,
          arrivalPoint, arrivalDate, flightPlan, equipmentQualifier))
        } catch {
        case x: Throwable => None
      }
    } else None
  }
}