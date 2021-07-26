package gov.nasa.mesa.nextgen.verification.monitors.daut

import com.typesafe.config.Config
import gov.nasa.mesa.core.DautMonitor
import gov.nasa.mesa.nextgen.core.FlightState
import gov.nasa.race.air.FlightPosHeadingChecker
import gov.nasa.race.config.ConfigUtils.ConfigWrapper
import gov.nasa.race.track.TrackedObject.TrackProblem
import gov.nasa.race.uom.Angle.Degrees


class FlightHeading_Monitor(config: Config) extends DautMonitor(config) {
    val maxHeadingChange = Degrees(config.getDoubleOrElse("max-heading-change", 45.0))


  /** A RACE checker that checks for inconsistencies in ADS-B data */
  val checker = new FlightPosHeadingChecker(config)

  always {
    case prevState@FlightState(_, cs, _, _, _, _, _, _) =>
      watch {
        case currState@FlightState(_, `cs`, _, _, _, _, _, _) =>
          checkForSbsInconsistency(currState, prevState)
      }
  }

  def checkForSbsInconsistency(currState: FlightState,
                               prevState: FlightState): Boolean = {
    val inconsistency = checker.checkPair(currState, prevState)
    if(inconsistency.isDefined) {
      error(reportViolation(inconsistency))
    }
    inconsistency.isEmpty
  }

  def reportViolation(inconsistency: Option[TrackProblem]): String = {
    val res = inconsistency.get
    val msg = new StringBuilder
    msg.append(s"--- ADS-B position inconsistency: ${res.problem}\n")
    msg.append(s"current: ${res.fpos}\n")
    msg.append(s"previous:    ${res.lastFpos}\n")
    msg.toString
  }
}
