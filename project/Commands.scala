import sbt._

import org.apache.commons.io.FileUtils

/** Used to define sbt commands.
  *
  * TODO - use the value of the sbt property "sbt.global.base" to build "path"
  * instead of hardcoding.
  */
object Commands {

  /** Defines the command "staging-clean" used to clean up the content of
    * "staging" dir in ~/.sbt.
    *
    * The Daut and TraceContract are cloned from github and built as part of
    * MESA build.sbt. Once they are cloned, they do not get updated in
    * subsequent build. To enforce update, one needs to execute this sbt
    * command to delete the existing copies which makes build.sbt to re-clone
    * them.
    */
  def stagingClean = Command.command("staging-clean") {
    state => {
      val path = sys.env("HOME") + "/.sbt/1.0/staging"

      val dir = new File(path)
      for (file <- dir.listFiles) {
        if (file.isDirectory) FileUtils.deleteDirectory(file)
        else file.delete
      }
    }
      state
  }

  val stagingCmds = Seq(stagingClean)
}
