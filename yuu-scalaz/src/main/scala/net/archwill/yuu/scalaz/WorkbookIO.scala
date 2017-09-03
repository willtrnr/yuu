package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.WorkbookIO

trait WorkbookIOInstances {

  implicit val workbookIOInstances: Monad[WorkbookIO] = new Monad[WorkbookIO] {

    override def point[A](a: => A): WorkbookIO[A] =
      WorkbookIO.pure(a)

    override def map[A, B](fa: WorkbookIO[A])(f: A => B): WorkbookIO[B] =
      fa map f

    override def ap[A, B](fa: => WorkbookIO[A])(f: => WorkbookIO[A => B]): WorkbookIO[B] =
      f flatMap { fa map _ }

    override def bind[A, B](fa: WorkbookIO[A])(f: A => WorkbookIO[B]): WorkbookIO[B] =
      fa flatMap f

  }

}

object workbookIO extends WorkbookIOInstances
