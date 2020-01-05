package ch.epfl.esl.sportstracker;

import android.location.Location;

import java.util.ArrayList;

public class Player {
    private Location location;
    private int heart_rate;
    private ArrayList<ObjectAR> treasures_found;
    /** TO DO: add profile */
    //private Profile profile;

    public Player()
    {
        location = new Location("");
        treasures_found = new ArrayList<>();
    }

    public void setHeartRate (int heart_rate)           { this.heart_rate = heart_rate; }
    public void     setLocation(Location location)      { this.location = location;}
    public Location getLocation()                       { return location;}
    public void addTreasureFound(ObjectAR treasure)      { treasures_found.add(treasure);}

    public ArrayList<ObjectAR> getTrasuresFound() {
        return treasures_found;
    }
}
