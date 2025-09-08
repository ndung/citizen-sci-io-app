package io.sci.citizen.client;

import android.content.Context;

import io.sci.citizen.R;

public class ApiUtils {

    public static String SOMETHING_WRONG;

    public static AuthService AuthService(Context context){
        SOMETHING_WRONG = context.getString(R.string.failed_check_your_internet_connection);
        return RetrofitClient.getClient(context).create(AuthService.class);
    }
    public static RecordService RecordService(Context context){
        SOMETHING_WRONG = context.getString(R.string.failed_check_your_internet_connection);
        return RetrofitClient.getClient(context).create(RecordService.class);
    }
    public static SurveyService SurveyService(Context context){
        SOMETHING_WRONG = context.getString(R.string.failed_check_your_internet_connection);
        return RetrofitClient.getClient(context).create(SurveyService.class);
    }
    public static ProjectService ProjectService(Context context){
        SOMETHING_WRONG = context.getString(R.string.failed_check_your_internet_connection);
        return RetrofitClient.getClient(context).create(ProjectService.class);
    }
}
