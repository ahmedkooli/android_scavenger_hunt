package ch.epfl.esl.sportstracker;

import android.location.Location;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;

/** This class holds the objects and the settings of the game, it is used to keep game's data tidy and clean **/
public class Game {
    public static final float threshold_distance = 2;   // distance below which a treasure is considered to be found (meters)
    public static final int min_available_time = 5000;     // min time available to find a treasure, if treasure is far a bonus is added
    public static final int bonus_ms_per_meter = 50;       // a bonus of tot milliseconds for each meter is added according to the distance of the player from the treasure

    private GameMap map;
    private ArrayList<Player> opponents;
    private Player player;
    private ObjectAR treasure_to_find;
    private long starting_time;
    private long time_limit;


    public Game()
    {
        opponents = new ArrayList<Player>();
        player = new Player();
        treasure_to_find = new ObjectAR();
        map = new GameMap();
    }

    // setters
    public void setOpponents(ArrayList<Player> opponents)       { this.opponents = opponents; }
    public void setPlayer(Player player)                        { this.player = player; }
    public void setTreasureToFind(ObjectAR treasure_to_find)    { this.treasure_to_find = treasure_to_find;}
    public void setStartingTime(long starting_time)            { this.starting_time = starting_time;}
    public void setTimeLimit(long time_limit)                   { this.time_limit = time_limit;}
    public void setPlayerLocation(Location location)            { player.setLocation(location);}
    public void setHeartRate(int heart_rate)                    { player.setHeartRate(heart_rate);}
    public void setMap( GameMap map )                           { this.map = map; }


    // getters
    public ArrayList<ObjectAR>  getTreasures()                  { return map.getTreasures(); }
    public GameMap              getMap()                        { return map; }
    public Player               getPlayer()                     { return player; }
    public Location             getPlayerLocation()             { return player.getLocation(); }
    public ObjectAR             getTreasureToFind()             { return treasure_to_find; }
    public long                 getTimeLimit()                  { return time_limit; }
    public long                 getStartingTime()               { return starting_time; }


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
        return player.getLocation().distanceTo(treasure_to_find.getLocation());
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
        dataMap.putDouble(BuildConfig.W_next_clue_latitude, treasure_to_find.getLocation().getLatitude());
        dataMap.putDouble(BuildConfig.W_next_clue_longitude, treasure_to_find.getLocation().getLongitude());

        // puts time info
        dataMap.putLong(BuildConfig.W_starting_time, starting_time);
        dataMap.putLong(BuildConfig.W_time_limit, time_limit);

        return dataMap;
    }

}
