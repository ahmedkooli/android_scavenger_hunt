package ch.epfl.esl.sportstracker;

import android.location.Location;

public class Treasure {
    private Location location;

    // constructors
    public Treasure()
    {
        location = new Location("");
    }
    public Treasure(double latitude, double longitude)
    {
        location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
    }
    public Treasure(Location location)          { this.location = new Location(location); }


    // setters
    public void setLocation(Location location){ this.location = location;}
    public void setLocation(double latitude, double longitude)
    {
        this.location.setLatitude(latitude);
        this.location.setLongitude(longitude);
    }

    // getters
    public Location getLocation()               { return this.location; }
    public double getLatitude()                 { return location.getLatitude();}
    public double getLongitude()                { return location.getLongitude();}
}
