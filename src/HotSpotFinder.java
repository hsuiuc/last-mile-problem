import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by haosun on 4/21/18.
 */
public class HotSpotFinder {
    private static final double walkingDistanceIn10Min = 0.6;
    private static final double drivingDistanceIn60Min = 7.5;
    private static final int reachableBusStops = 6;
    private static final String outputFilePath = "./data/debug_1.csv";

    /**
     * compute distance between two places, assum these two places are near,
     * consider the earth as a flat plane
     * (-87.3526, 41.4637, -87.3749, 41.9857)
     * @param lat1 latitude of the first place
     * @param lon1 longitude of the first place
     * @param lat2 latitude of the second place
     * @param lon2 longitude of the second place
     * @return distance in miles
     */
    private static double getDistanceFromLatLonInKm(double lat1, double lon1,
                                                    double lat2, double lon2) {

        double a = (lat1-lat2) * distPerLat((lat1 + lat2) / 2);
        double b = (lon1-lon2)* distPerLng((lon1 + lon2) / 2);
        return Math.sqrt(a*a+b*b) / 1609;

    }

    private static double distPerLng(double lon){
        return 0.0003121092*Math.pow(lon, 4)
                +0.0101182384*Math.pow(lon, 3)
                -17.2385140059*lon*lon
                +5.5485277537*lon+111301.967182595;
    }

    private static double distPerLat(double lat){
        return -0.000000487305676*Math.pow(lat, 4)
                -0.0033668574*Math.pow(lat, 3)
                +0.4601181791*lat*lat
                -1.4558127346*lat+110579.25662316;
    }

    /**
     * get all the coordinates within a distance range from a point
     * @param coordinates all the coordinates, sorted by latitudes
     * @param lat the latitude of the point
     * @param lon the longitude of the point
     * @param range the range in miles
     * @return all the coordinates
     */
    private static List<? extends Coordinate> getCoordinatesWithinRange(List<? extends Coordinate> coordinates,
                                                                        double lat, double lon, double range) {
        List<Double> latList = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            latList.add(coordinate.getPoint_x());
        }
        int index = Collections.binarySearch(latList, lat);
        if (index < 0) index = ~index;

        int upperBound = 0;
        int lo = 0, hi = index - 1;
        while (lo <= hi) {
            upperBound = lo + (hi - lo) / 2;
            double dis = getDistanceFromLatLonInKm(coordinates.get(upperBound).getPoint_x(), lon, lat, lon);
            if (dis <= range) {
                hi = upperBound - 1;
            } else {
                lo = upperBound + 1;
            }
        }

        int lowerBound = coordinates.size() - 1;
        lo = index; hi = coordinates.size() - 1;
        while (lo <= hi) {
            lowerBound = lo + (hi - lo) / 2;
            double dis = getDistanceFromLatLonInKm(coordinates.get(lowerBound).getPoint_x(), lon, lat, lon);
            if (dis <= range) {
                lo = lowerBound + 1;
            } else {
                hi = lowerBound - 1;
            }
        }

        List<Coordinate> result = new LinkedList<>();
        for (int i = Math.max(0, upperBound); i <= Math.min(coordinates.size() - 1, lowerBound); i++) {
            double busStopLat = coordinates.get(i).getPoint_x();
            double busStopLon = coordinates.get(i).getPoint_y();
            double distance = getDistanceFromLatLonInKm(busStopLat, busStopLon, lat, lon);
            if (distance < range) {
                result.add(coordinates.get(i));
            }
        }
        return result;
    }

    private static Set<BusStop> getReachableBusStops(Map<String, BusLine> busLineMap, List<BusStop> startingBusStops) {
        Set<BusStop> result = new HashSet<>();
        for (BusStop start : startingBusStops) {
            for (String route : start.getRoutes()) {
                if (busLineMap.containsKey(route)) {
                    BusLine busLine = busLineMap.get(route);
                    List<BusStop> busStops = busLine.getStops();
                    int index = Collections.binarySearch(busStops, start);

                    for (int i = 0; i < reachableBusStops; i++) {
                        if (index - i >= 0)
                            result.add(busStops.get(index - i));
                        if (index + i < busStops.size())
                            result.add(busStops.get(index + i));
                    }
                }
            }
        }
        return result;
    }

    private static Set<HotSpot> getReachableHotSpots(List<HotSpot> hotSpots, Set<BusStop> busStops) {
        Set<HotSpot> result = new HashSet<>();
        List<HotSpot> list = null;
        for (BusStop busStop : busStops) {
            list = (List<HotSpot>) getCoordinatesWithinRange(hotSpots, busStop.getPoint_x(), busStop.getPoint_y(), walkingDistanceIn10Min);
            result.addAll(list);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new Date());
        FileParser fileParser = new FileParser();
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath));

        Map<String, BusLine> busLineMap = fileParser.getBusLinesFromFile();

        List<BusStop> busStopList = fileParser.getBusStopsFromFile(busLineMap);

        List<HotSpot> hotSpotList = fileParser.getHotSpotsFromFile();

        List<Coordinate> parcelList = fileParser.getParcelFromFile();

        StringBuilder sb = new StringBuilder(1000);


        int counter = 0;
        int size = parcelList.size();
        int batch = size / 100;
        for (Coordinate parcel : parcelList) {
            if (counter++ % batch == 0) {
                System.out.println(counter  / batch);
            }

            sb.setLength(0);
            List<BusStop> startingBusStops = (List<BusStop>) HotSpotFinder.getCoordinatesWithinRange(busStopList, parcel.getPoint_x(),
                    parcel.getPoint_y(), walkingDistanceIn10Min);

            Set<BusStop> reachableBusStops = HotSpotFinder.getReachableBusStops(busLineMap, startingBusStops);

            List<HotSpot> hotSpotsInDrivingDistance = (List<HotSpot>) HotSpotFinder.getCoordinatesWithinRange(hotSpotList, parcel.getPoint_x(),
                    parcel.getPoint_y(), drivingDistanceIn60Min);

            Set<HotSpot> reachableHotSpots = HotSpotFinder.getReachableHotSpots(hotSpotList, reachableBusStops);

            double reachableIndex = (double) (reachableHotSpots.size()) / (double) (hotSpotsInDrivingDistance.size()) * 100;

            sb.append(parcel).append(',').append(reachableIndex).append('\n');
            bw.write(sb.toString());

        }

        bw.close();

        System.out.println(new Date());
    }
}
