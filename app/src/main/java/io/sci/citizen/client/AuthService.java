package io.sci.citizen.client;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {

    @POST("auth/check-credential-id")
    Call<Response> checkPhoneNumber(@Body RequestBody s);

    @POST("auth/create-password")
    Call<Response> createPassword(@Body Map<String, String> map);

    @POST("auth/sign-in")
    Call<Response> signIn(@Body Map<String, String> map);

    @POST("auth/sign-up")
    Call<Response> signUp(@Body Map<String, String> map);

    @POST("user/update-profile")
    Call<Response> updateProfile(@Body Map<String, String> map);

    @POST("user/change-pwd")
    Call<Response> changePassword(@Body Map<String, String> map);
}
