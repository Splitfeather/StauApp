package com.sta.dhbw.stauapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.sta.dhbw.jambeaconrestclient.IHeartbeatCallback;
import com.sta.dhbw.jambeaconrestclient.JamBeaconRestClient;
import com.sta.dhbw.stauapp.dialogs.ConnectionIssueDialogFragment;
import com.sta.dhbw.stauapp.gcm.RequestGcmTokenService;
import com.sta.dhbw.stauapp.services.BeaconService;
import com.sta.dhbw.stauapp.settings.PrefFields;
import com.sta.dhbw.stauapp.settings.SettingsActivity;
import com.sta.dhbw.stauapp.util.Utils;
import com.sta.dhbw.stauapp.util.Utils.ConnectionIssue;

/**
 * The main activity of this application. From here, the user is able to view the list of jams, get to the
 * settings, and start the BeaconService.
 */
public class MainActivity extends Activity implements IHeartbeatCallback
{
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int BEACON_NOTIFICATION = 1337;

    private static final String TAG = MainActivity.class.getSimpleName();

    public static JamBeaconRestClient restClient;
    RestIssueBroadcastReceiver restIssueBroadcastReceiver = null;
    boolean receiverIsRegistered = false;

    private static BeaconBroadcastReceiver beaconBroadcastReceiver = null;


    Button jamListButton, beaconButton, developerButton;
    GoogleCloudMessaging gcm;
    NotificationManager notificationManager;

    static boolean beaconStarted = false;

    Context context;

