package io.sci.citizen;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.AuthService;
import io.sci.citizen.client.Response;
import io.sci.citizen.fragment.CreatePasswordFragment;
import io.sci.citizen.fragment.EnterPasswordFragment;
import io.sci.citizen.fragment.InputUsernameFragment;
import io.sci.citizen.fragment.SignUpFragment;
import io.sci.citizen.model.LoginDetails;
import io.sci.citizen.util.GsonDeserializer;
import io.sci.citizen.util.StringUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class SignInActivity extends BaseActivity {

    private FrameLayout mainFrame;
    private Button btNext;
    private FragmentManager fragmentManager;

    private static int STEP_INPUT_PHONE = 1;
    private static int STEP_VERIFY_PHONE = 2;
    private static int STEP_INPUT_PASSWORD = 3;
    private static int STEP_SIGN_UP = 4;
    private static int STEP_CREATE_PASSWORD = 5;

    private static int STEP = 1;

    private AuthService authService;

    private InputUsernameFragment inputUsernameFragment;
    private EnterPasswordFragment enterPasswordFragment;
    private CreatePasswordFragment createPasswordFragment;
    private SignUpFragment signUpFragment;

    private String username = null;

    private static final String TAG = SignInActivity.class.toString();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_signin);
        mainFrame = findViewById(R.id.main_frame);
        authService = ApiUtils.AuthService(this);

        fragmentManager = getSupportFragmentManager();

        inputUsernameFragment = new InputUsernameFragment();
        enterPasswordFragment = new EnterPasswordFragment();
        createPasswordFragment = new CreatePasswordFragment();
        signUpFragment = new SignUpFragment();
        setFragment(inputUsernameFragment);


        STEP = STEP_INPUT_PHONE;
        btNext = findViewById(R.id.bt_next);
        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (STEP == STEP_INPUT_PHONE) {
                    checkUsername();
                } else if (STEP == STEP_INPUT_PASSWORD) {
                    signIn();
                } else if (STEP == STEP_CREATE_PASSWORD) {
                    createPassword();
                } else if (STEP == STEP_SIGN_UP) {
                    signUp();
                }
            }

        });
    }

    Double status = 0d;

    private void checkUsername() {
        username = inputUsernameFragment.getUsername().getEditText().getText().toString();
        if (!username.isEmpty()) {
            showPleaseWaitDialog();
            Map<String, String> map = new HashMap<>();
            map.put("username", username);
            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), username);

            authService.checkPhoneNumber(body).enqueue(new Callback<Response>() {
                @Override
                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                    dissmissPleaseWaitDialog();
                    if (response.isSuccessful()) {
                        Response body = response.body();
                        status = (double) body.getData();

                        if (status > 0) {
                            STEP = STEP_INPUT_PASSWORD;
                            setFragment(enterPasswordFragment);
                        }else if (status < 0){
                            showSnackbar(getString(R.string.username_is_not_found));
                        }else{
                            showSnackbar(getString(R.string.user_has_not_been_verified));
                        }
                    } else if (response.errorBody() != null) {
                        try {
                            JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                            inputUsernameFragment.getUsername().setError(jObjError.getString("message"));
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
                    Log.e(TAG, "error", t);
                    showSnackbar(ApiUtils.SOMETHING_WRONG);

                }
            });
        } else {
            inputUsernameFragment.getUsername().setError(getString(R.string.username_must_be_filled));
        }
    }

    private void signIn() {
        final String pwd = enterPasswordFragment.getEtPass().getEditText().getText().toString();
        if (!pwd.isEmpty()) {
            if (!username.isEmpty()) {
                showPleaseWaitDialog();
                Map<String, String> map = new HashMap<>();
                map.put("username", username);
                map.put("password", pwd);
                authService.signIn(map).enqueue(new Callback<Response>() {
                    @Override
                    public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                        dissmissPleaseWaitDialog();
                        if (response.isSuccessful()) {
                            onSuccessfulLogin(response);
                        } else if (response.errorBody() != null) {
                            try {
                                JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                                enterPasswordFragment.getEtPass().setError(jObjError.getString("message"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else if (response.body() != null && response.body().getMessage() != null) {
                            showSnackbar(response.body().getMessage());
                        } else {
                            showSnackbar(ApiUtils.SOMETHING_WRONG);
                        }
                    }

                    @Override
                    public void onFailure(Call<Response> call, Throwable t) {
                        dissmissPleaseWaitDialog();
                        Log.e(TAG, "error", t);
                        showSnackbar(ApiUtils.SOMETHING_WRONG);

                    }
                });
            }
        } else {
            enterPasswordFragment.getEtPass().setError(getString(R.string.password_cant_be_empty));
        }
    }

    private void createPassword() {
        String pwd = createPasswordFragment.getEtPass().getEditText().getText().toString();
        if (!pwd.isEmpty()) {
            if (StringUtils.isPasswordValid(pwd)) {
                if (!username.isEmpty()) {
                    showPleaseWaitDialog();
                    Map<String, String> map = new HashMap<>();
                    map.put("username", username);
                    map.put("password", pwd);
                    authService.createPassword(map).enqueue(new Callback<Response>() {
                        @Override
                        public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                            dissmissPleaseWaitDialog();
                            if (response.isSuccessful()) {
                                onSuccessfulLogin(response);
                            } else if (response.errorBody() != null) {
                                try {
                                    JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                                    createPasswordFragment.getEtPass().setError(jObjError.getString("message"));
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
                            Log.e(TAG, "error", t);
                            showSnackbar(ApiUtils.SOMETHING_WRONG);

                        }
                    });
                }
            } else {
                createPasswordFragment.getEtPass().setError(getString(R.string.password_length_must_be_at_least_6_characters));
            }
        } else {
            createPasswordFragment.getEtPass().setError(getString(R.string.password_cant_be_empty));
        }
    }

    private void setFragment(Fragment fragment) {
        fragmentManager.beginTransaction().replace(mainFrame.getId(), fragment).commitAllowingStateLoss();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (STEP==STEP_VERIFY_PHONE||STEP==STEP_CREATE_PASSWORD||STEP==STEP_INPUT_PASSWORD||STEP==STEP_SIGN_UP){
            STEP = STEP_INPUT_PHONE;
            setFragment(inputUsernameFragment);
        }else{
            finish();
        }
    }

    private void signUp(){
        String name = signUpFragment.getEtName().getEditText().getText().toString();
        if (name.isEmpty()){
            signUpFragment.getEtName().setError(getString(R.string.name_cant_be_empty));
            return;
        }
        signUpFragment.getEtName().setError("");
        String email = signUpFragment.getEtEmail().getEditText().getText().toString();
        if (email.isEmpty()){
            signUpFragment.getEtEmail().setError(getString(R.string.email_cant_be_empty));
            return;
        }
        if (!StringUtils.isEmailValid(email)){
            signUpFragment.getEtEmail().setError(getString(R.string.email_must_be_valid));
            return;
        }
        signUpFragment.getEtEmail().setError("");
        String password = signUpFragment.getEtPassword().getEditText().getText().toString();
        if (password.isEmpty()){
            signUpFragment.getEtPassword().setError(getString(R.string.password_cant_be_empty));
            return;
        }
        if (!StringUtils.isPasswordValid(password)) {
            signUpFragment.getEtPassword().setError(getString(R.string.password_length_must_be_at_least_6_characters));
            return;
        }
        signUpFragment.getEtPassword().setError("");
        /**String age = signUpFragment.getEtAge().getEditText().getText().toString();
        if (age.isEmpty()){
            signUpFragment.getEtAge().setError(getString(R.string.age_cant_be_empty));
            return;
        }
        try{
            int num = Integer.parseInt(age);
            if (num<0 || num>99){
                signUpFragment.getEtAge().setError(getString(R.string.age_must_be_bigger_than_0_and_smaller_than_99));
                return;
            }
        } catch (NumberFormatException e) {
            signUpFragment.getEtAge().setError(getString(R.string.age_must_be_valid));
            return;
        }
        signUpFragment.getEtAge().setError("");
        String address = signUpFragment.getEtAddress().getEditText().getText().toString();
        if (address.isEmpty()){
            signUpFragment.getEtAddress().setError(getString(R.string.address_cant_be_empty));
            return;
        }
        signUpFragment.getEtAddress().setError("");
        String zipCode = signUpFragment.getEtZipCode().getEditText().getText().toString();
        if (zipCode.isEmpty()){
            signUpFragment.getEtZipCode().setError(getString(R.string.zip_code_cant_be_empty));
            return;
        }
        if (zipCode.length()!=5){
            signUpFragment.getEtZipCode().setError(getString(R.string.zip_code_length_must_be_5));
            return;
        }
        signUpFragment.getEtZipCode().setError("");
        String job = signUpFragment.getEtJob().getEditText().getText().toString();
        if (job.isEmpty()){
            signUpFragment.getEtJob().setError(getString(R.string.occupation_cant_be_empty));
            return;
        }
        signUpFragment.getEtJob().setError("");
        String affiliation = signUpFragment.getEtAffiliation().getEditText().getText().toString();
        String gender = signUpFragment.getSpGender().getSelectedItem().toString();
        String education = signUpFragment.getSpEducation().getSelectedItem().toString();
        String distance = signUpFragment.getSpDistance().getSelectedItem().toString();*/

        Map<String,String> map = new HashMap<>();
        map.put("username", username);
        map.put("fullName", name);
        map.put("email", email);
        map.put("password", password);
        map.put("confirmPassword", password);
        /**map.put("gender", gender);
        map.put("address", address);
        map.put("postalCode", zipCode);
        map.put("profession", job);
        map.put("education", education);*/
        authService.signUp(map).enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                dissmissPleaseWaitDialog();
                if (response.isSuccessful()) {
                    setFragment(inputUsernameFragment);
                    //onSuccessfulLogin(response);
                } else if (response.errorBody() != null) {
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string().trim());
                        signUpFragment.getEtUserName().setError(jObjError.getString("message"));
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
                Log.e(TAG, "error", t);
                showSnackbar(ApiUtils.SOMETHING_WRONG);

            }
        });
    }

    private void onSuccessfulLogin(retrofit2.Response<Response> response){
        Response body = response.body();
        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();
        Log.d(TAG,body.toString());
        JsonObject jsonObject = gson.toJsonTree(body.getData()).getAsJsonObject();
        LoginDetails details = gson.fromJson(jsonObject, LoginDetails.class);
        if (details != null) {
            String token = response.headers().get("Token");
            App.instance().memory().setUser(getApplicationContext(), details.getUser());
            App.instance().memory().setToken(getApplicationContext(), token);
            App.instance().memory().setLoginFlag(getApplicationContext(), true);
            App.instance().memory().setProjects(details.getProjects());
            startActivity(new Intent(getApplicationContext(), ProjectActivity.class));
            //FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(details.getUser().getId()));
            //FirebaseMessaging.getInstance().subscribeToTopic("global");
            WelcomeActivity.activity.finish();
            finish();
        } else {
            showSnackbar(body.getMessage());
        }
    }
}