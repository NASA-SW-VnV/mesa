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
package gov.nasa.mesa.core

import com.typesafe.config.Config
import daut.DautOptions
import gov.nasa.mesa.reporting.stats.GlobalStatsProfiler

/** The root type for all MESA monitors which is used as a mixin by the classes
  * with property specification capabilities.
  */
trait MesaMonitor {
  def verifyEvent(event: Any): Unit
}

/** This class is used to integrate the TraceContract DSL into MESA.
  *
  * tracecontract.Monitor is the key class in TraceContract which provides the
  * capabilities for specifying properties.
  *
  * TraceContract: https://github.com/havelund/tracecontract
  *
  * @param config the configuration
  */
class TraceContractMonitor(val config: Config) extends
  tracecontract.Monitor[Any] with MesaMonitor {

  setPrint(false)

  def verifyEvent(event: Any): Unit = {
    val before = getMonitorResult.numberOfErrors
    verify(event)
    val after = getMonitorResult.numberOfErrors
    if (after > before) GlobalStatsProfiler.incErrorCounter
  }
}

/** This class is used to integrate the Daut DSL into MESA.
  *
  * daut.Monitor is the key class in Daut which provides the capabilities for
  * specifying properties.
  *
  * Daut: https://github.com/havelund/daut
  *
  * @param config the configuration
  */
class DautMonitor(val config: Config) extends daut.Monitor[Any]
  with MesaMonitor {

  DautOptions.DEBUG = false
  DautOptions.PRINT_ERROR_BANNER = false

  def verifyEvent(event: Any): Unit = {
    val before = getErrorCount
    verify(event)
    val after = getErrorCount
    if (after > before) GlobalStatsProfiler.incErrorCounter
  }
}
