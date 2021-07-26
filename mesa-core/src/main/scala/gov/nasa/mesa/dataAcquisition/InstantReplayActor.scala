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
package gov.nasa.mesa.dataAcquisition

import akka.actor.ActorRef
import com.typesafe.config.Config
import gov.nasa.race.actor.FilteringPublisher
import gov.nasa.race.archive.ArchiveReader
import gov.nasa.race.common.Counter
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.{ClockAdjuster, ContinuousTimeRaceActor}
import gov.nasa.race.uom.DateTime

import scala.language.postfixOps

/** This class is a modified version of gov.nasa.race.actor.ReplayActor. It is
  * an actor that reads ArchiveEntry objects from an archive with a path
  * specified in the given config ("reader.pathname"), and writes them in
  * a channel specified in the given config ("write-to"). Unlike ReplayActor
  * in RACE, this class ignores the ArchiveEntry object time-stamps, and replays
  * messages as it reads them.
  *
  * @param config the actor configuration
  */
class InstantReplayActor(override val config: Config) extends
  ContinuousTimeRaceActor with FilteringPublisher with Counter with
  ClockAdjuster {

  /** This case class defines the type of messages handled by this actor. It
    * wraps each message read from an archive (msg) in an instance of Replay
    * and forwards it to itself.
    *
    * @param msg a message
    */
  case class Replay(msg: Any)

  val counterThreshold: Int = config.getIntOrElse("break-after", 20)
  val delayMillis: Int = config.getIntOrElse("delay-millis", 0)

  val reader: ArchiveReader = createReader
  var noMoreData: Boolean = !reader.hasMoreData

  /** Creates an instance of gov.nasa.race.archive.ArchiveReader to read
    * ArchiveEntry objects from the given archive.

    * @return a new instance of ArchiveReader used to read data from an archive.
    */
  def createReader: ArchiveReader = getConfigurable[ArchiveReader]("reader")

  if (noMoreData) {
    warning(s"no data for ${reader.pathName}")
  } else {
    info(s"initializing replay of ${reader.pathName} starting at $simTime")
  }

  override def onStartRaceActor(originator: ActorRef): Boolean = {
    val result = super.onStartRaceActor(originator) && scheduleFirst
    start = DateTime.now
    result
  }

  /** Called when terminating this actor.
    *
    * @param originator the actor sending the termination request
    * @return true if the termination is successful, otherwise returns false.
    */
  override def onTerminateRaceActor(originator: ActorRef): Boolean = {
    reader.close
    noMoreData = true
    super.onTerminateRaceActor(originator)
  }

  /** Defines the core behavior of this actor. This method is of type of
    * partial function Receive (PartialFunction[Any, Unit]) that defines
    * actor response to received messages.
    *
    * @return a partial function with the InstantReplayActor logic.
    */
  override def handleMessage: Receive = {
    case Replay(msg) =>
      publishFiltered(msg)
      scheduleNext
  }

  /** A helper method that wraps the message msg, read from the archive, into
    * a Replay instance and sends it to itself.
    *
    * @param msg the message to be replayed
    * @param date the message timestamp
    */
  def replayMessageNow(msg: Any, date: DateTime): Unit = {
    self ! Replay(msg)
  }

  /** Used to measure the elapsed time used to replay the archive. */
  var start: DateTime = _

  /** Reads the first message from the archive and sends to itself to get
    * replayed by handleReplayMessage.
    *
    * @return true if it successfully obtains an entry from the archive, or
    *         false if there are no more archived messages to read.
    */
  final def scheduleFirst: Boolean = {
    reader.readNextEntry match {
      case Some(e) =>
        val date = e.date
        val msg = e.msg
        replayMessageNow(msg, date)
        true
      case None => // no more archived messages
        reachedEndOfArchive
        false
    }
  }

  // store the last message read from the archive.
  var lastMsg: Any = _
  var i = 0

  /** Reads the next message from the archive and sends to itself to get
    * replayed by handleReplayMessage.
    */
  def scheduleNext: Unit = {
    Thread.sleep(delayMillis)
    if (!noMoreData) {
      reader.readNextEntry match {
        case Some(e) =>
          val date = e.date
          val msg = e.msg
          replayMessageNow(msg, date)
          lastMsg = msg
          i = i + 1
        case None => reachedEndOfArchive
      }
    }
  }

  /** Used when the actor reaches the end of the archive. It closes the reader
    * (ArchiveReader) and prints out log info along with the elapsed time used
    * to go through the entire archive.
    */
  def reachedEndOfArchive: Unit = {
    val end = DateTime.now
    info(s"reached end of replay stream ${reader.pathName}   Last " +
      s"Msg: $lastMsg\n\n#msg: $i")
    reader.close
    noMoreData = true
    val period = DateTime.timeBetween(start, end).toHMSms
    println(s"InstantReplayActor elapsed time: $period")
  }
}


