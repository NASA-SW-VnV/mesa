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
package gov.nasa.mesa.reporting.stats.printers

import java.io.PrintWriter

import akka.actor.ActorRef
import gov.nasa.mesa.reporting.stats.ActorStats

import scala.collection.mutable.Map

/**
  * This class represents a csv printer for writing individual actor statistics
  * in the comma-separated values (csv) format.
  *
  * @param pw a printer for formatted representations of objects to a
  *           text-output stream
  */
class ActorStatsCSVPrinter(pw: PrintWriter) {
  /** Prints the header in the csv format for individual actor statistics.
    */
  def printHeader = {
    pw.println("actor name," +
      "ave queue wait,var queue wait," +
      "ave queue size,var queue size," +
      "queued msg#," +
      "ave service time,var service time," +
      "processed msg#")
  }

  /** Prints actor statistics collected so far in the csv format.
    *
    * @param actorStats a map from actor references to objects storing their
    *                   corresponding statistics
    */
  def printStats(actorStats: Map[ActorRef, ActorStats]) = {
    actorStats.values.foreach(e => pw.println(
      s"${e.actorName}," +
        s"${e.aveQueueWait},${e.varQueueWait}," +
        s"${e.aveQueueSize},${e.varQueueSize}," +
        s"${e.numMsgQueued}," +
        s"${e.aveServiceTime},${e.varServiceTime}," +
        s"${e.numMsgProcessed}"))
  }

  /** Closes the stream and its associated resources.
    */
  def close = {
    pw.close
  }
}

/** This class represents a tabular printer for writing individual actor
  * statistics in a tabular format.
  *
  * @param pw a printer for formatted representations of objects to a
  *           text-output stream
  */
class ActorStatsTabularPrinter(val pw: PrintWriter) {
  /** Prints the header in a tabular format for individual actor statistics.
    */
  def printHeader = {
    pw.println("         actor name            " +
      "ave queue wait   var queue wait   " +
      "ave queue size    var queue size    " +
      "queued msg#    " +
      "ave service time   var service time   " +
      "processed msg#")
    pw.println("----------------------------   " +
      "--------------   --------------   " +
      "--------------   --------------   " +
      "-------------   " +
      "----------------   ----------------   " +
      "--------------")
  }

  /** Prints actor statistics collected so far in a tabular format.
    *
    * @param actorStats a map from actor references to objects storing their
    *                   corresponding statistics
    */
  def printStats(actorStats: Map[ActorRef, ActorStats]) = {
    actorStats.values.foreach(e => pw.println(
      f"${e.actorName}%-28s   " +
        f"${e.aveQueueWait}%14.3f   ${e.varQueueWait}%14.3f   " +
        f"${e.aveQueueSize}%14.3f   ${e.varQueueSize}%14.3f   " +
        f"${e.numMsgQueued}%13d   " +
        f"${e.aveServiceTime}%16.3f   ${e.varServiceTime}%16.3f   " +
        f"${e.numMsgProcessed}%14d"))
  }

  /** Closes the stream and its associated resources.
    */
  def close = {
    pw.close
  }
}