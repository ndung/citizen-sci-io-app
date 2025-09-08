package io.sci.citizen.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;
import io.sci.citizen.R;

public class SignUpFragment extends Fragment {

    private TextInputLayout etUserName, etName, etEmail, etPassword, etAge, etDob, etAddress, etZipCode, etJob, etAffiliation;
    private AppCompatSpinner spGender, spEducation, spDistance;
    private LinearLayout tncLayout;

    public SignUpFragment() {
        // Required empty public constructor
    }

    private Calendar myCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_signup, container, false);
        etUserName = view.findViewById(R.id.et_username);
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_pwd);
        /**etAge = view.findViewById(R.id.et_age);
        spGender = view.findViewById(R.id.sp_gender);
        spEducation = view.findViewById(R.id.sp_education);
        spDistance = view.findViewById(R.id.sp_distance);
        etDob = view.findViewById(R.id.et_bod);
        etAddress = view.findViewById(R.id.et_address);
        etZipCode = view.findViewById(R.id.et_zip_code);
        etJob = view.findViewById(R.id.et_job);
        etAffiliation = view.findViewById(R.id.et_affiliation);*/
        tncLayout = view.findViewById(R.id.layout_tnc);

        /**etDob.getEditText().setFocusable(false);
        etDob.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(getActivity(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar, new OnDateSetListener(etDob.getEditText()),
                        myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        ArrayAdapter<String> gender = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, Arrays.asList("Male", "Female"));
        gender.setDropDownViewResource(R.layout.spinner_item);
        spGender.setAdapter(gender);

        ArrayAdapter<String> education = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, Arrays.asList("Dibawah SMA", "SMA", "S1", "S2", "S3"));
        education.setDropDownViewResource(R.layout.spinner_item);
        spEducation.setAdapter(education);

        ArrayAdapter<String> distance = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, Arrays.asList("kurang dari 1 kilometer", "antara 1 kilometer sampai 10 kilometer", "diatas 10 kilometer"));
        distance.setDropDownViewResource(R.layout.spinner_item);
        spDistance.setAdapter(distance);*/

        tncLayout.setOnClickListener(v -> {
            showBottomSheetDialog();
            //startActivity(new Intent(getActivity(), TncActivity.class));
        });
        return view;
    }

    class OnDateSetListener implements DatePickerDialog.OnDateSetListener{

        EditText editText;

        OnDateSetListener(EditText editText){
            this.editText = editText;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
            editText.setText(sdf.format(myCalendar.getTime()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public TextInputLayout getEtUserName() {
        return etUserName;
    }

    public TextInputLayout getEtName() {
        return etName;
    }

    public TextInputLayout getEtEmail() {
        return etEmail;
    }

    public TextInputLayout getEtPassword() {
        return etPassword;
    }

    public AppCompatSpinner getSpGender() {
        return spGender;
    }

    public TextInputLayout getEtAge() {
        return etAge;
    }

    public AppCompatSpinner getSpEducation() {
        return spEducation;
    }

    public AppCompatSpinner getSpDistance() {
        return spDistance;
    }

    public TextInputLayout getEtDob() {
        return etDob;
    }

    public TextInputLayout getEtAddress() {
        return etAddress;
    }

    public TextInputLayout getEtZipCode() {
        return etZipCode;
    }

    public TextInputLayout getEtJob() {
        return etJob;
    }

    public TextInputLayout getEtAffiliation() {
        return etAffiliation;
    }

    private void showBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getActivity());
        bottomSheetDialog.setContentView(R.layout.layout_tnc);
        bottomSheetDialog.show();
    }
}