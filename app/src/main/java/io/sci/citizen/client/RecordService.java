package io.sci.citizen.client;

import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RecordService {

    @Multipart
    @POST("record/upload")
    Call<Response> upload(@Part MultipartBody.Part model,
                          @Part MultipartBody.Part[] images,
                          @Part MultipartBody.Part survey);

    @POST("record/list-by-user")
    Call<Response> listByUser(@Body Map<String, Integer> map);

    @POST("record/list-by-project")
    Call<Response> listByProject(@Body Map<String, Integer> map);

    @GET("record/user-summary")
    Call<Response> summary();

}
