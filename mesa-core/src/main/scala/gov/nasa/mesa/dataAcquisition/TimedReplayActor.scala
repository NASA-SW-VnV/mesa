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
import gov.nasa.race.actor.ReplayActor
import gov.nasa.race.uom.DateTime

import akka.actor.ActorRef
import com.typesafe.config.Config
import gov.nasa.race.archive.{ArchiveEntry, ArchiveReader}
import gov.nasa.race.common.Counter
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.{ClockAdjuster, ContinuousTimeRaceActor}
import gov.nasa.race.uom.DateTime

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * This class extends gov.nasa.race.core.ReplayActor with functionality that
  * measures the time took to replay the data.
  *
  * ReplayActor is an actor that reads time-stamped ArchiveEntry objects from
  * an archive with a path specified in the given config (as 'reader
  * .pathname'), and writes them in a channel specified in the given config
  * (as 'write-to').
  *
  * @param config the actor configuration
  */
class TimedReplayActor(override val config: Config)
  extends ReplayActor(config: Config) {

  /** Used to measure the elapsed time used to replay the archive. */
  var start: DateTime = _

  override def onStartRaceActor(originator: ActorRef): Boolean = {
    val result = super.onStartRaceActor(originator)
    start = DateTime.now
    result
  }

  /** It is supposed to be invoked when the end of archive is reached. It
    * closes the archive and prints the time took to replay the archive entries.
    */
  override def reachedEndOfArchive: Unit = {
   var end = DateTime.now
    super.reachedEndOfArchive
    val period = DateTime.timeBetween(start,end).toHMSms
    println(s"ReplayActor elapsed time: $period")
  }
}