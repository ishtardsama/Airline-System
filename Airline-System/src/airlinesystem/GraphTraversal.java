package airlinesystem;

import java.util.*;

public class GraphTraversal {

    private final FlightGraph graph;

    private static class Node implements Comparable<Node> {
        String code;
        double cost;
        Node(String code, double cost) { this.code = code; this.cost = cost; }
        @Override
        public int compareTo(Node o) { return Double.compare(this.cost, o.cost); }
    }

    public GraphTraversal(FlightGraph graph) { this.graph = graph; }

    public void BFS(String startCode) {
        startCode = startCode.toUpperCase();
        if (!graph.airportExists(startCode)) {
            System.out.println("  [!] Airport [" + startCode + "] not found.");
            return;
        }

        Airport start = graph.getAirport(startCode);
        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("BFS  -  Breadth First Search", 68));
        System.out.printf(" Start : [%s] %s%n", startCode, start.getCity());
        System.out.printf(" Method: Queue (FIFO)  |  Time: O(V+E)%n");
        System.out.println(bar('-', 68));
        System.out.printf(" %-6s  %-5s  %-30s  %s%n", "Step", "Code", "City / State", "Region");
        System.out.println(bar('-', 68));

        Set<String>   visited = new LinkedHashSet<>();
        Queue<String> queue   = new LinkedList<>();
        List<String>  order   = new ArrayList<>();

        queue.offer(startCode);
        visited.add(startCode);
        int step = 1;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Airport a = graph.getAirport(current);
            System.out.printf(" %-6d  %-5s  %-30s  %s%n", step++, current, a.getCity(), a.getRegion());
            order.add(current);

