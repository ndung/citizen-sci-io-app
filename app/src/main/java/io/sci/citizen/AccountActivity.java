package io.sci.citizen;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.RecordService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.DataSummary;
import io.sci.citizen.model.User;
import io.sci.citizen.util.GsonDeserializer;
import retrofit2.Call;
import retrofit2.Callback;

public class AccountActivity extends BaseActivity {

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvUsername;
    private TextView tvRecords;
    private TextView tvVersion;
    private CircleImageView imageView;
    private User user;
    private RecordService recordService;
    private LinearLayout changeProfile, changePassword, submitted;

    Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_account);
        tvUsername = findViewById(R.id.tv_username);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvRecords = findViewById(R.id.tv_records);
        tvVersion = findViewById(R.id.tv_version);
        //imageView = findViewById(R.id.iv_profile);
        changeProfile = findViewById(R.id.ll_change_profile);
        changePassword = findViewById(R.id.ll_change_password);
        submitted = findViewById(R.id.ll_records);
        //FirebaseAuth auth = FirebaseAuth.getInstance();
        user = App.instance().memory().getUser(this);
        tvVersion.setText(BuildConfig.VERSION_NAME);
        //Picasso.with(getActivity()).load(auth.getCurrentUser().getPhotoUrl()).into(imageView);

        recordService = createRecordService();
        changeProfile.setOnClickListener(v -> startActivity(new Intent(this, ChangeProfileActivity.class)));
        changePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        submitted.setOnClickListener(v -> startDataMapActivity());
        refresh();

    }

    RecordService createRecordService() {
        return ApiUtils.RecordService(this);
    }

    private void startDataMapActivity(){
        Intent intent = new Intent(this, DataMapActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refresh();
    }

    private void refresh(){
        tvUsername.setText(user.getUsername());
        tvName.setText(user.getFullName());
        tvEmail.setText(user.getEmail());
        recordService.summary().enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.isSuccessful()) {
                    Response body = response.body();
                    Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();
                    JsonObject jsonObject = gson.toJsonTree(body.getData()).getAsJsonObject();

                    DataSummary summary = gson.fromJson(jsonObject, DataSummary.class);
                    if (summary != null) {
                        tvRecords.setText(summary.getUploaded() + "/" + summary.getVerified());
                    } else {
                        showSnackbar(body.getMessage());
                    }
                } else if (response.errorBody() != null) {
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                        showSnackbar(jObjError.getString("message"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (response.body() != null && response.body().getMessage() != null) {
                    showSnackbar(response.body().getMessage());
                } else {
                    showSnackbar("Error!");
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                showSnackbar("Error!");
            }
        });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
