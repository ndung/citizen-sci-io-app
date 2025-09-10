package io.sci.citizen;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import io.sci.citizen.adapter.SliderImageAdapter;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.RecordService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Image;
import io.sci.citizen.util.GsonDeserializer;
import retrofit2.Call;
import retrofit2.Callback;

public class DataMapActivity extends BaseActivity implements OnMapReadyCallback {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();
    private MapView mapView;
    private GoogleMap googleMap;
    protected RecordService recordService;
    private AppCompatSpinner appCompatSpinner;
    private ViewPager2 pager;
    private TabLayout dots;
    private SliderImageAdapter adapter;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

    protected TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_map);

        recordService = ApiUtils.RecordService(this);
        tv = findViewById(R.id.tv_title);
        tv.setText(getString(R.string.data_for_user));
        mapView = findViewById(R.id.mapView);
        appCompatSpinner = findViewById(R.id.sp_data);

        ArrayAdapter<String> data = new ArrayAdapter<>(
                this, R.layout.spinner_item,
                Arrays.asList(
                        getString(R.string.uploaded_record),
                        getString(R.string.verified_data),
                        getString(R.string.all_data)
                )
        );
        data.setDropDownViewResource(R.layout.spinner_item);
        appCompatSpinner.setAdapter(data);
        appCompatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // Google map is ready
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Data) {
                annotate((Data) tag);
                return true; // consume, we show our own dialog
            }
            return false;
        });

        // initial load
        refresh(appCompatSpinner != null ? appCompatSpinner.getSelectedItemPosition() : 0);
    }

    private void annotate(@NonNull Data data){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_data);

        pager = dialog.findViewById(R.id.vpSlider);
        dots  = dialog.findViewById(R.id.dots);

        adapter = new SliderImageAdapter(this);
        pager.setAdapter(adapter);

        new TabLayoutMediator(dots, pager, (tab, pos) -> tab.setText(
                String.valueOf(pos+1))).attach();

        if (data.getImages() != null) {
            for (Image image : data.getImages()) {
                adapter.add(Uri.parse(App.IMAGE_URL+image.getUuid()));
            }
        }
        TextView project = dialog.findViewById(R.id.tv_project);
        TextView notes = dialog.findViewById(R.id.tv_metadata);
        TextView submitted = dialog.findViewById(R.id.tv_uploaded);
        TextView uuid = dialog.findViewById(R.id.tv_uuid);
        Button button = dialog.findViewById(R.id.button);
        button.setOnClickListener(v -> dialog.dismiss());
        project.setText(data.getProject().getName());
        uuid.setText(data.getUuid());
        notes.setText(data.getDetails());
        if (data.getCreatedAt() != null) {
            submitted.setText(sdf.format(data.getCreatedAt()));
        }
        dialog.show();
    }

    protected Call<Response> callApi(Map<String,Integer> map){
        return recordService.listByUser(map);
    }

    public void refresh(Integer type){
        if (recordService == null) return;
        Map<String,Integer> map = new HashMap<>();
        map.put("type", type);
        callApi(map).enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                Response resp = response.body();
                JsonObject jsonObject = gson.toJsonTree(resp).getAsJsonObject();

                List<Data> list = gson.fromJson(jsonObject.getAsJsonArray("data"),
                        new TypeToken<List<Data>>(){}.getType());

                if (googleMap == null) return;

                googleMap.clear();

                if (list == null || list.isEmpty()) return;

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boolean hasPoint = false;
                Random rnd = new Random();

                // Some distinct marker hues
                float[] hues = new float[]{
                        BitmapDescriptorFactory.HUE_RED,
                        BitmapDescriptorFactory.HUE_ORANGE,
                        BitmapDescriptorFactory.HUE_YELLOW,
                        BitmapDescriptorFactory.HUE_GREEN,
                        BitmapDescriptorFactory.HUE_CYAN,
                        BitmapDescriptorFactory.HUE_AZURE,
                        BitmapDescriptorFactory.HUE_BLUE,
                        BitmapDescriptorFactory.HUE_VIOLET,
                        BitmapDescriptorFactory.HUE_ROSE,
                        BitmapDescriptorFactory.HUE_MAGENTA
                };

                for (Data d : list){
                    Double lat = d.getLatitude();
                    Double lng = d.getLongitude();
                    if (lat == null || lng == null) continue;

                    LatLng pos = new LatLng(lat, lng);
                    boundsBuilder.include(pos);
                    hasPoint = true;

                    MarkerOptions mo = new MarkerOptions()
                            .position(pos)
                            .title("") // leave empty; dialog shows details
                            .icon(BitmapDescriptorFactory.defaultMarker(hues[rnd.nextInt(hues.length)]));

                    Marker marker = googleMap.addMarker(mo);
                    if (marker != null) marker.setTag(d);
                }

                if (hasPoint) {
                    try {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                    } catch (IllegalStateException ignore) {
                        // Single point fallback
                        Data first = list.get(0);
                        if (first.getLatitude() != null && first.getLongitude() != null) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(first.getLatitude(), first.getLongitude()), 12f));
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                // Optionally log/Toast
            }
        });
    }

    // Forward MapView lifecycle to avoid leaks
    @Override protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
