package io.sci.citizen.step;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.sci.citizen.App;
import io.sci.citizen.R;
import io.sci.citizen.adapter.SurveyAdapter;
import io.sci.citizen.fragment.BaseFragment;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.SurveyQuestion;

public class SurveySectionStep extends BaseFragment implements Step {

    private static final String TAG = SurveySectionStep.class.toString();
    private List<SurveyQuestion> list = null;
    private RecyclerView recyclerView;
    private Data data;
    private SurveyAdapter adapter;

    public SurveySectionStep(List<SurveyQuestion> list){
        this.list = list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.layout_questionnaire, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerView);
        adapter = new SurveyAdapter(this.getActivity(), list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        return rootView;
    }

    @Nullable
    @Override
    public VerificationError verifyStep() {
        for (int i=0;i<list.size();i++){
            try {
                SurveyQuestion question = list.get(i);
                boolean bool = App.instance().memory().getSurveyAnswers(question.getId())!=null;
                if (question.getType() == 2) {
                    if (question.isRequired() && !bool) {
                        return new VerificationError(getResources().getString(R.string.survey_empty_rb_answer) + getString(R.string.at_question_no) + (i + 1));
                    }
                } else if (question.getType() == 3 || question.getType() == 7 || question.getType() == 8) {
                    if (question.isRequired() && !bool){
                        return new VerificationError(getResources().getString(R.string.survey_empty_et_answer) + getString(R.string.at_question_no) + (i + 1));
                    }
                } else if (question.getType() == 4) {
                    if (question.isRequired() && !bool) {
                        return new VerificationError(getResources().getString(R.string.survey_empty_cb_answer) + getString(R.string.at_question_no) + (i + 1));
                    }
                } else if (question.getType() == 5) {
                    if (question.isRequired() && !bool){
                        return new VerificationError(getResources().getString(R.string.survey_empty_et_answer) + getString(R.string.at_question_no) + (i + 1));
                    }
                } else if (question.getType() == 6) {
                    if (question.isRequired() && !bool) {
                        return new VerificationError(getResources().getString(R.string.survey_empty_cb_answer) + getString(R.string.at_question_no) + (i + 1));
                    }
                }
            }catch(Exception ex){
                Log.e(TAG, "error", ex);
            }
        }
        data.setSurvey(App.instance().memory().getSurveyAnswers());
        App.instance().memory().setData(data);
        return null;
    }


    @Override
    public void onSelected() {
        data = App.instance().memory().getData();
        if (data==null){
            data = new Data();
            data.setUuid(Settings.Secure.getString(getContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID)+"_"+ UUID.randomUUID());
            data.setProjectId(App.instance().memory().project().getId());
            data.setStartDate(new Date());
        }
        if (data.getSurvey()!=null) {
            App.instance().memory().setSurveyAnswers(data.getSurvey());
            Map<String, Object> map = new LinkedHashMap<>();
            for (SurveyQuestion question : list) {
                List<QuestionParameter> pars = data.getSurvey().get(question.getId());
                if (pars != null && pars.size() == 1) {
                    map.put(question.getAttribute(), pars.get(0).getDescription());
                } else if (pars == null || pars.isEmpty()) {
                    map.put(question.getAttribute(), "");
                } else {
                    List<String> ansList = new ArrayList<>();
                    for (QuestionParameter ans : pars) {
                        ansList.add(ans.getDescription());
                    }
                    map.put(question.getAttribute(), ansList);
                }
            }
            adapter.setAnswers(map);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onError(@NonNull VerificationError verificationError) {
        showSnackbar(verificationError.getErrorMessage());
    }
}
