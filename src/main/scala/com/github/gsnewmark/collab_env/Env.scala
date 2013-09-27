package com.github.gsnewmark.collab_env

object Env {
  val sessionServiceType = "collab-env-session"
  val sessionServiceName = "collab-env-session-"
  val floorServiceType = "collab-env-floor"
  val floorServiceName = "collab-env-floor-"

  val joinConversationId = "join"
  val leaveConversationId = "leave"
  val joinFloorConversationId = "join-floor"
  val leaveFloorConversationId = "leave-floor"
  val suspendFloorConversationId = "suspend-floor"
  val resumeFloorConversationId = "resume-floor"

  val masterRole = "master"
  val slaveRole = "slave"

  val joinRequest = "join-request"
  val leaveRequest = "leave-request"
  val joinFloorRequest = "join-floor-request"
  val leaveFloorRequest = "leave-floor-request"
  val suspendFloorRequest = "suspend-request"
  val resumeFloorRequest = "resume-request"
}
