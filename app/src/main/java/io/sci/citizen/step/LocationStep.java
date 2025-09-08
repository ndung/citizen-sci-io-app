package io.sci.citizen.step;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.SupportMapFragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import java.util.Date;
import java.util.UUID;

import io.sci.citizen.App;
import io.sci.citizen.R;
import io.sci.citizen.fragment.BaseFragment;
import io.sci.citizen.model.Data;

public class LocationStep extends BaseFragment implements Step, OnMapReadyCallback {

    private static final String TAG = LocationStep.class.toString();

    private TextView txtAccuracy;
    private Button setBtn, resetBtn;
    private EditText etLat;
    private EditText etLng;
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private Data data;

    BroadcastReceiver mLocationBroadcastReceiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.step_recording_location, container, false);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        txtAccuracy = view.findViewById(R.id.tv_accuracy);
        etLat = view.findViewById(R.id.et_lat);
        etLng = view.findViewById(R.id.et_lng);
        setBtn = view.findViewById(R.id.btn_set);
        resetBtn = view.findViewById(R.id.btn_reset);
        resetBtn.setOnClickListener(v -> {
            etLat.setText("");
            etLng.setText("");
            data.setEditMode(false);
            data.setAccuracy(null);
            data.setLatitude(null);
            data.setLongitude(null);
            App.instance().memory().setData(data);
        });

        setBtn.setOnClickListener(v -> {
            if (etLat.getText().equals("")){
                etLat.setError(getString(R.string.latitude_cant_be_empty));
                return;
            }
            Double lat = 0d;
            try {
                lat = Double.parseDouble(etLat.getText().toString());
            }catch (Exception ex){
                etLat.setError(getString(R.string.latitude_must_be_a_floating_value));
                return;
            }
            if (lat==0d){
                etLat.setError(getString(R.string.latitude_cant_be_0));
                return;
            }
            if (etLng.getText().equals("")){
                etLng.setError(getString(R.string.longitude_cant_be_empty));
                return;
            }
            Double lng = 0d;
            try {
                lng = Double.parseDouble(etLng.getText().toString());
            }catch (Exception ex){
                etLng.setError(getString(R.string.longitude_must_be_a_floating_value));
                return;
            }
            if (lng==0d){
                etLat.setError(getString(R.string.longitude_cant_be_0));
                return;
            }
            if (data != null && !raceCondition) {
                data.setAccuracy(0d);
                data.setLatitude(lat);
                data.setLongitude(lng);
                data.setEditMode(true);
                App.instance().memory().setData(data);
                viewMap();
            }
        });

        mLocationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(App.LOCATION_NOTIFICATION)) {
                    setLocation();
                }
            }
        };
        return view;
    }

    public void setLocation() {
        if (data != null && (!data.isEditMode() || data.getLatitude() == null || data.getLongitude() == null || data.getAccuracy() == null ||
                (data.getLatitude() == 0d && data.getLongitude() == 0d && data.getAccuracy() == 0d))) {
            String accuracy = App.instance().memory().getString(getActivity(), App.ACCURACY);
            String latitude = App.instance().memory().getString(getActivity(), App.LATITUDE);
            String longitude = App.instance().memory().getString(getActivity(), App.LONGITUDE);
            if (accuracy != null && !accuracy.isEmpty()
                    && latitude != null && !latitude.isEmpty()
                    && longitude != null && !longitude.isEmpty()
                    && !raceCondition) {
                data.setAccuracy(Double.parseDouble(accuracy));
                data.setLatitude(Double.parseDouble(latitude));
                data.setLongitude(Double.parseDouble(longitude));
                App.instance().memory().setData(data);
                viewMap();
            }
        } else if (data != null && data.isEditMode()){
            viewMap();
        }

    }

    private void viewMap() {
        if (data != null && this.googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(data.getLatitude(), data.getLongitude())).draggable(true));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(data.getLatitude(), data.getLongitude()), 15));
            txtAccuracy.setText(getResources().getString(R.string.accuracy) + data.getAccuracy());
        }
    }


    @Nullable
    @Override
    public VerificationError verifyStep() {
        if ((data.getLatitude() != null && data.getLongitude() != null) &&
                (data.getLatitude() == 0 && data.getLongitude() == 0)) {
            return new VerificationError(getResources().getString(R.string.your_location_is_not_precise));
        }
        return null;
    }

    @Override
    public void onSelected() {
        data = App.instance().memory().getData();
        if (data == null){
            data = new Data();
            data.setProjectId(App.instance().memory().project().getId());
            data.setUuid(Settings.Secure.getString(getContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID)+"_"+ UUID.randomUUID());
            data.setStartDate(new Date());
        }
        setLocation();
    }

    private boolean raceCondition = false;

    @Override
    public void onError(@NonNull VerificationError verificationError) {
        showSnackbar(verificationError.getErrorMessage());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.googleMap.setMyLocationEnabled(true);
        }
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);

        this.googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.getPosition() != null && marker.getPosition().latitude != 0 && marker.getPosition().longitude != 0) {
                    raceCondition = true;
                    data.setLatitude(marker.getPosition().latitude);
                    data.setLongitude(marker.getPosition().longitude);
                    App.instance().memory().setData(data);
                }
            }
        });

        viewMap();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mLocationBroadcastReceiver, new IntentFilter(App.LOCATION_NOTIFICATION));
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mLocationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
