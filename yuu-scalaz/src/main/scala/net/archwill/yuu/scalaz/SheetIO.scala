package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.SheetIO

trait SheetIOInstances {

  implicit val sheetIOInstances: Monad[SheetIO] = new Monad[SheetIO] {

    override def point[A](a: => A): SheetIO[A] =
      SheetIO.pure(a)

    override def map[A, B](fa: SheetIO[A])(f: A => B): SheetIO[B] =
      fa map f

    override def ap[A, B](fa: => SheetIO[A])(f: => SheetIO[A => B]): SheetIO[B] =
      f flatMap { fa map _ }

    override def bind[A, B](fa: SheetIO[A])(f: A => SheetIO[B]): SheetIO[B] =
      fa flatMap f

  }

}

object sheetIO extends SheetIOInstances
