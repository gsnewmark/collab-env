package com.github.gsnewmark.collab_env

import scala.util.Random

import jade.core.{Agent, AID}
import jade.core.behaviours.{OneShotBehaviour, WakerBehaviour}
import jade.domain.{DFService, FIPAException}
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription, ServiceDescription}

/** Represents a User of the environment.
  *
  * Each user tries to join an existing session (either as master or slave),
  * and then exits from joined session after a configurable delay.
  */
class UserAgent extends Agent {
  val leaveSessionDelay = 3000 + Random.nextInt(10000)

  protected override def setup() = {
    println(s"User was created: ${getAID().getName()}")
    addBehaviour(new JoinSessionBehaviour)
  }

  private class JoinSessionBehaviour() extends OneShotBehaviour {
    override def action() = {
      println("join session")

      // Update the list of sessions
      val template: DFAgentDescription = new DFAgentDescription
      val sd: ServiceDescription = new ServiceDescription
      sd.setType(Env.serviceType);
      template.addServices(sd);
      try {
        val sessions: List[AID] =
          DFService.search(myAgent, template).toList.map(_.getName)
        // Perform the request
        val session = Random.shuffle(sessions).head
        myAgent.addBehaviour(
          new LeaveSessionBehaviour(myAgent, session, leaveSessionDelay))
      } catch {
        case e: FIPAException => e.printStackTrace
      }
    }
  }

  private class LeaveSessionBehaviour(
    val agent: Agent, val session:AID, val delay: Long)
      extends WakerBehaviour(agent, delay) {
    protected override def onWake() = {
      println("awoken" + myAgent + " " + session + " " + delay)
      myAgent.doDelete()
    }
  }
}
