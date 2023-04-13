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
package gov.nasa.mesa.reporting.stats

import java.io.{File, PrintWriter}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import com.typesafe.config.Config
import gov.nasa.mesa.reporting.stats.printers.{GlobalProfilerCSVPrinter,
  GlobalProfilerTabularPrinter}
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.uom.DateTime

/**
  * This object is used to collect stats including number of msgs, monitors,
  * and violations, start time, end time, and execution time.
  *
  * To use this functionality one needs to invoke the methods below to update
  * counters from appropriate points in the application specific
  * monitor/dispatcher actors:
  *
  * incErrorCounter()
  * incDispatchedMsgCount()
  * incMonitorCounter() // when a monitor is
  * incMsgCounter()
  *
  * This class can be used to terminate the execution by reaching the value of
  * the configurable variable maxMsgCount (value of the key "global-profiler
  * .trace-size" in the config file).
  *
  * How to configure GlobalStatsProfiler:
  * global-profiler {
  *   trace-size = // integer representing the size of the trace to be get
  *                // processed
  *   output-path = // path for the cvs files. If not set, cvs files do not
  *                 // get generated.
  *   print-on-console = // if set to true, it prints the results on console
  *   print-frequency = // incoming event frequency at which it prints results
  * }
  */
object GlobalStatsProfiler {
  println(s"Max Number of Process Avialable to VM: " +
    s"${Runtime.getRuntime.availableProcessors}")

  // frequency in terms of number of msgs used to print stats
  var printFrequency: Int = Int.MaxValue
  var maxMsgCount: Int = Int.MaxValue

  var start: DateTime = DateTime.Date0
  var end: DateTime = DateTime.Date0

  var monitoredMsgCount: AtomicInteger = new AtomicInteger(0)
  var dispatchedMsgCount: AtomicInteger = new AtomicInteger(0)
  var dispatchedMonitorCount: AtomicInteger = new AtomicInteger(0)
  var errorCount: AtomicInteger = new AtomicInteger(0)
  var terminationAction: () => Unit = () => {}
  var terminated = new AtomicBoolean(false)

  var consoleTabularPrinter: Option[GlobalProfilerTabularPrinter] =
    None: Option[GlobalProfilerTabularPrinter]
  var csvFilePrinter: Option[GlobalProfilerCSVPrinter] =
    None: Option[GlobalProfilerCSVPrinter]

  var activated = false
  /** Initializes the GlobalStatsProfiler object.
    *
    * @param config the actor system configuration
    * @param action a partial function representing the termination action
    */
  def initialize(config: Config, action: () => Unit): Unit = {
    activated = config.hasPath("global-profiler")
    maxMsgCount = config.getIntOrElse("global-profiler.trace-size", Int
      .MaxValue)
    printFrequency = config.getIntOrElse("global-profiler.print-frequency",
      1000)

    terminationAction = action

    initializePrinters(config)
  }

  /** Initializes the printers.
    *
    * @param config the actor system configuration
    */
  def initializePrinters(config: Config): Unit = {
    consoleTabularPrinter =
      if (config.getBooleanOrElse("global-profiler.print-on-console",
        fallback = false))
        Some(new GlobalProfilerTabularPrinter(new PrintWriter(System.out,
          true)))
      else None

    val outputPath = config.getOptionalString("global-profiler.output-path")
    csvFilePrinter =
      if (outputPath.nonEmpty) {
        val file = new File(outputPath.get)
        val dir = file.getParentFile
        if (!dir.isDirectory) dir.mkdirs
        Some(new GlobalProfilerCSVPrinter(new PrintWriter(file)))
      }
      else None
  }

  /** Increments the counter storing the number of errors.
    *
    * @return the updated number of errors.
    */
  def incErrorCounter: Int = errorCount.incrementAndGet

  /** Increments the counter storing the number of messages dispatched by the
    *  dispatcher(s).
    *
    * @return the updated number of dispatched messages.
    */
  def incDispatchedMsgCount: Unit = dispatchedMsgCount.incrementAndGet

  /** Increments the counter storing the number of monitors.
    *
    * @return the updated number of monitors.
    */
  def incMonitorCounter: Int = dispatchedMonitorCount.incrementAndGet

  /** Increments the counter storing the number of messages that have been
    * processed by actors. It also checks if the number of messages processed
    * reached the limit specified in the configuration using the key
    * 'trace-size', and if so, it starts the termination process of the system.
    */
  def incMsgCounter: Unit = {
    end = DateTime.now
    monitoredMsgCount.incrementAndGet

    if (notStarted) {
      start = DateTime.now
      printHeaders
    }

    if (processedAllMsgs && !alreadyTerminated) terminate
    else if (printNow) printResults

  }

  /** Checks if profiling has started.
    *
    * @return true if profiling has started, otherwise returns false.
    */
  def notStarted: Boolean = start == DateTime.Date0

  /** Checks if the number of dispatched messages reached the limit specified
    *  in the configuration using the key 'trace-size'.
    *
    * @return true if the number of dispatched messages reached the limit,
    *         otherwise returns false.
    */
  def dispatchedAllMsgs: Boolean = dispatchedMsgCount.get >= maxMsgCount

  /** Checks if the number of processed messages reached the limit specified
    *  in the configuration using the key 'trace-size'.
    *
    * @return true if the number of processed messages reached the limit,
    *         otherwise returns false.
    */
  def processedAllMsgs: Boolean = monitoredMsgCount.get >= maxMsgCount

  /** Checks if it is time to print the results according to the frequency
    * specified in the configuration using the key 'print-frequency'.
    *
    * @return true if it is time to print the results, otherwise returns false.
    */
  def printNow: Boolean = monitoredMsgCount.get % printFrequency == 0

  /** Checks if the termination process has started.
    *
    * @return true if the termination process has started, otherwise returns
    *         false.
    */
  def alreadyTerminated: Boolean = terminated.get

  /** Prints the results in a tabular format on the console. */
  def consolePrint: Unit = if (consoleTabularPrinter.isDefined)
    consoleTabularPrinter.get.printStats

  /** Prints the results in the csv format in a file. */
  def filePrint: Unit = if (csvFilePrinter.isDefined) csvFilePrinter.get
    .printStats

  /** Prints the results */
  def printResults: Unit = {
    consolePrint
    filePrint
  }

  /** Prints the headers for statistics captured by the global profiler. */
  def printHeaders: Unit = {
    if (consoleTabularPrinter.isDefined) consoleTabularPrinter.get.printHeader
    if (csvFilePrinter.isDefined) csvFilePrinter.get.printHeader
  }

  /** Starts the termination of the system. */
  def terminate: Unit = {
    printResults
    csvFilePrinter.get.close
    terminationAction.apply
    terminated.set(true)
  }
}