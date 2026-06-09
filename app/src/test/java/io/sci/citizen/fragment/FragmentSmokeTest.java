package io.sci.citizen.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import io.sci.citizen.ActivitySmokeTest;
import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.R;
import io.sci.citizen.RecordActivity;
import io.sci.citizen.SignUpActivity;
import io.sci.citizen.TncActivity;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.Section;
import io.sci.citizen.model.User;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class FragmentSmokeTest {

    private App app;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
        User user = new User();
        user.setUsername("citizen");
        user.setFullName("Citizen Scientist");
        user.setEmail("citizen@example.test");
        app.memory().setUser(app, user);
        app.memory().setProject(project());
    }

    @Test
    public void inputUsernameFragmentInflatesAndStartsSignUp() {
        InputUsernameFragment fragment = new InputUsernameFragment();
        ActivitySmokeTest.TestBaseActivity activity = attach(fragment);

        fragment.getView().findViewById(R.id.tv_sign_up).performClick();

        assertNotNull(fragment.getUsername());
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(SignUpActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void passwordAndVerificationFragmentsInflateInputs() {
        EnterPasswordFragment enterPassword = new EnterPasswordFragment();
        attach(enterPassword);
        assertNotNull(enterPassword.getEtPass());

        VerifyPhoneFragment verifyPhone = new VerifyPhoneFragment();
        attach(verifyPhone);
        assertNotNull(verifyPhone.getEtVerificationCode());

        CreatePasswordFragment createPassword = new CreatePasswordFragment();
        ActivitySmokeTest.TestBaseActivity activity = attach(createPassword);
        createPassword.getView().findViewById(R.id.layout_tnc).performClick();
        assertNotNull(createPassword.getEtPass());
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(TncActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void signUpFragmentInflatesPrimaryInputs() {
        SignUpFragment fragment = new SignUpFragment();
        attach(fragment);

        assertNotNull(fragment.getEtUserName());
        assertNotNull(fragment.getEtName());
        assertNotNull(fragment.getEtEmail());
        assertNotNull(fragment.getEtPassword());
    }

    @Test
    public void baseFragmentCanStartRecordActivity() {
        TestBaseFragment fragment = new TestBaseFragment();
        ActivitySmokeTest.TestBaseActivity activity = attach(fragment);

        fragment.openRecord();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(RecordActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void recordFragmentRefreshesStoredData() {
        Data data = new Data();
        data.setUuid("data-1");
        data.setStartDate(new java.util.Date(1_000L));
        app.memory().putData(data.getUuid(), data);
        RecordFragment fragment = new RecordFragment();

        attach(fragment);

        RecyclerView recyclerView = fragment.getView().findViewById(R.id.rv);
        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void serviceHeavyFragmentsCanBeConstructed() {
        assertNotNull(new AccountFragment());
        assertNotNull(new MapFragment());
        assertNotNull(new WebviewFragment());
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

    private static Project project() {
        Section section = new Section();
        section.setId(10L);
        section.setSequence(1);
        section.setType("survey");
        section.setName("Survey");
        section.setQuestions(java.util.Collections.emptyList());
        Project project = new Project();
        project.setId(9L);
        project.setName("River");
        project.setIconUrl("");
        project.setSections(Arrays.asList(section));
        return project;
    }

    public static class TestBaseFragment extends BaseFragment {
        public void openRecord() {
            startRecordActivity();
        }
    }
}
