package io.sci.citizen.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.textfield.TextInputLayout;

import androidx.fragment.app.Fragment;
import io.sci.citizen.R;
import io.sci.citizen.TncActivity;

public class CreatePasswordFragment extends Fragment {

    TextInputLayout etPass;
    LinearLayout tncLayout;

    public CreatePasswordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_create_password, container, false);
        etPass = view.findViewById(R.id.et_pass);
        tncLayout = view.findViewById(R.id.layout_tnc);
        tncLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), TncActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public TextInputLayout getEtPass() {
        return etPass;
    }
}