            for (Flight f : graph.getNeighbors(current)) {
                String nb = f.getDestination().getCode();
                if (!visited.contains(nb)) { visited.add(nb); queue.offer(nb); }
            }
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Visited : %d / %d airports%n", visited.size(), graph.totalAirports());
        System.out.printf(" Order   : %s%n", order);
        System.out.println(bar('=', 68));
    }

    public void DFS(String startCode) {
        startCode = startCode.toUpperCase();
        if (!graph.airportExists(startCode)) {
            System.out.println("  [!] Airport [" + startCode + "] not found.");
            return;
        }

        Airport start = graph.getAirport(startCode);
        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("DFS  -  Depth First Search", 68));
        System.out.printf(" Start : [%s] %s%n", startCode, start.getCity());
        System.out.printf(" Method: Stack (LIFO)  |  Time: O(V+E)%n");
        System.out.println(bar('-', 68));
        System.out.printf(" %-6s  %-5s  %-30s  %s%n", "Step", "Code", "City / State", "Region");
        System.out.println(bar('-', 68));

        Set<String>   visited = new LinkedHashSet<>();
        Stack<String> stack   = new Stack<>();
        List<String>  order   = new ArrayList<>();

        stack.push(startCode);
        int step = 1;

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (visited.contains(current)) continue;

            visited.add(current);
            Airport a = graph.getAirport(current);
            System.out.printf(" %-6d  %-5s  %-30s  %s%n", step++, current, a.getCity(), a.getRegion());
            order.add(current);

            List<Flight> neighbors = graph.getNeighbors(current);
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                String nb = neighbors.get(i).getDestination().getCode();
                if (!visited.contains(nb)) stack.push(nb);
            }
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Visited : %d / %d airports%n", visited.size(), graph.totalAirports());
        System.out.printf(" Order   : %s%n", order);
        System.out.println(bar('=', 68));
    }

    public void dijkstraByDistance(String srcCode, String destCode) {
        srcCode  = srcCode.toUpperCase();
        destCode = destCode.toUpperCase();
        if (!graph.airportExists(srcCode) || !graph.airportExists(destCode)) {
            System.out.println("  [!] One or both airports not found.");
            return;
        }
        if (srcCode.equals(destCode)) {
            System.out.println("  [!] Source and destination are the same.");
            return;
        }
        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("DIJKSTRA  -  Shortest Distance Path", 68));
        System.out.printf(" From   : [%s]  %s%n", srcCode,  graph.getAirport(srcCode).getCity());
        System.out.printf(" To     : [%s]  %s%n", destCode, graph.getAirport(destCode).getCity());
        System.out.printf(" Weight : Distance (km)  |  Time: O((V+E) log V)%n");
        System.out.println(bar('=', 68));
        runDijkstra(srcCode, destCode, "distance");
    }

    public void dijkstraByCheapestPrice(String srcCode, String destCode) {
        srcCode  = srcCode.toUpperCase();
        destCode = destCode.toUpperCase();
        if (!graph.airportExists(srcCode) || !graph.airportExists(destCode)) {
            System.out.println("  [!] One or both airports not found.");
            return;
        }
        if (srcCode.equals(destCode)) {
            System.out.println("  [!] Source and destination are the same.");
            return;
        }
        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("DIJKSTRA  -  Cheapest Price Path", 68));
        System.out.printf(" From   : [%s]  %s%n", srcCode,  graph.getAirport(srcCode).getCity());
        System.out.printf(" To     : [%s]  %s%n", destCode, graph.getAirport(destCode).getCity());
        System.out.printf(" Weight : Ticket Price (RM)  |  Time: O((V+E) log V)%n");
        System.out.println(bar('=', 68));
        runDijkstra(srcCode, destCode, "price");
    }

    private void runDijkstra(String srcCode, String destCode, String mode) {
        Map<String, Double> cost     = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String>         settled  = new HashSet<>();
        PriorityQueue<Node> pq       = new PriorityQueue<>();

        for (String c : graph.getAirports().keySet()) {
            cost.put(c, Double.MAX_VALUE);
            previous.put(c, null);
        }
        cost.put(srcCode, 0.0);
        pq.offer(new Node(srcCode, 0.0));

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (settled.contains(cur.code)) continue;
            settled.add(cur.code);
            if (cur.code.equals(destCode)) break;

            for (Flight f : graph.getNeighbors(cur.code)) {
                String nb     = f.getDestination().getCode();
                if (settled.contains(nb)) continue;
                double weight  = mode.equals("distance") ? f.getDistance() : f.getPrice();
                double newCost = cost.get(cur.code) + weight;
                if (newCost < cost.get(nb)) {
                    cost.put(nb, newCost);
                    previous.put(nb, cur.code);
                    pq.offer(new Node(nb, newCost));
                }
            }
        }

        if (cost.get(destCode) == Double.MAX_VALUE) {
            System.out.println(" No path found from [" + srcCode + "] to [" + destCode + "].");
            System.out.println(bar('=', 68));
            return;
        }

        LinkedList<String> path = new LinkedList<>();
        for (String c = destCode; c != null; c = previous.get(c)) path.addFirst(c);

        System.out.printf(" %-8s  %-5s    %-5s  %-24s  %6s  %5s  %9s%n",
                "Flight", "From", "To", "Destination City", "km", "min", "Price(RM)");
        System.out.println(bar('-', 68));

        double totalKm = 0, totalMin = 0, totalRM = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            for (Flight f : graph.getNeighbors(from)) {
                if (f.getDestination().getCode().equals(to)) {
                    System.out.printf(" %-8s  %-5s -> %-5s  %-24s  %6.0f  %5.0f  RM%6.2f%n",
                            f.getFlightCode(), from, to,
                            f.getDestination().getCity(),
                            f.getDistance(), f.getDuration(), f.getPrice());
                    totalKm  += f.getDistance();
                    totalMin += f.getDuration();
                    totalRM  += f.getPrice();
                    break;
                }
            }
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Path  : %s%n", String.join(" -> ", path));
        System.out.printf(" Total : %.0f km  |  %.0f min  |  RM %.2f%n", totalKm, totalMin, totalRM);
        System.out.println(bar('=', 68));
    }

    private static String bar(char ch, int n)    { return FlightGraph.bar(ch, n);    }
    private static String center(String t, int w) { return FlightGraph.center(t, w); }
}
