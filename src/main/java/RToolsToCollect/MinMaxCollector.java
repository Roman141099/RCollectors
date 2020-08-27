package RToolsToCollect;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;

public class MinMaxCollector<T>
        implements Collector<T, MinMaxCollector.MinMaxCombiner<T>, Optional<MinMaxCollector.MinMaxCombiner<T>>>, Serializable {
    final private Comparator<T> comparator;

    public MinMaxCollector(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Supplier<MinMaxCombiner<T>> supplier() {
        return MinMaxCombiner::new;
    }

    @Override
    public BiConsumer<MinMaxCombiner<T>, T> accumulator() {
        return (o1, o2) -> {
            if (o1.min == null) o1.min = o2;
            if (o1.max == null) o1.max = o2;
            if (comparator.compare(o1.min, o2) > 0) {
                o1.setMin(o2);
            }
            if (comparator.compare(o1.max, o2) < 0) {
                o1.setMax(o2);
            }
        };
    }

    @Override
    public BinaryOperator<MinMaxCombiner<T>> combiner() {
        return (o1, o2) -> {
            if (o1.equals(o2)) return o1;
            if (comparator.compare(o1.min, o2.min) > 0) o1.setMin(o2.getMin());
            if (comparator.compare(o1.max, o2.max) < 0) o1.setMax(o2.getMax());
            return o1;
        };
    }

    @Override
    public Function<MinMaxCombiner<T>, Optional<MinMaxCombiner<T>>> finisher() {
        return o -> o.max == null || o.min == null ? Optional.empty() : Optional.of(o);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }

    public Comparator<T> getComparator() {
        return comparator;
    }

    public static class MinMaxCombiner<Y> {
        private Y min;
        private Y max;

        public MinMaxCombiner() {

        }
        /**
         *  Function that accepts two input arguments of same type and returns other value
         **/
        public <R> R converter(BiFunction<Y, Y, R> combine) {
            return combine.apply(min, max);
        }

        public Y getMin() {
            return min;
        }

        public void setMin(Y min) {
            this.min = min;
        }

        public Y getMax() {
            return max;
        }

        public void setMax(Y max) {
            this.max = max;
        }

        public String toString() {
            return "Max : " + max +
                    ",\nMin : " + min;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MinMaxCombiner<?> that = (MinMaxCombiner<?>) o;
            return Objects.equals(min, that.min) &&
                    Objects.equals(max, that.max);
        }
    }
}
