package com.vrinda.rider;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;

    private Realm realm;
    private RideInfo rideInfo;
    private boolean isServiceStarted = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("Started Service");
        realm = getRealm();
        rideInfo = realm.where(RideInfo.class).findFirst();
    }

    @Override
    public void onDestroy() {
        log("Service Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceStarted)
            startTracking();
        isServiceStarted = true;
        return START_STICKY;
    }

    //Location listner for update location
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            log("position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());
            realm.beginTransaction();

            if (location.getSpeed() > 0 && !rideInfo.isActive()) {
                rideInfo.setActive(true);
                rideInfo.setStartTime(System.currentTimeMillis());
            }

            if (rideInfo.isActive()) {
                if (location.getSpeed() == 0) {
                    if (!rideInfo.isIdle())
                        rideInfo.setIdleStartTime(System.currentTimeMillis());
                    rideInfo.setIdle(true);
                } else
                    rideInfo.setIdle(false);

                if (rideInfo.isIdle() && (System.currentTimeMillis() - rideInfo.getIdleStartTime()) > 10 * 1000) {
                    rideInfo.init();
                } else {
                    rideInfo.setSpeed(location.getSpeed());

                    if (rideInfo.getMaxSpeed() < rideInfo.getSpeed())
                        rideInfo.setMaxSpeed(rideInfo.getSpeed());

                    if (rideInfo.getCurLocationLang() != 0) {
                        Location prevLocation = new Location("");
                        prevLocation.setLatitude(rideInfo.getCurLocationLat());
                        prevLocation.setLongitude(rideInfo.getCurLocationLang());
                        rideInfo.setDistance(rideInfo.getDistance() + prevLocation.distanceTo(location));
                        rideInfo.setAvgSpeed(rideInfo.getDistance() / (System.currentTimeMillis() - rideInfo.getStartTime()));
                    }

                    rideInfo.setCurLocationLat(location.getLatitude());
                    rideInfo.setCurLocationLang(location.getLongitude());
                }
            }
            realm.commitTransaction();
        }
    }

    private void startTracking() {
        log("startTracking");
        //Checking location service with google location Api
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        } else {
            log("unable to connect to google play services.");
            stopSelf();
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        log("onConnected");

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // milliseconds
        locationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        log("LocationSettingsStatusCodes.SUCCESS");
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        try {
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    googleApiClient, locationRequest, LocationService.this);
                        } catch (SecurityException e) {
                            stopLocationUpdates();
                            stopSelf();
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        log("LocationSettingsStatusCodes.RESOLUTION_REQUIRED");
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        stopLocationUpdates();
                        stopSelf();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        log("LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE");
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        stopLocationUpdates();
                        stopSelf();
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log("onConnectionFailed");
        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onConnectionSuspended(int i) {
        log("GoogleApiClient connection has been suspend");
        stopLocationUpdates();
        stopSelf();
    }

    private void stopLocationUpdates() {
        log("stopLocationUpdates");
        if (googleApiClient != null) {
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (googleApiClient.isConnected())
                googleApiClient.disconnect();
        }
    }

    private void log(String log) {
        Log.d("Rider.LocationService", log);
    }

    public Realm getRealm() {
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
}
