package com.github.gsnewmark.collab_env

import jade.core.Agent

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
  }
}
