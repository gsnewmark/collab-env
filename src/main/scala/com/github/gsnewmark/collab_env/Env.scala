package com.github.gsnewmark.collab_env

object Env {
  val serviceType = "collab-env"
  val sessionServiceName = "collab-env-session"

  val joinConversationId = "session-join"
  val leaveConversationId = "session-leave"

  val masterRole = "master"
  val slaveRole = "slave"

  val joinRequest = "join-request"
  val leaveRequest = "leave-request"
}
