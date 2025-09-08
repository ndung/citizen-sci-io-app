package io.sci.citizen.client;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ProjectService {

    @GET("projects")
    Call<Response> projects();
}
