import RToolsToCollect.RCollectors;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class main {
    public static void main(String[] args) {
//        String s = new Random().ints(5, 40).parallel().distinct().
//                limit(20).peek(System.out::println).boxed().collect(RCollectors.minMax(Integer::compare)).
//                orElse(null).converter((o1, o2) -> "Min : " + o1 + ", max : " + o2);
//        System.out.println(s);
        List<Integer> l = Arrays.asList(1, 1, 1, 3, 2, 2, 2, 0, 1, 6, 6, 6);
        String mapka = l.stream().collect(RCollectors.doOperationWithSameObjs(
                ArrayList::new,
                (o1, o2) -> o1 - o2,
                o -> o.stream().map(String::valueOf).collect(Collectors.joining(", "))
        ));
        System.out.println(mapka);
    }
}
