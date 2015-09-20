package org.roaringbitmap.realdata;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.roaringbitmap.realdata.state.BenchmarkState;
import org.roaringbitmap.realdata.wrapper.BitmapAggregator;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RealDataBenchmarkWideAndNaive {

   @Benchmark
   public int wideAnd_naive(BenchmarkState bs) {
      BitmapAggregator aggregator = bs.bitmaps.get(0).naiveAndAggregator();
      return aggregator.aggregate(bs.bitmaps).cardinality();
   }

}
