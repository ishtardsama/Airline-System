package airlinesystem;

import java.util.*;

/**
 * Graph algorithms for the airline network.
 *
 * BFS  : traversal / reachability using a Queue
 * DFS  : traversal / reachability using a Stack
 * Dijkstra: weighted shortest path using a PriorityQueue
 *           - distance (km)
 *           - duration (minutes)
 *           - price (RM)
 */
public class GraphTraversal {

    private final FlightGraph graph;

    private static class Node implements Comparable<Node> {
        final String code;
        final double cost;

        Node(String code, double cost) {
            this.code = code;
            this.cost = cost;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    /**
     * Reusable result object for GUI/console integration.
     */
    public static class PathResult {
        private final boolean found;
        private final List<String> path;
        private final List<Flight> flights;
        private final double totalDistance;
        private final double totalDuration;
        private final double totalPrice;

        private PathResult(boolean found,
                           List<String> path,
                           List<Flight> flights,
                           double totalDistance,
                           double totalDuration,
                           double totalPrice) {
            this.found = found;
            this.path = Collections.unmodifiableList(new ArrayList<>(path));
            this.flights = Collections.unmodifiableList(new ArrayList<>(flights));
            this.totalDistance = totalDistance;
            this.totalDuration = totalDuration;
            this.totalPrice = totalPrice;
        }

        public boolean isFound() { return found; }
        public List<String> getPath() { return path; }
        public List<Flight> getFlights() { return flights; }
        public double getTotalDistance() { return totalDistance; }
        public double getTotalDuration() { return totalDuration; }
        public double getTotalPrice() { return totalPrice; }
    }

    private enum WeightMode {
        DISTANCE,
        DURATION,
        PRICE
    }

    public GraphTraversal(FlightGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("FlightGraph cannot be null.");
        }
        this.graph = graph;
    }

    // ============================================================
    // BFS
    // ============================================================

    /** Console BFS traversal. Time O(V + E), space O(V). */
    public void BFS(String startCode) {
        List<String> order = bfsOrder(startCode);
        if (order.isEmpty()) return;

        String start = normalize(startCode);
        Airport airport = graph.getAirport(start);

        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("BFS  -  Breadth First Search", 68));
        System.out.printf(" Start : [%s] %s%n", start, airport.getCity());
        System.out.printf(" Method: Queue (FIFO)  |  Time: O(V+E)%n");
        System.out.println(bar('-', 68));
        System.out.printf(" %-6s  %-5s  %-30s  %s%n", "Step", "Code", "City / State", "Region");
        System.out.println(bar('-', 68));

        int step = 1;
        for (String code : order) {
            Airport a = graph.getAirport(code);
            System.out.printf(" %-6d  %-5s  %-30s  %s%n",
                    step++, code, a.getCity(), a.getRegion());
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Visited : %d / %d airports%n", order.size(), graph.totalAirports());
        System.out.printf(" Order   : %s%n", order);
        System.out.println(bar('=', 68));
    }

    /** Reusable BFS order for GUI/tests. */
    public List<String> bfsOrder(String startCode) {
        String start = normalize(startCode);
        if (!graph.airportExists(start)) {
            System.out.println("  [!] Airport [" + start + "] not found.");
            return Collections.emptyList();
        }

        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        List<String> order = new ArrayList<>();

        visited.add(start);
        queue.offer(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            order.add(current);

            for (Flight flight : graph.getNeighbors(current)) {
                String next = flight.getDestination().getCode().toUpperCase(Locale.ROOT);
                if (visited.add(next)) {
                    queue.offer(next);
                }
            }
        }

        return order;
    }

    // ============================================================
    // DFS
    // ============================================================

