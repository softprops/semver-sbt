name := "simple-build"

version := "0.1.0-SNAPSHOT"

seq(semverSettings: _*)

logLevel := Level.Debug

InputKey[Unit]("tee-contents") <<= inputTask {
  (argsTask: TaskKey[Seq[String]]) =>
    (argsTask, streams, semverTeeFile) map {
      (args, out, teeFile) =>
        out.log.info("running tee-contents task for %s" format teeFile)
        (args, teeFile) match {
          case (Seq(expected), Some(teeFile)) =>
            out.log.info("expected %s in %s" format(expected, teeFile))
            if (teeFile.exists) {
              if (IO.read(teeFile) == expected) out.log.info(
                "tee-contents valid"
              ) else out.log.info(
                "Context of tee file does not match %s" format expected
              )
            } else out.log.error(
              "tee file %s does not exist" format teeFile
            )
          case _ => out.log.error("usage: tee-contents <contents>")
        }
    }
}

InputKey[Unit]("expect-version") <<= inputTask {
  (argsTask: TaskKey[Seq[String]]) =>
    (argsTask, streams, semver) map {
      (args, out, semver) =>
        out.log.info("running expect version with %s" format args)
        args match {
          case Seq(expected) =>
            if (semver.toString == expected) out.log.info(
              "version correct"
            ) else out.log.error(
              "expected version %s but got %s".format(expected, semver)
            )
        }
    }
}
