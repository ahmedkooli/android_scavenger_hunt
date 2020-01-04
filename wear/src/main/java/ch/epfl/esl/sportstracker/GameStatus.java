package ch.epfl.esl.sportstracker;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.wearable.DataMap;


public class GameStatus implements Parcelable {
    private Location player_location;
    private Location next_treasure_location;
    private long starting_time;
    private long time_limit;
    private int heart_rate;

    public GameStatus()
    {
        player_location = new Location("unknown provider");
        next_treasure_location = new Location("unknown provider");
        starting_time = 0;
        time_limit = 0;
        heart_rate = 0;
    }

    // converts the class into a "DataMap" object (data is stored in key : value pairs)
    public DataMap toDataMap(){

        DataMap dataMap = new DataMap();

        dataMap.putDouble(BuildConfig.W_player_latitude, player_location.getLatitude());
        dataMap.putDouble(BuildConfig.W_player_longitude, player_location.getLongitude());

        dataMap.putDouble(BuildConfig.W_next_clue_latitude, next_treasure_location.getLatitude());
        dataMap.putDouble(BuildConfig.W_next_clue_longitude, next_treasure_location.getLongitude());

        dataMap.putLong(BuildConfig.W_starting_time, starting_time);
        dataMap.putLong(BuildConfig.W_time_limit, time_limit);
        dataMap.putInt(BuildConfig.W_heart_rate, heart_rate);

        return dataMap;
    }

    public void setPlayerLocation(Location location)
    {
        this.player_location = location;
    }
    public void setPlayerLocation(double latitude, double longitude) {
        if (player_location == null)
            player_location = new Location("unknown provider");
        player_location.setLatitude(latitude);
        player_location.setLongitude(longitude);
    }
    public Location getPlayer_location() { return this.player_location; }

    public void setHeartRate(int heart_rate) { this.heart_rate = heart_rate; }

    public int getHeartRate() { return this.heart_rate; }
    public long getTimeLimit() { return this.time_limit; }
    public long getRemainingTime()
    {
        if (time_limit > System.currentTimeMillis())
            return time_limit - System.currentTimeMillis();
        else
            return 0;
    }

    public void setTimeLimit(long time_limit) {this.time_limit = time_limit;}

    public void setNextClueLocation(Location location)
    {
        this.next_treasure_location = location;
    }
    public void setNextClueLocation(double latitude, double longitude) {
        if (next_treasure_location == null)
            next_treasure_location = new Location("unknown provider");
        next_treasure_location.setLatitude(latitude);
        next_treasure_location.setLongitude(longitude);
    }
    public Location getNextClueLocation() { return next_treasure_location; }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.player_location, flags);
        dest.writeParcelable(this.next_treasure_location, flags);
        dest.writeLong(this.starting_time);
        dest.writeLong(this.time_limit);
        dest.writeInt(this.heart_rate);
    }

    // Parcelling part
    public GameStatus(Parcel in){
        this.player_location= in.readParcelable(Location.class.getClassLoader());
        this.next_treasure_location = in.readParcelable(Location.class.getClassLoader());
        this.starting_time = in.readLong();
        this.time_limit = in.readLong();
        this.heart_rate = in.readInt();
    }

    public static final Creator CREATOR = new Creator() {
        public GameStatus createFromParcel(Parcel in) {
            return new GameStatus(in);
        }

        public GameStatus[] newArray(int size) {
            return new GameStatus[size];
        }
    };
}


