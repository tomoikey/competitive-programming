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
    def one: Reader[String] = Reader { s =>
      val (matched, remains) = s.foldLeftWhile(("", s))((b, a) =>
        Option.when(!a.isWhitespace && a != '\n')((b._1 + a, b._2.tail))
      )
      (matched, if (remains.nonEmpty) remains.tail else Nil)
    }
    def listOfN(n: Int): Reader[List[String]] =
      sequence((for (_ <- 0 until n) yield one).toList)

    def sequence[A](list: List[Reader[A]]): Reader[List[A]] =
      list.foldLeft(unit(List.empty[A]))((b, a) => a.map2(b)(_ :: _))
  }

  def main(args: Array[String]): Unit = {
    val rawString =
      """|1 3
         |a
         |b
         |c
         |d""".stripMargin

    val aaa = (for {
      num1 <- Reader.one
      num2 <- Reader.one
      array <- Reader.listOfN(num1.toInt + num2.toInt)
    } yield (num1, num2, array)).run(rawString.toList)._1

    println(aaa)
  }
}
