package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.SheetReader

trait SheetReaderInstances {

  implicit val sheetReaderInstances: Monad[SheetReader] = new Monad[SheetReader] {

    override def point[A](a: => A): SheetReader[A] =
      SheetReader.pure(a)

    override def map[A, B](fa: SheetReader[A])(f: A => B): SheetReader[B] =
      fa map f

    override def ap[A, B](fa: => SheetReader[A])(f: => SheetReader[A => B]): SheetReader[B] =
      f flatMap { ff => fa map ff }

    override def bind[A, B](fa: SheetReader[A])(f: A => SheetReader[B]): SheetReader[B] =
      fa flatMap f
  
  }

}

object sheetReader extends SheetReaderInstances
