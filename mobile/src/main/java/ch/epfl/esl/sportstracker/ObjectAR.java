package ch.epfl.esl.sportstracker;

import android.location.Location;

public class ObjectAR {
    private Location location;

    // constructors
    public ObjectAR()
    {
        location = new Location("");
    }
    public ObjectAR(double latitude, double longitude)
    {
        location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
    }
    public ObjectAR(Location location)          { this.location = new Location(location); }


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
