package io.sci.citizen.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Map;

import io.sci.citizen.App;
import io.sci.citizen.R;
import io.sci.citizen.UploadService;
import io.sci.citizen.adapter.RecordAdapter;
import io.sci.citizen.model.Data;

public class RecordFragment extends BaseFragment{

    private static final String TAG = RecordFragment.class.toString();
    private SwipeRefreshLayout layout;
    private RecyclerView recyclerView;
    private RecordAdapter recordAdapter;
    private Button button;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.layout_record, container, false);

        layout = rootView.findViewById(R.id.layout);
        recyclerView = rootView.findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        recordAdapter = new RecordAdapter(getActivity(), new RecordAdapter.OnItemClickListener() {

            @Override
            public void onEdit(Data data) {
                data.setEditMode(true);
                App.instance().memory().setData(data);
                startRecordActivity();
            }

            @Override
            public void onUpload(Data data) {
                upload(data);
            }

            @Override
            public void onDelete(Data data) {
                App.instance().memory().deleteData(data.getUuid());
                refresh();
            }

            @Override
            public void stopUploading(Data data) {
                data.setUploaded(false);
                data.setUploading(false);
                App.instance().memory().putData(data.getUuid(), data);
                showSnackbar("Upload failed, please retry!");
                refresh();
            }

        });

        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        button = rootView.findViewById(R.id.uploadAll);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadAll();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(App.UPLOAD_NOTIFICATION)) {
                    refresh();
                }
            }
        };
        return rootView;
    }

    private void uploadAll(){
        Map<String, Data> list = App.instance().memory().getDataMap();
        for (String key : list.keySet()){
            Data data = list.get(key);
            if (!data.isUploaded() && !data.isUploading()) {
                upload(data);
            }
        }
    }

    private void upload(final Data data){
        data.setUploading(true);
        App.instance().memory().putData(data.getUuid(), data);
        refresh();
        Intent intent = new Intent(getActivity(), UploadService.class);
        intent.putExtra("io", data.getId());
        ContextCompat.startForegroundService(getActivity(), intent);
        //((HomeActivity)getActivity()).getUploadService().upload(redTide.getId());
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh();
    }

    private void refresh(){
        recordAdapter.clear();
        Map<String, Data> map = App.instance().memory().getDataMap();
        int i=0;
        for (String key : map.keySet()){
            Data data = map.get(key);
            recordAdapter.add(data, i);
            i=i+1;
        }
        recyclerView.setAdapter(recordAdapter);
        layout.setRefreshing(false);
    }

    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(App.UPLOAD_NOTIFICATION));
        super.onResume();
        refresh();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

}
