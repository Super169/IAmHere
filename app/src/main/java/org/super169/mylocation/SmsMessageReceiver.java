package org.super169.mylocation;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.super169.mylocation.R;

/**
 * Created by James on 10/12/2014.
 */
public class SmsMessageReceiver extends BroadcastReceiver {
    /** Tag string for our debug logs */
    private static final String TAG = "SmsMessageReceiver";
    public static final String ACTION_SMS_SENT = "org.super169.mylocation.SMS_SENT_ACTION";

    private static GPSTracker gps;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        Object[] pdus = (Object[]) extras.get("pdus");
        String keyword, data_keyword;
        keyword = getPrefData(context, context.getString(R.string.pref_key_keyword), context.getString(R.string.pref_keyword_default)).toLowerCase();
        data_keyword = '#' + keyword;

        for (int i = 0; i < pdus.length; i++) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
            String fromAddress = message.getOriginatingAddress();
            String msgBody = message.getMessageBody().toString().toLowerCase();

            if (msgBody.equals(keyword) || msgBody.equals(data_keyword)) {
                abortBroadcast();
                Toast.makeText(context, "Get Location: " + keyword, Toast.LENGTH_SHORT).show();
                this.sendLocation(context, fromAddress, (msgBody.equals(data_keyword)));
            }
        }
    }

    private void sendLocation(Context context, String recipient, boolean data_only) {
        String msgContent = "";
        SmsManager sms = SmsManager.getDefault();

        if (gps == null) gps = new GPSTracker(context);

        if (gps.canGetLocation()) {
            gps.getLocation(false);
            float accuracy;
            double latitude, longitude, speed;
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(gps.getTime());
            String sTime = format.format(date);
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            speed = gps.getSpeed();
            // altitude = gps.getAltitude();
            accuracy = gps.getAccuracy();
            String msgFormatter;
            String dataVersion = context.getString(R.string.app_data_version);
            if (data_only) {
                // msgFormatter = context.getString(R.string.sms_location_data_formatter);
                msgFormatter = "#location#%s#%s;%s;%f;%f;%f;%f#";
                /*
                                        msgContent = '#' + gps.getProvider() + ';' +
                                                     sTime + ';' +
                                                     String.format("%f", latitude) + ';' +
                                                     String.format("%f", longitude) + ';' +
                                                     String.format("%f", accuracy) + ';' +
                                                     String.format("%f", speed) + '#';
                                */
                // msgContent = String.format(msgFormatter, gps.getProvider(), sTime, latitude, longitude, accuracy, speed);

            } else {
                // msgFormatter = context.getString(R.string.sms_location_url_formatter);
                msgFormatter = "%s: %s: %s http://maps.google.com/maps?q=%f+%f  {A: %.2fm; S:%.2fm}";

                /*
                                    msgContent = gps.getProvider() + ": ";
                                    msgContent += sTime + " http://maps.google.com/maps?q=";
                                    msgContent +=  String.format("%f", latitude) + "+" + String.format("%f", longitude);
                                    msgContent += "  {A: " +  String.format("%.2f", accuracy) + "m; S:" + String.format("%.2f",speed) + "m}";
                                */
            }
            msgContent = String.format(msgFormatter, dataVersion, gps.getProvider(), sTime, latitude, longitude, accuracy, speed);
        }

        List<String> messages = sms.divideMessage(msgContent);

        for (String message : messages) {
            sms.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SMS_SENT), 0), null);
        }

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

}
