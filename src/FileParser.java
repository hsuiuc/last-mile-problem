import java.io.*;
import java.util.*;

/**
 * Created by haosun on 4/20/18.
 */
public class FileParser {
    private final String inputFilePath = "./data/Bus_Stop.txt";
    private final String outputFilePath = "./data/Bus_Stop_Test.txt";
    private final File input = new File(inputFilePath);
    private final File output = new File(outputFilePath);

    public void writeToFile() {
        int counter = 0;
        String nextLine;
        Scanner sc = null;
        BufferedWriter bw = null;
        try {
            sc = new Scanner(input);
            bw = new BufferedWriter(new FileWriter(outputFilePath));

            while (sc.hasNextLine()) {
                nextLine = sc.nextLine();
                counter = counter % 50;
                if (counter++ == 0) {
                    bw.write(nextLine);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sc != null) {
                sc.close();
            }
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Map<String, BusLine> getBusLinesFromFile() {
        String inputBusLineFilePath = "./data/Bus_Line.txt";
        File inputBusLineFile = new File(inputBusLineFilePath);
        Map<String, BusLine> result = new HashMap<>();
        Scanner sc = null;
        try {
            sc = new Scanner(inputBusLineFile);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] tokens = line.split(",");
                BusLine busLine = new BusLine(tokens[0], tokens[1]);
                result.putIfAbsent(tokens[0], busLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
        return result;
    }

    public List<BusStop> getBusStopsFromFile(Map<String, BusLine> busLineMap) {
        String inputBusStopFilePath = "./data/Bus_Stop_Test.txt";
        File inputBusStopFile = new File(inputBusStopFilePath);
        Scanner sc = null;
        List<BusStop> result = new LinkedList<>();
        try {
            sc = new Scanner(inputBusStopFile);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] tokens = line.replaceAll("\"", "").split(",");
                int length = tokens.length;
                int systemStop = ((int) Double.parseDouble(tokens[0]));
                String direction = tokens[1];
                String publicName = tokens[length - 3];
                double point_x = Double.parseDouble(tokens[length - 2]);
                double point_y = Double.parseDouble(tokens[length - 1]);
                int from = 2;
                int to = length - 4;
                String[] routes = Arrays.copyOfRange(tokens, from ,to);
                BusStop busStop = new BusStop(systemStop, direction, routes, publicName, point_x, point_y);
                result.add(busStop);
                for (String route : busStop.getRoutes()) {
                    if (busLineMap.containsKey(route)) {
                        int index = Collections.binarySearch(busLineMap.get(route).getStops(), busStop);
                        if (index < 0) index = ~index;
                        busLineMap.get(route).getStops().add(index, busStop);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        result.sort((o1, o2) -> o1.getPoint_x() == o2.getPoint_x() ? 0 :
                o1.getPoint_x() < o2.getPoint_x() ? -1 : 1);
        return result;
    }

    public static void main(String[] args) {
        FileParser fileParser = new FileParser();
//        fileParser.writeToFile();
//        String string = "7256.000000000000000,EB,\"26,30\",91st Street & Brandon,-87.546989280000005,41.730031799999999";
//        String[] tokens = string.replaceAll("\"", "").split(",");
//        Double d = Double.parseDouble(tokens[0]);
//        System.out.println(d.intValue());
//        for (String token : tokens) {
//            System.out.println(token);
//        }
        Map<String, BusLine> busLineMap = fileParser.getBusLinesFromFile();
        List<BusStop> busStopList = fileParser.getBusStopsFromFile(busLineMap);
        for (BusLine busLine : busLineMap.values()) {
            if (busLine.getStops().size() != 0) {
                System.out.println(busLine);
            }
        }
    }
}
