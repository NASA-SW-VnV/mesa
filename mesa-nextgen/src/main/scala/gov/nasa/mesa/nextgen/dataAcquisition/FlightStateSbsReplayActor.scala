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
package gov.nasa.mesa.nextgen.dataAcquisition

import com.typesafe.config.Config
import gov.nasa.mesa.nextgen.core.FlightState
import gov.nasa.race.actor.Replayer
import gov.nasa.race.air.SbsUpdater
import gov.nasa.race.air.actor.{SBSReader, SbsImporter}
import gov.nasa.race.common.ConfigurableStreamCreator.{configuredPathName, createInputStream}
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.PeriodicRaceActor
import gov.nasa.race.track.{TrackDropped, TrackedObject}
import gov.nasa.race.uom.Time._
import gov.nasa.race.uom.{DateTime, Time}
import scala.concurrent.duration._

import java.io.InputStream
import java.time.ZoneId
import scala.concurrent.duration.Duration

/** Similar to SbsReplayActor in RACE, this class is a ReplayActor for SBS
  * text archives. The only difference is it uses FlightStateSbsReader
  * instead of SBSReader to replay the data as FlightState objects instead of
  * FlightPos.
  *
  * @param config the actor configuration
  */
class FlightStateSbsReplayActor(val config: Config)
  extends Replayer[FlightStateSbsReader] with PeriodicRaceActor
    with SbsImporter {
    class DropCheckSBSReader (conf: Config)
      extends FlightStateSbsReader(config) {
      override def dropTrack (id: String, cs: String, date: DateTime,
                              inactive: Time): Unit = {
        publish(TrackDropped(id,cs,date,Some(stationId)))
        info(s"dropping $id ($cs) at $date after $inactive")
      }
    }

    override def createReader = new DropCheckSBSReader(config)

    val dropAfter = Milliseconds(config.getFiniteDurationOrElse("drop-after",
      Duration.Zero).toMillis.toInt)
    override def startScheduler = if (dropAfter.nonZero) super.startScheduler
    override def defaultTickInterval = 30.seconds // wall clock time
    override def onRaceTick(): Unit = reader.dropStale(updatedSimTime,dropAfter)
}

/** This class is an archive reader (gov.nasa.race.archive.ArchiveReader) for
  * SBS text archives.
  */
class FlightStateSbsReader(iStream: InputStream, pathName: String="<unknown>",
                           bufLen: Int, defaultZone: ZoneId)
  extends SBSReader(iStream, pathName, bufLen, defaultZone) {

  def this(conf: Config) = this(createInputStream(conf),
    configuredPathName(conf),
    conf.getIntOrElse("buffer-size",4096),
    conf.getMappedStringOrElse("default-zone", ZoneId.of, ZoneId.systemDefault)
  )

  override val updater: SbsUpdater = new FstateSbsArchiveUpdater

  class FstateSbsArchiveUpdater extends SbsUpdater(updateTrack,dropTrack,
    Some(this), defaultZone) {
    override protected def acquireMoreData: Boolean = refillBuf
  }

  override def updateTrack (track: TrackedObject): Boolean = {
    val fstate = new FlightState(track.id, track.cs, track.position,
      track.speed, track.heading, track.vr, track.date, track.status)
    next = someEntry(fstate.date, fstate)
    false
  }
}