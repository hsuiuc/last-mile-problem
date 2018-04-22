import java.util.*;

/**
 * Created by haosun on 4/21/18.
 */
public class HotSpotFinder {
    private static final double walkingDistanceIn10Min = 0.1d;
    private static final double drivingDistanceIn60Min = 30d;
    private static final int reachableBusStops = 10;

    private static double getDistanceFromLatLonInKm(double lat1, double lon1,
                                                    double lat2, double lon2) {
        double R = 6371d; // Radius of the earth in km
        double dLat = deg2rad(lat2-lat1);  // deg2rad below
        double dLon = deg2rad(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return c * R;

    }

    private static double deg2rad(double deg) {
        return deg * Math.PI / 180d;
    }

    private static List<? extends Coordinate> getCoordinatesWithinRange(List<? extends Coordinate> coordinates,
                                                                        double lat, double lon, double range) {
        List<Double> latList = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            latList.add(coordinate.getPoint_x());
        }
        int index = Collections.binarySearch(latList, lat);
        if (index < 0) index = ~index;
        int upperBound = index - 1;
        while (upperBound >= 1 && getDistanceFromLatLonInKm(coordinates.get(upperBound).getPoint_x(),
                lon, lat, lon) < range) {
            upperBound--;
        }

        int lowerBound = index;
        while (lowerBound < coordinates.size() - 1 && getDistanceFromLatLonInKm(coordinates.get(lowerBound).getPoint_x(),
                lon, lat, lon) < range) {
            lowerBound++;
        }

//        System.out.println("upper" + upperBound);
//        System.out.println("lower" + lowerBound);
        List<Coordinate> result = new LinkedList<>();
        for (int i = upperBound; i <= lowerBound; i++) {
            double busStopLat = coordinates.get(i).getPoint_x();
            double busStopLon = coordinates.get(i).getPoint_y();
            double distance = getDistanceFromLatLonInKm(busStopLat, busStopLon, lat, lon);
            if (distance < range) {
//                System.out.println("distance" + distance);
//                System.out.println("range" + range);
                result.add(coordinates.get(i));
            }
        }
        return result;
    }

    private static List<BusStop> getStartingBusStops(List<BusStop> busStopList, double lat, double lon) {
        List<Double> latList = new ArrayList<>();
        for (BusStop busStop : busStopList) {
            latList.add(busStop.getPoint_x());
        }
        int index = Collections.binarySearch(latList, lat);
        if (index < 0) index = ~index;
        int upperBound = index - 1;
        while (upperBound >= 1 && getDistanceFromLatLonInKm(busStopList.get(upperBound).getPoint_x(),
                lon, lat, lon) < walkingDistanceIn10Min) {
            upperBound--;
        }

        int lowerBound = index;
        while (lowerBound < busStopList.size() - 1 && getDistanceFromLatLonInKm(busStopList.get(lowerBound).getPoint_x(),
                lon, lat, lon) < walkingDistanceIn10Min) {
            lowerBound++;
        }

//        System.out.println("upper" + upperBound);
//        System.out.println("lower" + lowerBound);
        List<BusStop> result = new LinkedList<>();
        for (int i = upperBound; i <= lowerBound; i++) {
            double busStopLat = busStopList.get(i).getPoint_x();
            double busStopLon = busStopList.get(i).getPoint_y();
            if (getDistanceFromLatLonInKm(busStopLat, busStopLon, lat, lon) < walkingDistanceIn10Min) {
                result.add(busStopList.get(i));
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
//                    System.out.println("index" + index);
                    for (int i = 0; i < reachableBusStops; i++) {
                        if (index - i >= 0)
                            result.add(busStops.get(index - i));
                        if (index + i < busStops.size())
                            result.add(busStops.get(index + i));
                    }
                } else {
                    System.out.println("no route" + route);
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

    public static void main(String[] args) {
//        double lat1 = -87.626114299999998, lon1 = 41.801872260000003;
//        double lat2 = -87.626114299999998, lon2 = 41.801872260000003;
//        double distance = HotSpotFinder.getDistanceFromLatLonInKm(lat2, lon2, lat1, lon1);
//        System.out.println(distance);
        double startLat = -87.643604, startLon = 41.883919;
        FileParser fileParser = new FileParser();
        Map<String, BusLine> busLineMap = fileParser.getBusLinesFromFile();

        List<BusStop> busStopList = fileParser.getBusStopsFromFile(busLineMap);
//        Iterator<BusLine> iterator = busLineMap.values().iterator();
//        for (int i = 0; i < 20; i++) {
//            if (iterator.hasNext())
//                System.out.println(iterator.next());
//        }
        //System.out.println("bus stop : " + busStopList.size());
//        for (int i = 0; i < 100; i++) {
//            System.out.println(busStopList.get(i));
//        }

        List<HotSpot> hotSpotList = fileParser.getHotSpotsFromFile();
        //System.out.println("hot spot : " + hotSpotList.size());

        List<Coordinate> parcelList = fileParser.getParcelFromFile();

        for (Coordinate parcel : parcelList) {
            List<BusStop> startingBusStops = (List<BusStop>) HotSpotFinder.getCoordinatesWithinRange(busStopList, parcel.getPoint_x(),
                    parcel.getPoint_y(), walkingDistanceIn10Min);
            System.out.println("start bus stop : " + startingBusStops.size());

            Set<BusStop> reachableBusStops = HotSpotFinder.getReachableBusStops(busLineMap, startingBusStops);
            System.out.println("reachable bus stop : " + reachableBusStops.size());

            List<HotSpot> hotSpotsInDrivingDistance = (List<HotSpot>) HotSpotFinder.getCoordinatesWithinRange(hotSpotList, parcel.getPoint_x(),
                    parcel.getPoint_y(), drivingDistanceIn60Min);
            System.out.println("hot spot in driving distance : " + hotSpotsInDrivingDistance.size());

            Set<HotSpot> reachableHotSpots = HotSpotFinder.getReachableHotSpots(hotSpotList, reachableBusStops);
            System.out.println("reachable hot spot : " + reachableHotSpots.size());

            double reachableIndex = (double) (reachableHotSpots.size()) / (double) (hotSpotsInDrivingDistance.size());
            System.out.println(reachableIndex);
        }

//        double dis = 0;
//        double[] array = new double[10];
//        for (HotSpot aHotSpotList : hotSpotList) {
//            dis = getDistanceFromLatLonInKm(aHotSpotList.getPoint_x(), aHotSpotList.getPoint_y(), startLat, startLon);
//            if (dis < 10) {
//                array[(int) dis]++;
//            }
//            if (dis < 1) {
//                System.out.println(aHotSpotList.getPoint_x());
//                System.out.println(aHotSpotList.getPoint_y());
//            }
//        }
//        for (double d : array)
//            System.out.println(d);

    }
}
