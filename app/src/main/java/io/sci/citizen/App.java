package io.sci.citizen;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import io.sci.citizen.util.LocationAssistant;
import io.sci.citizen.util.Memory;

public class App extends Application implements LocationAssistant.Listener {

    private static final String TAG = App.class.getSimpleName();
    public static final String API_URL = "http://192.168.1.11/api/";
    public static final String IMAGE_URL = "https://citizen-sci-io-c296af702ec9.herokuapp.com/files/";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String SPEED = "SPEED";
    public static final String ACCURACY = "ACCURACY";
    public static final String LOCATION_NOTIFICATION = "LOCATION_NOTIFICATION";
    public static final String UPLOAD_NOTIFICATION = "UPLOAD_NOTIFICATION";

    private static App mInstance;

    public void onCreate() {
        super.onCreate();

        mInstance = this;

        createNotificationChannel();
    }

    public static final String CHANNEL_NAME = "Foreground Service Channel";
    public static final String NOTIF_CHANNEL_ID = "Channel_Id";

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static synchronized App instance() {
        return mInstance;
    }

    private Memory memory;

    public Memory memory() {
        if (memory == null) {
            memory = new Memory(this);
        }

        return memory;
    }

    private String platformId;

    public String getPlatformId(){
        if (platformId==null){
            platformId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return platformId;
    }


    protected String getAppVersion() {
        int verCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            verCode = pInfo.versionCode;
        } catch (Exception ex) {
        }
        return String.valueOf(verCode);
    }

    private LocationAssistant assistant;
    private Location location;

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startService() {
        if (assistant==null) {
            assistant = new LocationAssistant(this, this, LocationAssistant.Accuracy.LOW, 5000, false);
        }
        assistant.start();
    }

    public void stopService() {

        if (assistant != null) {
            assistant.stop();
        }
    }

    @Override
    public void onNeedLocationPermission() {
        //assistant.requestAndPossiblyExplainLocationPermission();
    }

    @Override
    public void onExplainLocationPermission() {
        /**new AlertDialog.Builder(this)
         .setMessage(R.string.permission_rationale)
         .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        assistant.requestLocationPermission();
        }
        })
         .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        }
        })
         .show();*/
    }

    @Override
    public void onLocationPermissionPermanentlyDeclined(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        /**Intent intent = new Intent();
         intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
         Uri uri = Uri.fromParts("package", this.getPackageName(), null);
         intent.setData(uri);
         this.startActivity(intent);
         Toast.makeText(getApplicationContext(), "Please enable permission. Exit the system...", Toast.LENGTH_LONG).show();
         System.exit(0);*/
    }

    @Override
    public void onNeedLocationSettingsChange() {
        //assistant.changeLocationSettings();
    }

    @Override
    public void onFallBackToSystemSettings(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        /**Toast.makeText(getApplicationContext(), "Please enable location. Exit the system...", Toast.LENGTH_LONG).show();
         Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
         this.startActivity(intent);
         System.exit(0);*/
    }

    @Override
    public void onNewLocationAvailable(Location location) {
        App.instance().memory().putString(this, App.ACCURACY, String.valueOf(location.getAccuracy()));
        App.instance().memory().putString(this, App.SPEED, String.valueOf(location.getSpeed()));
        App.instance().memory().putString(this, App.LATITUDE, String.valueOf(location.getLatitude()));
        App.instance().memory().putString(this, App.LONGITUDE, String.valueOf(location.getLongitude()));

        Intent locationNotification = new Intent(App.LOCATION_NOTIFICATION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(locationNotification);
        Double distance = null;
        if (this.location != null && location != null) {
            distance = distance(this.location.getLatitude(), location.getLatitude(), this.location.getLongitude(), location.getLongitude(), 0d, 0d);
        }
        this.location = location;
    }


    public static Double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    @Override
    public void onMockLocationsDetected(View.OnClickListener fromView, DialogInterface.OnClickListener fromDialog) {
        Toast.makeText(getApplicationContext(), "Location is from mock provider. Exit the system...", Toast.LENGTH_LONG).show();
        System.exit(0);
    }

    @Override
    public void onError(LocationAssistant.ErrorType type, String message) {
        Toast.makeText(getApplicationContext(), "Location error! Type : " + type + ", Message : " + message, Toast.LENGTH_LONG).show();
    }

    public LocationAssistant getLocationAssistant() {
        return assistant;
    }

    public void setAssistant(LocationAssistant assistant) {
        this.assistant = assistant;
    }

    @Override
    public void onTerminate() {
        stopService();
        super.onTerminate();
    }

}
