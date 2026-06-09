package io.sci.citizen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowActivity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.sci.citizen.client.RecordService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.Section;
import io.sci.citizen.model.SurveyQuestion;
import io.sci.citizen.model.User;
import retrofit2.Call;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class ActivitySmokeTest {

    private App app;
    private User user;
    private Project project;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
        user = user();
        project = project();
        Map<Long, Project> projects = new LinkedHashMap<>();
        projects.put(project.getId(), project);
        app.memory().setUser(app, user);
        app.memory().setProject(project);
        app.memory().setProjects(projects);
        app.memory().setLoginFlag(app, false);
    }

    @Test
    public void appStoresLocationValuesAndComputesDistance() {
        android.location.Location location = new android.location.Location("gps");
        location.setLatitude(-6.2d);
        location.setLongitude(106.8d);
        location.setAccuracy(4.5f);
        location.setSpeed(1.25f);

        app.onNewLocationAvailable(location);

        assertEquals("4.5", app.memory().getString(app, App.ACCURACY));
        assertEquals("1.25", app.memory().getString(app, App.SPEED));
        assertEquals("-6.2", app.memory().getString(app, App.LATITUDE));
        assertEquals("106.8", app.memory().getString(app, App.LONGITUDE));
        assertEquals(0d, App.distance(0, 0, 0, 0, 0, 0), 0.001d);
        assertNotNull(app.memory());
        app.getPlatformId();
    }

    @Test
    public void baseActivityStartsActivities() {
        TestBaseActivity activity = Robolectric.buildActivity(TestBaseActivity.class)
                .setup()
                .get();

        activity.showActivity(activity, WelcomeActivity.class);

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(WelcomeActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(SignInActivity.class.getName(),
                activity.getIntent(activity, SignInActivity.class).getComponent().getClassName());
    }

    @Test
    public void welcomeButtonStartsSignInWhenLoggedOut() {
        WelcomeActivity activity = Robolectric.buildActivity(WelcomeActivity.class)
                .setup()
                .get();

        activity.findViewById(R.id.btn_login).performClick();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(SignInActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void welcomeRedirectsToProjectsWhenLoggedIn() {
        app.memory().setLoginFlag(app, true);

        WelcomeActivity activity = Robolectric.buildActivity(WelcomeActivity.class)
                .setup()
                .get();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(ProjectActivity.class.getName(), started.getComponent().getClassName());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void signInEmptyUsernameShowsValidationError() {
        SignInActivity activity = Robolectric.buildActivity(SignInActivity.class)
                .setup()
                .get();
        activity.getSupportFragmentManager().executePendingTransactions();

        activity.findViewById(R.id.bt_next).performClick();

        TextInputLayout username = activity.findViewById(R.id.username);
        assertEquals(activity.getString(R.string.username_must_be_filled), username.getError().toString());
    }

    @Test
    public void signUpEmptySubmitShowsUsernameError() {
        SignUpActivity activity = Robolectric.buildActivity(SignUpActivity.class)
                .setup()
                .get();

        activity.findViewById(R.id.bt_change).performClick();

        TextInputLayout username = activity.findViewById(R.id.et_username);
        assertEquals(activity.getString(R.string.username_cant_be_empty), username.getError().toString());
    }

    @Test
    public void changePasswordEmptySubmitShowsOldPasswordError() {
        ChangePasswordActivity activity = Robolectric.buildActivity(ChangePasswordActivity.class)
                .setup()
                .get();

        activity.findViewById(R.id.bt_change).performClick();

        TextInputLayout oldPassword = activity.findViewById(R.id.et_old_password);
        assertEquals(activity.getString(R.string.old_password_cant_be_empty), oldPassword.getError().toString());
    }

    @Test
    public void changeProfilePrefillsUserAndValidatesName() {
        ChangeProfileActivity activity = Robolectric.buildActivity(ChangeProfileActivity.class)
                .setup()
                .get();
        TextInputLayout name = activity.findViewById(R.id.et_name);
        TextInputLayout email = activity.findViewById(R.id.et_email);

        assertEquals(user.getFullName(), name.getEditText().getText().toString());
        assertEquals(user.getEmail(), email.getEditText().getText().toString());

        name.getEditText().setText("");
        activity.findViewById(R.id.bt_change).performClick();

        assertEquals(activity.getString(R.string.name_cant_be_empty), name.getError().toString());
    }

    @Test
    public void projectActivityShowsUserAndNavigatesToAccount() {
        ProjectActivity activity = Robolectric.buildActivity(ProjectActivity.class)
                .setup()
                .get();

        TextView username = activity.findViewById(R.id.tv_username);
        username.performClick();

        assertEquals(user.getUsername(), username.getText().toString());
        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(AccountActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void homeActivityLoadsBottomNavigationAndInitialFragment() {
        HomeActivity activity = Robolectric.buildActivity(HomeActivity.class)
                .setup()
                .get();

        assertNotNull(activity.findViewById(R.id.nav_view));
        assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
    }

    @Test
    public void dataListActivityShowsEmptyState() {
        DataListActivity activity = Robolectric.buildActivity(DataListActivity.class)
                .setup()
                .get();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.tv).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.layout).getVisibility());
    }

    @Test
    public void recordActivityCreatesStepperScreen() {
        RecordActivity activity = Robolectric.buildActivity(RecordActivity.class)
                .setup()
                .get();

        TextView title = activity.findViewById(R.id.tv_title);
        assertTrue(title.getText().toString().contains(project.getName()));
    }

    @Test
    public void tncCloseButtonFinishesActivity() {
        TncActivity activity = Robolectric.buildActivity(TncActivity.class)
                .setup()
                .get();

        activity.findViewById(R.id.iv_finish).performClick();

        assertTrue(activity.isFinishing());
    }

    @Test
    public void dataMapActivitiesDelegateToExpectedRecordServiceCall() {
        FakeRecordService service = new FakeRecordService();
        DataMapActivity dataMapActivity = new DataMapActivity();
        dataMapActivity.recordService = service;
        Map<String, Integer> userMap = new LinkedHashMap<>();

        dataMapActivity.callApi(userMap);

        assertSame(userMap, service.listByUserMap);

        FakeRecordService projectService = new FakeRecordService();
        ProjectDataMapActivity projectDataMapActivity = new ProjectDataMapActivity();
        projectDataMapActivity.recordService = projectService;
        Map<String, Integer> projectMap = new LinkedHashMap<>();

        projectDataMapActivity.callApi(projectMap);

        assertSame(projectMap, projectService.listByProjectMap);
        assertEquals(Integer.valueOf(project.getId().intValue()), projectMap.get("projectId"));
    }

    @Test
    public void uploadServiceBinderReturnsService() {
        ServiceController<UploadService> controller = Robolectric.buildService(UploadService.class).create();
        UploadService service = controller.get();

        IBinder binder = service.onBind(new Intent());

        assertSame(service, ((UploadService.LocalBinder) binder).getService());
    }

    public static class TestBaseActivity extends BaseActivity {
    }

    private static User user() {
        User user = new User();
        user.setUsername("citizen-user");
        user.setFullName("Citizen Scientist");
        user.setEmail("citizen@example.test");
        return user;
    }

    private static Project project() {
        SurveyQuestion question = new SurveyQuestion();
        question.setId(100L);
        question.setQuestion("Color?");
        question.setAttribute("color");
        question.setType(3);
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
        project.setCreatedAt(new java.util.Date(1_000L));
        project.setSections(Arrays.asList(section));
        return project;
    }

    private static class FakeRecordService implements RecordService {
        Map<String, Integer> listByUserMap;
        Map<String, Integer> listByProjectMap;

        @Override
        public Call<Response> upload(okhttp3.MultipartBody.Part model, okhttp3.MultipartBody.Part[] images, okhttp3.MultipartBody.Part survey) {
            return TestCalls.success(new Response());
        }

        @Override
        public Call<Response> listByUser(Map<String, Integer> map) {
            listByUserMap = map;
            return TestCalls.success(new Response());
        }

        @Override
        public Call<Response> listByProject(Map<String, Integer> map) {
            listByProjectMap = map;
            return TestCalls.success(new Response());
        }

        @Override
        public Call<Response> summary() {
            return TestCalls.success(new Response());
        }
    }
}
