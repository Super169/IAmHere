package org.super169.mylocation;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by James on 10/12/2014.
 */
public class GPSTracker extends Service implements LocationListener {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude
    String gpsType= "";
    float accuracy;
    String provider;
    double altitude;
    long lastTime;

    boolean locationUpdated;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters; request to update even no change

    // The minimum time between updates in milliseconds
//    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private static final long MIN_TIME_BW_UPDATES = 5000; //  5 second

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation(false);
    }

    public Location getLocation(boolean gpsOnly) {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                gpsType = "GPS Not available";
            } else {

                this.canGetLocation = true;

                Location gpsLocation = null;
                Location networkLocation = null;

                // First get location from Network Provider
                if (isNetworkEnabled && !gpsOnly) {
                    locationUpdated = false;
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
//                    while (!locationUpdated);
                    if (locationManager != null) {
                        networkLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
/*                        if (location != null) {
                            gpsType = "GPS: Network";
                            accuracy = location.getAccuracy();
                            provider = location.getProvider();
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            lastTime = location.getTime();
                        }*/
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
//                    if (location == null) {
                    locationUpdated = false;
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("GPS Enabled", "GPS Enabled");
//                    while (!locationUpdated);
                    if (locationManager != null) {
                        gpsLocation = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
/*                            if (location != null) {
                                if (location.getTime() > lastTime) {
                                    gpsType = "GPS: GPS";
                                    accuracy = location.getAccuracy();
                                    provider = location.getProvider();
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }*/
                    }
                    //                  }
                }

                long gpsTime = 0;
                long networkTime = 0;
                if (gpsLocation != null) gpsTime = gpsLocation.getTime();
                if (networkLocation != null) networkTime = networkLocation.getTime();

                if (gpsTime > networkTime) {
                    location = gpsLocation;
                } else {
                    location = networkLocation;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    /**
     * Function to get GPS type
     * */
    public String getGpsType() {
        return gpsType;
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to get accuracy
     * */
    public float getAccuracy(){
        if(location != null){
            accuracy = location.getAccuracy();
        }
        // return latitude
        return accuracy;
    }

    /**
     * Function to get provider
     * */
    public String getProvider(){
        if(location != null){
            provider = location.getProvider();
        }

        // return longitude
        return provider;
    }

    /**
     * Function to get altitude
     * */
    public double getAltitude(){
        if(location != null){
            altitude = location.getAltitude();
        }
        // return latitude
        return altitude;
    }

    /**
     * Function to get speed
     * */
    public float getSpeed(){
        float speed = 0;
        if(location != null){
            speed = location.getSpeed();
        }
        // return latitude
        return speed;
    }

    /**
     * Function to get speed
     * */
    public float getBearing(){
        float bearing = 0;
        if(location != null){
            bearing = location.getBearing();
        }
        // return latitude
        return bearing;
    }

    /**
     * Function to get speed
     * */
    public long getTime(){
        long time = 0;
        if(location != null){
            time = location.getTime();
        }
        // return latitude
        return time;
    }


    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        locationUpdated = true;
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}