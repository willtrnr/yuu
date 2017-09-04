package net.archwill.yuu

import scala.util.Try

import java.io.{File, InputStream}

import org.apache.poi.ss.usermodel.{Sheet, Workbook}

final class WorkbookIO[A](val run: Workbook => A) {

  def map[B](f: A => B): WorkbookIO[B] =
    WorkbookIO(run andThen f)

  def flatMap[B](f: A => WorkbookIO[B]): WorkbookIO[B] =
    WorkbookIO(wb => f(run(wb)).run(wb))

  def run(path: String): Try[A] = ???

  def run(file: File): Try[A] = ???

  def run(is: InputStream): Try[A] = ???

}

object WorkbookIO {
  
  def apply[A](f: Workbook => A): WorkbookIO[A] =
    new WorkbookIO(f)

  def pure[A](a: => A): WorkbookIO[A] =
    apply(_ => a)

  def getSheet(idx: Int): WorkbookIO[Sheet] =
    apply(_.getSheetAt(idx))

  def getSheet(name: String): WorkbookIO[Sheet] =
    apply(_.getSheet(name))

}
