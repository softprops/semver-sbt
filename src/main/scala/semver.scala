package semver

import semverfi._
import sbt._

/**
 * Provides semantic versioning for sbt projects
 */
object Plugin extends sbt.Plugin {
  import complete.DefaultParsers._
  import complete.Parser
  import sbt.Keys._

  // bumping

  private sealed trait BumpSpec
  private case object Major extends BumpSpec
  private case object Minor extends BumpSpec
  private case object Patch extends BumpSpec

  // appending

  private sealed trait Extra
  private case class PreReleaseExtra(value: String) extends Extra
  private case class BuildExtra(value: String) extends Extra

  private val major = token("major" ^^^ Major)
  private val minor = token("minor" ^^^ Minor)
  private val patch = token("patch" ^^^ Patch)
  private val bumpSpec: Parser[BumpSpec] =
    Space ~> (major | minor | patch)

  private val appendOpts: Parser[String] =
    literal("build") | literal("prerelease")
  private val appender =
    token(appendOpts) ~ Space ~ token(any.+).examples("SNAPSHOT", "123") map {
      case ("build" ~ _ ~ extra)      => BuildExtra(extra.mkString(""))
      case ("prerelease" ~ _ ~ extra) => PreReleaseExtra(extra.mkString(""))
    }
  private val appendSpec:Parser[Extra] = Space ~> appender

  val semver = SettingKey[SemVersion](
    "semver", "Semantic version of project")

  val semverBump = Command("semver-bump")(_ => bumpSpec)(versionBump)

  val semverAppend = Command("semver-append")(_ => appendSpec)(versionAppend)
  
  val semverNormalize = Command.command("semver-normalize")(versionNormalize)

  val semverTeeFile = SettingKey[Option[java.io.File]](
    "semver-tee-file", "Optional file to tee semver operation results to"
  )

  def semverSettings: Seq[Setting[_]] =
    semverVersions ++ semverCommands
 
  def semverCommands: Seq[Setting[_]] = Seq(
    commands ++= Seq(semverBump, semverAppend, semverNormalize)
  )

  def semverVersions: Seq[Setting[_]] = Seq(
    semver <<= version(Version(_)),
    semverTeeFile <<= baseDirectory(bd => Some(bd / ".semver"))
  )

  private def bumpingTo(version: Valid): PartialFunction[BumpSpec, SemVersion] = {
    case Major => version.bumpMajor
    case Minor => version.bumpMinor
    case Patch => version.bumpPatch
  }

  private def versionNormalize(state: State) = {
    val log = state.log
    val extracted = Project extract state
    val app = Apply(extracted, state)_
    extracted.getOpt(semver).map({
      case Invalid(invalid) =>
        log.error("Could not normalize version info to non-semantic version %s"
                  .format(invalid))
        state.fail
      case v: Valid =>
        app(v.normalize)
    }).getOrElse {
      log.warn("state not changed")
      state
    }
  }

  private def versionAppend(state: State, extra: Extra) = {
    val log = state.log
    val extracted = Project extract state
    val app = Apply(extracted, state)_
    extracted.getOpt(semver).map({
      case Invalid(invalid) =>
        log.error("Could not append version info to non-semantic version %s"
                  .format(invalid))
        state.fail
      case v: Valid =>
        app(extra match {
          case PreReleaseExtra(pr) => v.prerelease(pr)
          case BuildExtra(b) => v.build(b)
        })
    }).getOrElse {
      log.warn("state not changed")
      state
    }
  }

  private def versionBump(state: State, spec: BumpSpec) = {   
    val log = state.log
		val extracted = Project extract state
    println(extracted.currentRef.project)
    extracted.getOpt(semver).map({
      case Invalid(invalid) =>
        log.error("Could not bump non-semantic version %s" format invalid)
        state.fail
      case version: Valid =>
        bumpingTo(version)(spec) match {
          case Invalid(inv) =>
            log.error("Could not bump non semantic version %s" format inv)
            state.fail
          case next: Valid =>
            Apply(extracted, state)(next)
        }
    }).getOrElse {
      log.warn("state not changed")
      state
    }
	}
}
