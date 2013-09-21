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
  def genDelay = 500 + Random.nextInt(500)

  protected override def setup() = {
    println(s"User was created: ${getAID().getName()}")
    addBehaviour(new JoinSessionRequestBehaviour)
  }

  protected override def takeDown() = {
    println(s"User ${getAID().getName()} terminated.");
  }

  // TODO DRY

  private class JoinSessionRequestBehaviour() extends OneShotBehaviour {
    override def action(): Unit = {
      // Update the list of sessions
      val template: DFAgentDescription = new DFAgentDescription
      val sd: ServiceDescription = new ServiceDescription
      sd.setType(Env.sessionServiceType)
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
              new AcquireLevelRequestBehaviour(myAgent, session, 0))
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
      received
    }
  }

  private class AcquireLevelRequestBehaviour(
    val agent: Agent, val session: AID, val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val template: DFAgentDescription = new DFAgentDescription
      val sd: ServiceDescription = new ServiceDescription
      sd.setType(Env.levelServiceType)
      template.addServices(sd)
      var levels: List[AID] = List()
      while (levels.isEmpty) {
        try {
          levels = DFService.search(myAgent, template).toList.map(_.getName)
        } catch {
          case e: FIPAException => e.printStackTrace
        }
      }

      val level = Random.shuffle(levels).head
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(level)
      message.setContent(Env.joinLevelRequest)
      message.setConversationId(Env.joinLevelConversationId)
      message.setReplyWith(
        "joinLevel" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new AcquireLevelResponseBehaviour(message, session, level))
      myAgent.send(message)
    }
  }

  private class AcquireLevelResponseBehaviour(
    val message: ACLMessage, val session: AID, val level: AID)
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.joinLevelConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new SuspendLevelRequestBehaviour(
                myAgent, session, level, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new AcquireLevelRequestBehaviour(myAgent, session, genDelay))
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      received
    }
  }

  private class SuspendLevelRequestBehaviour(
    val agent: Agent, val session: AID, val level: AID, val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(level)
      message.setContent(Env.suspendLevelRequest)
      message.setConversationId(Env.suspendLevelConversationId)
      message.setReplyWith(
        "suspendLevel" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new SuspendLevelResponseBehaviour(message, session, level))
      myAgent.send(message)
    }
  }

  private class SuspendLevelResponseBehaviour(
    val message: ACLMessage, val session: AID, val level: AID)
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.suspendLevelConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new ResumeLevelRequestBehaviour(
                myAgent, session, level, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new SuspendLevelRequestBehaviour(myAgent, session, level, genDelay))
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      received
    }
  }

  private class ResumeLevelRequestBehaviour(
    val agent: Agent, val session: AID, val level: AID, val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(level)
      message.setContent(Env.resumeLevelRequest)
      message.setConversationId(Env.resumeLevelConversationId)
      message.setReplyWith(
        "resumeLevel" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new ResumeLevelResponseBehaviour(message, session, level))
      myAgent.send(message)
    }
  }

  private class ResumeLevelResponseBehaviour(
    val message: ACLMessage, val session: AID, val level: AID) extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.resumeLevelConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new ReleaseLevelRequestBehaviour(
                myAgent, session, level, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new ResumeLevelRequestBehaviour(myAgent, session, level, genDelay))
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      received
    }
  }

  private class ReleaseLevelRequestBehaviour(
    val agent: Agent, val session: AID, val level: AID, val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(level)
      message.setContent(Env.leaveLevelRequest)
      message.setConversationId(Env.leaveLevelConversationId)
      message.setReplyWith(
        "leaveLevel" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new ReleaseLevelResponseBehaviour(message, session, level))
      myAgent.send(message)
    }
  }

  private class ReleaseLevelResponseBehaviour(
    val message: ACLMessage, val session: AID, val level: AID) extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.leaveLevelConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new LeaveSessionRequestBehaviour(
                myAgent, session, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new ReleaseLevelRequestBehaviour(myAgent, session, level, genDelay))
        }
        received = true
      } else {
        block()
      }
    }

    def done(): Boolean = {
      received
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
      received
    }
  }
}
