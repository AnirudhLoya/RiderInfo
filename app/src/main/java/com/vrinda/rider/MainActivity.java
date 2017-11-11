package com.vrinda.rider;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION = 111;
    private static final int LOCATION_WAKEUP_TIME = 60000;
    private Realm realm;
    private RideInfo rideInfo;
    private TextView speed, maxSpeed, avgSpeed, rideDistance, rideTime;
    private Handler refreshHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speed = (TextView) findViewById(R.id.speed);
        maxSpeed = (TextView) findViewById(R.id.max_speed);
        avgSpeed = (TextView) findViewById(R.id.avg_speed);
        rideDistance = (TextView) findViewById(R.id.ride_distance);
        rideTime = (TextView) findViewById(R.id.ride_time);

        //Realm initialization
        realm = getRealm();

        rideInfo = realm.where(RideInfo.class).findFirst();
        if (rideInfo == null) {
            realm.beginTransaction();
            rideInfo = realm.copyToRealm(new RideInfo());
            realm.commitTransaction();
        } else
            setLayoutData(rideInfo);

        rideInfo.addChangeListener(new RealmChangeListener<RealmModel>() {
            @Override
            public void onChange(RealmModel element) {
                setLayoutData(rideInfo);
            }
        });

        // Get Location Manager and check for GPS & Network location services
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION);
        } else {
            // Show location settings when the user acknowledges
            startService(new Intent(MainActivity.this, LocationService.class));
            startWakeupAlarm();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRefreshHandler();
    }

    @Override
    public void onResume() {
        super.onResume();
        startRefreshHandler();
    }

    private void startRefreshHandler() {
        if (refreshHandler != null)
            refreshHandler.removeCallbacksAndMessages(null);

        refreshHandler = new Handler();

        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshHandler.postDelayed(this, 1000);
                setLayoutData(rideInfo);
            }
        }, 1000);

        setLayoutData(rideInfo);
    }

    private void stopRefreshHandler() {
        if (refreshHandler != null)
            refreshHandler.removeCallbacksAndMessages(null);
    }

    private void setLayoutData(RideInfo rideInfo) {
        if (rideInfo == null)
            return;

        speed.setText(MPS_TO_KMPH(rideInfo.getSpeed()));
        maxSpeed.setText(MPS_TO_KMPH(rideInfo.getMaxSpeed()));
        avgSpeed.setText(MPMS_TO_KMPH(rideInfo.getAvgSpeed()));
        rideDistance.setText(MTR_TO_KM(rideInfo.getDistance()));
        if (rideInfo.isActive())
            rideTime.setText(MS_TO_SEC(System.currentTimeMillis() - rideInfo.getStartTime()));
        else
            rideTime.setText(MS_TO_SEC(0));

        if (rideInfo.isActive() && rideInfo.isIdle() && (System.currentTimeMillis() - rideInfo.getIdleStartTime()) > 10 * 1000) {
            realm.beginTransaction();
            rideInfo.init();
            realm.commitTransaction();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService(new Intent(MainActivity.this, LocationService.class));
                    startWakeupAlarm();
                }
            }
        }
    }

    private void startWakeupAlarm() {
        Context context = getBaseContext();

        Intent intent = new Intent(context, LocationBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Repeat After 60000 seconds
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                LOCATION_WAKEUP_TIME,
                pendingIntent);
    }

    private Realm getRealm() {
        if (realm == null) {
            Realm.init(this);
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .name(Realm.DEFAULT_REALM_NAME)
                    .schemaVersion(0)
                    .deleteRealmIfMigrationNeeded()
                    .build();
            Realm.setDefaultConfiguration(realmConfiguration);
            realm = Realm.getInstance(realmConfiguration);
        }
        return realm;
    }

    //Coverting metre per sec to kilometre per hour
    private String MPS_TO_KMPH(double mps) {
        return String.format(Locale.getDefault(), "%.2f %s", mps * 3.6, "KMPH");
    }

    //Converting metre to kilimetre
    private String MTR_TO_KM(double meters) {
        return String.format(Locale.getDefault(), "%.2f %s", meters * 0.001, "KM");
    }

    //Converting micro metre per sec to kilometre per hour
    private String MPMS_TO_KMPH(double mpms) {
        return String.format(Locale.getDefault(), "%.2f %s", mpms * 3600, "KMPH");
    }

    //Converting millisec to sec
    private String MS_TO_SEC(long millis) {
        return String.format(Locale.getDefault(), "%s %s", millis / 1000, " SECONDS");
    }
}
