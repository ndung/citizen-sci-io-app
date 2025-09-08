package io.sci.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.Map;

import io.sci.citizen.adapter.ProjectAdapter;
import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.ProjectService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.User;
import io.sci.citizen.util.GsonDeserializer;
import retrofit2.Call;
import retrofit2.Callback;

public class ProjectActivity extends BaseActivity {

    SwipeRefreshLayout layout;
    ProjectService projectService;
    ProjectAdapter projectAdapter;

    protected final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new GsonDeserializer()).create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_project);
        TextView tvUsername = findViewById(R.id.tv_username);
        Button btnLogout = findViewById(R.id.btn_logout);
        layout = findViewById(R.id.layout);
        RecyclerView recyclerView = findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        User user = App.instance().memory().getUser(this);
        tvUsername.setText(user.getUsername());
        tvUsername.setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));

        btnLogout.setOnClickListener(v -> logout());

        projectAdapter = new ProjectAdapter(this, project -> {
            App.instance().memory().setProject(project);
            startDataListActivity();
        });
        Map<Long,Project> projects = App.instance().memory().getProjects();
        int i=0;
        for (Long id : projects.keySet()){
            projectAdapter.add(projects.get(id), i);
            i=i+1;
        }

        recyclerView.setAdapter(projectAdapter);
        layout.setOnRefreshListener(this::refreshProjects);
        projectService = ApiUtils.ProjectService(this);
    }

    private void startDataListActivity(){
        startActivity(new Intent(this, DataListActivity.class));
    }

    private void refreshProjects() {
        projectService.projects().enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response resp = response.body();
                    JsonObject jsonObject = gson.toJsonTree(resp).getAsJsonObject();

                    Map<Long, Project> list = gson.fromJson(
                            jsonObject.getAsJsonObject("data"),
                            new TypeToken<Map<Long, Project>>() {
                            }.getType()
                    );
                    App.instance().memory().setProjects(list);
                    projectAdapter.clear();
                    int i = 0;
                    for (Long id : list.keySet()) {
                        projectAdapter.add(list.get(id), i);
                        i = i + 1;
                    }
                }
                layout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                layout.setRefreshing(false);
            }
        });
    }

    private void logout() {
        showPleaseWaitDialog();
        signOut();
    }
    private void signOut(){
        startActivity(new Intent(this, WelcomeActivity.class));
        App.instance().memory().setLoginFlag(this, false);
        App.instance().memory().setUser(this, null);
        App.instance().memory().setToken(this, "");
        finish();
    }
}