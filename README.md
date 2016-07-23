# Java Money Profiling

**Java Money Profiling** explores a few different ways to model a monetary amount in Java.

It also provides unit tests and benchmarks that demonstrate relevant API:s and output profiling results; most notably *time cost* for serialization and *byte sizes*.

JavaDoc provided [here].

### Tech

* [Gradle] - Build tool.
* [TestNG] - Unit test runner.
* [JMH] - Benchmark runner.
* [JSR 354] - Money and Currency API.
* [Moneta] - JSR 354 Reference Implementation.
* [Chronicle Map] - Off-heap and key-value based data store.
* [FST] - Serialization framework.
* [Kryo] - Serialization framework.
* [JSR 353] - Java API for JSON Processing.
* [JSON Processing] - JSR 353 Reference Implementation.

### How to model a monetary amount

Before diving into the unit tests and benchmarks in this project, it is worthwhile taking a minute to understand the core problem any financial application written in Java face.

How do we represent a monetary amount - for example, 0.1 US dollars? With a double? BigDecimal? Something else?

Some floating-point numbers can not be represented exactly using a binary base. This has lead - for good reasons - the majority of all Java applications to shun Java's `float` and `double` in preference of a whole number, such as `int`. Float and double may cause a lot of headache for the unwary developer.

Most things we need to represent nummerically can be modelled using an integer value. For example, don't store 1.23 kilograms. Store 123 gram. Similarly, we could model 0.1 US dollars as 0 dollars and 10 cents.

If all financial markets out there traded instruments in USD, agreed on one single rounding model, and restricted cents to two digits (floating-point "scale"), then we would have no problem. We could easily model this monetary amount as two integral values. But the real world is not a kind place for us lazy people.

Essentially, a financial application has to make a decision whether or not a high degree of accuracy is needed (`BigDecimal`), or if *performance* is a greater concern (something else than BigDecimal).

