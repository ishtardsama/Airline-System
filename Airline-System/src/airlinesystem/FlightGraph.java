package airlinesystem;

import java.util.*;

public class FlightGraph {

    private Map<String, Airport>            airports;
    private Map<String, LinkedList<Flight>> adjList;

    // ============================================================
    //  Constructor
    // ============================================================
    public FlightGraph() {
        airports = new LinkedHashMap<>();
        adjList  = new LinkedHashMap<>();
    }

    // ============================================================
    //  VERTEX OPERATIONS
    // ============================================================

    /** Add airport (vertex). Average O(1). */
    public boolean addAirport(Airport airport) {
        if (airport == null) {
            System.out.println("  [!] Airport data cannot be null.");
            return false;
        }

        String code = normalizeCode(airport.getCode());
        if (!isValidAirportCode(code)) {
            System.out.println("  [!] Airport code must contain exactly 3 letters (e.g. KUL).");
            return false;
        }
        if (isBlank(airport.getName()) || isBlank(airport.getCity()) || isBlank(airport.getRegion())) {
            System.out.println("  [!] Airport name, city and region are required.");
            return false;
        }
        if (airports.containsKey(code)) {
            System.out.println("  [!] Airport [" + code + "] already exists.");
            return false;
        }

        // Keep the Airport object consistent with the key stored in the graph.
        airport.setCode(code);
        airports.put(code, airport);
        adjList.put(code, new LinkedList<>());
        return true;
    }

    /** Remove airport (vertex) and all incoming/outgoing edges. O(V + E). */
    public boolean removeAirport(String code) {
        code = normalizeCode(code);
        if (!airports.containsKey(code)) {
            System.out.println("  [!] Airport [" + code + "] not found.");
            return false;
        }

        // Removing this adjacency-list entry removes every outgoing route.
        adjList.remove(code);
        airports.remove(code);

        // Remove every incoming route from the remaining vertices.
        final String removedCode = code;
        for (LinkedList<Flight> flights : adjList.values()) {
            flights.removeIf(f -> normalizeCode(f.getDestination().getCode()).equals(removedCode));
        }

        System.out.println("  [OK] Airport [" + code + "] and all its routes removed.");
        return true;
    }

    // ============================================================
    //  EDGE OPERATIONS
    // ============================================================

    /** Add a directed, weighted flight edge. O(E) because duplicate checks scan routes. */
    public boolean addFlight(Flight flight) {
        if (flight == null || flight.getSource() == null || flight.getDestination() == null) {
            System.out.println("  [!] Flight, source and destination are required.");
            return false;
        }

        String src  = normalizeCode(flight.getSource().getCode());
        String dest = normalizeCode(flight.getDestination().getCode());
        String flightCode = normalizeFlightCode(flight.getFlightCode());

        if (!airports.containsKey(src)) {
            System.out.println("  [!] Source [" + src + "] not found.");
            return false;
        }
        if (!airports.containsKey(dest)) {
            System.out.println("  [!] Destination [" + dest + "] not found.");
            return false;
        }
        if (src.equals(dest)) {
            System.out.println("  [!] Source and destination must be different airports.");
            return false;
        }
        if (flightCode.isEmpty()) {
            System.out.println("  [!] Flight code is required.");
            return false;
        }
        if (!isPositiveFinite(flight.getDistance()) ||
            !isPositiveFinite(flight.getDuration()) ||
            !isPositiveFinite(flight.getPrice())) {
            System.out.println("  [!] Distance, duration and price must be positive finite values.");
            return false;
        }
        if (searchFlightByCode(flightCode) != null) {
            System.out.println("  [!] Flight code [" + flightCode + "] already exists.");
            return false;
        }
        if (getDirectFlight(src, dest) != null) {
            System.out.println("  [!] Direct route [" + src + " -> " + dest + "] already exists.");
            return false;
        }

        // Canonicalize references so every edge points to vertices owned by this graph.
        flight.setFlightCode(flightCode);
        flight.setSource(airports.get(src));
        flight.setDestination(airports.get(dest));
        adjList.get(src).add(flight);
        return true;
    }

