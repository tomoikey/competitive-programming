import Main.{FoldLeftWhile, ParserString, stringMonoid}

import scala.language.implicitConversions
import scala.util.chaining.scalaUtilChainingOps

object Main {
  implicit class FoldLeftWhile[A](itOnce: IterableOnce[A]) {
    def foldLeftWhile[B](z: B)(op: (B, A) => Option[B]): B = {
      val iterator = itOnce.iterator
      var result = z
      var next = iterator.nextOption()
      while (next.isDefined) {
        op(result, next.get).fold {
          next = None
        } { b =>
          result = b
          next = iterator.nextOption()
        }
      }
      result
    }
  }

  implicit object ParserInt extends Parser[Int] {
    override def to[B](from: B): Int = from.toString.toInt
  }

  implicit object ParserString extends Parser[String] {
    override def to[B](from: B): String = from.toString
  }

  implicit val stringMonoid: Monoid[String] = new Monoid[String] {
    override def op(a1: String, a2: String): String = a1 + a2

    override def zero: String = ""
  }

  implicit def listMonoid[A]: Monoid[List[A]] = new Monoid[List[A]] {
    override def op(a1: List[A], a2: List[A]): List[A] = a1 ++ a2

    override def zero: List[A] = Nil
  }

  implicit val intAddition: Monoid[Int] = new Monoid[Int] {
    override def op(a1: Int, a2: Int): Int = a1 + a2

    override def zero: Int = 0
  }

  implicit def optionMonoid[A](monoid: Monoid[A]): Monoid[Option[A]] =
    new Monoid[Option[A]] {
      override def op(a1: Option[A], a2: Option[A]): Option[A] = for {
        aa1 <- a1
        aa2 <- a2
      } yield monoid.op(aa1, aa2)

      override def zero: Option[A] = None
    }

  // 競プロのコードはここのmain関数に書いて下さい
  def main(args: Array[String]): Unit = {
    val sampleInput =
      """|1 3
         |a
         |b
         |c
         |d
         |hoge fuga piyo
         |I'm a champiyoyoyyyon""".stripMargin

    (for {
      num1 <- Reader.one[Int]
      num2 <- Reader.one[Int]
      array <- Reader.listOfN[String](num1 + num2)
      line <- Reader.readOneLine
      line2 <- Reader.readOneLine
    } yield (num1, num2, array, line, line2))
      .run(sampleInput.toList)
      ._1
      .tap(println)
  }
}

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}

trait Parser[A] {
  def to[B](from: B): A
}

final case class Reader[+A](run: List[Char] => (A, List[Char])) {
  def map[B](f: A => B): Reader[B] = flatMap(n => Reader.unit(f(n)))

  def map2[B, C](that: Reader[B])(f: (A, B) => C): Reader[C] = for {
    one <- this
    two <- that
  } yield f(one, two)

  def flatMap[B](f: A => Reader[B]): Reader[B] = Reader { s =>
    val (a, ss) = run(s)
    f(a).run(ss)
  }
}

private object Reader {
  def unit[A](a: => A): Reader[A] = Reader((a, _))

  def extractor[A](
      cond: Char => Boolean
  )(implicit monoid: Monoid[A], parser: Parser[A]): Reader[A] = Reader { s =>
    val (matched, remains) = s.foldLeftWhile((monoid.zero, s))((b, a) =>
      Option.when(cond(a))((monoid.op(b._1, parser.to(a)), b._2.tail))
    )
    (matched, if (remains.nonEmpty) remains.tail else Nil)
  }

  def one[A](implicit monoid: Monoid[A], parser: Parser[A]): Reader[A] =
    extractor[A](a => !a.isWhitespace && a != '\n')

  def readOneLine: Reader[String] =
    extractor(_ != '\n')(stringMonoid, ParserString)

  def readMultipleLine(n: Int): Reader[List[String]] = sequence(
    (for (_ <- 0 until n) yield readOneLine).toList
  )

  def listOfN[A: Parser: Monoid](n: Int): Reader[List[A]] =
    sequence((for (_ <- 0 until n) yield one[A]).toList)

  def sequence[A](list: List[Reader[A]]): Reader[List[A]] =
    list.foldLeft(unit(List.empty[A]))((b, a) => a.map2(b)(_ :: _))
}
