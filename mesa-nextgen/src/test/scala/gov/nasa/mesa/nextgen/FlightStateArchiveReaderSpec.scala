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
package gov.nasa.mesa.nextgen

import gov.nasa.mesa.nextgen.core.{ExtendedFlightState, FlightStateArchiveReader}
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.uom.DateTime
import org.scalatest.wordspec.AnyWordSpecLike

import java.io.FileInputStream

/**
  * A test suite for gov.nasa.mesa.nextgen.core.FlightStateArchiveReader
  */
class FlightStateArchiveReaderSpec extends RaceActorSpec with AnyWordSpecLike {
  val fileName = "tracks-spec.txt"
  val path = getClass.getProtectionDomain.getCodeSource.getLocation.getPath + fileName

  "FlightStateArchiveReader" must {
    "generate archive entries encapsulating ExtendedFlightState objects" in {
      val fis = new FileInputStream(path)
      val reader: FlightStateArchiveReader = new FlightStateArchiveReader(fis, path)

      assert(reader.readNextEntry.get.msg.isInstanceOf[ExtendedFlightState])
      assert(reader.readNextEntry.get.msg.isInstanceOf[ExtendedFlightState])

      // reaching the end of the archive, so we should get None
      assert(reader.readNextEntry.isEmpty)
    }
  }

  "FlightStateArchiveReader" must {
    "generate ExtendedFlightState objects including the archived sfdps fields" in {
      val fis = new FileInputStream(path)
      val reader: FlightStateArchiveReader = new FlightStateArchiveReader(fis, path)
      val fstate = reader.readNextEntry.get.msg

      assert(fstate.isInstanceOf[ExtendedFlightState])

      assertResult("553")
      {fstate.asInstanceOf[ExtendedFlightState].id}

      assertResult(DateTime.ofEpochMillis(1676333740000L))
      {fstate.asInstanceOf[ExtendedFlightState].departureDate}

      assertResult("LICC./.CAM094024..ALB.V489.COATE..KTEB")
      {fstate.asInstanceOf[ExtendedFlightState].fplan.route}
    }
  }
}