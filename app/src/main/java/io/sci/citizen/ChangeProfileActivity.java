package io.sci.citizen;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.AuthService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.User;
import io.sci.citizen.util.GsonDeserializer;
import io.sci.citizen.util.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;

public class ChangeProfileActivity extends BaseActivity {

    private TextView tvTitle;
    private TextInputLayout etName, etEmail, etAge, etAddress, etZipCode, etJob, etAffiliation;
    private AppCompatSpinner spGender, spEducation, spDistance;
    private LinearLayout tncLayout, pwdLayout, usernameLayout;
    private AuthService authService;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_signup);
        tvTitle = findViewById(R.id.tv_title);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        /**
        etAge = findViewById(R.id.et_age);
        spGender = findViewById(R.id.sp_gender);
        spEducation = findViewById(R.id.sp_education);
        spDistance = findViewById(R.id.sp_distance);
        etDob = view.findViewById(R.id.et_bod);
        etAddress = findViewById(R.id.et_address);
        etZipCode = findViewById(R.id.et_zip_code);
        etJob = findViewById(R.id.et_job);
        etAffiliation = findViewById(R.id.et_affiliation);
         */
        tncLayout = findViewById(R.id.layout_tnc);
        tncLayout.setVisibility(View.GONE);
        pwdLayout = findViewById(R.id.layout_pwd);
        pwdLayout.setVisibility(View.GONE);
        usernameLayout = findViewById(R.id.layout_username);
        usernameLayout.setVisibility(View.GONE);

        user = App.instance().memory().getUser(this);
        etEmail.getEditText().setText(user.getEmail());
        etName.getEditText().setText(user.getFullName());

        /**
        ArrayAdapter<String> gender = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList("Laki-laki", "Perempuan"));
        gender.setDropDownViewResource(R.layout.spinner_item);
        spGender.setAdapter(gender);

        ArrayAdapter<String> education = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList("Dibawah SMA", "SMA", "S1", "S2", "S3"));
        education.setDropDownViewResource(R.layout.spinner_item);
        spEducation.setAdapter(education);

        ArrayAdapter<String> distance = new ArrayAdapter<>(this, R.layout.spinner_item, Arrays.asList("kurang dari 1 kilometer", "antara 1 kilometer sampai 10 kilometer", "diatas 10 kilometer"));
        distance.setDropDownViewResource(R.layout.spinner_item);
        spDistance.setAdapter(distance);
        etAddress.getEditText().setText(user.getAddress());
        etAge.getEditText().setText(String.valueOf(user.getAge()));
        selectSpinnerItemByValue(spGender, user.getGender());
        selectSpinnerItemByValue(spEducation, user.getEducation());
        selectSpinnerItemByValue(spDistance, user.getDistance());
        etAffiliation.getEditText().setText(user.getInstitution());
        etZipCode.getEditText().setText(user.getPostalCode());
        etJob.getEditText().setText(user.getProfession());
         */

        authService = ApiUtils.AuthService(this);
        tvTitle.setText(R.string.update_profile);
        Button button = findViewById(R.id.bt_change);
        button.setVisibility(View.VISIBLE);
        button.setText(getString(R.string.update));
        button.setOnClickListener(v -> changeProfile());
    }

    public static void selectSpinnerItemByValue(AppCompatSpinner spnr, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spnr.getAdapter();
        for (int position = 0; position < adapter.getCount(); position++) {
            if(((String)adapter.getItem(position)).equalsIgnoreCase(value)) {
                spnr.setSelection(position);
                return;
            }
        }
    }

    private void changeProfile(){
        String name = etName.getEditText().getText().toString();
        if (name.isEmpty()){
            etName.setError(getString(R.string.name_cant_be_empty));
            return;
        }
        etName.setError("");
        String email = etEmail.getEditText().getText().toString();
        if (email.isEmpty()){
            etEmail.setError(getString(R.string.email_cant_be_empty));
            return;
        }
        if (!StringUtils.isEmailValid(email)){
            etEmail.setError(getString(R.string.email_must_be_valid));
            return;
        }
        etEmail.setError("");
        /**String age = etAge.getEditText().getText().toString();
        if (age.isEmpty()){
            etAge.setError(getString(R.string.age_cant_be_empty));
            return;
        }
        try{
            int num = Integer.parseInt(age);
            if (num<0 || num>99){
                etAge.setError(getString(R.string.age_must_be_bigger_than_0_and_smaller_than_99));
                return;
            }
        } catch (NumberFormatException e) {
            etAge.setError(getString(R.string.age_must_be_valid));
            return;
        }
        etAge.setError("");
        String address = etAddress.getEditText().getText().toString();
        if (address.isEmpty()){
            etAddress.setError(getString(R.string.address_cant_be_empty));
            return;
        }
        etAddress.setError("");
        String zipCode = etZipCode.getEditText().getText().toString();
        if (zipCode.isEmpty()){
            etZipCode.setError(getString(R.string.zip_code_cant_be_empty));
            return;
        }
        if (zipCode.length()!=5){
            etZipCode.setError(getString(R.string.zip_code_length_must_be_5));
            return;
        }
        etZipCode.setError("");
        String job = etJob.getEditText().getText().toString();
        if (job.isEmpty()){
            etJob.setError(getString(R.string.occupation_cant_be_empty));
            return;
        }
        etJob.setError("");
        String affiliation = etAffiliation.getEditText().getText().toString();
        String gender = spGender.getSelectedItem().toString();
        String education = spEducation.getSelectedItem().toString();
        String distance = spDistance.getSelectedItem().toString();*/

        Map<String,String> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("fullName", name);
        map.put("email", email);
        /**map.put("gender", gender);
        map.put("age", "0");
        map.put("address", address);
        map.put("postalCode", zipCode);
        map.put("distance", "");
        map.put("profession", job);
        map.put("institution", "");
        map.put("education", education);*/
        authService.updateProfile(map).enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                dissmissPleaseWaitDialog();
                if (response.isSuccessful()) {
                    Response body = response.body();
                    Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();
                    JsonObject jsonObject = gson.toJsonTree(body.getData()).getAsJsonObject();
                    User user = gson.fromJson(jsonObject, User.class);
                    App.instance().memory().setUser(getApplicationContext(), user);
                    if (user != null) {
                        new AlertDialog.Builder(ChangeProfileActivity.this)
                                .setTitle(R.string.success)
                                .setPositiveButton(android.R.string.yes, null)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .show();
                    } else {
                        showSnackbar(body.getMessage());
                    }
                } else if (response.errorBody() != null) {
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                        etName.setError(jObjError.getString("message"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    showSnackbar(ApiUtils.SOMETHING_WRONG);
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                dissmissPleaseWaitDialog();
                showSnackbar(ApiUtils.SOMETHING_WRONG);

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
