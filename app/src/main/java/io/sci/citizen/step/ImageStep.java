package io.sci.citizen.step;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import io.sci.citizen.App;
import io.sci.citizen.R;
import io.sci.citizen.RecordActivity;
import io.sci.citizen.adapter.SliderImageAdapter;
import io.sci.citizen.fragment.BaseFragment;
import io.sci.citizen.model.Data;
import io.sci.citizen.util.UriUtils;

public class ImageStep extends BaseFragment implements BlockingStep {

    public static final String TAG = ImageStep.class.toString();
    private Data data;
    private List<String> imagePaths;
    private Long sectionId;

    public ImageStep(Long sectionId){
        this.sectionId = sectionId;
    }

    private Uri currentPhotoUri;

    // --- Launchers ---
    private ActivityResultLauncher<PickVisualMediaRequest> pickGallery;
    private ActivityResultLauncher<Uri> takePicture;
    private ActivityResultLauncher<String> requestCameraPermission;

    private ViewPager2 pager;
    private TabLayout dots;
    private SliderImageAdapter adapter;

    // Auto-slide (optional)
    private final long AUTO_MS = 3000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoSlide = new Runnable() {
        @Override public void run() {
            if (adapter.getItemCount() <= 1) return;
            int next = (pager.getCurrentItem() + 1) % adapter.getItemCount();
            pager.setCurrentItem(next, true);
            handler.postDelayed(this, AUTO_MS);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.step_image, container, false);

        pager = v.findViewById(R.id.vpSlider);
        dots  = v.findViewById(R.id.dots);

        adapter = new SliderImageAdapter(getActivity());
        pager.setAdapter(adapter);

        // Connect dots to pager
        new TabLayoutMediator(dots, pager, (tab, pos) -> tab.setText(
                String.valueOf(pos+1))).attach();

        Button capture = v.findViewById(R.id.btn_capture);
        capture.setOnClickListener(this::onClick);

        Button delete = v.findViewById(R.id.btn_delete);
        delete.setOnClickListener(view -> {
            if (!imagePaths.isEmpty()) {
                int count = adapter.getItemCount();
                if (count == 0) return;

                int pos = pager.getCurrentItem();
                adapter.removeAt(pos);

                int newCount = adapter.getItemCount();
                if (newCount == 0) {
                    Toast.makeText(getActivity(), R.string.no_images_left, Toast.LENGTH_SHORT).show();
                }

                // Keep pager in bounds after removal
                int newPos = Math.min(pos, newCount - 1);
                pager.setCurrentItem(newPos, false);

                Map<Long,List<String>> map = data.getImagePaths();
                if (map==null){
                    map = new TreeMap<>();
                }

                imagePaths.remove(pos);
                map.put(sectionId,imagePaths);
                data.setImagePaths(map);
                App.instance().memory().setData(data);
            }
            else{
                adapter.removeAll();
            }
        });

        // ---- Photo Picker (gallery) ----
        pickGallery = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(5),   // allow up to 5
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        populateImages(uris);
                    }
                });

        // ---- Camera capture (full-resolution to Uri) ----
        takePicture = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        populateImages(List.of(currentPhotoUri));
                    }
                });

        // ---- CAMERA permission (only if you declared it) ----
        requestCameraPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openCamera();
                    else {
                        Toast.makeText(getActivity(), getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                });


        return v;
    }

    private void populateImages(List<Uri> list) {
        if (list!=null && !list.isEmpty()){
            if (imagePaths==null){
                imagePaths = new ArrayList<>();
            }
            for (Uri uri : list) {
                try {
                    File saved = UriUtils.copyToCache(getActivity(), uri);
                    Uri localUri = FileProvider.getUriForFile(getActivity(),
                            getActivity().getPackageName() + ".fileprovider", saved);
                    imagePaths.add(localUri.toString());
                    adapter.add(localUri);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            Map<Long,List<String>> map = this.data.getImagePaths();
            if (map==null){
                map = new TreeMap<>();
            }
            map.put(sectionId,imagePaths);
            this.data.setImagePaths(map);
            App.instance().memory().setData(this.data);
        }
    }

    @Override
    public VerificationError verifyStep() {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new VerificationError(getResources().getString(R.string.please_select_image_first));
        }
        return null;
    }

    @Override
    public void onSelected() {
        data = App.instance().memory().getData();
        if (data ==null){
            data = new Data();
            data.setProjectId(App.instance().memory().project().getId());
            data.setUuid(Settings.Secure.getString(getContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID)+"_"+ UUID.randomUUID());
            data.setStartDate(new Date());
        }
        if (data.getImagePaths()!=null && data.getImagePaths().get(sectionId)!=null) {
            imagePaths = data.getImagePaths().get(sectionId);
            if (imagePaths != null) {
                adapter.removeAll();
                for (String path : imagePaths) {
                    adapter.add(Uri.parse(path));
                }
            }
        }
    }

    @Override
    public void onError(@NonNull VerificationError error) {
        showSnackbar(error.getErrorMessage());
    }

    private void showSourceChooser() {
        RecordActivity.SECTION = sectionId;
        final String[] items = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(getActivity())
                .setTitle("Select source")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) { // Camera
                        // If you added CAMERA permission in manifest, request at runtime if needed.
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestCameraPermission.launch(Manifest.permission.CAMERA);
                        } else {
                            openCamera();
                        }
                    } else { // Gallery (Photo Picker)
                        PickVisualMediaRequest req = new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build();
                        pickGallery.launch(req);
                    }
                })
                .show();
    }

    private void openCamera() {
        currentPhotoUri = createImageUri(); // where the camera will write
        if (currentPhotoUri == null) {
            Toast.makeText(getActivity(), "Unable to create image location", Toast.LENGTH_SHORT).show();
            return;
        }
        takePicture.launch(currentPhotoUri);
    }

    /** Creates a writable Uri for the camera (MediaStore on API 29+, FileProvider below). */
    @Nullable
    private Uri createImageUri() {
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/citizen-sci-io"); // shown in Photos/Gallery
            try {
                return getActivity().getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                Log.e("ImagePicker", "MediaStore insert failed", e);
                return null;
            }
        } else {
            // Older devices: use app-specific external dir + FileProvider
            File imagesDir = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YourApp");
            if (!imagesDir.exists() && !imagesDir.mkdirs()) return null;
            File image = new File(imagesDir, fileName);
            try {
                return FileProvider.getUriForFile(
                        getActivity(), getActivity().getPackageName() + ".fileprovider", image);
            } catch (IllegalArgumentException e) {
                Log.e("ImagePicker", "FileProvider uri failed", e);
                return null;
            }
        }
    }

    @Override
    public void onNextClicked(StepperLayout.OnNextClickedCallback onNextClickedCallback) {
        onNextClickedCallback.goToNextStep();
    }

    @Override
    public void onCompleteClicked(StepperLayout.OnCompleteClickedCallback onCompleteClickedCallback) {
        onCompleteClickedCallback.complete();
    }

    @Override
    public void onBackClicked(StepperLayout.OnBackClickedCallback onBackClickedCallback) {
        onBackClickedCallback.goToPrevStep();
    }

    private void onClick(View view) {
        showSourceChooser();
    }
}
