package dataClasses;
import java.util.Map;
import static java.util.Map.entry; 

public class CircleMoveMap {
    public final static Map<Integer, Coordinate> moves = Map.ofEntries(
        entry(6, new Coordinate(1)),
        entry(1, new Coordinate(6)),  
        entry(12, new Coordinate(2)), 
        entry(2, new Coordinate(12)), 
        entry(4, new Coordinate(11)),
        entry(11, new Coordinate(4)),
        entry(3, new Coordinate(17)),
        entry(17, new Coordinate(3)),
        entry(24, new Coordinate(31)),
        entry(31, new Coordinate(24)),
        entry(18, new Coordinate(32)),
        entry(32, new Coordinate(18)),
        entry(23, new Coordinate(33)),
        entry(33, new Coordinate(23)),
        entry(34, new Coordinate(29)),
        entry(29, new Coordinate(34))
    );
}