I will leave the technical discussion for now. Please refer to links provided at the [bottom](#double-versus-bigdecimal-links).

### Our models and approach

The goal of this project is to model a monetary amount in a few different ways, and profile the model´s performance in terms of serialization time- and space cost. A few different serialization frameworks will be used, including the standard [Java serialization framework].

This project will mimic a complete life cycle of monetary amounts used in financial applications. They are often read as a JSON representation and cached or otherwise persisted in a backing data store. The data store we use is [Chronicle Map].

The models in this project use the rather straight forward `double` and `BigDecimal`, but will also take advantage of two [`MonetaryAmount`](https://github.com/JavaMoney/jsr354-api/blob/master/src/main/java/javax/money/MonetaryAmount.java) (JSR 354) implementations [`Money`](https://github.com/JavaMoney/jsr354-ri/blob/master/src/main/java/org/javamoney/moneta/Money.java) and [`FastMoney`](https://github.com/JavaMoney/jsr354-ri/blob/master/src/main/java/org/javamoney/moneta/FastMoney.java):

* [DoublePrice.java] > `double`
* [BigDecimalPrice.java] > `BigDecimal`
* [MoneyPrice.java] > [`Money`](https://github.com/JavaMoney/jsr354-ri/blob/master/src/main/java/org/javamoney/moneta/Money.java)
* [FastMoneyPrice.java] > [`FastMoney`](https://github.com/JavaMoney/jsr354-ri/blob/master/src/main/java/org/javamoney/moneta/FastMoney.java)

### Setup

```sh
git clone https://github.com/MartinanderssonDotcom/money-profiling
```

You don't have to install Gradle. This project uses a thing called [Gradle Wrapper] that will take care of that for you once `gradlew` is executed.

### Unit tests

[PriceSerializationTest.java] serialize/deserialize all models for a given monetary amount, for each [serialization framework], to/from a `byte[]`. It will also output the serialized byte size.

```sh
gradlew test --tests *PriceSerializationTest
```

[ChronicleMapTest.java] kind of repeat the previous test case but does not use one single monetary amount. Instead, this test case will serialize all closing prices ("end of day") for Apple Inc. as provided by [Quandl] on 2016-06-26 (8 961 closing prices), to/from a temporary file using [Chronicle Map].

```sh
gradlew test --tests *ChronicleMapTest
```

`ChronicleMapTest` output the total file sizes and will widen our perspective how the models and serialization frameworks perform in terms of space cost when dealing with many price points.

Of course, you may execute all tests in one go:

```sh
gradlew test
```

Running one or all tests yield an HTML report you can find here:

> build/reports/index.html

If the goal is to minimize space cost, then the winning combination is [`FastMoney`](https://github.com/JavaMoney/jsr354-ri/blob/master/src/main/java/org/javamoney/moneta/FastMoney.java) + [Kryo].

### Benchmarks

The unit tests are mostly concerned with space cost; how many bytes will it cost to serialize a model given a serialization framework.

However, benchmarking a piece of code in order to study the *time cost* is a science of its own. This project use [JMH] which, although not a perfect product, will make us better equipt to draw conclusions about the time cost.

[ChronicleMapBaselineBenchmark.java] provide a *baseline* for [ChronicleMapRealBenchmark.java].

The baseline will measure time cost for writting/reading a [FastMoneyPrice] to/from different `Map` implementations, including an in-memory Chronicle Map.

The "real" benchmark put Chronicle Map to the metal and perform serialization/deserialization using all the serialization frameworks. As with the baseline, only `FastMoneyPrice` is used. And as with unit tests (space cost), [Kryo] is the winner.

Which benchmark to execute is a regex passed to JMH by specifying a Gradle- or system property (-P, -D) mapped by key "[r]". This will execute the baseline:

```sh
gradlew bench -Pr=ChronicleMapBaselineBenchmark
```

This project "overrides" JMH:s regex input and offer a convenient way to launch a benchmark using only capitalized letters of a benchmark class. For example, this will launch the real Chronicle Map benchmark:

```sh
gradlew bench -Pr=CMRB
```

JMH is quite verbose in its output. You may redirect the output to a file by specifying property "[f]":

```sh
gradlew bench -Pr=ReadJsonNumberBenchmark -Pf=blabla.txt
```

Dare devils may execute all benchmarks:

```sh
gradlew bench
```

### Double versus BigDecimal links

Let me take the moment to give praise to Chronicle Map as the best Java data store of its kind. The source code and documentation is absolutely amazing. Chronicle Map is clearly written by enthusiastic gurus.

One of these gurus is [Peter Lawrey] who has authored numerous amount of blog posts about `double`versus `BigDecimal`:

 - http://vanillajava.blogspot.com/2010/06/accuracy-of-double-representation-for.html
 - http://vanillajava.blogspot.com/2011/08/double-your-money-again.html
 - http://vanillajava.blogspot.com/2012/03/different-results-summing-double.html
 - http://vanillajava.blogspot.com/2012/04/why-mathround0499999999999999917-rounds.html
 - http://vanillajava.blogspot.com/2012/11/why-doublenandoublenan-is-false.html
 - http://vanillajava.blogspot.com/2014/07/if-bigdecimal-is-answer-it-must-have.html
 - http://vanillajava.blogspot.com/2014/07/compounding-double-error.html

Also noteworthy:

 - https://lemnik.wordpress.com/2011/03/25/bigdecimal-and-your-money/

   [here]: <https://martinanderssondotcom.github.io/money-profiling/api/>
   [Gradle]: <https://gradle.org>
   [TestNG]: <http://testng.org>
   [JMH]: <http://openjdk.java.net/projects/code-tools/jmh>
   [JSR 354]: <https://github.com/JavaMoney/jsr354-api>
   [Moneta]: <http://javamoney.github.io/ri.html>
   [Chronicle Map]: <http://chronicle.software/products/chronicle-map>
   [FST]: <https://ruedigermoeller.github.io/fast-serialization>
   [Kryo]: <https://github.com/EsotericSoftware/kryo>
   [JSR 353]: <https://jcp.org/en/jsr/detail?id=353>
   [JSON Processing]: <https://jsonp.java.net>
   [Java serialization framework]: <https://docs.oracle.com/javase/8/docs/platform/serialization/spec/serialTOC.html>
   [DoublePrice.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/model/DoublePrice.java>
   [BigDecimalPrice.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/model/BigDecimalPrice.java>
   [MoneyPrice.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/model/MoneyPrice.java>
   [FastMoneyPrice.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/model/FastMoneyPrice.java>
   [Gradle Wrapper]: <https://docs.gradle.org/current/userguide/gradle_wrapper.html>
   [PriceSerializationTest.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/unittest/PriceSerializationTest.java>
   [serialization framework]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/serializer/SerializationFramework.java>
   [ChronicleMapTest.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/unittest/ChronicleMapTest.java>
   [Quandl]: <https://www.quandl.com>
   [ChronicleMapBaselineBenchmark.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/benchmark/ChronicleMapBaselineBenchmark.java>
   [ChronicleMapRealBenchmark.java]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/benchmark/ChronicleMapRealBenchmark.java>
   [FastMoneyPrice]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/model/FastMoneyPrice.java>
   [r]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/SystemProperties.java#L14-L44>
   [f]: <https://github.com/MartinanderssonDotcom/money-profiling/blob/master/src/test/java/com/martinandersson/money/lib/SystemProperties.java#L46-L54>
   [Peter Lawrey]: <http://stackoverflow.com/users/57695>
