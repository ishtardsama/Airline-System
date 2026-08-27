/*
 * AirlineSystem.java
 * Main entry point for the Malaysia Airline Flight Network System.
 *
 * System Overview:
 *   - Graph Type   : Directed Weighted Graph
 *   - Representation: Adjacency List
 *   - Airports     : 18 Malaysian airports (Peninsular, Sabah, Sarawak)
 *   - Flight Routes: 55 domestic flight routes
 *
 * Features:
 *   - Add / Remove Airports and Flights
 *   - Display Adjacency List
 *   - Check direct flights and view neighbors
 *   - BFS and DFS traversals
 *   - Dijkstra shortest distance and cheapest price path
 *   - Graph statistics
 *
 * DSA Assignment - Malaysia Airline Flight Network Graph
 */
package airlinesystem;

import java.util.*;

/**
 * AirlineSystem - Main class with console menu and sample data loader.
 */
public class AirlineSystem {

    private static FlightGraph    graph;
    private static GraphTraversal traversal;
    private static Scanner        sc;

    // ============================================================
    //  MAIN METHOD
    // ============================================================
    public static void main(String[] args) {
        graph     = new FlightGraph();
        traversal = new GraphTraversal(graph);
        sc        = new Scanner(System.in);

        // Load all Malaysian airports and flight routes
        System.out.println("\n  Loading Malaysia Airline Flight Network...");
        loadSampleData();
        System.out.printf("  Ready! %d airports and %d flight routes loaded.%n%n",
                graph.totalAirports(), graph.totalFlights());

        int choice = -1;
        while (choice != 0) {
            displayMainMenu();
            System.out.print("  Enter your choice: ");
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = -1;
            }
            clearScreen();
            switch (choice) {
                case 1:  graph.displayAllAirports();        break;
                case 2:  graph.displayAdjacencyList();      break;
                case 3:  menuAddAirport();                  break;
                case 4:  menuAddFlight();                   break;
                case 5:  menuRemoveAirport();               break;
                case 6:  menuRemoveFlight();                break;
                case 7:  menuCheckDirectFlight();           break;
                case 8:  menuViewNeighbors();               break;
                case 9:  menuSearchFlightCode();            break;
                case 10: menuBFS();                         break;
                case 11: menuDFS();                         break;
                case 12: menuDijkstraDistance();            break;
                case 13: menuDijkstraPrice();               break;
                case 14: graph.displayStatistics();         break;
                case 0:
                    System.out.println();
                    System.out.println(FlightGraph.bar('=', 55));
                    System.out.println(FlightGraph.center("Thank you! Selamat Jalan!", 55));
                    System.out.println(FlightGraph.bar('=', 55));
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Please enter a number from 0 to 14.");
            }
        }
        sc.close();
    }

    // ============================================================
    //  MAIN MENU DISPLAY
    // ============================================================
    private static void displayMainMenu() {
        System.out.println();
        System.out.println(FlightGraph.bar('=', 68));
        System.out.println(FlightGraph.center("MALAYSIA AIRLINE FLIGHT NETWORK SYSTEM", 68));
        System.out.println(FlightGraph.center("Data Structure & Algorithm  |  Graph", 68));
        System.out.println(FlightGraph.bar('=', 68));
        System.out.println();
        System.out.println("      GRAPH OPERATIONS");
        System.out.println(FlightGraph.bar('-', 68));
        System.out.println("      [1]  Display All Airports");
        System.out.println("      [2]  Display All Flight Routes (Adjacency List)");
        System.out.println("      [3]  Add New Airport");
        System.out.println("      [4]  Add New Flight Route");
        System.out.println("      [5]  Remove Airport");
        System.out.println("      [6]  Remove Flight Route");
        System.out.println("      [7]  Check Direct Flight Between Two Airports");
        System.out.println("      [8]  View All Direct Destinations from Airport");
        System.out.println("      [9]  Search Flight by Flight Code");
        System.out.println();
        System.out.println("      GRAPH TRAVERSAL");
        System.out.println(FlightGraph.bar('-', 68));
        System.out.println("     [10]  BFS  -  Breadth-First Search");
        System.out.println("     [11]  DFS  -  Depth-First Search");
        System.out.println();
        System.out.println("      SHORTEST PATH  (Dijkstra's Algorithm)");
        System.out.println(FlightGraph.bar('-', 68));
        System.out.println("     [12]  Find Shortest Distance Path   (by km)");
        System.out.println("     [13]  Find Cheapest Price Path      (by RM)");
        System.out.println();
        System.out.println("      INFO");
        System.out.println(FlightGraph.bar('-', 68));
        System.out.println("     [14]  Graph Statistics");
        System.out.println("      [0]  Exit");
        System.out.println();
        System.out.println(FlightGraph.bar('=', 68));
    }

    // ============================================================
    //  MENU HANDLER METHODS
    // ============================================================

    /** Option 3: Add a new airport */
    private static void menuAddAirport() {
        System.out.println(FlightGraph.bar('-', 50));
        System.out.println("  ADD NEW AIRPORT");
        System.out.println(FlightGraph.bar('-', 50));
        System.out.print("  IATA Code  (e.g. KUL) : ");
        String code = sc.nextLine().trim().toUpperCase();
        System.out.print("  Airport Name          : ");
        String name = sc.nextLine().trim();
        System.out.print("  City / State          : ");
        String city = sc.nextLine().trim();
        System.out.println("  Region: 1=Peninsular  2=Sabah  3=Sarawak");
        System.out.print("  Choice (1/2/3)        : ");
        String regionInput = sc.nextLine().trim();
        String region;
        switch (regionInput) {
            case "1": region = "Peninsular Malaysia"; break;
            case "2": region = "Sabah";               break;
            case "3": region = "Sarawak";             break;
            default:  region = "Peninsular Malaysia";
        }
        boolean added = graph.addAirport(new Airport(code, name, city, region));
        if (added) System.out.printf("  [OK] Airport [%s] %s added.%n", code, name);
    }

    /** Option 4: Add a new flight route */
    private static void menuAddFlight() {
        System.out.println(FlightGraph.bar('-', 50));
        System.out.println("  ADD NEW FLIGHT ROUTE");
        System.out.println(FlightGraph.bar('-', 50));
        System.out.print("  Flight Code  (e.g. MH001) : ");
        String code = sc.nextLine().trim().toUpperCase();
        System.out.print("  Departure Airport Code    : ");
        String src  = sc.nextLine().trim().toUpperCase();
        System.out.print("  Arrival Airport Code      : ");
        String dest = sc.nextLine().trim().toUpperCase();

        Airport srcAirport  = graph.getAirport(src);
        Airport destAirport = graph.getAirport(dest);
        if (srcAirport  == null) { System.out.println("  [!] [" + src  + "] not found."); return; }
        if (destAirport == null) { System.out.println("  [!] [" + dest + "] not found."); return; }

        double dist, dur, price;
        try {
            System.out.print("  Distance   (km)           : ");
            dist  = Double.parseDouble(sc.nextLine().trim());
            System.out.print("  Duration   (minutes)      : ");
            dur   = Double.parseDouble(sc.nextLine().trim());
            System.out.print("  Price      (RM)           : ");
            price = Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid number. Flight not added.");
            return;
        }
        boolean added = graph.addFlight(
                new Flight(code, srcAirport, destAirport, dist, dur, price));
        if (added) System.out.printf("  [OK] Flight %s [%s -> %s] added.%n", code, src, dest);
    }

    /** Option 5: Remove an airport */
    private static void menuRemoveAirport() {
        System.out.println(FlightGraph.bar('-', 50));
        System.out.println("  REMOVE AIRPORT");
        System.out.println(FlightGraph.bar('-', 50));
        System.out.print("  Airport Code to remove : ");
        String code = sc.nextLine().trim().toUpperCase();
        if (!graph.airportExists(code)) {
            System.out.println("  [!] Airport [" + code + "] not found.");
            return;
        }
        Airport a = graph.getAirport(code);
        System.out.printf("  Airport : [%s] %s%n", code, a.getName());
        System.out.println("  WARNING : This removes the airport AND all its routes.");
        System.out.print("  Confirm? (y/n) : ");
        if (sc.nextLine().trim().equalsIgnoreCase("y")) graph.removeAirport(code);
        else System.out.println("  Cancelled.");
    }

    /** Option 6: Remove a flight route */
    private static void menuRemoveFlight() {
        System.out.println(FlightGraph.bar('-', 50));
        System.out.println("  REMOVE FLIGHT ROUTE");
        System.out.println(FlightGraph.bar('-', 50));
        System.out.print("  Departure Airport Code : ");
        String src  = sc.nextLine().trim().toUpperCase();
        System.out.print("  Arrival Airport Code   : ");
        String dest = sc.nextLine().trim().toUpperCase();
        graph.removeFlight(src, dest);
    }

    /** Option 7: Check if a direct flight exists */
    private static void menuCheckDirectFlight() {
        System.out.println(FlightGraph.bar('-', 55));
        System.out.println("  CHECK DIRECT FLIGHT");
        System.out.println(FlightGraph.bar('-', 55));
        System.out.print("  From Airport Code : ");
        String src  = sc.nextLine().trim().toUpperCase();
        System.out.print("  To   Airport Code : ");
        String dest = sc.nextLine().trim().toUpperCase();

        Flight f = graph.getDirectFlight(src, dest);
        System.out.println();
        if (f != null) {
            System.out.println("  [YES] Direct flight available!");
            System.out.println(FlightGraph.bar('-', 55));
            System.out.printf("  Flight  : %s%n",        f.getFlightCode());
            System.out.printf("  From    : [%s] %s%n",   f.getSource().getCode(), f.getSource().getCity());
            System.out.printf("  To      : [%s] %s%n",   f.getDestination().getCode(), f.getDestination().getCity());
            System.out.printf("  Distance: %.0f km%n",   f.getDistance());
            System.out.printf("  Duration: %.0f min%n",  f.getDuration());
            System.out.printf("  Price   : RM %.2f%n",   f.getPrice());
        } else {
            System.out.println("  [NO] No direct flight from [" + src + "] to [" + dest + "].");
            System.out.println("  Tip: Use [12] or [13] to find an indirect route.");
        }
    }

    /** Option 8: View all direct destinations from an airport */
    private static void menuViewNeighbors() {
        System.out.print("  Airport Code : ");
        graph.displayNeighbors(sc.nextLine().trim().toUpperCase());
    }

    /** Option 9: Search a flight by its flight code */
    private static void menuSearchFlightCode() {
        System.out.print("  Flight Code (e.g. MH2618) : ");
        String code = sc.nextLine().trim().toUpperCase();
        Flight f = graph.searchFlightByCode(code);
        System.out.println();
        if (f != null) {
            System.out.println(FlightGraph.bar('-', 55));
            System.out.printf("  Flight  : %s%n",           f.getFlightCode());
            System.out.printf("  From    : [%s] %s%n",      f.getSource().getCode(), f.getSource().getName());
            System.out.printf("  To      : [%s] %s%n",      f.getDestination().getCode(), f.getDestination().getName());
            System.out.printf("  Distance: %.0f km%n",      f.getDistance());
            System.out.printf("  Duration: %.0f min (%.1f hrs)%n", f.getDuration(), f.getDuration()/60.0);
            System.out.printf("  Price   : RM %.2f%n",      f.getPrice());
            System.out.println(FlightGraph.bar('-', 55));
        } else {
            System.out.println("  [!] Flight [" + code + "] not found.");
        }
    }

    /** Option 10: BFS Traversal */
    private static void menuBFS() {
        System.out.print("  Starting Airport Code : ");
        traversal.BFS(sc.nextLine().trim().toUpperCase());
    }

    /** Option 11: DFS Traversal */
    private static void menuDFS() {
        System.out.print("  Starting Airport Code : ");
        traversal.DFS(sc.nextLine().trim().toUpperCase());
    }

    /** Option 12: Dijkstra Shortest Distance */
    private static void menuDijkstraDistance() {
        System.out.print("  From Airport Code : ");
        String src  = sc.nextLine().trim().toUpperCase();
        System.out.print("  To   Airport Code : ");
        String dest = sc.nextLine().trim().toUpperCase();
        traversal.dijkstraByDistance(src, dest);
    }

    /** Option 13: Dijkstra Cheapest Price */
    private static void menuDijkstraPrice() {
        System.out.print("  From Airport Code : ");
        String src  = sc.nextLine().trim().toUpperCase();
        System.out.print("  To   Airport Code : ");
        String dest = sc.nextLine().trim().toUpperCase();
        traversal.dijkstraByCheapestPrice(src, dest);
    }

    // ============================================================
    //  SAMPLE DATA LOADER
    //  18 Malaysian airports: 8 Peninsular, 5 Sabah, 5 Sarawak
    //  55 domestic flight routes
    // ============================================================
    private static void loadSampleData() {

        // --------------------------------------------------------
        // AIRPORTS (VERTICES)
        // --------------------------------------------------------

        // ---- Peninsular Malaysia (8 airports) ----
        graph.addAirport(new Airport("KUL", "Kuala Lumpur International Airport",
                "Sepang, Selangor",               "Peninsular Malaysia"));
        graph.addAirport(new Airport("PEN", "Penang International Airport",
                "Georgetown, Penang",              "Peninsular Malaysia"));
        graph.addAirport(new Airport("JHB", "Senai International Airport",
                "Johor Bahru, Johor",              "Peninsular Malaysia"));
        graph.addAirport(new Airport("LGK", "Langkawi International Airport",
                "Langkawi, Kedah",                 "Peninsular Malaysia"));
        graph.addAirport(new Airport("KBR", "Sultan Ismail Petra Airport",
                "Kota Bharu, Kelantan",            "Peninsular Malaysia"));
        graph.addAirport(new Airport("TGG", "Sultan Mahmud Airport",
                "Kuala Terengganu, Terengganu",    "Peninsular Malaysia"));
        graph.addAirport(new Airport("AOR", "Sultan Abdul Halim Airport",
                "Alor Setar, Kedah",               "Peninsular Malaysia"));
        graph.addAirport(new Airport("IPH", "Sultan Azlan Shah Airport",
                "Ipoh, Perak",                     "Peninsular Malaysia"));

        // ---- Sabah (5 airports) ----
        graph.addAirport(new Airport("BKI", "Kota Kinabalu International Airport",
                "Kota Kinabalu, Sabah",            "Sabah"));
        graph.addAirport(new Airport("TWU", "Tawau Airport",
                "Tawau, Sabah",                    "Sabah"));
        graph.addAirport(new Airport("SDK", "Sandakan Airport",
                "Sandakan, Sabah",                 "Sabah"));
        graph.addAirport(new Airport("LDU", "Lahad Datu Airport",
                "Lahad Datu, Sabah",               "Sabah"));
        graph.addAirport(new Airport("KUD", "Kudat Airport",
                "Kudat, Sabah",                    "Sabah"));

        // ---- Sarawak (5 airports) ----
        graph.addAirport(new Airport("KCH", "Kuching International Airport",
                "Kuching, Sarawak",                "Sarawak"));
        graph.addAirport(new Airport("MYY", "Miri Airport",
                "Miri, Sarawak",                   "Sarawak"));
        graph.addAirport(new Airport("BTU", "Bintulu Airport",
                "Bintulu, Sarawak",                "Sarawak"));
        graph.addAirport(new Airport("SBW", "Sibu Airport",
                "Sibu, Sarawak",                   "Sarawak"));
        graph.addAirport(new Airport("LMN", "Limbang Airport",
                "Limbang, Sarawak",                "Sarawak"));

        // --------------------------------------------------------
        // FLIGHT ROUTES (DIRECTED WEIGHTED EDGES)
        // Format: addFlight(code, src, dest, distance_km, duration_min, price_RM)
        // --------------------------------------------------------

        // === FROM KUL (Kuala Lumpur) ===
        addFlight("MH1202", "KUL", "PEN",   322,  55,  180.00);
        addFlight("MH1180", "KUL", "JHB",   330,  55,  160.00);
        addFlight("MH1168", "KUL", "LGK",   452,  65,  200.00);
        addFlight("MH1134", "KUL", "KBR",   477,  70,  220.00);
        addFlight("MH1152", "KUL", "TGG",   443,  65,  210.00);
        addFlight("MH1114", "KUL", "AOR",   450,  65,  195.00);
        addFlight("MH1124", "KUL", "IPH",   197,  45,  145.00);
        addFlight("MH2618", "KUL", "BKI",  1597, 155,  450.00);
        addFlight("MH2506", "KUL", "KCH",  1393, 140,  400.00);
        addFlight("MH2676", "KUL", "MYY",  1741, 165,  480.00);
        addFlight("MH2526", "KUL", "BTU",  1551, 150,  430.00);
        addFlight("MH2516", "KUL", "SBW",  1452, 145,  410.00);

        // === FROM PENINSULAR AIRPORTS BACK TO KUL ===
        addFlight("MH1203", "PEN", "KUL",   322,  55,  185.00);
        addFlight("MH1181", "JHB", "KUL",   330,  55,  165.00);
        addFlight("MH1169", "LGK", "KUL",   452,  65,  205.00);
        addFlight("MH1135", "KBR", "KUL",   477,  70,  225.00);
        addFlight("MH1153", "TGG", "KUL",   443,  65,  215.00);
        addFlight("MH1115", "AOR", "KUL",   450,  65,  200.00);
        addFlight("MH1125", "IPH", "KUL",   197,  45,  150.00);

        // === PENINSULAR CROSS-ROUTES ===
        addFlight("AK6350", "PEN", "BKI",  1856, 180,  310.00);
        addFlight("AK6382", "JHB", "BKI",  1766, 175,  300.00);
        addFlight("AK6322", "JHB", "KCH",  1578, 155,  290.00);

        // === FROM BKI (Kota Kinabalu) ===
        addFlight("MH2619", "BKI", "KUL",  1597, 155,  455.00);
        addFlight("MH3264", "BKI", "TWU",   345,  60,  180.00);
        addFlight("MH3254", "BKI", "SDK",   277,  55,  160.00);
        addFlight("MH3256", "BKI", "LDU",   283,  55,  165.00);
        addFlight("MH3246", "BKI", "KUD",   157,  40,  120.00);
        addFlight("MH2862", "BKI", "KCH",  1290, 135,  380.00);
        addFlight("MH2840", "BKI", "MYY",   651,  90,  250.00);

        // === SABAH INTERNAL ROUTES ===
        addFlight("MH3265", "TWU", "BKI",   345,  60,  185.00);
        addFlight("MH3285", "TWU", "SDK",   185,  45,  140.00);
        addFlight("MH3255", "SDK", "BKI",   277,  55,  165.00);
        addFlight("MH3284", "SDK", "TWU",   185,  45,  140.00);
        addFlight("MH3257", "LDU", "BKI",   283,  55,  170.00);
        addFlight("MH3247", "KUD", "BKI",   157,  40,  125.00);

        // === FROM KCH (Kuching) ===
        addFlight("MH2507", "KCH", "KUL",  1393, 140,  405.00);
        addFlight("MH2863", "KCH", "BKI",  1290, 135,  385.00);
        addFlight("MH3216", "KCH", "SBW",   247,  50,  160.00);
        addFlight("MH3238", "KCH", "BTU",   389,  60,  190.00);
        addFlight("MH3244", "KCH", "MYY",   646,  90,  255.00);

        // === FROM MYY (Miri) ===
        addFlight("MH2677", "MYY", "KUL",  1741, 165,  485.00);
        addFlight("MH2841", "MYY", "BKI",   651,  90,  255.00);
        addFlight("MH3242", "MYY", "BTU",   265,  55,  165.00);
        addFlight("MH3245", "MYY", "KCH",   646,  90,  255.00);
        addFlight("MH3268", "MYY", "LMN",   190,  45,  140.00);
        addFlight("MH3252", "MYY", "SBW",   480,  75,  220.00);

        // === FROM BTU (Bintulu) ===
        addFlight("MH2527", "BTU", "KUL",  1551, 150,  435.00);
        addFlight("MH3243", "BTU", "MYY",   265,  55,  165.00);
        addFlight("MH3233", "BTU", "SBW",   180,  45,  140.00);
        addFlight("MH3239", "BTU", "KCH",   389,  60,  195.00);

        // === FROM SBW (Sibu) ===
        addFlight("MH2517", "SBW", "KUL",  1452, 145,  415.00);
        addFlight("MH3217", "SBW", "KCH",   247,  50,  165.00);
        addFlight("MH3232", "SBW", "BTU",   180,  45,  140.00);
        addFlight("MH3253", "SBW", "MYY",   480,  75,  225.00);

        // === FROM LMN (Limbang) ===
        addFlight("MH3269", "LMN", "MYY",   190,  45,  140.00);
    }

    /**
     * Helper method: creates and adds a Flight using airport codes.
     */
    private static void addFlight(String flightCode, String srcCode, String destCode,
                                   double distance, double duration, double price) {
        Airport src  = graph.getAirport(srcCode);
        Airport dest = graph.getAirport(destCode);
        if (src == null || dest == null) return;
        graph.addFlight(new Flight(flightCode, src, dest, distance, duration, price));
    }

    // ============================================================
    //  UTILITY: Clear terminal screen (ANSI escape code)
    // ============================================================
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

}
