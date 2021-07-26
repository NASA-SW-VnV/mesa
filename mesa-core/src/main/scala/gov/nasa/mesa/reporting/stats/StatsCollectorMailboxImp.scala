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

import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{Envelope, MessageQueue, ProducesMessageQueue,
  UnboundedMessageQueueSemantics}
import akka.event.LoggerMessageQueueSemantics

/** This class represents a mailbox queue used to record information about the
  * mailbox such as its size, and the message entry time to and the exit time
  * from the mailbox. It also publishes a data container with the recorded
  * information to this actor system bus accessed by stat-collector.
  *
  * @param system the actor system
  */
class StatsQueue(val system: ActorSystem) extends MessageQueue
  with UnboundedMessageQueueSemantics with LoggerMessageQueueSemantics {

  private final val queue = new ConcurrentLinkedQueue[MsgEntryStats]()

  /** It wraps the given message, 'env', in a MsgEntryStats container
    * which also stores information about actor mailbox and the enqueue time,
    * and then it adds the container to the mailbox queue.
    *
    * @param receiver
    * @param env
    */
  def enqueue(receiver: ActorRef, env: Envelope): Unit = {
    val envelop = MsgEntryStats(
      queue.size() + 1,
      receiver,
      TimeUnit.NANOSECONDS.toMillis(System.nanoTime),
      env)
    queue.add(envelop)
  }

  /** It dequeues the next MsgEntryStats entry from the mailbox. It removes the
    * the entry from the mailbox, generates an instance of MailboxStats using
    * the information stored in the MsgEntryStats object, and retrieves the
    * original message to be processed by the actor.
    *
    * @return the original message to be processed by the actor.
    */
  def dequeue(): Envelope = {
    val msg = queue.poll

    if (msg != null) {
      msg.envelope.message match {
        case stat: MailboxStats => //skip message
        case _ => {
          val stat = MailboxStats(
            msg.queueSize,
            msg.receiver,
            msg.envelope.sender,
            msg.entryTime,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime))
          system.eventStream.publish(stat)
        }
      }
      // returning the original envelope back to Akka
      msg.envelope
    } else {
      null
    }
  }

  /** Checks if the mailbox queue is empty.
    *
    * @return true if the mailbox queue is not empty, and returns false if
    *         the queue is empty.
    */
  def hasMessages: Boolean = !queue.isEmpty

  /** Gets the number of messages waiting in the actor mailbox queue.
    *
    * @return the number of messages in the actor mailbox.
    */
  def numberOfMessages: Int = queue.size

  /** Invoked when the mailbox is disposed to transfer the remaining messages
    * in the queue to the dead letter queue.
    *
    * @param owner the actor to which the deadLetters mailbox belong to
    * @param deadLetters a dead letter queue including those messages that made
    *                    it to the actor mailboxes but never got processed by
    *                    the actors.
    */
  def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    if (hasMessages) {
      var envelope = dequeue
      while (envelope ne null) {
        deadLetters.enqueue(owner, envelope)
        envelope = dequeue
      }
    }
  }
}

import com.typesafe.config.Config

/** This class represents a factory class the creates a mailbox.
  *
  * @param settings the overall ActorSystem settings
  * @param config the configuration
  */
class StatsMailboxType(settings: ActorSystem.Settings, config: Config)
  extends akka.dispatch.MailboxType
    with ProducesMessageQueue[StatsQueue] {

  /** It creates a MessageQueue for the given actor which is one of the core
    * components in forming an Akka mailbox.
    *
    * @param owner the actor to which the new mailbox belongs
    * @param system the actor system to which the actor belongs
    * @return a queue of type MessageQueue for the actor mailbox.
    */
  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    system match {
      case Some(sys) =>
        new StatsQueue(sys)
      case _ =>
        throw new IllegalArgumentException("requires a system")
    }
  }
}

