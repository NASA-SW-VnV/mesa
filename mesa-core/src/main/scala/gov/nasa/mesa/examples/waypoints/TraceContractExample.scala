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
package gov.nasa.mesa.examples.waypoints

import com.typesafe.config.Config
import gov.nasa.mesa.core.TraceContractMonitor

import scala.collection.JavaConverters._

//--------------------------------------------------------------------------//
//-  Different ways of specifying a property in TraceContract that checks  -//
//-  if a sequence of given waypoints are visited in the specified order   -//
//--------------------------------------------------------------------------//

/** Represents the event where the aerial vehicle reaches the waypoint wp
  */
case class Waypoint(wp: Long)

/** This checks if the sequences of given waypoints are visited in order.
  * This works for three waypoints. It uses 'strong', where no other waypoints
  * can be observed in between the waypoint in the given sequence.
  */
class WaypointsOrderMonitor_strong(config: Config) extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")
  val P1 = seq.get(0).toLong
  val P2 = seq.get(1).toLong
  val P3 = seq.get(2).toLong

  select { case Waypoint(_) => true}

  property {
    strong {
      case Waypoint(P1) =>
        strong {
          case Waypoint(P2) =>
            strong {
              case Waypoint(P3) =>
                ok("Property WaypointsOrderMonitor_strong is satisfied")
            }
        }
    }
  }
}

/** This checks if the sequences of given waypoints are visited in order.
  * This works for three waypoints. This is the ltl version of
  * WaypointsOrderMonitor_statelogic.
  */
class WaypointsOrderMonitor_ltl(config: Config)
  extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")
  val P1 = seq.get(0)
  val P2 = seq.get(1)
  val P3 = seq.get(2)

  property {
    not(Waypoint(P2) or Waypoint(P3)) until(Waypoint(P1) and
      strongnext(not(Waypoint(P1) or Waypoint(P3)) until(Waypoint(P2) and
        strongnext(not(Waypoint(P1) or Waypoint(P2)) until Waypoint(P3)))))
  }
}

/** This checks if the sequences of given waypoints are visited in order.
  * This works for three waypoints.
  */
class WaypointsOrderMonitor_statelogic(config: Config) extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")
  val P1 = seq.get(0).toLong
  val P2 = seq.get(1).toLong
  val P3 = seq.get(2).toLong

  property {
    state {
      case Waypoint(P2) | Waypoint(P3) => error
      case Waypoint(P1) =>
        state {
          case Waypoint(P1) | Waypoint(P3) => error
          case Waypoint(P2) =>
            state {
              case Waypoint(P1) | Waypoint(P2) => error
              case Waypoint(P3) => ok("Property WaypointsOrderMonitor_statelogic is satisfied")
            }
        }
    }
  }
}

/** This checks if the sequences of given waypoints are visited in order.
  * The number of waypoints is variable.
  */
class WaypointsOrderMonitor_variable_1(config: Config)
  extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")

  /**
    * The index of the next waypoint in the list seq, that is, seq.get(i) is
    * the next waypoint in the list to receive.
    */
  var i = 0

  property {
    waypointsOrderFormula
  }

  def waypointsOrderFormula: Formula =
    state {
      case Waypoint(p) if seq.contains(p) && seq.get(i)!=p =>
        error
      case Waypoint(p) if seq.contains(p) && seq.get(i)==p && i<seq.size-1 =>
        i = i+1
        waypointsOrderFormula
      case Waypoint(p) if seq.contains(p) && seq.get(i)==p && i==seq.size-1 =>
        ok("Property WaypointsOrderMonitor_variable_1 is satisfied")
    }
}

/** This checks if the sequences of given waypoints are visited in order.
  * The number of waypoints is variable. Note that this is an optimal
  * version of WaypointsOrderMonitor_variable_1
  */
class WaypointsOrderMonitor_variable(config: Config)
  extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")

  /**
    * The index of the next waypoint in the list seq, that is, seq.get(i) is the
    * next waypoint in the list to receive.
    */
  var i = 0

  // waypoints_ordering_variable_size
  property {
    waypointsOrderFormula(0)
  }

  def waypointsOrderFormula(i: Int): Formula =
    state {
      case Waypoint(p) if seq.contains(p) =>
        if (seq.get(i) != p)
          error
        else if (i == seq.size - 1) {
          ok("Property WaypointsOrderMonitor_variable is satisfied")
        } else
          waypointsOrderFormula(i + 1)
    }
}

/** This checks if the sequences of given waypoints are visited in order.
  * The number of waypoints is variable. Moreover, each waypoint can be
  * visited more than once.
  */
class WaypointsOrderMonitor_dup_1(config: Config)
  extends TraceContractMonitor(config) {

  // retrieving the list of waypoints from the config file
  val seq = config.getLongList("monitor.waypoints")

  /**
    * The index of the next waypoint in the list seq, that is, seq.get(i)
    * is the next waypoint in the list to receive.
    */
  var i = 0

  property {
    waypointsOrderFormula_1(0)
  }

  def waypointsOrderFormula_1(i: Int): Formula =
    state {
      case Waypoint(p) if seq.contains(p) =>
        if (seq.get(i) != p)
          if (i > 0 && seq.get(i - 1) == p)
            waypointsOrderFormula_1(i)
          else
            error
        else if (i == seq.size - 1) {
          ok("Property WaypointsOrderMonitor_dup_1 is satisfied")
        } else
          waypointsOrderFormula_1(i + 1)
    }
}

/** Allowing the same waypoint to occur multiple times.
  */
class WaypointsOrderMonitor_dup_2(config: Config)
  extends TraceContractMonitor(config) {

  val seq = config.getLongList("monitor.waypoints").asScala.toList.map(x => Long2long(x))

  property {
    waypointsOrderFormula_2(seq)
  }

  def waypointsOrderFormula_2(wps: List[Long]): Formula =
    state {
      case Waypoint(p) if seq.contains(p) =>
        wps match {
          case `p` :: _ => waypointsOrderFormula_2(wps)
          case _ :: `p` :: rest =>  waypointsOrderFormula_2(wps.tail)
          case _ => error
        }
    }
}
