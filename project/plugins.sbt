//addSbtPlugin("me.lessis" % "semver-sbt" % "0.1.0-SNAPSHOT")

libraryDependencies <+= (sbtVersion)("org.scala-sbt" % "scripted-plugin" % _)
