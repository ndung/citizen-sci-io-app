package io.sci.citizen;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;

import io.sci.citizen.fragment.AccountFragment;
import io.sci.citizen.fragment.MapFragment;
import io.sci.citizen.fragment.RecordFragment;

public class HomeActivity extends BaseActivity {

    BottomNavigationView navView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.nav_my_records) {
                loadFragment(new RecordFragment());
                return true;
            } else if (id == R.id.nav_my_account) {
                loadFragment(new AccountFragment());
                return true;
            } else if (id == R.id.nav_map) {
                loadFragment(new MapFragment());
                return true;
            /**} else if (id == R.id.nav_identifier) {
                loadFragment(new WebviewFragment());
                return true;*/
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home);
        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        loadFragment(new RecordFragment());
    }

    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /**if (id == R.id.action_settings) {
            return true;
        } else*/
            if (id == R.id.action_logout) {
            showPleaseWaitDialog();
            signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut(){
        startActivity(new Intent(this, WelcomeActivity.class));
        App.instance().memory().setLoginFlag(this, false);
        finish();
    }

    /**@Override
    protected void onStart() {
        super.onStart();
        bindService(uploadServiceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        //uploadService.stopForeground(true);
        //uploadService.stopSelf();
    }

    public UploadService getUploadService() {
        return uploadService;
    }*/

}
