package org.roaringbitmap;

import org.junit.Assert;
import org.junit.Test;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.roaringbitmap.RandomisedTestData.ITERATIONS;
import static org.roaringbitmap.RandomisedTestData.randomBitmap;
import static org.roaringbitmap.Util.toUnsignedLong;

public class BufferFuzzer {

  @FunctionalInterface
  interface IntBitmapPredicate {
    boolean test(int index, MutableRoaringBitmap bitmap);
  }

  public static <T> void verifyInvarianceArray(Function<ImmutableRoaringBitmap[], T> left,
                                               Function<ImmutableRoaringBitmap[], T> right) {
    verifyInvarianceArray(ITERATIONS, 1 << 5, 96, left, right);
  }

  @FunctionalInterface
  interface RangeBitmapPredicate {
    boolean test(long min, long max, ImmutableRoaringBitmap bitmap);
  }

  public static void verifyInvariance(int maxKeys, RangeBitmapPredicate pred) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    IntStream.range(0, ITERATIONS)
            .parallel()
            .mapToObj(i -> randomBitmap(maxKeys))
            .forEach(bitmap -> {
              long min = random.nextLong(1L << 32);
              long max = random.nextLong(min,1L << 32);
              Assert.assertTrue(pred.test(min, max, bitmap));
            });
  }

  public static <T> void verifyInvarianceArray(int count,
                                               int maxKeys,
                                               int setSize,
                                               Function<ImmutableRoaringBitmap[], T> left,
                                               Function<ImmutableRoaringBitmap[], T> right) {
    IntStream.range(0, count)
            .parallel()
            .mapToObj(i -> IntStream.range(0, setSize)
                    .mapToObj(j -> randomBitmap(maxKeys))
                    .toArray(ImmutableRoaringBitmap[]::new))
            .forEach(bitmap -> Assert.assertEquals(left.apply(bitmap), right.apply(bitmap)));
  }

  public static <T> void verifyInvariance(Function<MutableRoaringBitmap, T> left, Function<MutableRoaringBitmap, T> right) {
    verifyInvariance(ITERATIONS, 1 << 8, rb -> true, left, right);
  }

  public static <T> void verifyInvariance(Predicate<MutableRoaringBitmap> validity,
                                          Function<MutableRoaringBitmap, T> left,
                                          Function<MutableRoaringBitmap, T> right) {
    verifyInvariance(ITERATIONS, 1 << 8, validity, left, right);
  }

  public static <T> void verifyInvariance(int count,
                                          int maxKeys,
                                          Predicate<MutableRoaringBitmap> validity,
                                          Function<MutableRoaringBitmap, T> left,
                                          Function<MutableRoaringBitmap, T> right) {
    IntStream.range(0, count)
            .parallel()
            .mapToObj(i -> randomBitmap(maxKeys))
            .filter(validity)
            .forEach(bitmap -> Assert.assertEquals(left.apply(bitmap), right.apply(bitmap)));
  }

  public static <T> void verifyInvariance(BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> left,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> right) {
    verifyInvariance(ITERATIONS, 1 << 8, left, right);
  }

  public static <T> void verifyInvariance(BiPredicate<MutableRoaringBitmap, MutableRoaringBitmap> validity,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> left,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> right) {
    verifyInvariance(validity,ITERATIONS, 1 << 8, left, right);
  }


  public static <T> void verifyInvariance(int count,
                                          int maxKeys,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> left,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> right) {
    verifyInvariance((l, r) -> true, count, maxKeys, left, right);
  }

  public static <T> void verifyInvariance(BiPredicate<MutableRoaringBitmap, MutableRoaringBitmap> validity,
                                          int count,
                                          int maxKeys,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> left,
                                          BiFunction<MutableRoaringBitmap, MutableRoaringBitmap, T> right) {
    IntStream.range(0, count)
            .parallel()
            .forEach(i -> {
              MutableRoaringBitmap one = randomBitmap(maxKeys);
              MutableRoaringBitmap two = randomBitmap(maxKeys);
              if (validity.test(one, two)) {
                Assert.assertEquals(left.apply(one, two), right.apply(one, two));
              }
            });
  }

  public static void verifyInvariance(IntBitmapPredicate predicate) {
    verifyInvariance(rb -> true, predicate);
  }

  public static void verifyInvariance(Predicate<MutableRoaringBitmap> validity,
                                      IntBitmapPredicate predicate) {
    verifyInvariance(validity, ITERATIONS, 1 << 3, predicate);
  }

  public static void verifyInvariance(Predicate<MutableRoaringBitmap> validity,
                                      int count,
                                      int maxKeys,
                                      IntBitmapPredicate predicate) {
    IntStream.range(0, count)
            .parallel()
            .mapToObj(i -> randomBitmap(maxKeys))
            .filter(validity)
            .forEach(bitmap -> {
              for (int i = 0; i < bitmap.getCardinality(); ++i) {
                Assert.assertTrue(predicate.test(i, bitmap));
              }
            });
  }

  public static <T> void verifyInvariance(T value, Function<MutableRoaringBitmap, T> func) {
    verifyInvariance(ITERATIONS, 1 << 9, value, func);
  }

  public static <T> void verifyInvariance(int count,
                                          int maxKeys,
                                          T value,
                                          Function<MutableRoaringBitmap, T> func) {
    IntStream.range(0, count)
            .parallel()
            .mapToObj(i -> randomBitmap(maxKeys))
            .forEach(bitmap -> Assert.assertEquals(value, func.apply(bitmap)));
  }

  @Test
  public void rankSelectInvariance() {
    verifyInvariance(bitmap -> !bitmap.isEmpty(), (i, rb) -> rb.rank(rb.select(i)) == i + 1);
  }

  @Test
  public void selectContainsInvariance() {
    verifyInvariance(bitmap -> !bitmap.isEmpty(), (i, rb) -> rb.contains(rb.select(i)));
  }

  @Test
  public void firstSelect0Invariance() {
    verifyInvariance(bitmap -> !bitmap.isEmpty(),
                     bitmap -> bitmap.first(),
                     bitmap -> bitmap.select(0));
  }

  @Test
  public void lastSelectCardinalityInvariance() {
    verifyInvariance(bitmap -> !bitmap.isEmpty(),
                     bitmap -> bitmap.last(),
                     bitmap -> bitmap.select(bitmap.getCardinality() - 1));
  }

  @Test
  public void intersectsRangeFirstLastInvariance() {
    verifyInvariance(true, rb -> rb.intersects(toUnsignedLong(rb.first()), toUnsignedLong(rb.last())));
  }

  @Test
  public void containsRangeFirstLastInvariance() {
    verifyInvariance(true,
            rb -> MutableRoaringBitmap.add(rb.clone(), toUnsignedLong(rb.first()), toUnsignedLong(rb.last()))
                    .contains(toUnsignedLong(rb.first()), toUnsignedLong(rb.last())));
  }

  @Test
  public void andCardinalityInvariance() {
    verifyInvariance(ITERATIONS, 1 << 9,
            (l, r) -> MutableRoaringBitmap.and(l, r).getCardinality(),
            (l, r) -> MutableRoaringBitmap.andCardinality(l, r));
  }

  @Test
  public void orCardinalityInvariance() {
    verifyInvariance(ITERATIONS, 1 << 9,
            (l, r) -> MutableRoaringBitmap.or(l, r).getCardinality(),
            (l, r) -> MutableRoaringBitmap.orCardinality(l, r));
  }

  @Test
  public void xorCardinalityInvariance() {
    verifyInvariance(ITERATIONS, 1 << 9,
            (l, r) -> MutableRoaringBitmap.xor(l, r).getCardinality(),
            (l, r) -> MutableRoaringBitmap.xorCardinality(l, r));
  }

  @Test
  public void containsContainsInvariance() {
    verifyInvariance((l, r) -> l.contains(r) && !r.equals(l),
            (l, r) -> false,
            (l, r) -> !r.contains(l));
  }

  @Test
  public void containsAndInvariance() {
    verifyInvariance((l, r) -> l.contains(r), (l, r) -> MutableRoaringBitmap.and(l, r).equals(r));
  }

  @Test
  public void andCardinalityContainsInvariance() {
    verifyInvariance((l, r) -> MutableRoaringBitmap.andCardinality(l, r) == 0,
            (l, r) -> false,
            (l, r) -> l.contains(r) || r.contains(l));
  }

  @Test
  public void sizeOfUnionOfDisjointSetsEqualsSumOfSizes() {
    verifyInvariance((l, r) -> MutableRoaringBitmap.andCardinality(l, r) == 0,
            (l, r) -> l.getCardinality() + r.getCardinality(),
            (l, r) -> MutableRoaringBitmap.orCardinality(l, r));
  }

  @Test
  public void sizeOfDifferenceOfDisjointSetsEqualsSumOfSizes() {
    verifyInvariance((l, r) -> MutableRoaringBitmap.andCardinality(l, r) == 0,
            (l, r) -> l.getCardinality() + r.getCardinality(),
            (l, r) -> MutableRoaringBitmap.xorCardinality(l, r));
  }

  @Test
  public void equalsSymmetryInvariance() {
    verifyInvariance((l, r) -> l.equals(r), (l, r) -> r.equals(l));
  }

  @Test
  public void orOfDisjunction() {
    verifyInvariance(ITERATIONS, 1 << 8,
            (l, r) -> l,
            (l, r) -> MutableRoaringBitmap.or(l, MutableRoaringBitmap.and(l, r)));
  }

  @Test
  public void orCoversXor() {
    verifyInvariance(ITERATIONS, 1 << 8,
            (l, r) -> MutableRoaringBitmap.or(l, r),
            (l, r) -> MutableRoaringBitmap.or(l, MutableRoaringBitmap.xor(l, r)));
  }

  @Test
  public void xorInvariance() {
    verifyInvariance(ITERATIONS, 1 << 9,
            (l, r) -> MutableRoaringBitmap.xor(l, r),
            (l, r) -> MutableRoaringBitmap.andNot(MutableRoaringBitmap.or(l, r), MutableRoaringBitmap.and(l, r)));
  }

  @Test
  public void rangeCardinalityVsMaterialisedRange() {
    verifyInvariance(1 << 9,
            (min, max, bitmap) -> {
              MutableRoaringBitmap range = new MutableRoaringBitmap();
              range.add(min, max);
              return bitmap.rangeCardinality(min, max) == ImmutableRoaringBitmap.andCardinality(range, bitmap);
            });
  }

  private static MutableRoaringBitmap randomBitmap(int maxKeys) {
    return RandomisedTestData.randomBitmap(maxKeys).toMutableRoaringBitmap();
  }
}
