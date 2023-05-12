# Competitive Programming Standard Input with Scala
# 使い方
1mmも外部ライブラリに依存してないので`Main.scala`をコピペすればどこでも動くはずです
```scala
Reader.one[A]: Reader[A] ... スペースあるいは改行を見つけるまで文字を結合しAに型変換する

Reader.listOfN[A](n: Int): Reader[List[A]] ... Reader.one[A]をn回実行する

Reader.readOneLine[A]: Reader[A] ... 改行を見つけるまで文字を結合しAに型変換する

Reader.readMultipleLine[A](n: Int): Reader[List[A]] ... Reader.readOneLine[A]をn回実行する
```

# 例外
　`A`に変換できなかった場合は例外を投げます。
 ```scala
 Reader.one[Int].run("1".toList) ... OK
 Reader.one[Int].run("hello".toList) ... Error
 ```
 
 # Tuple
 Tuple2, Tuple3まで対応した。競プロで1行に4つ並んでいて尚且つそれが複数列になってるところを見たことなかったので
 ```scala
 Reader.readOneLine[(Int, String)].run("1 hoge".toList) ... OK
 Reader.readOneLine[(Int, String, Int)].run("1 hoge 23".toList) ... OK
 Reader.readOneLine[(Int, String, Int, String)].run("1 hoge 23 fuga".toList) ... NG: 未対応
 ```
