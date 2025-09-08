package io.sci.citizen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import io.sci.citizen.util.LocationAssistant;

public abstract class BaseActivity extends AppCompatActivity {

    private static Context context;
    private AlertDialog materialDialogPleaseWait;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        uploadServiceIntent = new Intent(context, UploadService.class);
        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        ((TextView) view.findViewById(R.id.tvMessage)).setText("Please wait…");

        materialDialogPleaseWait = new AlertDialog.Builder(this) // use 'this' in Activity
                .setView(view)
                .setCancelable(false)
                .create();
        materialDialogPleaseWait.setCanceledOnTouchOutside(false);
    }

    public void showMessage(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public void showSnackbar(String message){
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    public void showActivity(Context packageContext, Class<?> cls) {
        Intent intent = getIntent(packageContext, cls);
        showActivity(intent);
    }

    public Intent getIntent(Context packageContext, Class<?> cls) {
        return new Intent(packageContext, cls);
    }

    public void showActivity(Intent intent) {
        startActivity(intent);
    }

    public void showActivityAndFinishCurrentActivity(Context packageContext, Class<?> cls) {
        Intent intent = getIntent(packageContext, cls);
        showActivity(intent);
        finish();
    }

    public void showPleaseWaitDialog(){
        materialDialogPleaseWait.show();

    }

    public void dissmissPleaseWaitDialog(){
        if(materialDialogPleaseWait != null){
            materialDialogPleaseWait.dismiss();
        }
    }

    private static final int TIME_DELAY = 2000;
    private static boolean doubleBackToExitPressedOnce = false;

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (doubleBackToExitPressedOnce) {
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        //Toast.makeText(this, getResources().getString(R.string.press_back_once_again_to_exit_from_the_app), Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, TIME_DELAY);

    }


    /**
     * Return the current state of the permissions needed.
     */
    protected boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    //private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 14;

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                LocationAssistant.REQUEST_LOCATION_PERMISSION);
    }


    protected void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            startLocationPermissionRequest();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationAssistant.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                App.instance().stopService();
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                App.instance().startService();
                int[] results = new int[]{grantResults[0]};
                App.instance().getLocationAssistant().onPermissionsUpdated(requestCode, results);
            } else {
                // Permission denied.
                App.instance().stopService();
                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }


    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(this.findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    protected UploadService uploadService;
    protected Intent uploadServiceIntent;

    protected ServiceConnection connection = new ServiceConnection()
    {
        @SuppressLint("ForegroundServiceType")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // The binder of the service that returns the instance that is created.
            UploadService.LocalBinder binder = (UploadService.LocalBinder) service;
            // The getter method to acquire the service.
            uploadService = binder.getService();

            ContextCompat.startForegroundService(context, uploadServiceIntent);

            //int notificationId = Sequence.nextValue();
            Notification notification = new NotificationCompat.Builder(context, App.NOTIF_CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .build();
            // This is the key: Without waiting Android Framework to call this method
            // inside Service.onCreate(), immediately call here to post the notification.
            uploadService.startForeground(2147483647, notification);
            // Release the connection to prevent leaks.
        }

        @Override
        public void onBindingDied(ComponentName name)
        {
        }

        @Override
        public void onNullBinding(ComponentName name)
        {
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
        }
    };
}