package com.github.gsnewmark.collab_env

import jade.core.{ AID, Agent }
import jade.core.behaviours.CyclicBehaviour
import jade.domain.{ DFService, FIPAException }
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription,
  ServiceDescription
}
import jade.lang.acl.{ ACLMessage, MessageTemplate }

/**
 * Represents a Session of the environment.
 *
 * Serves as a container for Users. First joined User becomes master, all
 * others are slaves.
 *
 * Ensures that master can't leave until there are some slaves left.
 */
class SessionAgent extends Agent {
  protected override def setup() = {
    println(s"Session was created: ${getAID().getName()}")

    val dfd: DFAgentDescription = new DFAgentDescription
    dfd.setName(getAID())
    val sd: ServiceDescription = new ServiceDescription()
    sd.setName(Env.sessionServiceName)
    sd.setType(Env.serviceType)
    dfd.addServices(sd)
    try {
      DFService.register(this, dfd)
    } catch {
      case fe: FIPAException => fe.printStackTrace()
    }
    addBehaviour(new RequestHandler)
  }

  protected override def takeDown() = {
    try {
      DFService.deregister(this)
    } catch {
      case fe: FIPAException => fe.printStackTrace()
    }
    println(s"Session ${getAID().getName()} terminated.");
  }

  /** Processes join session messages. */
  private class RequestHandler extends CyclicBehaviour {
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
              println(s"Master joined: ${sender.getName()}")
            } else {
              slaves = slaves + sender
              reply.setContent(Env.slaveRole)
              println(s"Slave joined: ${sender.getName()}. Current number of slaves: ${slaves.size}")
            }
          case Env.leaveRequest =>
            if (!master.isEmpty && master.get == sender && !slaves.isEmpty) {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"Master ${master.get.getName()} can't leave because slaves exist")
            } else {
              if (master.get == sender) {
                master = None
                println(s"Master left: ${sender.getName()}")
              } else {
                slaves = slaves - sender
                println(s"Slave left: ${sender.getName()}. Slaves left: ${slaves.size}")
              }
              reply.setPerformative(ACLMessage.AGREE)
            }
        }
        myAgent.send(reply)
      }
    }
  }
}
