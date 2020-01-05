package ch.epfl.esl.sportstracker;

import android.location.Location;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import java.util.ArrayList;

public class GameMap {
    private ArrayList<ObjectAR> treasures;
    private ArrayList<ObjectAR> clues;
    private String name;
    private LatLng south_west_bound;
    private LatLng north_east_bound;
    private boolean bounds_defined = false;

    GameMap()
    {
        treasures = new ArrayList<>();
        clues = new ArrayList<>();
    }

    // setters
    public void setName( String name )              { this.name = name; }
    public void setBounds(double south_west_latitude, double south_west_longitude, double north_east_latitude, double north_east_longitude)
    {
        south_west_bound = new LatLng(south_west_latitude, south_west_longitude);
        north_east_bound = new LatLng(north_east_latitude, north_east_longitude);
        bounds_defined = true;
    }

    // getters
    public ArrayList<ObjectAR>  getTreasures()          { return treasures; }
    public ObjectAR             getTreasure(int index)  { return treasures.get(index); }
    public ArrayList<ObjectAR>  getClues()              { return clues; }
    public int                  getTreasuresCount()     { return treasures.size(); }
    public boolean              getBoundsDefined()      { return bounds_defined; }
    public LatLngBounds         getLatLngBounds()       { return LatLngBounds.from(north_east_bound.getLatitude(),
                                                                                    north_east_bound.getLongitude(),
                                                                                    south_west_bound.getLatitude(),
                                                                                    south_west_bound.getLongitude());}


    public void addTreasure(ObjectAR treasure)      { treasures.add(treasure); }
    public void addClue(ObjectAR clue)              { clues.add(clue); }
}
