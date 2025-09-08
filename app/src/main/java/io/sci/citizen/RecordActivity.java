package io.sci.citizen;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Section;
import io.sci.citizen.model.SurveyQuestion;
import io.sci.citizen.step.LocationStep;
import io.sci.citizen.step.ImageStep;
import io.sci.citizen.step.SurveySectionStep;

public class RecordActivity extends BaseActivity implements StepperLayout.StepperListener{

    private static final String TAG = RecordActivity.class.toString();
    public static Long SECTION = 0L;
    private Map<Long, ImageStep> map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<Section> sections = App.instance().memory().project().getSections();
        Collections.sort(sections, (o1, o2) -> Integer.compare(o1.getSequence(), o2.getSequence()));
        setContentView(R.layout.layout_data);
        TextView tv = findViewById(R.id.tv_title);
        tv.setText(getString(R.string.contribute_data_for_project)+App.instance().memory().project().getName());
        StepperLayout mStepperLayout = findViewById(R.id.stepperLayout);
        mStepperLayout.setAdapter(new AbstractFragmentStepAdapter(getSupportFragmentManager(), this) {
            @Override
            public int getCount() {
                return sections.size();
            }

            @Override
            public Step createStep(int position) {
                Section section = sections.get(position);
                if (section.getType().equals("image")){
                    ImageStep step = new ImageStep(section.getId());
                    map.put(section.getId(),step);
                    return step;
                }else if (section.getType().equals("survey")){
                    List<SurveyQuestion> questions = section.getQuestions();
                    return new SurveySectionStep(questions);
                }else if (section.getType().equals("location")){
                    return new LocationStep();
                }

                return null;
            }

            @NonNull
            @Override
            public StepViewModel getViewModel(@IntRange(from = 0) int position) {
                //Override this method to set Step title for the Tabs, not necessary for other stepper types
                Section section = sections.get(position);
                if (position == 0) {
                    return new StepViewModel.Builder(context)
                            .setBackButtonLabel(getResources().getString(R.string.cancel))
                            .setEndButtonLabel(getResources().getString(R.string.next))
                            .setTitle(section.getName()).create();
                }else if (position==sections.size()-1){
                    return new StepViewModel.Builder(context)
                            .setBackButtonLabel(getResources().getString(R.string.back))
                            .setEndButtonLabel(getResources().getString(R.string.finish))
                            .setTitle(section.getName()).create();
                }else{
                    return new StepViewModel.Builder(context)
                            .setBackButtonLabel(getResources().getString(R.string.back))
                            .setEndButtonLabel(getResources().getString(R.string.next))
                            .setTitle(section.getName()).create();
                }

            }

        });
        mStepperLayout.setListener(this);

        if (!checkPermissions()) {
            requestPermissions();
        }else{
            App.instance().startService();
        }
    }

    /**@Override
    protected void onStart() {
        super.onStart();
        bindService(uploadServiceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        //uploadService.stopForeground(true);
        //uploadService.stopSelf();
    }*/

    @Override
    public void onCompleted(View view) {
        final Data data = App.instance().memory().getData();
        data.setUploaded(false);
        App.instance().memory().setData(data);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_submit);
        Button upload = dialog.findViewById(R.id.uploadButton);
        upload.setOnClickListener(view1 -> {
            dialog.dismiss();
            upload(App.instance().memory().saveData());
        });
        Button save = dialog.findViewById(R.id.saveButton);
        save.setOnClickListener(view12 -> {
            App.instance().memory().saveData();
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    private void upload(final Data data){
        data.setUploading(true);
        App.instance().memory().putData(data.getUuid(), data);
        Intent intent = new Intent(this, UploadService.class);
        intent.putExtra("io", data.getUuid());
        ContextCompat.startForegroundService(this, intent);
        finish();
    }

    @Override
    public void onError(VerificationError verificationError) {

    }

    @Override
    public void onStepSelected(int i) {

    }

    @Override
    public void onReturn() {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        map.get(SECTION).onActivityResult(requestCode, resultCode, data);

    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
