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
package gov.nasa.mesa.nextgen.dataProcessing.filters

import com.typesafe.config.Config
import gov.nasa.mesa.nextgen.core.FlightTrack
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.config.ConfigurableFilter

/** This class represents a filter for FlightTrack objects. It allows actors to
  * filter out FlightTracks based on the setting specified in the filter
  * configuration.
  *
  * @param config the actor configuration
  */
class FlightTrackFilter(val config: Config) extends ConfigurableFilter {

  val emptySeq = Seq.empty[String]

  // ids
  val id: Seq[String] = config.getStringListOrElse("airportId", emptySeq)
  val cs: Seq[String] = config.getStringListOrElse("cs", emptySeq)

  // airports
  val departurePoint: Seq[String] =
    config.getStringListOrElse("departure-point", emptySeq)
  val arrivalPoint: Seq[String] =
    config.getStringListOrElse("arrival-point", emptySeq)

  // equipments
  val flightRules: Seq[String] =
    config.getStringListOrElse("flight-rules", emptySeq)
  val equipmentQualifier: Seq[String] =
    config.getStringListOrElse("equipment-qualifier", emptySeq)

  // procedures
  val departureProcedure: Seq[String] =
    config.getStringListOrElse("departure-procedure", emptySeq)
  val departureTransition: Seq[String] =
    config.getStringListOrElse("departure-transition", emptySeq)
  val arrivalProcedure: Seq[String] =
    config.getStringListOrElse("arrival-procedure", emptySeq)
  val arrivalTransition: Seq[String] =
    config.getStringListOrElse("arrival-transition", emptySeq)

  /** Filters out those FlightTrack object that do not match specifications
    * in the actor configuration.
    *
    * @param o a message
    * @return true if the message is of type of FlightTrack and matches the
    *         specifications in the configuration file, otherwise returns false.
    */
  override def pass(o: Any): Boolean = {
    o match {
      case flightTrack: FlightTrack => matchConfig(flightTrack)
      case _ => false
    }
  }

  /** Matches the given FlightTrack with the config setting. FlightTracks that
    * doesn't match the setting are getting filtered.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the FlightTrack object matches the specifications in the
    *         configuration file, otherwise returns false.
    */
  def matchConfig(flightTrack: FlightTrack): Boolean = {
    // match ids
    matchId(flightTrack) && matchCs(flightTrack) &&
    // match airports
    matchDeparturePoint(flightTrack) && matchArrivalPoint(flightTrack) &&
    // match equipments
    matchFlightRules(flightTrack) && matchEquipmentQualifier(flightTrack) &&
    // match departure procedures
    matchDepartureProcedure(flightTrack) && matchDepartureTransition(flightTrack) &&
    // match arrival procedures
    matchArrivalProcedure(flightTrack) && matchArrivalTransition(flightTrack)
  }

  //--- auxiliary matching methods

  /** Checks if the flight id matches the specification in the configuration
    * file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight id matches the specification in the
    *         configuration file, otherwise returns false.
    */
  def matchId(flightTrack: FlightTrack): Boolean = {
    id.isEmpty || id.contains(flightTrack.id)
  }

  /** Checks if the flight call sign matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight call sign matches the specification in the
    *         configuration file, otherwise returns false.
    */
  def matchCs(flightTrack: FlightTrack): Boolean = {
    cs.isEmpty || cs.contains(flightTrack.cs)
  }

  /** Checks if the flight departure airport matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight departure airport matches the specification in
    *         the configuration file, otherwise returns false.
    */
  def matchDeparturePoint(flightTrack: FlightTrack): Boolean = {
    departurePoint.isEmpty || departurePoint.contains(flightTrack.departurePoint)
  }

  /** Checks if the flight arrival airport matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight arrival airport matches the specification in
    *         the configuration file, otherwise returns false.
    */
  def matchArrivalPoint(flightTrack: FlightTrack): Boolean = {
    arrivalPoint.isEmpty || arrivalPoint.contains(flightTrack.arrivalPoint)
  }

  /** Checks if the flight rules matches the specification in the configuration
    * file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight rules matches the specification in the
    *         configuration file, otherwise returns false.
    */
  def matchFlightRules(flightTrack: FlightTrack): Boolean = {
    flightRules.isEmpty || flightRules.contains(flightTrack.flightRules)
  }

  /** Checks if the flight equipment qualifier matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight equipment qualifier matches the specification
    *         in the configuration file, otherwise returns false.
    */
  def matchEquipmentQualifier(flightTrack: FlightTrack): Boolean = {
    equipmentQualifier.isEmpty ||
      equipmentQualifier.contains(flightTrack.equipmentQualifier)
  }

  /** Checks if the flight departure procedure matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight departure procedure matches the specification
    *         in the configuration file, otherwise returns false.
    */
  def matchDepartureProcedure(flightTrack: FlightTrack): Boolean = {
    departureProcedure.isEmpty ||
      flightTrack.fplan.hasDepartureProcedure &&
        departureProcedure.contains(flightTrack.fplan.departure.get.name)
  }

  /** Checks if the flight departure transition matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight departure transition matches the specification
    *         in the configuration file, otherwise returns false.
    */
  def matchDepartureTransition(flightTrack: FlightTrack): Boolean = {
    departureTransition.isEmpty ||
      flightTrack.fplan.hasDepartureProcedure &&
        departureTransition.contains(flightTrack.fplan.departure.get.transition)
  }

  /** Checks if the flight arrival procedure matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight arrival procedure matches the specification
    *         in the configuration file, otherwise returns false.
    */
  def matchArrivalProcedure(flightTrack: FlightTrack): Boolean = {
    arrivalProcedure.isEmpty ||
      flightTrack.fplan.hasArrivalProcedure &&
        arrivalProcedure.contains(flightTrack.fplan.arrival.get.name)
  }

  /** Checks if the flight arrival transition matches the specification in the
    * configuration file.
    *
    * @param flightTrack an object storing the flight track information
    * @return true if the flight arrival transition matches the specification
    *         in the configuration file, otherwise returns false.
    */
  def matchArrivalTransition(flightTrack: FlightTrack): Boolean = {
    arrivalTransition.isEmpty ||
      flightTrack.fplan.hasArrivalProcedure &&
        arrivalTransition.contains(flightTrack.fplan.arrival.get.transition)
  }
}
