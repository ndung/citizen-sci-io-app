package io.sci.citizen.step;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.stepstone.stepper.VerificationError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import io.sci.citizen.ActivitySmokeTest;
import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.Section;
import io.sci.citizen.model.SurveyQuestion;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class StepTest {

    private App app;
    private SurveyQuestion requiredQuestion;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
        requiredQuestion = question();
        app.memory().setProject(project(requiredQuestion));
    }

    @Test
    public void surveySectionStepRequiresAnswersThenSavesSurveyToCurrentData() {
        SurveySectionStep step = new SurveySectionStep(Arrays.asList(requiredQuestion));
        attach(step);
        step.onSelected();

        VerificationError missing = step.verifyStep();

        assertNotNull(missing);
        assertTrue(missing.getErrorMessage().contains("1"));

        QuestionParameter answer = new QuestionParameter();
        answer.setId(1L);
        answer.setDescription("clear");
        app.memory().putSurveyAnswers(requiredQuestion.getId(), Arrays.asList(answer));

        assertNull(step.verifyStep());
        assertNotNull(app.memory().getData().getSurvey().get(requiredQuestion.getId()));
    }

    @Test
    public void imageStepRequiresAtLeastOneImage() {
        ImageStep step = new ImageStep(10L);
        attach(step);
        step.onSelected();

        VerificationError error = step.verifyStep();

        assertNotNull(error);
    }

    @Test
    public void locationStepCanBeConstructed() {
        assertNotNull(new LocationStep());
    }

    private ActivitySmokeTest.TestBaseActivity attach(Fragment fragment) {
        ActivitySmokeTest.TestBaseActivity activity = Robolectric
                .buildActivity(ActivitySmokeTest.TestBaseActivity.class)
                .setup()
                .get();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        return activity;
    }

    private static SurveyQuestion question() {
        SurveyQuestion question = new SurveyQuestion();
        question.setId(50L);
        question.setAttribute("clarity");
        question.setQuestion("Clarity?");
        question.setType(3);
        question.setRequired(true);
        return question;
    }

    private static Project project(SurveyQuestion question) {
        Section section = new Section();
        section.setId(10L);
        section.setSequence(1);
        section.setType("survey");
        section.setName("Survey");
        section.setQuestions(Arrays.asList(question));
        Project project = new Project();
        project.setId(9L);
        project.setName("River");
        project.setIconUrl("");
        project.setSections(Arrays.asList(section));
        return project;
    }
}