    /** Console DFS traversal. Time O(V + E), space O(V). */
    public void DFS(String startCode) {
        List<String> order = dfsOrder(startCode);
        if (order.isEmpty()) return;

        String start = normalize(startCode);
        Airport airport = graph.getAirport(start);

        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("DFS  -  Depth First Search", 68));
        System.out.printf(" Start : [%s] %s%n", start, airport.getCity());
        System.out.printf(" Method: Stack (LIFO)  |  Time: O(V+E)%n");
        System.out.println(bar('-', 68));
        System.out.printf(" %-6s  %-5s  %-30s  %s%n", "Step", "Code", "City / State", "Region");
        System.out.println(bar('-', 68));

        int step = 1;
        for (String code : order) {
            Airport a = graph.getAirport(code);
            System.out.printf(" %-6d  %-5s  %-30s  %s%n",
                    step++, code, a.getCity(), a.getRegion());
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Visited : %d / %d airports%n", order.size(), graph.totalAirports());
        System.out.printf(" Order   : %s%n", order);
        System.out.println(bar('=', 68));
    }

    /** Reusable iterative DFS order for GUI/tests. */
    public List<String> dfsOrder(String startCode) {
        String start = normalize(startCode);
        if (!graph.airportExists(start)) {
            System.out.println("  [!] Airport [" + start + "] not found.");
            return Collections.emptyList();
        }

        Set<String> visited = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        List<String> order = new ArrayList<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!visited.add(current)) continue;
            order.add(current);

