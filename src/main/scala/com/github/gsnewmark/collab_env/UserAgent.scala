package com.github.gsnewmark.collab_env

import jade.core.{ AID, Agent }
import jade.core.behaviours.{ Behaviour, OneShotBehaviour, WakerBehaviour }
import jade.domain.{ DFService, FIPAException }
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription,
  ServiceDescription
}
import jade.lang.acl.{ ACLMessage, MessageTemplate }
import scala.collection.mutable.Stack
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
    // Update the list of sessions
    val template: DFAgentDescription = new DFAgentDescription
    val sd: ServiceDescription = new ServiceDescription
    sd.setType(Env.sessionServiceType)
    template.addServices(sd)
    var sessions: Stack[AID] = Stack()
    while (sessions.isEmpty) {
      try {
        sessions ++= DFService.search(this, template).map(_.getName)
      } catch {
        case e: FIPAException => e.printStackTrace
      }
    }
    addBehaviour(new JoinSessionRequestBehaviour(sessions))
  }

  protected override def takeDown() = {
    println(s"User ${getAID().getName()} terminated.");
  }

  // TODO DRY

  private class JoinSessionRequestBehaviour(
    val sessions: Stack[AID]) extends OneShotBehaviour {
    override def action(): Unit = {
      // Send request to join random session
      val session = sessions.pop()
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(session)
      message.setContent(Env.joinRequest)
      message.setConversationId(Env.joinConversationId)
      message.setReplyWith(
        "join" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new JoinSessionResponseBehaviour(message, session, sessions))
      myAgent.send(message)
    }
  }

  private class JoinSessionResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID]) extends Behaviour {
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
            val template: DFAgentDescription = new DFAgentDescription
            val sd: ServiceDescription = new ServiceDescription
            sd.setType(Env.floorServiceType)
            template.addServices(sd)
            var floors: Stack[AID] = Stack()
            while (floors.isEmpty) {
              try {
                floors ++= DFService.search(myAgent, template).map(_.getName)
              } catch {
                case e: FIPAException => e.printStackTrace
              }
            }
            myAgent.addBehaviour(
              new AcquireFloorRequestBehaviour(
                myAgent, session, sessions, floors, 0))
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

  private class AcquireFloorRequestBehaviour(
    val agent: Agent, val session: AID,
    val sessions: Stack[AID], val floors: Stack[AID], val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val floor = floors.pop()
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(floor)
      message.setContent(Env.joinFloorRequest)
      message.setConversationId(Env.joinFloorConversationId)
      message.setReplyWith(
        "joinFloor" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new AcquireFloorResponseBehaviour(
          message, session, sessions, floor, floors))
      myAgent.send(message)
    }
  }

  private class AcquireFloorResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID], val floor: AID, val floors: Stack[AID])
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.joinFloorConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new SuspendFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
          case ACLMessage.REFUSE =>
            floors.push(floor)
            myAgent.addBehaviour(
              new AcquireFloorRequestBehaviour(
                myAgent, session, sessions, floors, genDelay))
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

  private class SuspendFloorRequestBehaviour(
    val agent: Agent, val session: AID, val sessions: Stack[AID],
    val floor: AID, val floors: Stack[AID], val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(floor)
      message.setContent(Env.suspendFloorRequest)
      message.setConversationId(Env.suspendFloorConversationId)
      message.setReplyWith(
        "suspendFloor" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new SuspendFloorResponseBehaviour(
          message, session, sessions, floor, floors))
      myAgent.send(message)
    }
  }

  private class SuspendFloorResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID], val floor: AID, val floors: Stack[AID])
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.suspendFloorConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new ResumeFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new SuspendFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
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

  private class ResumeFloorRequestBehaviour(
    val agent: Agent, val session: AID, val sessions: Stack[AID],
    val floor: AID, val floors: Stack[AID], val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(floor)
      message.setContent(Env.resumeFloorRequest)
      message.setConversationId(Env.resumeFloorConversationId)
      message.setReplyWith(
        "resumeFloor" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(new ResumeFloorResponseBehaviour(
        message, session, sessions, floor, floors))
      myAgent.send(message)
    }
  }

  private class ResumeFloorResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID], val floor: AID, val floors: Stack[AID])
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.resumeFloorConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            myAgent.addBehaviour(
              new ReleaseFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new ResumeFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
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

  private class ReleaseFloorRequestBehaviour(
    val agent: Agent, val session: AID, val sessions: Stack[AID],
    val floor: AID, val floors: Stack[AID], val delay: Long)
    extends WakerBehaviour(agent, delay) {
    override def onWake(): Unit = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(floor)
      message.setContent(Env.leaveFloorRequest)
      message.setConversationId(Env.leaveFloorConversationId)
      message.setReplyWith(
        "leaveFloor" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new ReleaseFloorResponseBehaviour(
          message, session, sessions, floor, floors))
      myAgent.send(message)
    }
  }

  private class ReleaseFloorResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID], val floor: AID, val floors: Stack[AID])
    extends Behaviour {
    val mt = MessageTemplate.and(
      MessageTemplate.MatchConversationId(Env.leaveFloorConversationId),
      MessageTemplate.MatchInReplyTo(message.getReplyWith))
    var received = false

    override def action(): Unit = {
      val reply = myAgent.receive(mt)
      if (reply != null) {
        reply.getPerformative() match {
          case ACLMessage.AGREE =>
            if (floors.isEmpty) {
              myAgent.addBehaviour(
                new LeaveSessionRequestBehaviour(
                  myAgent, session, sessions, genDelay))
            } else {
              myAgent.addBehaviour(
                new AcquireFloorRequestBehaviour(
                  myAgent, session, sessions, floors, 0))
            }
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new ReleaseFloorRequestBehaviour(
                myAgent, session, sessions, floor, floors, genDelay))
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
    val agent: Agent, val session: AID,
    val sessions: Stack[AID], val delay: Long)
    extends WakerBehaviour(agent, delay) {
    protected override def onWake() = {
      val message = new ACLMessage(ACLMessage.REQUEST)
      message.addReceiver(session)
      message.setContent(Env.leaveRequest)
      message.setConversationId(Env.leaveConversationId)
      message.setReplyWith(
        "leave" + myAgent.getAID() + System.currentTimeMillis())
      myAgent.addBehaviour(
        new LeaveSessionResponseBehaviour(message, session, sessions, delay))
      myAgent.send(message)
    }
  }

  private class LeaveSessionResponseBehaviour(
    val message: ACLMessage, val session: AID,
    val sessions: Stack[AID], val delay: Long)
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
            if (sessions.isEmpty) {
              myAgent.doDelete()
            } else {
              addBehaviour(new JoinSessionRequestBehaviour(sessions))
            }
          case ACLMessage.REFUSE =>
            myAgent.addBehaviour(
              new LeaveSessionRequestBehaviour(
                myAgent, session, sessions, delay))
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
