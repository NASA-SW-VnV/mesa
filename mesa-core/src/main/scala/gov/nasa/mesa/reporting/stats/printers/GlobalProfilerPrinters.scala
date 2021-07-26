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

import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler._
import gov.nasa.race.uom.DateTime

/**
  * This class represents a csv printer for the global profiler. It is used
  * to write the statistics collected by the profiler in the comma-separated
  * values (csv) format.
  *
  * @param pw a printer for formatted representations of objects to a
  *           text-output stream
  */
class GlobalProfilerCSVPrinter(var pw: PrintWriter) {
  /** Prints the header in the csv format for statistics captured by the global
    * profiler.
    */
  def printHeader: Unit = {
    pw.println("dispatched monitor#,monitored msg#,error#,dispatched msg#," +
      "elapsed time")
  }

  /** Prints statistics captured by the global profiler in the csv format.
    */
  def printStats: Unit = {
    pw.println(s"${dispatchedMonitorCount.get}," +
      s"${monitoredMsgCount.get}," +
      s"${errorCount.get}," +
      s"${dispatchedMsgCount.get}," +
      s"${DateTime.timeBetween(start, end).toMillis}")
  }

  /** Closes the stream and its associated resources.
    */
  def close: Unit = {
    pw.close
  }
}

/**
  * This class represents a tabular printer for the global profiler. It is used
  * to write the statistics collected by the profiler in a tabular format.
  *
  * @param pw a printer for formatted representations of objects to a
  *           text-output stream
  */
class GlobalProfilerTabularPrinter(var pw: PrintWriter) {

  /** Prints the header in a tabular format for statistics captured by the
    * global profiler.
    */
  def printHeader: Unit = {
    pw.print(s"\n${Console.YELLOW}")
    pw.println("  dispatched monitor#   monitored msg#     error#     " +
      "dispatched msg#    elapsed time ")
    pw.println("  -------------------   --------------   ----------   " +
      "---------------   --------------")
    pw.println(s"${Console.RESET}")
  }

  /** Prints statistics captured by the global profiler in a tabular format.
    */
  def printStats: Unit = {
    pw.println(f"${dispatchedMonitorCount.get}%21d   " +
      f"${monitoredMsgCount.get}%14d   " +
      f"${errorCount.get}%10d   " +
      f"${dispatchedMsgCount.get}%15d     " +
      f"${DateTime.timeBetween(start, end).toHMSms}")
  }

  /** Closes the stream and its associated resources.
    */
  def close: Unit = {
    pw.close
  }
}
