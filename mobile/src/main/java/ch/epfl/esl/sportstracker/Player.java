package ch.epfl.esl.sportstracker;

import android.location.Location;

import java.util.ArrayList;

public class Player {
    private Location location;
    private int heart_rate;
    private ArrayList<Treasure> trasures_found;
    //private Profile profile;

    public Player()
    {
        location = new Location("");
        trasures_found = new ArrayList<Treasure>();
    }

    public void setHeartRate (int heart_rate)           { this.heart_rate = heart_rate; }
    public void     setLocation(Location location)      { this.location = location;}
    public Location getLocation()                       { return location;}
    public void addTreasureFound(Treasure treasure)      { trasures_found.add(treasure);}
}
