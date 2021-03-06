package com.sta.dhbw.stauapp;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sta.dhbw.jambeaconrestclient.IHeartbeatCallback;
import com.sta.dhbw.jambeaconrestclient.JamBeaconRestClient;
import com.sta.dhbw.stauapp.dialogs.ConnectionIssueDialogFragment;
import com.sta.dhbw.stauapp.util.Utils;
import com.sta.dhbw.stauapp.util.Utils.ConnectionIssue;

/**
 * The Splash Screen will be called when the application has not been started previously or has been
 * stopped completely.<br>
 * The connectivity checks for GPS, network, and server will be executed here. If they fail, an appropriate error
 * will be displayed and the app will terminate.
 */
public class SplashScreen extends Activity implements IHeartbeatCallback
{
    private static final String TAG = SplashScreen.class.getSimpleName();

    private Intent intent;

    ProgressDialog progressDialog;

    @Override
    public void onCheckComplete(boolean success)
    {
        if (progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }

        if (success)
        {
            Log.d(TAG, "Starting Main Activity");
            startActivity(intent);
            finish();
        } else
        {
            Log.e(TAG, "Server Check failed on Splash Screen");
            DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.SERVER_NOT_AVAILABLE);
            fragment.show(getFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        intent = new Intent(this, MainActivity.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(true);

        //Check for DEBUG mode
        if (android.os.Debug.isDebuggerConnected())
        {
            progressDialog.setMessage("Welcome, developer...");
        } else
        {
            progressDialog.setMessage("Some work is done here...");
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        progressDialog.show();
        JamBeaconRestClient restClient = new JamBeaconRestClient();

        //Check GPS availability
        if (Utils.checkGps(this))
        {
            //Check internet connection
            if (!Utils.checkInternetConnection(this))
            {
                progressDialog.dismiss();
                DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.NETWORK_NOT_AVAILABLE);
                fragment.show(getFragmentManager(), "dialog");
            } else
            {
                restClient.checkServerAvailability(this);
            }
        } else
        {
            progressDialog.dismiss();
            DialogFragment fragment = ConnectionIssueDialogFragment.newInstance(ConnectionIssue.GPS_NOT_AVAILABLE);
            fragment.show(getFragmentManager(), "dialog");
        }
    }
}
