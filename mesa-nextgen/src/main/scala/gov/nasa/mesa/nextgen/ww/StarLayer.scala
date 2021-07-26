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
package gov.nasa.mesa.nextgen.ww

import java.awt.{Color, Font}

import com.typesafe.config.Config
import gov.nasa.mesa.nextgen.core.rnav.RnavStar
import gov.nasa.mesa.nextgen.core.{Airport, Waypoint}
import gov.nasa.race.air.{AirLocator, TrackedAircraft}
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.swing.Style._
import gov.nasa.race.swing.{IdAndNamePanel, StaticSelectionPanel}
import gov.nasa.race.uom.Length
import gov.nasa.race.uom.Length._
import gov.nasa.race.ww._
import gov.nasa.race.ww.track._
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.render._

/** Encapsulates a STAR waypoint to get displayed in worldwind.
  */
class WaypointSymbol(val waypoint: Waypoint, val layer: StarLayer)
  extends PointPlacemark(wwPosition(waypoint.position)) with RaceLayerPickable {

  var showDisplayName = false
  var attrs = new PointPlacemarkAttributes
  setLabelText(waypoint.id)
  setAltitudeMode(WorldWind.RELATIVE_TO_GROUND)
  attrs.setImage(null)
  attrs.setLabelColor(layer.waypointLabelColor)
  attrs.setLineColor(layer.waypointLabelColor)
  attrs.setLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 14))
  attrs.setUsePointAsDefaultImage(true)
  attrs.setScale(5d)
  setAttributes(attrs)

  override def layerItem: AnyRef = waypoint
}

/** A WorldWind layer that displays STAR waypoints.
  *
  * @param raceViewer a viewer state facade implemented in RACE which is
  *                   executing in the UI thread
  * @param config the configuration
  */
class StarLayer(val raceViewer: RaceViewer, val config: Config)
  extends ModelTrackLayer[TrackedAircraft] with AirLocator {

  val waypointLabelColor: String =
    toABGRString(config.getColorOrElse("waypoint-color", Color.white))

  // feet above ground
  val gotoAltitude: Length = Feet(config.getDoubleOrElse("goto-altitude",
    1500000d))

  val airportId: String = config.getString("airport")

  val starName: String = config.getString("star")

  val star: Option[RnavStar] = Airport.getRnavStar(airportId, starName)

  override def defaultColor: Color = Color.green

  override def defaultSubLabelFont = new Font(Font.MONOSPACED, Font.PLAIN, 11)

  override def defaultLabelThreshold: Length = Meters(600000.0)

  override def defaultIconThreshold: Length = Meters(200000.0)

  var selectedWaypoint: Option[Waypoint] = None

  showWaypointSymbols

  override def createLayerInfoPanel:
  InteractiveLayerInfoPanel[TrackEntry[TrackedAircraft]] = {
    new InteractiveLayerInfoPanel(this) {
      contents.insert(1, new StaticSelectionPanel[Waypoint,
        IdAndNamePanel[Waypoint]]("select Waypoint", Waypoint.NoWaypoint +:
        Airport.getRnavStar(config.getString("airport"),
          config.getString("star")).get.waypoints, 100,
        new IdAndNamePanel[Waypoint](_.id, _ => null), selectWaypoint).styled())
    }.styled("consolePanel")
  }

  def showWaypointSymbol(waypoint: Waypoint): Unit = {
    addRenderable(new WaypointSymbol(waypoint, this))
  }

  def showWaypointSymbols =
    if (star.isDefined) star.get.waypoints.foreach(showWaypointSymbol)

  def selectWaypoint(waypoint: Waypoint): Unit = {}
    //raceViewer.trackUserAction(gotoWaypoint(waypoint))

  def reset(): Unit = {
    selectedWaypoint = None
    clearTrackEntries
    releaseAll
    showWaypointSymbols
  }

  def setWaypoint(waypoint: Waypoint): Unit = {
    raceViewer.panTo(wwPosition(waypoint.position), gotoAltitude.toMeters)

    selectedWaypoint = Some(waypoint)
    requestTopic(selectedWaypoint)
  }

  def gotoWaypoint(waypoint: Waypoint): Unit = {
    if (waypoint == Waypoint.NoWaypoint) {
      reset
    } else {
      selectedWaypoint match {
        case Some(`waypoint`) =>
        case Some(lastTracon) =>
          reset
          setWaypoint(waypoint)
        case None =>
          setWaypoint(waypoint)
      }
    }
  }
}
