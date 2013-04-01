package semver

import semverfi.Valid

/** Abstracts away a lot of the boiler plate required to evaluate
 *  and apply a setting dynamically */
object Apply {
  import sbt._
  def apply(extracted: Extracted, state: State, commit: Boolean = true)
           (version: Valid) =
    if (commit) applyProjectVersion(version, extracted, state)
    else applyMemoryVersion(version, extracted, state)

  private def eval(version: Valid, extracted: Extracted) = {
    import BuiltinCommands.imports
    import extracted.{ currentLoader, session }
    val expr = setVersionStr(version)
    EvaluateConfigurations.evaluateSetting(
      session.currentEval(), // Eval
      "<set>",               // name str
      imports(extracted),    // imports  Seq[(String, Int)]
      expr,// expression str
      LineRange(0,0)         // line range ( use no range type - mark )
    )(currentLoader)
  }

  private def tee(version: Valid)(file: File) = {
    val to = new java.io.FileWriter(file)
    to.write(version.toString)
    to.flush()
    to.close()
  }

  private def applyProjectVersion(version: Valid, extracted: Extracted, state: State) = {
    import BuiltinCommands.{ reapply, DefaultBootCommands }
    import CommandStrings.{ DefaultsCommand, InitCommand }
    import extracted.{
      currentRef, rootProject, session, structure
    }

    // optionally tee version transformation to a alternate file.
    // this is useful for exposing semversions to
    // external tools.
    extracted.getOpt(Plugin.semverTeeFile).map(_.map(tee(version)))
          
    // transform
    val transformed = Load.transformSettings(
      Load.projectScope(currentRef),
      currentRef.build,
      rootProject,
      eval(version, extracted))
      
    // mix
    val expr = setVersionStr(version)
    val mixed = session.appendSettings(
      transformed map(a => (a, expr.split('\n').toList)))

    // reapply new settings saving persisting them build.sbt
    val commands = (CommandStrings.SessionCommand + " save") +:
                    DefaultsCommand +:
                    InitCommand +:
                    DefaultBootCommands
    reapply(mixed,
            structure,
            state.copy(remainingCommands = commands))
  }

  private def applyMemoryVersion(version: Valid, extracted: Extracted, state: State) =
    extracted.append(eval(version, extracted), state)

  private def setVersionStr(v: Valid) =
    """version := "%s"""" format v
}
