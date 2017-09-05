package net.archwill.yuu

import org.apache.poi.ss.usermodel.Sheet

final class SheetIO[A](val run: Sheet => A) {

  def map[B](f: A => B): SheetIO[B] =
    SheetIO(run andThen f)

  def flatMap[B](f: A => SheetIO[B]) =
    SheetIO(s => f(run(s)).run(s))

}

object SheetIO {

  def apply[A](f: Sheet => A): SheetIO[A] =
    new SheetIO(f)

  def pure[A](a: => A): SheetIO[A] =
    apply(_ => a)

  def read[A](implicit sr: SheetReader[A]): SheetIO[ReadResult[A]] =
    apply(sr.read)

}
