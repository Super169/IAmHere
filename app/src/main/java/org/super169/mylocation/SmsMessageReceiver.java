package org.super169.mylocation;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
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

import org.super169.mylocation.R;

/**
 * Created by James on 10/12/2014.
 */
public class SmsMessageReceiver extends BroadcastReceiver implements
        ConnectionCallbacks, OnConnectionFailedListener {
    /** Tag string for our debug logs */
    private static final String TAG = "SmsMessageReceiver";
    public static final String ACTION_SMS_SENT = "org.super169.mylocation.SMS_SENT_ACTION";

    private static GPSTracker gps;
    private static Location mLocationGps;
    private static Location mLocationNetwork;
    private static Location mLocationFused;

    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Provides the entry point to Google Play services.
     */
    private static GoogleApiClient mGoogleApiClient;

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

        Object[] pdus = (Object[]) extras.get("pdus");
        String keyword, dataKeyword, debugKeyword;
        keyword = getPrefData(context, context.getString(R.string.pref_key_keyword), context.getString(R.string.pref_keyword_default)).toLowerCase();
        dataKeyword = '#' + keyword;
        debugKeyword = '*' + keyword;

        SmsMessage[] msgs = msgs = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (int i = 0; i < msgs.length; i++) {
            SmsMessage message = msgs[i];
            String fromAddress = message.getOriginatingAddress();
            String msgBody = message.getMessageBody().toString().toLowerCase();

            if (msgBody.equals(keyword) || msgBody.startsWith(keyword+":")) mReqDataType = reqDataType.URL;
            else if (msgBody.equals(dataKeyword) || msgBody.startsWith(dataKeyword+":")) mReqDataType = reqDataType.DATA;
            else if (msgBody.equals(debugKeyword) || msgBody.startsWith(debugKeyword+":")) mReqDataType = reqDataType.DEBUG;
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

    private boolean getGpsLocation(Context context) {
        if (gps == null) gps = new GPSTracker(context);
        mLocationGps = gps.getLocationGps();
        return (mLocationGps != null);
    }

    private boolean getNetowrkLocation(Context context) {
        if (gps == null) gps = new GPSTracker(context);
        mLocationNetwork = gps.getLocationNetwork();
        return (mLocationNetwork != null);
    }

    private void prepareLocation(Context context, String recipient) {

        getGpsLocation(context);

        getNetowrkLocation(context);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (mGoogleApiClient == null) {
            Log.d(TAG, "GoogleApiClient not ready");
            sendLocation(context, recipient);
        }  else {
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


        String msgFormatter = "";
        String dataVersion = context.getString(R.string.app_data_version);
        Boolean useDefault = true;
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

    private String getLocationReturn(String formatter,  boolean singleOutput) {
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

        if (useDefault)  {
            Location mLocationDefault = mLocationFused;
            // In very rare case, fued locaiton is not available
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

    public String getLocationString(String formatter, Location location) {
        if (location == null) return "";
        return String.format(formatter, location.getProvider(), format.format(new Date(location.getTime())), location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getSpeed());
    }


    private void setPrefData(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getPrefData(Context context, String key, String defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");
        mLocationFused = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        resumeSendLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
        mLocationFused = null;
        resumeSendLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
        mLocationFused = null;
        resumeSendLocation();
    }

    private void resumeSendLocation() {
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        sendLocation(mContext, mRecipient);
    }
}
