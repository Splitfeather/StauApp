package com.sta.dhbw.stauapp;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sta.dhbw.jambeaconrestclient.JamBeaconRestClient;
import com.sta.dhbw.jambeaconrestclient.TrafficJam;

public final class JamToServerService extends IntentService
{
    private static final String TAG = JamToServerService.class.getSimpleName();

    private static JamBeaconRestClient client;

    public JamToServerService()
    {
        super("JamToServerService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Log.i(TAG, "Sending traffic jam to server");

        if (null == client)
        {
            synchronized (JamToServerService.class)
            {
                if (null == client)
                {
                    client = new JamBeaconRestClient(
                            this.getSharedPreferences(JamToServerService.class.getSimpleName(), Context.MODE_PRIVATE));
                }
            }
        }

        Bundle extras = intent.getExtras();

        if (!extras.isEmpty())
        {
            TrafficJam jam = extras.getParcelable("jam");

        }


    }
}
