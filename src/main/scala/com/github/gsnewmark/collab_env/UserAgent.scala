package com.github.gsnewmark.collab_env

import jade.core.{ AID, Agent }
import jade.core.behaviours.{ Behaviour, OneShotBehaviour, WakerBehaviour }
import jade.domain.{ DFService, FIPAException }
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription,
  ServiceDescription
}
import jade.lang.acl.{ ACLMessage, MessageTemplate }
import scala.util.Random

/**
 * Represents a User of the environment.
 *
 * Each user tries to join an existing session (either as master or slave),
 * and then exits from joined session after a configurable delay.
 */
class UserAgent extends Agent {
  val leaveSessionDelay = 2000 + Random.nextInt(4000)

  protected override def setup() = {
    println(s"User was created: ${getAID().getName()}")
    addBehaviour(new JoinSessionRequestBehaviour)
  }

  protected override def takeDown() = {
    println(s"User ${getAID().getName()} terminated.");
  }

  private class JoinSessionRequestBehaviour() extends OneShotBehaviour {
    override def action(): Unit = {
      // Update the list of sessions
      val template: DFAgentDescription = new DFAgentDescription
      val sd: ServiceDescription = new ServiceDescription
      sd.setType(Env.serviceType)
      template.addServices(sd)
      var sessions: List[AID] = List()
      while (sessions.isEmpty) {
        try {
          sessions = DFService.search(myAgent, template).toList.map(_.getName)
        } catch {
          case e: FIPAException => e.printStackTrace
        }
      }

      // Send request to join random session
      val session = Random.shuffle(sessions).head
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(session)
      message.setContent(Env.joinRequest)
      message.setConversationId(Env.joinConversationId)
      message.setReplyWith(
        "join" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new JoinSessionResponseBehaviour(message, session))
      myAgent.send(message)
    }
  }

  private class JoinSessionResponseBehaviour(
    val message: ACLMessage, val session: AID) extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.joinConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      // Process response
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new LeaveSessionRequestBehaviour(
                myAgent, session, leaveSessionDelay))
          case ACLMessage.REFUSE =>
            println(s"${myAgent.getName()} can't join the session")
            myAgent.doDelete()
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      return received;
    }
  }

  private class LeaveSessionRequestBehaviour(
    val agent: Agent, val session: AID, val delay: Long)
    extends WakerBehaviour(agent, delay) {
    protected override def onWake() = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(session)
      message.setContent(Env.leaveRequest)
      message.setConversationId(Env.leaveConversationId)
      message.setReplyWith(
        "leave" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new LeaveSessionResponseBehaviour(message, session, delay))
      myAgent.send(message)
    }
  }

  private class LeaveSessionResponseBehaviour(
    val message: ACLMessage, val session: AID, val delay: Long)
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.leaveConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      // Process response
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.doDelete()
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new LeaveSessionRequestBehaviour(myAgent, session, delay))
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      return received;
    }
  }
}
