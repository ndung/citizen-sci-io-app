package io.sci.citizen.client;


import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.sci.citizen.App;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String TAG = RetrofitClient.class.toString();

    public static Retrofit retrofit;

    public static Retrofit getClient(final Context context) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        final String token = App.instance().memory().getToken(context);

        httpClient.readTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(300, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(logging)
                .addInterceptor(new AddCookiesInterceptor(context))
                .addInterceptor(new ReceivedCookiesInterceptor(context))
                .addInterceptor(chain -> {
                    Request.Builder newRequest  = chain.request().newBuilder();
                    if (App.instance().memory().getLoginFlag(context) &&
                            !App.instance().memory().getToken(context).isEmpty()){
                        newRequest.addHeader("Authorization", token);
                    }
                    newRequest.addHeader("Content-Type", "application/json");
                    return chain.proceed(newRequest.build());
                })
        ;

        retrofit = new Retrofit.Builder()
                .baseUrl(App.API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
        return retrofit;

    }

}