            List<Flight> neighbors = graph.getNeighbors(current);
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                String next = neighbors.get(i).getDestination().getCode().toUpperCase(Locale.ROOT);
                if (!visited.contains(next)) {
                    stack.push(next);
                }
            }
        }

        return order;
    }

    // ============================================================
    // DIJKSTRA - PUBLIC CONSOLE METHODS
    // ============================================================

    public void dijkstraByDistance(String srcCode, String destCode) {
        printDijkstra(srcCode, destCode, WeightMode.DISTANCE,
                "DIJKSTRA  -  Shortest Distance Path",
                "Distance (km)");
    }

    /** New third weighted option: fastest total travel duration. */
    public void dijkstraByFastestDuration(String srcCode, String destCode) {
        printDijkstra(srcCode, destCode, WeightMode.DURATION,
                "DIJKSTRA  -  Fastest Duration Path",
                "Duration (minutes)");
    }

    public void dijkstraByCheapestPrice(String srcCode, String destCode) {
        printDijkstra(srcCode, destCode, WeightMode.PRICE,
                "DIJKSTRA  -  Cheapest Price Path",
                "Ticket Price (RM)");
    }

    // ============================================================
    // DIJKSTRA - REUSABLE METHODS FOR GUI / TESTING
    // ============================================================

    public PathResult shortestDistance(String srcCode, String destCode) {
        return runDijkstra(srcCode, destCode, WeightMode.DISTANCE);
    }

    public PathResult fastestDuration(String srcCode, String destCode) {
        return runDijkstra(srcCode, destCode, WeightMode.DURATION);
    }

    public PathResult cheapestPrice(String srcCode, String destCode) {
        return runDijkstra(srcCode, destCode, WeightMode.PRICE);
    }

    private void printDijkstra(String srcCode, String destCode,
                               WeightMode mode, String title, String weightLabel) {
        String src = normalize(srcCode);
        String dest = normalize(destCode);

        if (!validateEndpoints(src, dest)) return;

        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center(title, 68));
        System.out.printf(" From   : [%s]  %s%n", src, graph.getAirport(src).getCity());
        System.out.printf(" To     : [%s]  %s%n", dest, graph.getAirport(dest).getCity());
        System.out.printf(" Weight : %s  |  Time: O((V+E) log V)%n", weightLabel);
        System.out.println(bar('=', 68));

        PathResult result = runDijkstra(src, dest, mode);
        if (!result.isFound()) {
            System.out.println(" No path found from [" + src + "] to [" + dest + "].");
            System.out.println(bar('=', 68));
            return;
        }

        System.out.printf(" %-8s  %-5s    %-5s  %-24s  %6s  %5s  %9s%n",
                "Flight", "From", "To", "Destination City", "km", "min", "Price(RM)");
        System.out.println(bar('-', 68));

        for (Flight f : result.getFlights()) {
            System.out.printf(" %-8s  %-5s -> %-5s  %-24s  %6.0f  %5.0f  RM%6.2f%n",
                    f.getFlightCode(),
                    f.getSource().getCode(),
                    f.getDestination().getCode(),
                    f.getDestination().getCity(),
                    f.getDistance(),
                    f.getDuration(),
                    f.getPrice());
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Path  : %s%n", String.join(" -> ", result.getPath()));
        System.out.printf(" Total : %.0f km  |  %.0f min  |  RM %.2f%n",
                result.getTotalDistance(), result.getTotalDuration(), result.getTotalPrice());
        System.out.println(bar('=', 68));
    }

    /**
     * PriorityQueue Dijkstra implementation.
     * With adjacency list + binary heap: O((V + E) log V), space O(V + E).
     */
    private PathResult runDijkstra(String srcCode, String destCode, WeightMode mode) {
        String src = normalize(srcCode);
        String dest = normalize(destCode);

        if (!validateEndpointsSilently(src, dest)) {
            return noPath();
        }

        Map<String, Double> best = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();

        for (String code : graph.getAirports().keySet()) {
            best.put(code, Double.POSITIVE_INFINITY);
        }

        best.put(src, 0.0);
        pq.offer(new Node(src, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            // Ignore stale priority-queue entries.
            if (current.cost > best.get(current.code)) continue;
            if (current.code.equals(dest)) break;

            for (Flight flight : graph.getNeighbors(current.code)) {
                String next = flight.getDestination().getCode().toUpperCase(Locale.ROOT);
                double weight = edgeWeight(flight, mode);
                double candidate = current.cost + weight;

                if (candidate < best.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    best.put(next, candidate);
                    previous.put(next, current.code);
                    pq.offer(new Node(next, candidate));
                }
            }
        }

        if (!Double.isFinite(best.getOrDefault(dest, Double.POSITIVE_INFINITY))) {
            return noPath();
        }

        LinkedList<String> path = new LinkedList<>();
        String cursor = dest;
        while (cursor != null) {
            path.addFirst(cursor);
            if (cursor.equals(src)) break;
            cursor = previous.get(cursor);
        }

        if (path.isEmpty() || !path.getFirst().equals(src)) {
            return noPath();
        }

        List<Flight> flights = new ArrayList<>();
        double totalDistance = 0.0;
        double totalDuration = 0.0;
        double totalPrice = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            Flight flight = graph.getDirectFlight(path.get(i), path.get(i + 1));
            if (flight == null) {
                // Defensive safety: reconstructed path must map to real edges.
                return noPath();
            }
            flights.add(flight);
            totalDistance += flight.getDistance();
            totalDuration += flight.getDuration();
            totalPrice += flight.getPrice();
        }

        return new PathResult(true, path, flights,
                totalDistance, totalDuration, totalPrice);
    }

    private double edgeWeight(Flight flight, WeightMode mode) {
        switch (mode) {
            case DURATION:
                return flight.getDuration();
            case PRICE:
                return flight.getPrice();
            case DISTANCE:
            default:
                return flight.getDistance();
        }
    }

    private boolean validateEndpoints(String src, String dest) {
        if (!graph.airportExists(src) || !graph.airportExists(dest)) {
            System.out.println("  [!] One or both airports not found.");
            return false;
        }
        if (src.equals(dest)) {
            System.out.println("  [!] Source and destination are the same.");
            return false;
        }
        return true;
    }

    private boolean validateEndpointsSilently(String src, String dest) {
        return graph.airportExists(src)
                && graph.airportExists(dest)
                && !src.equals(dest);
    }

    private static PathResult noPath() {
        return new PathResult(false,
                Collections.emptyList(),
                Collections.emptyList(),
                0.0, 0.0, 0.0);
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String bar(char ch, int n) {
        return FlightGraph.bar(ch, n);
    }

    private static String center(String text, int width) {
        return FlightGraph.center(text, width);
    }
}
