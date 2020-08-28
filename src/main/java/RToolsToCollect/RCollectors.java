package RToolsToCollect;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RCollectors {
    /**
     * @param comparator describes comparator that comparing elements in stream
     * @param <T>        is an element type in stream
     * @return Collector that used for searching minimum and maximum element in stream
     * This collector has valid work with parallel streams and if stream contains one more
     * min or max element this collector returns first min and max element it sees
     */

    public static <T> Collector<T, ?, Optional<MinMaxCollector.MinMaxCombiner<T>>> minMax(Comparator<T> comparator) {
        return new MinMaxCollector<>(comparator);
    }

    public static <T, R extends Collection<T>> Collector<T, ?, List<T>> allMaxOrMin(Comparator<T> maxOrMin) {
        class AllMaxOrMinContainer {
            List<T> elements = new ArrayList<>();

            public AllMaxOrMinContainer() {

            }

            public AllMaxOrMinContainer(List<T> elements) {
                this.elements = elements;
            }

            void add(T t) {
                if (elements.size() == 0) {
                    elements.add(t);
                } else {
                    boolean allLower = elements.stream().
                            allMatch(o -> maxOrMin.compare(o, t) < 0);
                    boolean allSame = elements.stream().
                            allMatch(o -> maxOrMin.compare(o, t) == 0);
                    if (allLower) {
                        elements.clear();
                        elements.add(t);
                    }
                    if (allSame) {
                        elements.add(t);
                    }
                }
            }

            AllMaxOrMinContainer combine(AllMaxOrMinContainer other) {
                if (maxOrMin.compare(elements.get(0), other.elements.get(0)) > 0) {
                    return this;
                }
                if (maxOrMin.compare(elements.get(0), other.elements.get(0)) < 0) {
                    return other;
                }
                List<T> list = elements;
                list.addAll(other.elements);
                return new AllMaxOrMinContainer(list);
//                throw new UnsupportedOperationException("This collector is not supported by parallel stream");
            }
        }
        return Collector.of(
                AllMaxOrMinContainer::new,
                AllMaxOrMinContainer::add,
                AllMaxOrMinContainer::combine,
                o -> o.elements
        );
    }

    static class UnsupportedByParallelCollector extends RuntimeException {

        public UnsupportedByParallelCollector() {
            super("This collector is not support parallel operations");
        }

        public UnsupportedByParallelCollector(String message) {
            super(message);
        }
    }

    public static <T> Collector<T, ?, Collection<String>> pairCollection(String delimiter) {
        class Container {
            private T last;
            private final Collection<String> collection;
            private int count;

            public Container() {
                collection = new ArrayList<>();
            }

            public Container(Collection<String> collection) {
                this.collection = collection;
            }

            void add(T t) {
                if (count > 0) {
                    collection.add(last + delimiter + t);
                    last = t;
                } else {
                    last = t;
                    count++;
                }
            }

            Container combine(Container other) {
                throw new UnsupportedByParallelCollector();
            }
        }
        return Collector.of(
                Container::new,
                Container::add,
                Container::combine,
                o -> o.collection
        );
    }
//    public static <T, R extends Collection<T>> Collector<T, ?, List<T>> pairCollection(String delimiter,
//                                                                                    Supplier<R> chooser,
//                                                                                 Function<T, String> converter) {
//        BiFunction<? super R, ? super R, List<T>> finisher = (o1, o2) -> {
//            o1.addAll(o2);return o1;};
//        class Container {
//            private T last;
//            private final List<T> collection;
//            private int count;
//
//            public Container() {
//                collection = new ArrayList<>();
//            }
//
//            public Container(List<T> collection) {
//                this.collection = collection;
//            }
//
//            void add(T t) {
//                if (count > 0) {
//                    collection.add(last);
//                    last = t;
//                } else {
//                    last = t;
//                    count++;
//                }
//            }
//
//            Container combine(Container other) {
//                throw new UnsupportedByParallelCollector();
//            }
//        }
//        return Collector.of(
//                Container::new,
//                Container::add,
//                Container::combine,
//                o -> finisher.apply(chooser.get(), o.collection)
//        );
//    }

    /**
     * @param keyMapper     the function that accepts element of stream and returns an element of other type(it is a key in main map)
     * @param mainCreate    the supplier that returns target exit container
     * @param subMainCreate the supplier that returns target container of values in main map
     * @param valueMapper   the function that accepts element of stream and returns an element of other type(it is a value in sub container)
     * @param finish        the finisher function
     * @param <T>           type of input elements
     * @param <TT>          type of key in main map
     * @param <M>           type of sub container
     * @param <R>           type of finish result
     * @param <E>           type of elements in sub container
     * @return
     */
    public static <T, TT, M extends Collection<E>, E, R> Collector<T, ?, R> advancedGrouping(Function<? super T, ? extends TT> keyMapper,
                                                                                             Supplier<Map<TT, M>> mainCreate,
                                                                                             Supplier<M> subMainCreate,
                                                                                             Function<? super T, ? extends E> valueMapper,
                                                                                             Function<Map<TT, M>, ? extends R> finish) {
        class Container {
            final Map<TT, M> exitMap;

            Container() {
                exitMap = mainCreate.get();
            }

            Map<TT, M> getExitMap() {
                return exitMap;
            }

            void add(T t) {
                if (exitMap.containsKey(keyMapper.apply(t))) {
                    exitMap.get(keyMapper.apply(t)).add(valueMapper.apply(t));
                } else {
                    M l = subMainCreate.get();
                    l.add(valueMapper.apply(t));
                    exitMap.put(keyMapper.apply(t), l);
                }
            }

            Container combine(Container other) {
                Map<TT, M> notContainsMap = other.exitMap.entrySet().
                        stream().filter(o -> {
                    if (exitMap.containsKey(o.getKey())) {
                        exitMap.get(o.getKey()).addAll(o.getValue());
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
                exitMap.putAll(notContainsMap);
                return this;
            }
        }
        return Collector.of(
                Container::new,
                Container::add,
                Container::combine,
                container -> finish.apply(container.getExitMap())
        );
    }

    /**
     * This collector is not support input null elements
     * and not support operations with parallel streams, only sequential operations with streams
     * because we need do operations with elements that close to each other
     *
     * @param valListCreate the supplier that returns type of result collection
     * @param <T> type of input elements
     * @param <L> type of result collection
     * @param <R> type of finished result
     * @return a new Collector
     */
    public static <T, L extends Collection<T>, R> Collector<T, ?, R> doOperationWithSameObjs(
            Supplier<L> valListCreate,
            BinaryOperator<T> operation,
            Function<? super L,? extends R> finish) {
        class Container {
            private final Map<Integer, L> samples;
            private T element;
            private int index;

            Container() {
                samples = new HashMap<>();
            }

            void add(T el) {
                if (element == null) {
                    L valList = valListCreate.get();
                    valList.add(el);
                    samples.put(index, valList);
                    element = el;
                } else {
                    if (el.equals(element)) {
                        samples.get(index).add(el);
                    } else {
                        L valList = valListCreate.get();
                        valList.add(el);
                        samples.put(++index, valList);
                        element = el;
                    }
                }
            }

            Container combine(Container other) {
                throw new UnsupportedByParallelCollector();
            }

            public L doOperation(){
                return samples.values().stream().
                        map(o -> o.stream().reduce(operation).orElse(null)).
                        collect(Collectors.toCollection(valListCreate));
            }
        }
        return Collector.of(
                Container::new,
                Container::add,
                Container::combine,
                o -> finish.apply(o.doOperation())
        );
    }
}