package com.vrinda.rider;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class LocationBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Rider.LocationService", "WakefulLocationBroadcastReceiver");
        context.startService(new Intent(context, LocationService.class));
    }
}
