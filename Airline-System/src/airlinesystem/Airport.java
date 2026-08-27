package airlinesystem;

public class Airport {

    private String code;
    private String name;
    private String city;
    private String region;

    public Airport(String code, String name, String city, String region) {
        this.code   = code;
        this.name   = name;
        this.city   = city;
        this.region = region;
    }

    public String getCode()   { return code;   }
    public String getName()   { return name;   }
    public String getCity()   { return city;   }
    public String getRegion() { return region; }

    public void setCode(String code)     { this.code   = code;   }
    public void setName(String name)     { this.name   = name;   }
    public void setCity(String city)     { this.city   = city;   }
    public void setRegion(String region) { this.region = region; }

    @Override
    public String toString() {
        return String.format("%-5s | %-45s | %-30s | %s", code, name, city, region);
    }
}
