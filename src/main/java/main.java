import RToolsToCollect.RCollectors;

import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;

public class main {
    public static void main(String[] args) {
        String s = new Random().ints(5, 40).parallel().distinct().
                limit(20).peek(System.out::println).boxed().collect(RCollectors.minMax(Integer::compare)).
                orElse(null).converter((o1, o2) -> "Min : " + o1 + ", max : " + o2);
        System.out.println(s);
    }
}
