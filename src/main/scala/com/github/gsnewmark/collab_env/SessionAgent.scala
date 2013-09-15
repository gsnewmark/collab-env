package com.github.gsnewmark.collab_env

import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.lang.acl.{ACLMessage, MessageTemplate}
import jade.domain.{DFService, FIPAException}
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription, ServiceDescription}

/** Represents a Session of the environment.
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
    addBehaviour(new JoinSessionRequestHandler)
  }

  /** Processes join session messages. */
  private class JoinSessionRequestHandler extends CyclicBehaviour {
    private val mt: MessageTemplate =
        MessageTemplate.MatchPerformative(ACLMessage.CFP)

	def action() {
	  val msg: ACLMessage = myAgent.receive(mt)
      println(msg)
	  block()
	}
  }
}
