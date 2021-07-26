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
package gov.nasa.mesa.nextgen

import java.io.File

import gov.nasa.mesa.nextgen.core.{FlightState, FlightTrack}
import gov.nasa.mesa.nextgen.dataProcessing.translators
.{Sfdps2FlightStateTranslator, Sfdps2FlightTrackTranslator, SfdpsFullTranslator}
import gov.nasa.race.air.TrackedAircraft
import gov.nasa.race.test.RaceActorSpec
import gov.nasa.race.util.FileUtils.fileContentsAsUTF8String
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer

/**
  * A test suite for gov.nasa.mesa.nextgen.dataProcessing.translators
  * .SfdpsParser.
  */
class SfdpsFlightTrackParserSpec extends RaceActorSpec with AnyWordSpecLike {

  "SfdpsFullTranslator translator" must {
    "generate FlightTrack and FlightState objects from MessageCollection" in {
      val path = getClass.getProtectionDomain.getCodeSource.getLocation.getPath
      val xmlMsg = fileContentsAsUTF8String(new File(path, "fixm.xml")).get

      val flightReg = "<flight ".r
      val numFlightMsg = flightReg.findAllIn(xmlMsg).size

      val translator = new SfdpsFullTranslator //SfdpsMesaParser
      val tiObjList = translator.translate(xmlMsg)

      tiObjList match {
        case Some(it: Iterable[TrackedAircraft]) =>
          println(it)
          assert(it.size == numFlightMsg)
        case _ =>
          fail(s"failed to generate FlightTrack objects from FIXM messages " +
            s"- result: $None")
      }
    }
  }

  "Sfdps2FlightTrackTranslator parser" must {
    "retrieve the correct values for the cooresponding fields of " +
      "FlightTrack" in {
      val path = getClass.getProtectionDomain.getCodeSource.getLocation.getPath
      val xmlMsg = fileContentsAsUTF8String(new File(path, "fixm.xml")).get

      val translator = new Sfdps2FlightTrackTranslator
      val flightTrack = translator.translate(xmlMsg).get.
        asInstanceOf[ArrayBuffer[FlightTrack]](0)

      val id = "253"
      val cs = "SWA3651"
      val route = "KBWI.TERPZ6.MAULS.Q40.AEX.WAPPL4.KHOU/0209"
      val departureProcedure = "TERPZ6"
      val departureTransition = "MAULS"
      val arrivalProcedure = "WAPPL4"
      val arrivalTranstion = "AEX"
      val departureAirport = "KBWI"
      val arrivalAirport = "KHOU"

      assert(flightTrack.id == id)
      assert(flightTrack.cs == cs)
      assert(flightTrack.fplan.route == route)
      assert(flightTrack.fplan.departure.get.name == departureProcedure)
      assert(flightTrack.fplan.departure.get.transition == departureTransition)
      assert(flightTrack.fplan.arrival.get.name == arrivalProcedure)
      assert(flightTrack.fplan.arrival.get.transition == arrivalTranstion)
      assert(flightTrack.departurePoint == departureAirport)
      assert(flightTrack.arrivalPoint == arrivalAirport)
    }
  }

  "Sfdps2FlightStateTranslator parser" must {
    "retrieve the correct values for the cooresponding fields of " +
      "FlightState" in {
      val path = getClass.getProtectionDomain.getCodeSource.getLocation.getPath
      val xmlMsg = fileContentsAsUTF8String(new File(path, "fixm.xml")).get

      val translator = new Sfdps2FlightStateTranslator
      val flightTrack = translator.translate(xmlMsg).get.
        asInstanceOf[ArrayBuffer[FlightState]](0)

      val id = "760"
      val cs = "N925EM"
      val status = 0

      assert(flightTrack.id == id)
      assert(flightTrack.cs == cs)
      assert(flightTrack.status == status)
    }
  }
}
