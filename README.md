# collab-env

Playing with JADE agents in Scala.

Uses [sbt](http://www.scala-sbt.org/) to build/run the project.

## Setup

JADE is available in tilab Maven repository, so it could be added to your sbt
build in following way:

    resolvers += "tilab" at "http://jade.tilab.com/maven"

    libraryDependencies += "com.tilab.jade" % "jade" % "4.3.0"

## JADE basics

Agents are working inside the container. JADE uses `jade.Boot` class to start
such container.

Empty JADE container could be started with following command:

    sbt "run-main jade.Boot -local-host localhost -gui"

`-local-host` parameter specifies that container should run locally; `-gui`,
quite naturally, starts handy GUI for started container.

To start container populated with and agent execute:

    sbt "run-main jade.Boot -local-host localhost -gui john:some.package.Agent"

Here we specify the agent using pair `name:impl-class` as an argument. So in
this example agent with name `john` with implementation class
`some.package.Agent` was created.

To populate container with multiple agents simultaneously, separate them with
`;`:

    sbt "run-main jade.Boot -local-host localhost -gui john:some.package.Agent;jack:some.package.Agent"

Additionally, it's possible to pass parameters to agents: they are placed in a
comma-separated list enclosed in parenthesis, which goes right after the
agent definition:

    sbt "run-main jade.Boot -local-host localhost john:examples.bookTrading.some.package.Agent(Arg)"

Please note that string arguments are specified without quotes (`"`).

### Agent

Agent's implementation class must extend `jade.core.Agent`. It contains few
overridable lifecycle methods and handy utility methods, for instance:

 - `setup()` which is called during agent creation
 - `takeDown()` which is called before agent destruction
 - `getLocalName()` returns name under which current agent is created
 - `getArguments()` returns arguments passed to agent during its creation

Custom behaviour could be used by adding `jade.core.behaviours.Behaviour`
implementation to particular agent using `addBehaviour` method inherited from
`jade.core.Agent`.

## Usage

To start JADE container with added Session and number of User agents execute
following command:

    sbt "run-main jade.Boot -local-host localhost session:com.github.gsnewmark.collab_env.SessionAgent;floor:com.github.gsnewmark.collab_env.FloorAgent;user:com.github.gsnewmark.collab_env.UserAgent;user1:com.github.gsnewmark.collab_env.UserAgent;user2:com.github.gsnewmark.collab_env.UserAgent;user3:com.github.gsnewmark.collab_env.UserAgent;user4:com.github.gsnewmark.collab_env.UserAgent;user5:com.github.gsnewmark.collab_env.UserAgent;user6:com.github.gsnewmark.collab_env.UserAgent;user7:com.github.gsnewmark.collab_env.UserAgent;user8:com.github.gsnewmark.collab_env.UserAgent;user9:com.github.gsnewmark.collab_env.UserAgent;user10:com.github.gsnewmark.collab_env.UserAgent"
