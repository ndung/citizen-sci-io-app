package io.sci.citizen.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

import io.sci.citizen.App;
import io.sci.citizen.R;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.RecordService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.SurveyResponse;
import io.sci.citizen.model.User;
import io.sci.citizen.util.GsonDeserializer;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MapFragment extends BaseFragment implements OnMapReadyCallback {

    private static final String TAG = MapFragment.class.toString();

    private MapView mapView;
    private GoogleMap googleMap;

    private RecordService recordService;
    private User user;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonDeserializer())
            .create();

    private AppCompatSpinner appCompatSpinner;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.layout_map, container, false);

        user = App.instance().memory().getUser(getActivity());
        recordService = ApiUtils.RecordService(getActivity());

        mapView = rootView.findViewById(R.id.mapView);
        appCompatSpinner = rootView.findViewById(R.id.sp_data);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.spinner_item,
                Arrays.asList(
                        "Rekaman data yang saya kirim",
                        "Rekaman data saya yang terverifikasi",
                        "Semua data"
                )
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        appCompatSpinner.setAdapter(spinnerAdapter);

        appCompatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return rootView;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Data) {
                showDataDialog((Data) tag);
                return true; // consume the event
            }
            return false;
        });

        if (appCompatSpinner != null) {
            refresh(appCompatSpinner.getSelectedItemPosition());
        }
    }

    private void showDataDialog(@NonNull Data data) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_data);

        /**SliderLayout slider = dialog.findViewById(R.id.slider);
        slider.setPresetTransformer(SliderLayout.Transformer.Default);
        slider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        slider.setDuration(4000);
        slider.setVisibility(View.VISIBLE);

        if (data.getImages() != null) {
            for (Image image : data.getImages()) {
                DefaultSliderView sliderView = new DefaultSliderView(getActivity());
                sliderView.image(App.IMAGE_URL + image.getId())
                        .setScaleType(BaseSliderView.ScaleType.CenterCrop);
                sliderView.bundle(new Bundle());
                sliderView.getBundle().putString("extra", image.getSection().getName());
                sliderView.setOnSliderClickListener(sl ->
                        Toast.makeText(getActivity(),
                                String.valueOf(sl.getBundle().get("extra")),
                                Toast.LENGTH_SHORT).show());
                slider.addSlider(sliderView);
            }
        }*/

        TextView tvMetadata = dialog.findViewById(R.id.tv_metadata);
        TextView tvSubmitted = dialog.findViewById(R.id.tv_uploaded);
        TextView tvUuid = dialog.findViewById(R.id.tv_uuid);
        Button button = dialog.findViewById(R.id.button);
        button.setOnClickListener(v -> dialog.dismiss());

        StringBuilder sb = new StringBuilder();
        if (data.getSurveyResponses() != null) {
            for (SurveyResponse s : data.getSurveyResponses()) {
                sb.append(s.getQuestion().getAttribute())
                        .append(": ")
                        .append(s.getResponse())
                        .append("\n");
            }
        }
        tvMetadata.setText(sb.toString());
        tvUuid.setText(data.getUuid());
        if (data.getCreatedAt() != null) {
            tvSubmitted.setText(sdf.format(data.getCreatedAt()));
        }

        dialog.show();
    }

    public void refresh(Integer type) {
        if (recordService == null) return;
        Map<String,Integer> map = new HashMap<>();
        map.put("type", type);
        recordService.listByUser(map).enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                Response resp = response.body();
                JsonObject jsonObject = gson.toJsonTree(resp).getAsJsonObject();

                List<Data> list = gson.fromJson(
                        jsonObject.getAsJsonArray("data"),
                        new TypeToken<List<Data>>() {}.getType()
                );

                if (googleMap == null) return;

                // Clear previous map content (markers, etc.)
                googleMap.clear();

                if (list == null || list.isEmpty()) return;

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boolean hasValidPoint = false;

                for (Data d : list) {
                    Double lat = d.getLatitude();
                    Double lng = d.getLongitude();
                    if (lat == null || lng == null) continue;

                    LatLng position = new LatLng(lat, lng);
                    hasValidPoint = true;
                    boundsBuilder.include(position);

                    MarkerOptions mo = new MarkerOptions()
                            .position(position)
                            // Title/Snippet appear in InfoWindow if you ever return false in onMarkerClick
                            .title("") // keep empty; dialog handles details
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                    Marker marker = googleMap.addMarker(mo);
                    if (marker != null) marker.setTag(d);
                }

                if (hasValidPoint) {
                    try {
                        LatLngBounds bounds = boundsBuilder.build();
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
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
                // optionally log/Toast
            }
        });
    }

    // MapView lifecycle forwarding (recommended for Google MapView)
    @Override public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override public void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
