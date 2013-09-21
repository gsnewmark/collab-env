package com.github.gsnewmark.collab_env

object Env {
  val sessionServiceType = "collab-env-session"
  val sessionServiceName = "collab-env-session-"
  val levelServiceType = "collab-env-level"
  val levelServiceName = "collab-env-level-"

  val joinConversationId = "join"
  val leaveConversationId = "leave"
  val joinLevelConversationId = "join-level"
  val leaveLevelConversationId = "leave-level"
  val suspendLevelConversationId = "suspend-level"
  val resumeLevelConversationId = "resume-level"

  val masterRole = "master"
  val slaveRole = "slave"

  val joinRequest = "join-request"
  val leaveRequest = "leave-request"
  val joinLevelRequest = "join-level-request"
  val leaveLevelRequest = "leave-level-request"
  val suspendLevelRequest = "suspend-request"
  val resumeLevelRequest = "resume-request"
}
