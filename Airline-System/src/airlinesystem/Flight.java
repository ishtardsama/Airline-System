package airlinesystem;

public class Flight {

    private String  flightCode;
    private Airport source;
    private Airport destination;
    private double  distance;
    private double  duration;
    private double  price;

    public Flight(String flightCode, Airport source, Airport destination,
                  double distance, double duration, double price) {
        this.flightCode  = flightCode;
        this.source      = source;
        this.destination = destination;
        this.distance    = distance;
        this.duration    = duration;
        this.price       = price;
    }

    public String  getFlightCode()  { return flightCode;  }
    public Airport getSource()      { return source;      }
    public Airport getDestination() { return destination; }
    public double  getDistance()    { return distance;    }
    public double  getDuration()    { return duration;    }
    public double  getPrice()       { return price;       }

    public void setFlightCode(String flightCode)    { this.flightCode  = flightCode;  }
    public void setSource(Airport source)           { this.source      = source;      }
    public void setDestination(Airport destination) { this.destination = destination; }
    public void setDistance(double distance)        { this.distance    = distance;    }
    public void setDuration(double duration)        { this.duration    = duration;    }
    public void setPrice(double price)              { this.price       = price;       }

    @Override
    public String toString() {
        return String.format("%-8s | %s --> %-5s | %6.0f km | %4.0f min | RM %7.2f",
                flightCode, source.getCode(), destination.getCode(),
                distance, duration, price);
    }
}
