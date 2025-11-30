package master

import core.{FetchResult, FetchTask}


sealed trait MasterCommand

case class AssignTask(task: FetchTask) extends MasterCommand

case class WrappedResult(result: FetchResult) extends MasterCommand
