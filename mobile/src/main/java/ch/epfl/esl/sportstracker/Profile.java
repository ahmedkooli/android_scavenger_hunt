package ch.epfl.esl.sportstracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

class Profile implements Serializable {

    String username;
    String password;
    int height_cm;
    float weight_kg;
    String photoPath;

    Profile(String username, String password) {
        // When you create a new Profile, it's good to build it based on username and password
        this.username = username;
        this.password = password;
    }

    DataMap toDataMap() {
        DataMap dataMap = new DataMap();
        dataMap.putString("username", username);
        dataMap.putString("password", password);
        dataMap.putInt("height", height_cm);
        dataMap.putFloat("weight", weight_kg);
        final InputStream imageStream;
        try {
            imageStream = new FileInputStream(photoPath);
            final Bitmap userImage = BitmapFactory.decodeStream(imageStream);
            Asset asset = WearService.createAssetFromBitmap(userImage);
            dataMap.putAsset("photo", asset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return dataMap;
    }
}
