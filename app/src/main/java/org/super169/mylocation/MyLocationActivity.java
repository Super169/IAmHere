package org.super169.mylocation;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MyLocationActivity extends Activity {
    /** Tag string for our debug logs */
    private static final String TAG = "MyLocationActivity";

    public static final String SMS_RECIPIENT_EXTRA = "org.super169.mylocation.SMS_RECIPIENT";
    public static final String ACTION_SMS_SENT = "org.super169.mylocation.SMS_SENT_ACTION";

    private static BroadcastReceiver mReceiver;
    static GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_location);

        gps = new GPSTracker(MyLocationActivity.this);

        if (getIntent().hasExtra(SMS_RECIPIENT_EXTRA)) {
            ((TextView) findViewById(R.id.sms_recipient)).setText(getIntent().getExtras()
                    .getString(SMS_RECIPIENT_EXTRA));
            ((TextView) findViewById(R.id.sms_content)).requestFocus();
        }

        final TextView appVersion = (TextView) MyLocationActivity.this.findViewById(R.id.app_version);
        appVersion.setText(getString(R.string.app_name) + " (" + BuildConfig.VERSION_NAME + ")");


        // Enable or disable the broadcast receiver depending on the checked
        // state of the checkbox.
        CheckBox enableCheckBox = (CheckBox) findViewById(R.id.sms_enable_receiver);

        final PackageManager pm = this.getPackageManager();
        final ComponentName componentName = new ComponentName("org.super169.mylocation",
                "org.super169.mylocation.SmsMessageReceiver");

        enableCheckBox.setChecked(pm.getComponentEnabledSetting(componentName) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        enableCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, (isChecked ? "Enabling" : "Disabling") + " SMS receiver");

                pm.setComponentEnabledSetting(componentName,
                        isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        });

        final EditText keywordTextEdit = (EditText) MyLocationActivity.this
                .findViewById(R.id.et_keyword);
        String keyword = getPrefData(MyLocationActivity.this,getString(R.string.pref_key_keyword) );
        if (keyword.equals("")) {
            keyword =  getString(R.string.pref_keyword_default);
            setPrefData(MyLocationActivity.this, getString(R.string.pref_key_keyword),  keyword);
        }
        keywordTextEdit.setText(getPrefData(MyLocationActivity.this,getString(R.string.pref_key_keyword) ));

        Button updateKeywordButton = (Button) findViewById(R.id.btn_update_keyword);
        updateKeywordButton.setOnClickListener( new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       setPrefData(MyLocationActivity.this, getString(R.string.pref_key_keyword),  keywordTextEdit.getText().toString());
                       Toast.makeText(MyLocationActivity.this, "Keyword updated", Toast.LENGTH_SHORT).show();
                   }
        });

        Button restoreKeywordButton = (Button) findViewById(R.id.btn_restore_keyword);
        restoreKeywordButton.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        keywordTextEdit.setText(getPrefData(MyLocationActivity.this,getString(R.string.pref_key_keyword) ));
                        Toast.makeText(MyLocationActivity.this, "Restore OK", Toast.LENGTH_SHORT).show();
                    }
        });

        final EditText recipientTextEdit = (EditText) MyLocationActivity.this
                .findViewById(R.id.sms_recipient);
        final EditText contentTextEdit = (EditText) MyLocationActivity.this
                .findViewById(R.id.sms_content);
        final TextView statusView = (TextView) MyLocationActivity.this.findViewById(R.id.sms_status);

        // Watch for send button clicks and send text messages.
        Button sendButton = (Button) findViewById(R.id.sms_send_message);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (TextUtils.isEmpty(recipientTextEdit.getText())) {
                    Toast.makeText(MyLocationActivity.this, getString(R.string.msg_missing_recipient),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(contentTextEdit.getText())) {
                    Toast.makeText(MyLocationActivity.this, getString(R.string.msg_missing_message),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                recipientTextEdit.setEnabled(false);
                contentTextEdit.setEnabled(false);

                SmsManager sms = SmsManager.getDefault();

                List<String> messages = sms.divideMessage(contentTextEdit.getText().toString());

                String recipient = recipientTextEdit.getText().toString();
                for (String message : messages) {
                    sms.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(
                            MyLocationActivity.this, 0, new Intent(ACTION_SMS_SENT), 0), null);
                }
            }
        });

        Button sendLocationButton = (Button) findViewById(R.id.sms_send_location);
        sendLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (TextUtils.isEmpty(recipientTextEdit.getText())) {
                    Toast.makeText(MyLocationActivity.this, getString(R.string.msg_missing_recipient),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                recipientTextEdit.setEnabled(false);
                contentTextEdit.setEnabled(false);

                SmsManager sms = SmsManager.getDefault();

                String recipient = recipientTextEdit.getText().toString();

                String msgContent = "GPS not available";

                if (gps.canGetLocation()) {
                    gps.getLocation(false);
                    float accuracy;
                    double latitude, longitude, speed;
                    msgContent = gps.getProvider() + ": ";
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(gps.getTime());
                    String sTime = format.format(date);
                    msgContent += sTime + " http://maps.google.com/maps?q=";
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                    speed = gps.getSpeed();
                    // altitude = gps.getAltitude();
                    accuracy = gps.getAccuracy();
                    msgContent +=  String.format("%f", latitude) + "+" + String.format("%f", longitude);
                    msgContent += "  {A: " +  String.format("%.2f", accuracy) + "m; S:" + String.format("%.2f",speed) + "m}";
                }

                List<String> messages = sms.divideMessage(msgContent);

                for (String message : messages) {
                    sms.sendTextMessage(recipient, null, message, PendingIntent.getBroadcast(
                            MyLocationActivity.this, 0, new Intent(ACTION_SMS_SENT), 0), null);
                }
            }
        });



        // Register broadcast receivers for SMS sent and delivered intents
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = null;
                boolean error = true;
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        message = getString(R.string.msg_sms_sent);
                        error = false;
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        message = getString(R.string.msg_sms_sent_err_generic);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        message = getString(R.string.msg_sms_sent_err_no_service);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        message = getString(R.string.msg_sms_sent_err_null_pdu);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        message = getString(R.string.msg_sms_sent_err_radio_off);
                        break;
                }

                recipientTextEdit.setEnabled(true);
                contentTextEdit.setEnabled(true);
                contentTextEdit.setText("");

                statusView.setText(message);
                statusView.setTextColor(error ? Color.RED : Color.GREEN);
            }
        };
    }

    @Override
    public  void onResume() {
        // Register broadcast receivers for SMS sent and delivered intents
        registerReceiver(mReceiver, new IntentFilter(ACTION_SMS_SENT));
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    private void setPrefData(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getPrefData(Context context, String key) {
        return getPrefData(context, key, "");
    }

    private String getPrefData(Context context, String key, String defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }
}