    String regId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!receiverIsRegistered)
        {
            restIssueBroadcastReceiver = new RestIssueBroadcastReceiver();
            registerReceiver(restIssueBroadcastReceiver, new IntentFilter(RestIssueBroadcastReceiver.REST_EVENT));
            receiverIsRegistered = true;
        }

        if (beaconBroadcastReceiver == null)
        {
            beaconBroadcastReceiver = new BeaconBroadcastReceiver();
        }

        Log.d(TAG, "Registered RestIssueBroadcastReceiver");

        restClient = new JamBeaconRestClient();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        jamListButton = (Button) findViewById(R.id.view_traffic_issues);
        beaconButton = (Button) findViewById(R.id.start_beacon);
        developerButton = (Button) findViewById(R.id.start_developer_activity);

        if (beaconStarted)
        {
            beaconButton.setText(getString(R.string.stop_beacon_btn));
        } else
        {
            beaconButton.setText(getString(R.string.start_beacon_btn));
        }

        context = getApplicationContext();

        developerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(v.getContext(), DeveloperActivity.class));
            }
        });

        jamListButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(), JamListActivity.class);
                startActivity(intent);
            }
        });

        beaconButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!beaconStarted)
                {
                    MainActivity.this.startBeacon();
                } else
                {
                    MainActivity.this.stopBeacon();
                }
            }
        });

        //Check for Play Services APK. Proceed with GCM registration, if successful
        if (checkPlayServices())
        {
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId(context);

            if (regId.isEmpty())
            {
                Log.i(TAG, "Registering for GCM Services");
                registerInBackground();
            }
        } else
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!receiverIsRegistered)
        {
            restIssueBroadcastReceiver = new RestIssueBroadcastReceiver();
            registerReceiver(restIssueBroadcastReceiver, new IntentFilter(RestIssueBroadcastReceiver.REST_EVENT));
            receiverIsRegistered = true;
            Log.d(TAG, "Registered RestIssueBroadcastReceiver");
        }
        if (Utils.checkGps(context))
        {
            //Check internet connection
            if (!Utils.checkInternetConnection(context))
            {
                DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.NETWORK_NOT_AVAILABLE);
                fragment.show(getFragmentManager(), "dialog");
            } else
            {
                //Check if server is available
                restClient.checkServerAvailability(this);
            }
        } else
        {
            DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.GPS_NOT_AVAILABLE);
            fragment.show(getFragmentManager(), "dialog");
        }

        checkPlayServices();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (receiverIsRegistered)
        {
            unregisterReceiver(restIssueBroadcastReceiver);
            receiverIsRegistered = false;
            Log.d(TAG, "Unregistered RestIssueBroadcastReceiver");
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (receiverIsRegistered)
        {
            unregisterReceiver(restIssueBroadcastReceiver);
            receiverIsRegistered = false;
            Log.d(TAG, "Unregistered RestIssueBroadcastReceiver");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Checks if a valid Google Play Services APK is available
     *
     * @return TRUE if APK is available, FALSE otherwise
     */
    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else
            {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.i(TAG, "Google Play Services available");
        return true;
    }


    /**
     * Get the current GCM registration ID
     * <p/>
     * If the returned String is empty, the device needs to register.
     *
     * @param context The application context
     * @return The registration ID, if it exists. If not, an empty String is returned.
     */
    private String getRegistrationId(Context context)
    {
        final SharedPreferences prefs = getSharedPreferences();
        String registrationId = prefs.getString(PrefFields.PROPERTY_REG_ID, "");
        if (registrationId == null || registrationId.isEmpty())
        {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        //Check for app updates
        int registeredVersion = prefs.getInt(PrefFields.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion)
        {
            Log.i(TAG, "App version changed");
            return "";
        }
        return registrationId;
    }

    /**
     * @return The application's {@code SharedPreferences}
     */
    private SharedPreferences getSharedPreferences()
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context)
    {
        try
        {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e)
        {
            //should never happen. If so, your programming is bad, and you should feel bad.
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Starts the RequestGcmTokenService to retrieve a new GCM Token in the background.
     */
    private void registerInBackground()
    {
        Log.i(TAG, "Getting new Registration Id");

        SharedPreferences sharedPreferences = getSharedPreferences();

        Intent intent = new Intent(this, RequestGcmTokenService.class);
        startService(intent);

        sharedPreferences.edit().putInt(PrefFields.PROPERTY_APP_VERSION, getAppVersion(context)).apply();
    }

    /**
     * Starts the BeaconService and registers a {@code BroadcastReceiver} to receive messages from the service.
     */
    private void startBeacon()
    {
        SharedPreferences sharedPreferences = getSharedPreferences();
        int delay = Integer.parseInt(sharedPreferences.getString("beacon_delay", "3"));
        Toast.makeText(this, "Beacon aktiviert. Messung startet in " + delay + " Minuten", Toast.LENGTH_LONG).show();
        Intent beaconServiceIntent = new Intent(context, BeaconService.class);
        registerReceiver(beaconBroadcastReceiver, new IntentFilter(BeaconBroadcastReceiver.BEACON_STARTET));
        startService(beaconServiceIntent);
        beaconButton.setText(R.string.stop_beacon_btn);
        beaconStarted = true;
    }

    /**
     * Stops the BeaconService and unregisters the BeaconBroadcastReceiver.
     */
    private void stopBeacon()
    {
        Toast.makeText(this, "Beacon deaktiviert", Toast.LENGTH_SHORT).show();
        stopService(new Intent(context, BeaconService.class));
        unregisterReceiver(beaconBroadcastReceiver);
        beaconButton.setText(R.string.start_beacon_btn);
        beaconStarted = false;
        dismissBeaconNotification();
    }

    /**
     * Displays a notification in the Status Bar to inform the user that traffic jam detection has started.
     */
    private void showBeaconNotification()
    {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.dh_notification)
                .setContentTitle(getString(R.string.jambeacon_activated))
                .setContentText(getString(R.string.jam_detection_running))
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0))
                .setVibrate(new long[]{0, 1000}); //Vibrate for 1 second
        notificationManager.notify(BEACON_NOTIFICATION, builder.build());
    }

    /**
     * Dismisses the Status Bar notification when the BeaconService stops.
     */
    private void dismissBeaconNotification()
    {
        notificationManager.cancel(BEACON_NOTIFICATION);
    }

    /**
     * Custom {@code BroadcastReceiver} to receive broadcasts from the BeaconService.
     */
    public class BeaconBroadcastReceiver extends BroadcastReceiver
    {
        public static final String BEACON_STARTET = "com.sta.dhbw.stauapp.BEACON_STARTET";
        private final String TAG = BeaconBroadcastReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent)
        {
            showBeaconNotification();
        }
    }

    /**
     * Custom {@code BroadcastReceiver} to receive broadcasts in case a REST call failed or succeeded.
     */
    public class RestIssueBroadcastReceiver extends BroadcastReceiver
    {
        public static final String REST_EVENT = "com.sta.dhbw.stauapp.REST_EVENT";

        private final String TAG = RestIssueBroadcastReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean success = intent.getBooleanExtra("success", false);
            String reason = intent.getStringExtra("reason");

            if (success)
            {
                Toast.makeText(context, reason, Toast.LENGTH_LONG).show();
            } else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Error");
                builder.setMessage(reason);
                builder.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }

            Log.d(TAG, "Received REST Event");

        }
    }

    @Override
    public void onCheckComplete(boolean success)
    {
        if (!success)
        {
            DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.SERVER_NOT_AVAILABLE);
            fragment.show(getFragmentManager(), "dialog");
        }
    }
}

