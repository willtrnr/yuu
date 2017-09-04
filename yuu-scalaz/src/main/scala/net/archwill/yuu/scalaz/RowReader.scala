package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.RowReader

trait RowReaderInstances {

  implicit val rowReaderInstances: Monad[RowReader] = new Monad[RowReader] {

    override def point[A](a: => A): RowReader[A] =
      RowReader.pure(a)

    override def map[A, B](fa: RowReader[A])(f: A => B): RowReader[B] =
      fa map f

    override def ap[A, B](fa: => RowReader[A])(f: => RowReader[A => B]): RowReader[B] =
      f flatMap { ff => fa map ff }

    override def bind[A, B](fa: RowReader[A])(f: A => RowReader[B]): RowReader[B] =
      fa flatMap f
  
  }

}

object rowReader extends RowReaderInstances
