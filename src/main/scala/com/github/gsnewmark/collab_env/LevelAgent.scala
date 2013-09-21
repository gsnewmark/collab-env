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
 * Represents a Level of the environment.
 *
 * Serves as a resource for Users. One instance of Level could be hold only by
 * one user.
 *
 * Each Level could be either in active or suspended state.
 */
class LevelAgent extends Agent with ServiceAgent {
  val serviceName: String = Env.levelServiceName
  val serviceType: String = Env.levelServiceType
  val initialBehaviours = new RequestHandler :: Nil

  class RequestHandler extends CyclicBehaviour {
    private val mt: MessageTemplate =
      MessageTemplate.MatchPerformative(ACLMessage.REQUEST)

    private var master: Option[AID] = None
    private var isWorking: Boolean = true

    def action() {
      val msg: ACLMessage = myAgent.receive(mt)
      if (msg != null) {
        val reply = msg.createReply()
        val sender = msg.getSender()
        msg.getContent() match {
          case Env.joinLevelRequest =>
            if (master.isEmpty) {
              reply.setPerformative(ACLMessage.AGREE)
              master = Some(sender)
              reply.setContent(Env.masterRole)
              println(s"${fullServiceName} is acquired by ${sender.getName()}")
            } else {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"${fullServiceName} is already acquired, so ${sender.getName()} can't use it")
            }
          case Env.leaveLevelRequest =>
            if (!master.isEmpty && master.get == sender) {
              master = None
              reply.setPerformative(ACLMessage.AGREE)
              println(s"${fullServiceName} is released")
            } else {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"${sender.getName()} can't release ${fullServiceName}")
            }
          case Env.suspendLevelRequest =>
            if (!master.isEmpty && master.get == sender && isWorking) {
              isWorking = false
              reply.setPerformative(ACLMessage.AGREE)
              println(s"${fullServiceName} is suspended by ${sender.getName()}")
            } else {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"${sender.getName()} can't suspend ${fullServiceName}")
            }
          case Env.resumeLevelRequest =>
            if (!master.isEmpty && master.get == sender && !isWorking) {
              isWorking = true
              reply.setPerformative(ACLMessage.AGREE)
              println(s"${fullServiceName} is resumed by ${sender.getName()}")
            } else {
              reply.setPerformative(ACLMessage.REFUSE)
              println(s"${sender.getName()} can't resume ${fullServiceName}")
            }
        }
        myAgent.send(reply)
      } else {
        block()
      }
    }
  }
}
