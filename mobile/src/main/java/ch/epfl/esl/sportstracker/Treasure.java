package ch.epfl.esl.sportstracker;

import android.location.Location;

public class Treasure {
    private Location location;

    public Treasure()
    {
        location = new Location("");
    }

    public Treasure(Location location)
    {
        this.location = new Location(location);
    }

    public void Treasure(Location location)  {this.location = location;}

    public Location getLocation()               { return this.location; }



}
