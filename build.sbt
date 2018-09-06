lazy val root = RchainIdeaSupport.root

lazy val ideaRunner = RchainIdeaSupport.ideaRunner

lazy val rchainIdea = RchainIdeaSupport.rchainIdea

addCommandAlias("runIdea", "; idea-plugin/assembly; idea-runner/run")
