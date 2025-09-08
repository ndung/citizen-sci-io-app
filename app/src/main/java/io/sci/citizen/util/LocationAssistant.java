package io.sci.citizen.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationAssistant {

    // ===== Listener/Enums (kept same) =======================================
    public interface Listener {
        void onNeedLocationPermission();
        void onExplainLocationPermission();
        void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onNeedLocationSettingsChange();
        void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onNewLocationAvailable(Location location);
        void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog);
        void onError(ErrorType type, String message);
    }

    public enum Accuracy { HIGH, MEDIUM, LOW, PASSIVE }
    public enum ErrorType { SETTINGS, RETRIEVAL }

    public static final int REQUEST_CHECK_SETTINGS = 0;
    public static final int REQUEST_LOCATION_PERMISSION = 1;

    // ===== Parameters =======================================================
    protected Context context;
    private Activity activity;
    private Listener listener;
    private int priority;                  // com.google.android.gms.location.Priority.*
    private long updateInterval;
    private boolean allowMockLocations;
    private boolean verbose;
    private boolean quiet;

    // ===== Internal state ===================================================
    private boolean permissionGranted;
    private boolean locationRequested;
    private boolean locationStatusOk;
    private boolean changeSettings;
    private boolean updatesRequested;
    protected Location bestLocation;

    // New client objects
    private FusedLocationProviderClient fused;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private ResolvableApiException resolvable;  // store for changeLocationSettings()

    // Availability cache (async)
    private volatile boolean lastAvailability;

    // Mock detection helpers
    private boolean mockLocationsEnabled;
    private Location lastMockLocation;
    private int numGoodReadings;

    // Misc
    private int numTimesPermissionDeclined;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ===== Construction =====================================================
    public LocationAssistant(final Context context, Listener listener, Accuracy accuracy, long updateInterval,
                             boolean allowMockLocations) {
        this.context = context;
        if (context instanceof Activity) this.activity = (Activity) context;
        this.listener = listener;
        switch (accuracy) {
            case HIGH:   priority = Priority.PRIORITY_HIGH_ACCURACY; break;
            case MEDIUM: priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY; break;
            case LOW:    priority = Priority.PRIORITY_LOW_POWER; break;
            case PASSIVE:
            default:     priority = Priority.PRIORITY_PASSIVE;
        }
        this.updateInterval = updateInterval;
        this.allowMockLocations = allowMockLocations;

        fused = LocationServices.getFusedLocationProviderClient(context);
        settingsClient = LocationServices.getSettingsClient(context);
    }

    // ===== Public configuration ============================================
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public void setQuiet(boolean quiet) { this.quiet = quiet; }

    // ===== Lifecycle entry points ==========================================
    public void start() {
        checkMockLocations();
        acquireLocation();
    }

    public void stop() {
        try {
            if (updatesRequested && locationCallback != null) {
                fused.removeLocationUpdates(locationCallback);
            }
        } catch (Exception ignore) {}
        permissionGranted = false;
        locationRequested  = false;
        locationStatusOk   = false;
        updatesRequested   = false;
    }

    public void register(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        checkInitialLocation();
        acquireLocation();
    }

    public void unregister() {
        this.activity = null;
        this.listener = null;
    }

    public void reset() {
        permissionGranted = false;
        locationRequested  = false;
        locationStatusOk   = false;
        updatesRequested   = false;
        acquireLocation();
    }

    public Location getBestLocation() { return bestLocation; }

    // ===== Permissions ======================================================
    public void requestAndPossiblyExplainLocationPermission() {
        if (permissionGranted) return;
        if (activity == null) {
            if (!quiet) Log.e(getClass().getSimpleName(), "Need location permission, but no activity is registered!");
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                && listener != null) {
            listener.onExplainLocationPermission();
        } else {
            requestLocationPermission();
        }
    }

    public void requestLocationPermission() {
        if (activity == null) {
            if (!quiet) Log.e(getClass().getSimpleName(), "Need location permission, but no activity is registered!");
            return;
        }
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    public boolean onPermissionsUpdated(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) return false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            acquireLocation();
            return true;
        } else {
            numTimesPermissionDeclined++;
            if (!quiet) Log.i(getClass().getSimpleName(), "Location permission request denied.");
            if (numTimesPermissionDeclined >= 1 && listener != null) {
                listener.onLocationPermissionPermanentlyDeclined(onGoToAppSettingsFromView, onGoToAppSettingsFromDialog);
            }
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != REQUEST_CHECK_SETTINGS) return;
        if (resultCode == Activity.RESULT_OK) {
            changeSettings = false;
            locationStatusOk = true;
            requestLocationUpdates(); // begin updates after user enabled
        }
        acquireLocation();
    }

    // ===== Core flow ========================================================
    private void acquireLocation() {
        if (!permissionGranted) checkLocationPermission();
        if (!permissionGranted) {
            if (numTimesPermissionDeclined >= 2) return;
            if (listener != null) listener.onNeedLocationPermission();
            else if (!quiet) Log.e(getClass().getSimpleName(), "Need location permission, but no listener is registered!");
            return;
        }
        if (!locationRequested) {
            requestLocation(); // async
            return;
        }
        if (!locationStatusOk) {
            if (changeSettings) {
                if (listener != null) listener.onNeedLocationSettingsChange();
                else if (!quiet) Log.e(getClass().getSimpleName(), "Need location settings change, but no listener is registered!");
            } else {
                checkProviders();
            }
            return;
        }
        if (!updatesRequested) {
            requestLocationUpdates();
            // Periodically refresh availability (async; no blocking)
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    refreshAvailability();
                    // Re-run acquire to follow the same decision tree
                    acquireLocation();
                }
            }, 10_000);
            return;
        }
        // If we get here, updates are running. Nothing to do.
    }

    private void requestLocation() {
        // Build request using new Builder API
        locationRequest = new LocationRequest.Builder(priority, updateInterval)
                .setMinUpdateIntervalMillis(updateInterval)
                .build();

        LocationSettingsRequest settingsRequest =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)
                        .build();

        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        locationRequested = true;
                        locationStatusOk  = true;
                        changeSettings    = false;
                        checkInitialLocation();
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(Exception e) {
                        locationRequested = true;
                        locationStatusOk  = false;
                        if (e instanceof ResolvableApiException) {
                            changeSettings = true;
                            resolvable = (ResolvableApiException) e;
                        } else {
                            if (listener != null) listener.onError(ErrorType.SETTINGS, e.getMessage());
                            if (!quiet) Log.e(getClass().getSimpleName(), "checkLocationSettings failed: " + e);
                            changeSettings = false;
                        }
                        acquireLocation();
                    }
                });
    }

    private void requestLocationUpdates() {
        if (updatesRequested || locationRequest == null) return;

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override public void onLocationResult(LocationResult result) {
                    if (result == null) return;
                    for (Location l : result.getLocations()) {
                        onLocationChanged(l);
                    }
                }
            };
        }

        try {
            fused.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    .addOnFailureListener(new OnFailureListener() {
                        @Override public void onFailure(Exception e) {
                            if (listener != null) listener.onError(ErrorType.RETRIEVAL, e.getMessage());
                            if (!quiet) Log.e(getClass().getSimpleName(), "requestLocationUpdates failed: " + e);
                        }
                    });
            updatesRequested = true;
        } catch (SecurityException e) {
            if (!quiet) Log.e(getClass().getSimpleName(), "SecurityException while requesting updates: " + e);
            if (listener != null) listener.onError(ErrorType.RETRIEVAL, e.getMessage());
        }
    }

    private void checkInitialLocation() {
        if (!permissionGranted) return;
        try {
            fused.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override public void onSuccess(Location location) {
                            if (location != null) onLocationChanged(location);
                        }
                    });
        } catch (SecurityException e) {
            if (!quiet) Log.e(getClass().getSimpleName(), "Error while requesting last location: " + e);
            if (listener != null) listener.onError(ErrorType.RETRIEVAL, "Could not retrieve initial location:\n" + e.getMessage());
        }
    }

    // Async availability probe; updates lastAvailability and can trigger provider fallback
    private void refreshAvailability() {
        if (!permissionGranted) return;
        try {
            fused.getLocationAvailability()
                    .addOnSuccessListener(new OnSuccessListener<LocationAvailability>() {
                        @Override public void onSuccess(LocationAvailability la) {
                            lastAvailability = (la != null && la.isLocationAvailable());
                            if (!lastAvailability) checkProviders();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override public void onFailure(Exception e) {
                            if (!quiet) Log.e(getClass().getSimpleName(), "getLocationAvailability failed: " + e);
                            if (listener != null) listener.onError(ErrorType.RETRIEVAL, e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            if (!quiet) Log.e(getClass().getSimpleName(), "SecurityException while checking availability: " + e);
            if (listener != null) listener.onError(ErrorType.RETRIEVAL, e.getMessage());
        }
    }

    // For callers that used the old synchronous method. Returns last known (async) value.
    public boolean checkLocationAvailability() {
        return lastAvailability;
    }

    // ===== Settings resolution =============================================
    public void changeLocationSettings() {
        if (resolvable == null) return;
        if (activity == null) {
            if (!quiet) Log.e(getClass().getSimpleName(), "Need to resolve settings, but no activity is registered!");
            return;
        }
        try {
            resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            if (!quiet) Log.e(getClass().getSimpleName(), "Error resolving settings: " + e);
            if (listener != null) listener.onError(ErrorType.SETTINGS, "Could not resolve location settings:\n" + e.getMessage());
            changeSettings = false;
            acquireLocation();
        }
    }

    // ===== Providers + mock handling =======================================
    private void checkProviders() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = false, network = false;
        try {
            gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {}
        if (gps || network) return;
        if (listener != null) listener.onFallBackToSystemSettings(onGoToLocationSettingsFromView, onGoToLocationSettingsFromDialog);
        else if (!quiet) Log.e(getClass().getSimpleName(), "Providers disabled and no listener registered!");
    }

    private void checkMockLocations() {
        if (Build.VERSION.SDK_INT < 18 &&
                !"0".equals(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION))) {
            mockLocationsEnabled = true;
            if (listener != null) listener.onMockLocationsDetected(onGoToDevSettingsFromView, onGoToDevSettingsFromDialog);
        } else {
            mockLocationsEnabled = false;
        }
    }

    private boolean isLocationPlausible(Location location) {
        if (location == null) return false;
        boolean isMock = mockLocationsEnabled || (Build.VERSION.SDK_INT >= 18 && location.isFromMockProvider());
        if (isMock) {
            lastMockLocation = location;
            numGoodReadings = 0;
        } else {
            numGoodReadings = Math.min(numGoodReadings + 1, 1_000_000);
        }
        if (numGoodReadings >= 20) lastMockLocation = null;
        if (lastMockLocation == null) return true;
        double d = location.distanceTo(lastMockLocation);
        return (d > 1000);
    }

    public void onLocationChanged(Location location) {
        if (location == null) return;
        boolean plausible = isLocationPlausible(location);
        if (verbose && !quiet) Log.i(getClass().getSimpleName(), location + (plausible ? " -> plausible" : " -> not plausible"));
        if (!allowMockLocations && !plausible) {
            if (listener != null) listener.onMockLocationsDetected(onGoToDevSettingsFromView, onGoToDevSettingsFromDialog);
            return;
        }
        bestLocation = location;
        if (listener != null) listener.onNewLocationAvailable(location);
        else if (!quiet) Log.w(getClass().getSimpleName(), "New location available, but no listener registered!");
    }

    private boolean checkLocationPermission() {
        permissionGranted = Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return permissionGranted;
    }

    // ===== Intents for settings screens ====================================
    private final DialogInterface.OnClickListener onGoToLocationSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
    private final View.OnClickListener onGoToLocationSettingsFromView = new View.OnClickListener() {
        @Override public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
    private final DialogInterface.OnClickListener onGoToDevSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
    private final View.OnClickListener onGoToDevSettingsFromView = new View.OnClickListener() {
        @Override public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
    private final DialogInterface.OnClickListener onGoToAppSettingsFromDialog = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
    private final View.OnClickListener onGoToAppSettingsFromView = new View.OnClickListener() {
        @Override public void onClick(View v) {
            if (activity != null) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            } else if (!quiet) Log.e(getClass().getSimpleName(), "No activity registered!");
        }
    };
}
