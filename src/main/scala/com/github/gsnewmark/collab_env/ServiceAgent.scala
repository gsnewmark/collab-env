package com.github.gsnewmark.collab_env

import jade.core.{ AID, Agent }
import jade.core.behaviours.Behaviour
import jade.domain.{ DFService, FIPAException }
import jade.domain.FIPAAgentManagement.{
  DFAgentDescription,
  ServiceDescription
}

trait ServiceAgent {
  this: Agent =>

  val serviceName: String
  val serviceType: String
  val initialBehaviours: Traversable[Behaviour]

  def fullServiceName = { serviceName + getAID().getName() }

  override def setup(): Unit = {
    println(s"${fullServiceName} was created")
    val dfd: DFAgentDescription = new DFAgentDescription
    dfd.setName(getAID())
    val sd: ServiceDescription = new ServiceDescription()
    sd.setName(fullServiceName)
    sd.setType(serviceType)
    dfd.addServices(sd)
    try {
      DFService.register(this, dfd)
    } catch {
      case fe: FIPAException => fe.printStackTrace()
    }
    initialBehaviours.foreach(addBehaviour(_))
  }

  override def takeDown(): Unit = {
    try {
      DFService.deregister(this)
    } catch {
      case fe: FIPAException => fe.printStackTrace()
    }
    println(s"${fullServiceName} was terminated")
  }
}
