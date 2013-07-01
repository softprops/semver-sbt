organization := "me.lessis"

name := "semver-sbt"

version := "0.1.0-SNAPSHOT"

description := "Semantic versions for sbt projects"

sbtPlugin := true

sbtVersion in Global := "0.13.0-RC1"

scalaVersion in Global := "2.10.2"

scalacOptions := Seq("-deprecation")

libraryDependencies += "me.lessis" %% "semverfi" % "0.1.3"

publishTo := Some(Classpaths.sbtPluginReleases)

publishMavenStyle := false

licenses <<= version(v =>
  Seq("MIT" ->
      url("https://github.com/softprops/semver-sbt/blob/%s/LICENSE" format v)))

homepage := Some(url("https://github.com/softprops/semver-sbt/"))

pomExtra := (
  <scm>
    <url>git@github.com:softprops/semver-sbt.git</url>
    <connection>scm:git:git@github.com:softprops/semver-sbt.git</connection>
  </scm>
  <developers>
    <developer>
      <id>softprops</id>
      <name>Doug Tangren</name>
      <url>https://github.com/softprops</url>
    </developer>
  </developers>)

seq(scriptedSettings: _*)

scriptedBufferLog := false

logLevel := Level.Debug

//seq(semverSettings: _*)
