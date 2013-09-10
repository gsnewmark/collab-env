package com.github.gsnewmark.collab_env

import util.Random

import jade.core.Agent

/** Represents a User of the environment.
  *
  * Each user tries to join an existing session (either as master or slave),
  * and then exits from joined session after a configurable delay.
  */
class UserAgent extends Agent {
  val leaveSessionDelay = 3000 + Random.nextInt(10000)

  protected override def setup() = {
    println(s"User was created: ${getAID().getName()}")
  }
}
