package io.sci.citizen.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import androidx.fragment.app.Fragment;
import io.sci.citizen.R;
import io.sci.citizen.SignUpActivity;

public class InputUsernameFragment extends Fragment {

    TextInputLayout username;
    TextView tvSignUp;

    public InputUsernameFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_input_username, container, false);
        username = view.findViewById(R.id.username);
        tvSignUp = view.findViewById(R.id.tv_sign_up);
        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
        return view;
    }

    private void signUp(){
        Intent intent = new Intent(getActivity(), SignUpActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public TextInputLayout getUsername() {
        return username;
    }

}