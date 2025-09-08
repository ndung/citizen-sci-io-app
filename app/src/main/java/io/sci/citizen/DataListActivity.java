package io.sci.citizen;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import io.sci.citizen.adapter.RecordAdapter;
import io.sci.citizen.fragment.RecordFragment;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Section;

public class DataListActivity extends BaseActivity {

    private static final String TAG = RecordFragment.class.toString();
    private SwipeRefreshLayout layout;
    private RecyclerView recyclerView;
    private RecordAdapter recordAdapter;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_record);
        textView = findViewById(R.id.tv);
        layout = findViewById(R.id.layout);
        recyclerView = findViewById(R.id.rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recordAdapter = new RecordAdapter(this, new RecordAdapter.OnItemClickListener() {

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
                showDialog(data);
            }

            @Override
            public void stopUploading(Data data) {
                data.setUploaded(false);
                data.setUploading(false);
                App.instance().memory().putData(data.getUuid(), data);
                showSnackbar(getString(R.string.uploading_failed));
                refresh();
            }

        });

        layout.setOnRefreshListener(this::refresh);

        Button button = findViewById(R.id.uploadAll);
        button.setOnClickListener(view -> uploadAll());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), App.UPLOAD_NOTIFICATION)) {
                    refresh();
                }
            }
        };

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(App.instance().memory().project().getName());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                startRecordActivity();
                return true;
            }else if (item.getItemId() == R.id.action_del_all) {
                App.instance().memory().removeData();
                refresh();
                return true;
            }else if (item.getItemId() == R.id.action_data){
                startDataMapActivity();
            }
            return false;
        });
    }

    private void showDialog(Data data){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_data)
                .setMessage(R.string.are_you_sure_to_delete)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        App.instance().memory().deleteData(data.getUuid());
                        refresh();
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void startRecordActivity(){
        List<Section> list = App.instance().memory().project().getSections();
        if (list==null || list.isEmpty()){
            showSnackbar(getString(R.string.no_steps));
        }else {
            startActivity(new Intent(this, RecordActivity.class));
        }
    }

    private void startDataMapActivity(){
        startActivity(new Intent(this, ProjectDataMapActivity.class));
    }

    private void uploadAll(){
        Map<String, Data> list = App.instance().memory().getDataMap();
        for (String key : list.keySet()){
            Data data = list.get(key);
            if (!Objects.requireNonNull(data).isUploaded() && !data.isUploading()) {
                upload(data);
            }
        }
    }

    private void upload(final Data data){
        data.setUploading(true);
        App.instance().memory().putData(data.getUuid(), data);
        refresh();
        Intent intent = new Intent(this, UploadService.class);
        intent.putExtra("io", data.getUuid());
        ContextCompat.startForegroundService(this, intent);
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
        if (i==0){
            textView.setVisibility(VISIBLE);
            layout.setVisibility(GONE);
        }else{
            textView.setVisibility(GONE);
            layout.setVisibility(VISIBLE);
        }
        recyclerView.setAdapter(recordAdapter);
        layout.setRefreshing(false);
    }

    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(App.UPLOAD_NOTIFICATION));
        super.onResume();
        refresh();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
