package ch.epfl.esl.sportstracker;

import android.location.Location;

import java.util.ArrayList;

public class GameMap {
    private ArrayList<ObjectAR> treasures;
    private ArrayList<ObjectAR> clues;
    private String name;

    GameMap()
    {
        treasures = new ArrayList<>();
        clues = new ArrayList<>();
    }

    // setters
    public void setName( String name )              { this.name = name; }

    // getters
    public ArrayList<ObjectAR>  getTreasures()          { return treasures; }
    public ObjectAR             getTreasure(int index)  { return treasures.get(index); }
    public ArrayList<ObjectAR>  getClues()              { return clues; }
    public int                  getTreasuresCount()     { return treasures.size(); }


    public void addTreasure(ObjectAR treasure)      { treasures.add(treasure); }
    public void addClue(ObjectAR clue)              { clues.add(clue); }
}
