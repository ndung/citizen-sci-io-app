package io.sci.citizen;

import java.util.Map;
import io.sci.citizen.client.Response;
import retrofit2.Call;

public class ProjectDataMapActivity extends DataMapActivity {

    @Override
    protected Call<Response> callApi(Map<String,Integer> map){
        map.put("projectId", App.instance().memory().project().getId().intValue());
        return recordService.listByProject(map);
    }
}
