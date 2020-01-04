package ch.epfl.esl.sportstracker;

import android.location.Location;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;

/** This class holds the objects and the settings of the game, it is used to keep game's data tidy and clean **/
public class GameData {
    private ArrayList<Treasure> treasures;
    private ArrayList<Player> opponents;
    private Player player;
    private Treasure next_treasure;
    private long starting_time;
    private long time_limit;
    private float threshold_distance;   // distance below which a treasure is considered to be found

    public GameData()
    {
        treasures = new ArrayList<Treasure>();
        opponents = new ArrayList<Player>();
        player = new Player();
        next_treasure = new Treasure();
    }

    // setters
    public void setTreasures(ArrayList<Treasure> treasures)     { this.treasures = treasures; }
    public void setOpponents(ArrayList<Player> opponents)       { this.opponents = opponents; }
    public void setPlayer(Player player)                        { this.player = player; }
    public void setNext_treasure(Treasure next_treasure)        { this.next_treasure = next_treasure;}
    public void setStarting_time(long starting_time)            { this.starting_time = starting_time;}
    public void setTime_limit(long time_limit)                  { this.time_limit = time_limit;}
    public void setThreshold_distance(float distance)           { threshold_distance = distance;}

    public void setPlayerLocation(Location location)            { player.setLocation(location);}
    public void setHeartRate(int heart_rate)                    { player.setHeartRate(heart_rate);}


    // getters
    public ArrayList<Treasure> getTreasures()   { return treasures; }
    public float getThreshold_distance()        { return threshold_distance;}
    public Player getPlayer()                   { return player; }
    public Location getPlayerLocation()         { return player.getLocation(); }
    public Treasure getNext_treasure()          { return next_treasure; }
    public long getTimeLimit()                  { return time_limit; }
    public long getStartingTime()               { return starting_time; }

    public long getRemainingTime()
    {
        if (time_limit > System.currentTimeMillis())
            return time_limit - System.currentTimeMillis();
        else
            return 0;
    }

    public void addOpponent(Player new_opponent)
    {
        if (opponents == null)
            opponents = new ArrayList<Player>();
        opponents.add(new_opponent);
    }

    public float getPlayerDistanceFromTreasure()
    {
        return player.getLocation().distanceTo(next_treasure.getLocation());
    }

    public long getElapsedTime()
    {
        return System.currentTimeMillis() - starting_time;
    }

    // builds a dataMap used to send info to the watch (for now only relevant info is sent, but it can be customized)
    public DataMap toDataMap()
    {
        DataMap dataMap = new DataMap();

        // puts coordinates of player
        dataMap.putDouble(BuildConfig.W_player_latitude, player.getLocation().getLatitude());
        dataMap.putDouble(BuildConfig.W_player_longitude, player.getLocation().getLongitude());

        // puts coordinates of next treasure
        dataMap.putDouble(BuildConfig.W_next_clue_latitude, next_treasure.getLocation().getLatitude());
        dataMap.putDouble(BuildConfig.W_next_clue_longitude, next_treasure.getLocation().getLongitude());

        // puts time info
        dataMap.putLong(BuildConfig.W_starting_time, starting_time);
        dataMap.putLong(BuildConfig.W_time_limit, time_limit);

        return dataMap;
    }

}
