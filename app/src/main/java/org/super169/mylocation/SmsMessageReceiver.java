package org.super169.mylocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

/**
 * Created by James on 10/12/2014.
 * SmsMessageReceiver : customized sms receiver, check SMS content to find out request from WhereAreYou
 */
public class SmsMessageReceiver extends BroadcastReceiver implements
        ConnectionCallbacks, OnConnectionFailedListener {
    /** Tag string for our debug logs */
    private static final String TAG = "SmsMessageReceiver";
    private static final String ACTION_SMS_SENT = "org.super169.mylocation.SMS_SENT_ACTION";

    private static GPSTracker gps;
    private static Location mLocationGps;
    private static Location mLocationNetwork;
    private static Location mLocationFused;

    // It is intended to use a fixed date format.
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Provides the entry point to Google Play services.
     */
    private static GoogleApiClient mGoogleApiClient;

    // TODO: try to avoid putting Context in static variable in later version
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private static String mRecipient;
    private static String mReqParameters;

    enum reqDataType {
        OTHERS,
        URL,
        DATA,
        DEBUG
    }

    private static reqDataType mReqDataType = reqDataType.OTHERS;
    // 0 - URL; 1 - Data Only; 2 - Debug

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        String keyword, dataKeyword, debugKeyword;
        keyword = getPrefData(context, context.getString(R.string.pref_key_keyword), context.getString(R.string.pref_keyword_default)).toLowerCase();
        dataKeyword = '#' + keyword;
        debugKeyword = '*' + keyword;

        SmsMessage[] msgs = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage message : msgs) {
            String fromAddress = message.getOriginatingAddress();
            String msgBody = message.getMessageBody().toLowerCase();

            if (msgBody.equals(keyword) || msgBody.startsWith(keyword + ":"))
                mReqDataType = reqDataType.URL;
            else if (msgBody.equals(dataKeyword) || msgBody.startsWith(dataKeyword + ":"))
                mReqDataType = reqDataType.DATA;
            else if (msgBody.equals(debugKeyword) || msgBody.startsWith(debugKeyword + ":"))
                mReqDataType = reqDataType.DEBUG;
            else mReqDataType = reqDataType.OTHERS;

            if (mReqDataType != reqDataType.OTHERS) {
                abortBroadcast();
                String sAction = "";
                mReqParameters = "";
                switch (mReqDataType) {
                    case URL:
                        sAction = "URL - ";
                        mReqParameters = msgBody.substring(keyword.length());
                        break;
                    case DATA:
                        sAction = "DATA - ";
                        mReqParameters = msgBody.substring(dataKeyword.length());
                        break;
                    case DEBUG:
                        sAction = "DEBUG - ";
                        mReqParameters = msgBody.substring(debugKeyword.length());
                        break;
                }
                Toast.makeText(context, sAction + mReqParameters, Toast.LENGTH_SHORT).show();
                mLocationGps = null;
                mLocationNetwork = null;
                mLocationFused = null;
                this.prepareLocation(context, fromAddress);
            }
        }
    }

    private void getGpsLocation(Context context) {
        if (gps == null) gps = new GPSTracker();
        mLocationGps = gps.getLocationGps(context);
        // return (mLocationGps != null);
    }

    private void getNetworkLocation(Context context) {
        if (gps == null) gps = new GPSTracker();
        mLocationNetwork = gps.getLocationNetwork(context);
        // return (mLocationNetwork != null);
    }

    private void prepareLocation(Context context, String recipient) {

        getGpsLocation(context);

        getNetworkLocation(context);

        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }

        // Will it return null from GoogleApiClient builder?
        //noinspection ConstantConditions
        if (mGoogleApiClient == null) {
            Log.d(TAG, "GoogleApiClient not ready");
            sendLocation(context, recipient);
        } else {
            mContext = context;
            mRecipient = recipient;
            Log.d(TAG, "Wait for fusedLocation");
            mGoogleApiClient.connect();
        }
    }

    /*
            Result format:
                URL:        %s: %s: %s http://maps.google.com/maps?q=%f+%f  {A: %.2fm; S:%.2fm}
                DATA:       #location#%s#%s;%s;%f;%f;%f;%f#
                DEBUG:      #%s#%s;%s;%f;%f;%.2fm;%.2fm#
        */
    private void sendLocation(Context context, String recipient) {

        String msgContent = "";
        SmsManager sms = SmsManager.getDefault();


        String msgFormatter;
        String dataVersion = context.getString(R.string.app_data_version);
        switch (mReqDataType) {
            case URL:
                msgFormatter = ": %s: %s: http://maps.google.com/maps?q=%f+%f  {A: %.2fm; S:%.2fm}";
                msgContent = dataVersion + getLocationReturn(msgFormatter, true);
                break;
            case DATA:
                msgFormatter = "#%s;%s;%f;%f;%.2f;%.2f#";
                msgContent = "#location#" + dataVersion + getLocationReturn(msgFormatter, false);
                break;
            case DEBUG:
                msgFormatter = "#%s;%s;%f;%f;%.2f;%.2f#";
                msgContent = "#" + dataVersion + getLocationReturn(msgFormatter, false);
                break;
        }

        List<String> messages = sms.divideMessage(msgContent);

        for (String message : messages) {
            sms.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SMS_SENT), 0), null);
        }
    }

    private String getLocationReturn(String formatter, boolean singleOutput) {
        Boolean useDefault = true;
        String returnString = "";

        if (mReqParameters.contains(":g:")) {
            useDefault = false;
            returnString += getLocationString(formatter, mLocationGps);
            if (singleOutput) return returnString;
        }

        if (mReqParameters.contains(":n:")) {
            useDefault = false;
            returnString += getLocationString(formatter, mLocationNetwork);
            if (singleOutput) return returnString;
        }

        if (mReqParameters.contains(":f:")) {
            useDefault = false;
            returnString += getLocationString(formatter, mLocationFused);
            if (singleOutput) return returnString;
        }

        if (useDefault) {
            Location mLocationDefault = mLocationFused;
            // In very rare case, fused location is not available
            if (mLocationDefault == null) {
                if ((mLocationGps == null) || ((mLocationNetwork != null) && (mLocationNetwork.getTime() > mLocationGps.getTime()))) {
                    mLocationDefault = mLocationNetwork;
                } else {
                    mLocationDefault = mLocationGps;
                }
            }
            returnString += getLocationString(formatter, mLocationDefault);
        }
        return returnString;
    }

    private String getLocationString(String formatter, Location location) {
        if (location == null) return "";
        return String.format(formatter, location.getProvider(), format.format(new Date(location.getTime())), location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed());
    }


// --Commented out by Inspection START (29/12/2016 1:04 AM):
//    private void setPrefData(Context context, String key, String value) {
//        SharedPreferences sharedPref = context.getSharedPreferences(
//                context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putString(key, value);
//        editor.apply();
//    }
// --Commented out by Inspection STOP (29/12/2016 1:04 AM)

    private String getPrefData(Context context, String key, String defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");

        if (mContext != null) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mLocationFused = null;
            } else {
                mLocationFused = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }
        }
        resumeSendLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
        mLocationFused = null;
        resumeSendLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
        mLocationFused = null;
        resumeSendLocation();
    }

    private void resumeSendLocation() {
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        sendLocation(mContext, mRecipient);
    }
}
