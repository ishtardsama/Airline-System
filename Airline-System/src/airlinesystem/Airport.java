/*
 * Airport.java
 * Represents a vertex (node) in the Airline Flight Network Graph.
 * Each airport has a unique IATA 3-letter code, name, city, and region.
 *
 * DSA Assignment - Malaysia Airline Flight Network Graph
 */
package airlinesystem;

/**
 * Airport class - Vertex / Node of the Flight Network Graph.
 * Stores airport details used as graph nodes in the adjacency list.
 */
public class Airport {

    private String code;    // IATA 3-letter code  (e.g. KUL, BKI, KCH)
    private String name;    // Full airport name
    private String city;    // City and state
    private String region;  // Peninsular Malaysia / Sabah / Sarawak

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public Airport(String code, String name, String city, String region) {
        this.code   = code;
        this.name   = name;
        this.city   = city;
        this.region = region;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------
    public String getCode()   { return code;   }
    public String getName()   { return name;   }
    public String getCity()   { return city;   }
    public String getRegion() { return region; }

    // -------------------------------------------------------
    // Setters
    // -------------------------------------------------------
    public void setCode(String code)     { this.code   = code;   }
    public void setName(String name)     { this.name   = name;   }
    public void setCity(String city)     { this.city   = city;   }
    public void setRegion(String region) { this.region = region; }

    // -------------------------------------------------------
    // toString
    // -------------------------------------------------------
    @Override
    public String toString() {
        return String.format("%-5s | %-45s | %-30s | %s",
                code, name, city, region);
    }
}
