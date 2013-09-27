package com.github.gsnewmark.collab_env

import jade.core.{AID, Agent}
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.{ACLMessage, MessageTemplate}

/**
 * Represents a Session of the environment.
 *
 * Serves as a container for Users. First joined User becomes master, all
 * others are slaves.
 *
 * Ensures that master can't leave until there are some slaves left.
 */
class SessionAgent extends Agent with ServiceAgent {
  val serviceName: String = Env.sessionServiceName
  val serviceType: String = Env.sessionServiceType
  val initialBehaviours = new RequestHandler :: Nil

  /** Processes join session messages. */
  class RequestHandler extends CyclicBehaviour {
    private val mt: MessageTemplate =
      MessageTemplate.MatchPerformative(ACLMessage.REQUEST)

    private var master: Option[AID] = None
    private var slaves: Set[AID] = Set()

    def action() {
      val msg: ACLMessage = myAgent.receive(mt)
      if (msg != null) {
        val reply = msg.createReply()
        val sender = msg.getSender()
        msg.getContent() match {
          case Env.joinRequest =>
            reply.setPerformative(ACLMessage.AGREE)
            if (master.isEmpty) {
              master = Some(sender)
              reply.setContent(Env.masterRole)
              println(s"Master joined ${myAgent.getName()}: ${sender.getName()}")
            } else {
              slaves = slaves + sender
              reply.setContent(Env.slaveRole)
              println(s"Slave joined ${myAgent.getName()}: ${sender.getName()}. Current number of slaves: ${slaves.size}")
            }
          case Env.leaveRequest =>
            if (!master.isEmpty && master.get == sender && !slaves.isEmpty) {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"Master ${master.get.getName()} can't leave ${myAgent.getName()} because slaves exist")
            } else {
              if (master.get == sender) {
                master = None
                println(s"Master left ${myAgent.getName()}: ${sender.getName()}")
              } else {
                slaves = slaves - sender
                println(s"Slave left ${myAgent.getName()}: ${sender.getName()}. Slaves left: ${slaves.size}")
              }
              reply.setPerformative(ACLMessage.AGREE)
            }
        }
        myAgent.send(reply)
      } else {
        block()
      }
    }
  }
}
