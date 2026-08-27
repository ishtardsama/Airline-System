/*
 * Flight.java
 * Represents a directed weighted edge in the Airline Flight Network Graph.
 * Each flight connects a source airport to a destination airport with
 * associated weight attributes: distance (km), duration (min), price (RM).
 *
 * DSA Assignment - Malaysia Airline Flight Network Graph
 */
package airlinesystem;

/**
 * Flight class - Directed Weighted Edge of the Flight Network Graph.
 * Used as the edge object stored in each adjacency list entry.
 */
public class Flight {

    private String  flightCode;   // e.g.  MH2618
    private Airport source;       // Departure airport (edge origin)
    private Airport destination;  // Arrival airport   (edge target)
    private double  distance;     // Distance in kilometres
    private double  duration;     // Flight duration in minutes
    private double  price;        // Ticket price in Ringgit Malaysia (RM)

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public Flight(String flightCode, Airport source, Airport destination,
                  double distance, double duration, double price) {
        this.flightCode   = flightCode;
        this.source       = source;
        this.destination  = destination;
        this.distance     = distance;
        this.duration     = duration;
        this.price        = price;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------
    public String  getFlightCode()  { return flightCode;  }
    public Airport getSource()      { return source;      }
    public Airport getDestination() { return destination; }
    public double  getDistance()    { return distance;    }
    public double  getDuration()    { return duration;    }
    public double  getPrice()       { return price;       }

    // -------------------------------------------------------
    // Setters
    // -------------------------------------------------------
    public void setFlightCode(String flightCode)      { this.flightCode  = flightCode;  }
    public void setSource(Airport source)             { this.source      = source;      }
    public void setDestination(Airport destination)   { this.destination = destination; }
    public void setDistance(double distance)          { this.distance    = distance;    }
    public void setDuration(double duration)          { this.duration    = duration;    }
    public void setPrice(double price)                { this.price       = price;       }

    // -------------------------------------------------------
    // toString
    // -------------------------------------------------------
    @Override
    public String toString() {
        return String.format("%-8s | %s --> %-5s | %6.0f km | %4.0f min | RM %7.2f",
                flightCode, source.getCode(), destination.getCode(),
                distance, duration, price);
    }
}