    /** Remove a specific directed flight edge. O(out-degree). */
    public boolean removeFlight(String srcCode, String destCode) {
        srcCode  = normalizeCode(srcCode);
        destCode = normalizeCode(destCode);

        if (!airports.containsKey(srcCode)) {
            System.out.println("  [!] Source [" + srcCode + "] not found.");
            return false;
        }
        if (!airports.containsKey(destCode)) {
            System.out.println("  [!] Destination [" + destCode + "] not found.");
            return false;
        }

        Iterator<Flight> it = adjList.get(srcCode).iterator();
        while (it.hasNext()) {
            Flight f = it.next();
            if (normalizeCode(f.getDestination().getCode()).equals(destCode)) {
                it.remove();
                System.out.println("  [OK] Flight [" + srcCode + "] -> ["
                        + destCode + "] removed.");
                return true;
            }
        }

        System.out.println("  [!] No direct flight from [" + srcCode
                + "] to [" + destCode + "].");
        return false;
    }

    // ============================================================
    //  DISPLAY OPERATIONS
    // ============================================================

    /** Display all airports grouped by region. */
    public void displayAllAirports() {
        if (airports.isEmpty()) {
            System.out.println("  [!] No airports in the system.");
            return;
        }

        // Group by region
        Map<String, List<Airport>> byRegion = new LinkedHashMap<>();
        for (Airport a : airports.values())
            byRegion.computeIfAbsent(a.getRegion(), k -> new ArrayList<>()).add(a);

        System.out.println();
        System.out.println(bar('=', 68));
        System.out.println(center("MALAYSIA FLIGHT NETWORK  -  AIRPORTS", 68));
        System.out.println(bar('=', 68));
        System.out.printf(" %-3s  %-5s  %-27s  %-20s%n",
                "No.", "Code", "City / State", "Airport Name (Short)");
        System.out.println(bar('-', 68));

        int no = 1;
        for (Map.Entry<String, List<Airport>> entry : byRegion.entrySet()) {
            System.out.println("  >> " + entry.getKey());
            for (Airport a : entry.getValue()) {
                // Shorten airport name: take first 3 words
                String shortName = shortName(a.getName());
                System.out.printf(" %-3d  %-5s  %-27s  %s%n",
                        no++, a.getCode(), a.getCity(), shortName);
            }
        }

        System.out.println(bar('-', 68));
        System.out.printf(" Total: %d airports  |  %d flight routes%n",
                totalAirports(), totalFlights());
        System.out.println(bar('=', 68));
    }

    /** Display the full adjacency list. */
    public void displayAdjacencyList() {
        System.out.println();
        System.out.println(bar('=', 70));
        System.out.println(center("FLIGHT ROUTES  -  ADJACENCY LIST", 70));
        System.out.println(bar('=', 70));

        for (Map.Entry<String, LinkedList<Flight>> entry : adjList.entrySet()) {
            String  code = entry.getKey();
            Airport a    = airports.get(code);
            System.out.printf("%n [%s]  %s%n", code, a.getCity());

            if (entry.getValue().isEmpty()) {
                System.out.println("       (No outgoing flights)");
            } else {
                for (Flight f : entry.getValue()) {
                    System.out.printf("   %-8s -> %-5s  %-22s  %5.0fkm  %3.0fmin  RM%6.2f%n",
                            f.getFlightCode(),
                            f.getDestination().getCode(),
                            f.getDestination().getCity(),
                            f.getDistance(),
                            f.getDuration(),
                            f.getPrice());
                }
            }
        }

        System.out.println();
        System.out.println(bar('-', 70));
        System.out.printf(" %d airports (vertices)  |  %d flights (edges)%n",
                totalAirports(), totalFlights());
        System.out.println(bar('=', 70));
    }

    /** Display all direct destinations from one airport. */
    public void displayNeighbors(String code) {
        code = normalizeCode(code);
        if (!airports.containsKey(code)) {
            System.out.println("  [!] Airport [" + code + "] not found.");
            return;
        }
        Airport a = airports.get(code);
        List<Flight> neighbors = adjList.get(code);

        System.out.println();
        System.out.println(bar('=', 70));
        System.out.printf(  " Direct Destinations from [%s]  %s%n", code, a.getCity());
        System.out.println(bar('=', 70));

        if (neighbors.isEmpty()) {
            System.out.println("  No outgoing flights from [" + code + "].");
        } else {
            System.out.printf(" %-8s  %-5s  %-25s  %7s  %7s  %9s%n",
                    "Flight", "Dest", "City", "Dist(km)", "Min", "Price(RM)");
            System.out.println(bar('-', 70));
            for (Flight f : neighbors) {
                System.out.printf(" %-8s  %-5s  %-25s  %7.0f  %7.0f  RM%6.2f%n",
                        f.getFlightCode(),
                        f.getDestination().getCode(),
                        f.getDestination().getCity(),
                        f.getDistance(),
                        f.getDuration(),
                        f.getPrice());
            }
        }
        System.out.println(bar('-', 70));
        System.out.printf(" %d direct route(s) from [%s]%n", neighbors.size(), code);
        System.out.println(bar('=', 70));
    }

