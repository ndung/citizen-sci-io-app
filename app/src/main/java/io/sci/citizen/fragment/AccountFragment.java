package io.sci.citizen.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.Date;

import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.ChangePasswordActivity;
import io.sci.citizen.ChangeProfileActivity;
import io.sci.citizen.R;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.Response;
import io.sci.citizen.client.RecordService;
import io.sci.citizen.model.DataSummary;
import io.sci.citizen.model.User;
import io.sci.citizen.util.GsonDeserializer;
import retrofit2.Call;
import retrofit2.Callback;

public class AccountFragment extends BaseFragment{

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvUsername;
    private TextView tvRecords;
    private TextView tvVersion;
    private User user;
    private RecordService recordService;
    private LinearLayout changeProfile, changePassword;

    Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.layout_account, container, false);

        tvName = rootView.findViewById(R.id.tv_name);
        tvEmail = rootView.findViewById(R.id.tv_email);
        tvUsername= rootView.findViewById(R.id.tv_username);
        tvRecords = rootView.findViewById(R.id.tv_records);
        tvVersion = rootView.findViewById(R.id.tv_version);
        changeProfile = rootView.findViewById(R.id.ll_change_profile);
        changePassword = rootView.findViewById(R.id.ll_change_password);
        //FirebaseAuth auth = FirebaseAuth.getInstance();
        user = App.instance().memory().getUser(getActivity());
        tvVersion.setText(BuildConfig.VERSION_NAME);
        tvUsername.setText(user.getUsername());
        tvName.setText(user.getFullName());
        tvEmail.setText(user.getEmail());
        //Picasso.with(getActivity()).load(auth.getCurrentUser().getPhotoUrl()).into(imageView);

        recordService = ApiUtils.RecordService(getActivity());
        changeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ChangeProfileActivity.class));
            }
        });
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
            }
        });

        refresh();

        return rootView;
    }

    private void refresh(){
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

}
