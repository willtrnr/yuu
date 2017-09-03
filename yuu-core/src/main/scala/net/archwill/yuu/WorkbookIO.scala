package net.archwill.yuu

import org.apache.poi.ss.usermodel.Workbook

final class WorkbookIO[A](val run: Workbook => A) {

  def map[B](f: A => B): WorkbookIO[B] =
    WorkbookIO(run andThen f)

  def flatMap[B](f: A => WorkbookIO[B]): WorkbookIO[B] =
    WorkbookIO(wb => f(run(wb)).run(wb))

}

object WorkbookIO {
  
  def apply[A](f: Workbook => A): WorkbookIO[A] =
    new WorkbookIO(f)

  def pure[A](a: => A): WorkbookIO[A] =
    apply(_ => a)

}