    /** Display graph statistics. */
    public void displayStatistics() {
        System.out.println();
        System.out.println(bar('=', 55));
        System.out.println(center("GRAPH STATISTICS", 55));
        System.out.println(bar('=', 55));

        Map<String, Integer> regionCount = new LinkedHashMap<>();
        for (Airport a : airports.values())
            regionCount.merge(a.getRegion(), 1, Integer::sum);

        System.out.printf(" Total Airports (Vertices) : %d%n", totalAirports());
        for (Map.Entry<String, Integer> e : regionCount.entrySet())
            System.out.printf("   %-25s: %d%n", e.getKey(), e.getValue());
        System.out.printf(" Total Flights  (Edges)    : %d%n", totalFlights());

        // Most connected (highest out-degree)
        String most = null; int maxOut = 0;
        for (Map.Entry<String, LinkedList<Flight>> e : adjList.entrySet())
            if (e.getValue().size() > maxOut) { maxOut = e.getValue().size(); most = e.getKey(); }

        // Least connected (lowest out-degree)
        String least = null; int minOut = Integer.MAX_VALUE;
        for (Map.Entry<String, LinkedList<Flight>> e : adjList.entrySet())
            if (e.getValue().size() < minOut) { minOut = e.getValue().size(); least = e.getKey(); }

        if (most  != null) System.out.printf(" Most Connected  : [%s] %s (%d routes)%n",
                most,  airports.get(most).getCity(),  maxOut);
        if (least != null) System.out.printf(" Least Connected : [%s] %s (%d routes)%n",
                least, airports.get(least).getCity(), minOut);

        System.out.println(bar('=', 55));
    }

    // ============================================================
    //  SEARCH / QUERY OPERATIONS
    // ============================================================

    /** Returns Flight if a direct route exists, null otherwise. */
    public Flight getDirectFlight(String srcCode, String destCode) {
        srcCode  = normalizeCode(srcCode);
        destCode = normalizeCode(destCode);
        if (!airports.containsKey(srcCode) || !airports.containsKey(destCode)) return null;

        for (Flight f : adjList.get(srcCode)) {
            if (normalizeCode(f.getDestination().getCode()).equals(destCode)) return f;
        }
        return null;
    }

    /** Find a flight by its unique flight code (e.g. MH2618). */
    public Flight searchFlightByCode(String flightCode) {
        String target = normalizeFlightCode(flightCode);
        if (target.isEmpty()) return null;

        for (LinkedList<Flight> flights : adjList.values()) {
            for (Flight f : flights) {
                if (normalizeFlightCode(f.getFlightCode()).equals(target)) return f;
            }
        }
        return null;
    }

    // ============================================================
    //  HELPER / UTILITY
    // ============================================================
    public int totalAirports() { return airports.size(); }

    public int totalFlights() {
        int count = 0;
        for (LinkedList<Flight> flights : adjList.values()) count += flights.size();
        return count;
    }

    public boolean airportExists(String code) {
        return airports.containsKey(normalizeCode(code));
    }

    public Airport getAirport(String code) {
        return airports.get(normalizeCode(code));
    }

    public List<Flight> getNeighbors(String code) {
        LinkedList<Flight> flights = adjList.get(normalizeCode(code));
        return flights == null ? Collections.emptyList() : flights;
    }

    public Map<String, Airport> getAirports() { return airports; }
    public Map<String, LinkedList<Flight>> getAdjList() { return adjList; }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeFlightCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isValidAirportCode(String code) {
        return code.matches("[A-Z]{3}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }

    // ============================================================
    //  FORMATTING
    // ============================================================
    static String bar(char ch, int n) {
        char[] arr = new char[n]; Arrays.fill(arr, ch); return " " + new String(arr);
    }
    static String center(String text, int width) {
        int pad = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(pad) + text;
    }
    /** Returns first 3 words of an airport name as a short label. */
    private static String shortName(String name) {
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            if (i > 0) sb.append(" ");
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
