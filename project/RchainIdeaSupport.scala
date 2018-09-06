import org.jetbrains.sbtidea.Keys._
import org.jetbrains.sbtidea.SbtIdeaPlugin
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

object RchainIdeaSupport {
  lazy val commonSettings = Seq(
    version := "0.1.0",
    isSnapshot := true,
    organization := "coop.rchain.idea",
    scalaVersion := "2.12.6",
    scalacOptions := Seq(
      "-Ypartial-unification",
      "-encoding",
      "UTF-8",
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture",
      "-Ybreak-cycles",
      "-Xexperimental"
    ),
    fork in Test := true,
    scalacOptions in (Compile, console) -= "-Ywarn-unused-import"
  )

  lazy val rchainIdea = Project(id = "idea-plugin", base = file("idea-plugin"))
    .settings(commonSettings)
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      autoScalaLibrary := false,
      onLoad in Global ~= { _.andThen("idea-plugin/updateIdea" :: _) },
      assemblyExcludedJars in assembly ++= ideaFullJars.value,
      unmanagedJars in Compile += file(".") / "idea" / "tools.jar",
      ideaPluginName in ThisBuild := "RchainSupport",
      ideaBuild in ThisBuild := "182.4323.46",
      ideaExternalPlugins += IdeaPlugin.Zip(
        "scala-plugin",
        url("https://plugins.jetbrains.com/plugin/download?rel=true&updateId=49539")
      )
    )

  lazy val ideaRunner = Project(id = "idea-runner", base = file("idea-plugin/target"))
    .settings(
      scalaVersion := "2.12.6",
      autoScalaLibrary := false,
      unmanagedJars in Compile := (ideaMainJars in rchainIdea).value,
      unmanagedJars in Compile += file(System.getProperty("java.home")).getParentFile / "lib" / "tools.jar",
      fork in run := true,
      mainClass in (Compile, run) := Some("com.intellij.idea.Main"),
      javaOptions in run ++= Seq(
        "-Xmx2g",
        "-XX:ReservedCodeCacheSize=240m",
        "-XX:MaxPermSize=250m",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-ea",
        "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005",
        s"-Didea.home=${(ideaBaseDirectory in rchainIdea).value.getPath}",
        "-Didea.is.internal=true",
        "-Didea.debug.mode=true",
        "-Dapple.laf.useScreenMenuBar=true",
        s"-Dplugin.path=${(assemblyOutputPath in (rchainIdea, assembly)).value}",
        "-Didea.ProcessCanceledException=disabled"
      )
    )

  lazy val root = Project(id = "rchain-idea-support", base = file("."))
    .settings(commonSettings)
    .aggregate(rchainIdea, ideaRunner)
}
