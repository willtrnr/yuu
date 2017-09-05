package net.archwill.yuu

import scala.util.Try

import java.io.{File, FileInputStream, InputStream}

import org.apache.poi.ss.usermodel.{Sheet, Workbook}
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

final class WorkbookIO[A](val run: Workbook => A) {

  def map[B](f: A => B): WorkbookIO[B] =
    WorkbookIO(run andThen f)

  def flatMap[B](f: A => WorkbookIO[B]): WorkbookIO[B] =
    WorkbookIO(wb => f(run(wb)).run(wb))

  def runXls(path: String): Try[A] = runXls(new File(path))
  
  def runXls(file: File): Try[A] = Try {
    val is = new FileInputStream(file)
    try {
      val wb = new HSSFWorkbook(is)
      try {
        run(wb)
      } finally {
        wb.close()
      }
    } finally {
      is.close()
    }
  }

  def runXls(is: InputStream): Try[A] = Try {
    val wb = new HSSFWorkbook(is)
    try run(wb) finally wb.close()
  }

  def runXls(): Try[A] = Try {
    def wb = new HSSFWorkbook()
    try run(wb) finally wb.close()
  }

  def runXlsx(path: String): Try[A] =
    runXlsx(new File(path))

  def runXlsx(file: File): Try[A] = Try {
    val wb = new XSSFWorkbook(file)
    try run(wb) finally wb.close()
  }

  def runXlsx(is: InputStream): Try[A] = Try {
    val wb = new XSSFWorkbook(is)
    try run(wb) finally wb.close()
  }

  def runXlsx(): Try[A] = Try {
    val wb = new XSSFWorkbook()
    try run(wb) finally wb.close()
  }

}

object WorkbookIO {
  
  def apply[A](f: Workbook => A): WorkbookIO[A] =
    new WorkbookIO(f)

  def pure[A](a: => A): WorkbookIO[A] =
    apply(_ => a)

  def getSheetAt(idx: Int): WorkbookIO[Sheet] =
    apply(_.getSheetAt(idx))

  def getSheet(name: String): WorkbookIO[Sheet] =
    apply(_.getSheet(name))

  def withSheetAt[A](idx: Int)(k: SheetIO[A]): WorkbookIO[A] =
    getSheetAt(idx).map(k.run)

  def withSheet[A](name: String)(k: SheetIO[A]): WorkbookIO[A] =
    getSheet(name).map(k.run)

}
