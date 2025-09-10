package io.sci.citizen;

import android.os.Bundle;

import java.util.Map;
import io.sci.citizen.client.Response;
import retrofit2.Call;

public class ProjectDataMapActivity extends DataMapActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv.setText(getString(R.string.data_for_project)+App.instance().memory().project().getName());
    }

    @Override
    protected Call<Response> callApi(Map<String,Integer> map){
        map.put("projectId", App.instance().memory().project().getId().intValue());
        return recordService.listByProject(map);
    }
}
