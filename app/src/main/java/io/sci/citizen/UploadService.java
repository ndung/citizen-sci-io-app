package io.sci.citizen;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.Response;
import io.sci.citizen.client.RecordService;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.Section;
import io.sci.citizen.model.SurveyQuestion;
import io.sci.citizen.util.MultipartUtils;
import io.sci.citizen.util.Sequence;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;

public class UploadService extends Service {

    private static final String TAG = UploadService.class.toString();

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder
    {
        public UploadService getService()
        {
            return UploadService.this;
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String id = null;
        Bundle extras = intent.getExtras();
        if(extras != null){
            id = (String) extras.get("io");
        }

        int notificationId = Sequence.nextValue();
        Notification notification = new NotificationCompat.Builder(this, App.NOTIF_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getString(R.string.uploading)+id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), PendingIntent.FLAG_IMMUTABLE))
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(notificationId, notification);
        }
        upload(id);

        return super.onStartCommand(intent, flags, startId);
    }

    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").enableComplexMapKeySerialization().create();

    public void upload(String id){
        final Data data = App.instance().memory().getData(id);
        try {
            RecordService recordService = ApiUtils.RecordService(this);
            MultipartBody.Part model = MultipartBody.Part.createFormData("model", gson.toJson(data));
            List<MultipartBody.Part> list = new ArrayList<>();

            if (data.getImagePaths()!=null && !data.getImagePaths().isEmpty()) {
                for (Long section : data.getImagePaths().keySet()) {
                    for (String image : data.getImagePaths().get(section)) {
                        list.add(MultipartUtils.uriToPart(getApplicationContext(), String.valueOf(section), Uri.parse(image), "images"));
                    }
                }
            }
            Map<Long,Object> map = new LinkedHashMap<>();

            List<Section> sections = App.instance().memory().project().getSections();

            for (Section sec : sections){
                List<SurveyQuestion> questions = sec.getQuestions();
                for (SurveyQuestion s : questions) {
                    List<QuestionParameter> pars = data.getSurvey().get(s.getId());
                    if (pars != null && pars.size() == 1) {
                        map.put(s.getId(), pars.get(0).getDescription());
                    } else if (pars == null || pars.isEmpty()) {
                        //map.put(s.getId(), "");
                    } else {
                        List<String> ansList = new ArrayList<>();
                        for (QuestionParameter ans : pars) {
                            ansList.add(ans.getDescription());
                        }
                        map.put(s.getId(), ansList);
                    }
                }
            }
            MultipartBody.Part[] images = new MultipartBody.Part[list.size()];
            MultipartBody.Part results = MultipartBody.Part.createFormData("results", gson.toJson(map));
            recordService.upload(model, list.toArray(images), results).enqueue(new Callback<Response>() {
                @Override
                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                    if (response.isSuccessful() && response.body().getData() == Boolean.TRUE) {
                        onUploadSuccessful(data);
                    } else {
                        onUploadFailed(data);
                    }
                }

                @Override
                public void onFailure(Call<Response> call, Throwable t) {
                    onUploadFailed(data);
                }
            });
        }catch (Exception ex){
            Log.e(TAG, "upload exception", ex);
            onUploadFailed(data);
        }
    }

    private void onUploadSuccessful(Data data){
        data.setUploaded(true);
        data.setUploading(false);
        App.instance().memory().putData(data.getUuid(), data);
        Intent uploadNotification = new Intent(App.UPLOAD_NOTIFICATION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(uploadNotification);
        stopForeground(true);
        stopSelf();
        //notificationManager.cancel(notificationId);
    }

    private void onUploadFailed(Data data){
        if (data!=null) {
            data.setUploading(false);
            App.instance().memory().putData(data.getUuid(), data);
        }
        Intent uploadNotification = new Intent(App.UPLOAD_NOTIFICATION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(uploadNotification);
        Toast.makeText(this, getResources().getString(R.string.upload_failed_please_retry), Toast.LENGTH_LONG).show();
        stopForeground(true);
        stopSelf();
        //notificationManager.cancel(notificationId);
    }

}
