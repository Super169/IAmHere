package org.super169.mylocation;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by James on 10/12/2014.
 * GPSTracker class
 *   Retrieve GPS information as LocationListener
 */
public class GPSTracker extends Service implements LocationListener {

    public enum LocationType {
        GPS,
        NETWORK
    }

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters; request to update even no change

    // The minimum time between updates in milliseconds
//    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private static final long MIN_TIME_BW_UPDATES = 5000; //  5 second

    // Declaring a Location Manager
    private LocationManager locationManager;

    public GPSTracker() {

    }

    public Location getLocation(Context context) {
        Location mLocationGps, mLocationReturn;
        mLocationGps = getLocationGps(context);
        mLocationReturn = getLocationNetwork(context);

        if ((mLocationReturn == null) || ((mLocationGps != null) && (mLocationGps.getTime() > mLocationReturn.getTime()))) {
            mLocationReturn = mLocationGps;
        }
        return mLocationReturn;
    }

    public Location getLocationGps(Context context) {
        LocationResult mResult = requestLocation(context, LocationType.GPS);
        return mResult.location();
    }

    public Location getLocationNetwork(Context context) {
        LocationResult mResult = requestLocation(context, LocationType.NETWORK);
        return mResult.location();
    }

    public LocationResult requestLocation(Context context, LocationType mLocationType) {
        LocationResult mResult = new LocationResult();
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mResult.SetError(LocationResult.ResultStatus.NO_PERMISSION, "ACCESS_COARSE_LOCATION not granted");
                return mResult;
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mResult.SetError(LocationResult.ResultStatus.NO_PERMISSION, "ACCESS_FINE_LOCATION not granted");
                return mResult;
            }
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            if (locationManager == null) {
                mResult.SetError(LocationResult.ResultStatus.NO_SERVICE, "LOCATION_SERVICE is null");
                return mResult;
            }
            String provider = "";
            String service = "";
            switch (mLocationType) {
                case GPS:
                    provider = LocationManager.GPS_PROVIDER;
                    service = "GPS";
                    break;
                case NETWORK:
                    provider = LocationManager.NETWORK_PROVIDER;
                    service = "Network";
                    break;
                default:
                    mResult.SetError(LocationResult.ResultStatus.UNEXPECTED_ERROR, "Location Type: " + mLocationType.toString());
                    return mResult;
            }
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(
                        provider,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d("Get GPS Location", "GPS Enabled");
                if (locationManager != null) {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location == null) {
                        mResult.SetError(LocationResult.ResultStatus.FAIL_GET_LOCATION, "Fail getting " + service + " location");
                    } else {
                        mResult.SetLocation(location);
                    }
                }
            } else {
                mResult.SetError(LocationResult.ResultStatus.NO_SERVICE, service + " not available");
            }
        } catch (Exception e) {
            mResult.SetError(LocationResult.ResultStatus.UNEXPECTED_ERROR, e.getMessage());
        }
        return mResult;
    }

    @Override
    public void onLocationChanged(Location location) {
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