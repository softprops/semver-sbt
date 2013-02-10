package semver

import semverfi._
import sbt._

/**
 * Provides semantic versioning for sbt projects
 */
object Plugin extends sbt.Plugin {
  import complete.DefaultParsers._
  import sbt.Keys._

  private sealed trait BumpSpec
  private case object Major extends BumpSpec
  private case object Minor extends BumpSpec
  private case object Patch extends BumpSpec

  private val major = token("major" ^^^ Major)
  private val minor = token("minor" ^^^ Minor)
  private val patch = token("patch" ^^^ Patch)
  private val bumpSpec = Space ~> (major | minor | patch)

  val semver = SettingKey[SemVersion](
    "semver", "Semantic version of project")

  val bump = Command("semver-bump")(_ => bumpSpec)(versionBump)

  val buildVersion = Command.single("semver-build")(versionBuild)

  val prereleaseVersion = Command.single("semver-prerelease")(versionPrerelease)
  
  def semverSettings: Seq[Setting[_]] =
    semverVersions ++ semverCommands
 
  def semverCommands: Seq[Setting[_]] = Seq(
    commands ++= Seq(bump, buildVersion, prereleaseVersion)
  )

  def semverVersions: Seq[Setting[_]] = Seq(
    semver <<= version(Version(_))
  )

  private def bumpingTo(version: Valid): PartialFunction[BumpSpec, SemVersion] = {
    case Major => version.bumpMajor
    case Minor => version.bumpMinor
    case Patch => version.bumpPatch
  }

  /** append a build id to current semver */
  private def versionBuild(state: State, build: String) = {
    val log = state.log
    val extracted = Project extract state
    val app = Apply(extracted, state)_
    extracted.getOpt(semver).map({
      case Invalid(invalid) =>
        log.error("Could not append build id to non-semantic version %s"
                  .format(invalid))
        state.fail
      case NormalVersion(maj, min, pat) =>
        app(BuildVersion(maj, min, pat, Nil, Seq(build)))
      case PreReleaseVersion(maj, min, pat, cls) =>
        app(BuildVersion(maj, min, pat, cls, Seq(build)))
      case b: BuildVersion =>
        app(b.copy(build = Seq(build)))        
    }).getOrElse {
      log.warn("state not changed")
      state
    }
  }

  /** append a prerelease id to current semver */
  private def versionPrerelease(state: State, prerelease: String) = {
    val log = state.log
    val extracted = Project extract state
    val app = Apply(extracted, state)_
    extracted.getOpt(semver).map({
      case Invalid(invalid) =>
        log.error("Could not append prerelease id to non-semantic version %s"
                  .format(invalid))
        state.fail
      case NormalVersion(maj, min, pat) =>
        app(PreReleaseVersion(maj, min, pat, Seq(prerelease)))
      case PreReleaseVersion(maj, min, pat, _) =>
        app(PreReleaseVersion(maj, min, pat, Seq(prerelease)))
      case BuildVersion(maj, min, pat, _, _) =>
        app(PreReleaseVersion(maj, min, pat, Seq(prerelease)))
    }).getOrElse {
      log.warn("state not changed")
      state
    }
  }

  private def versionBump(state: State, spec: BumpSpec) = {   
    val log = state.log
		val extracted = Project extract state
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
