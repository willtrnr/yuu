package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.CellReader

trait CellReaderInstances {

  implicit val cellReaderInstances: Monad[CellReader] = new Monad[CellReader] {

    override def point[A](a: => A): CellReader[A] =
      CellReader.pure(a)

    override def map[A, B](fa: CellReader[A])(f: A => B): CellReader[B] =
      fa map f

    override def ap[A, B](fa: => CellReader[A])(f: => CellReader[A => B]): CellReader[B] =
      f flatMap { ff => fa map ff }

    override def bind[A, B](fa: CellReader[A])(f: A => CellReader[B]): CellReader[B] =
      fa flatMap f
  
  }

}

object cellReader extends CellReaderInstances
