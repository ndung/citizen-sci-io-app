package io.sci.citizen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import io.sci.citizen.client.ApiUtils;
import io.sci.citizen.client.AuthService;

public class WelcomeActivity extends BaseActivity {

    TextView tv;

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_welcome);
        tv = findViewById(R.id.tv);
        authService = ApiUtils.AuthService(this);

        Button button = findViewById(R.id.btn_login);
        button.setOnClickListener(view -> startSignIn());
        activity = this;
        boolean isLoggedIn = App.instance().memory().getLoginFlag(this);
        if (isLoggedIn){
            startActivity(new Intent(getApplicationContext(), ProjectActivity.class));
            finish();
        }
    }

    private static final int RC_SIGN_IN = 9001;

    private void startSignIn() {
        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            /**IdpResponse response = IdpResponse.fromResultIntent(data);
            tv.setText("resultCode:"+resultCode);
            if (resultCode == RESULT_OK) {
                // Sign in succeeded
                refresh();
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackbar(getResources().getString(R.string.no_internet_connection));
                    return;
                }
                tv.setText("error:"+response.getError());
                showSnackbar(getResources().getString(R.string.sign_in_error)+response.getError());
            }*/
        }
    }

    AuthService authService;

}
