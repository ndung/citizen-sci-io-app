package io.sci.citizen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.io.IOException;
import java.util.function.Supplier;

import io.sci.citizen.client.RecordService;
import io.sci.citizen.client.Response;
import io.sci.citizen.model.DataSummary;
import io.sci.citizen.model.User;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AccountActivityTest {

    private App app;
    private User user;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        user = new User();
        user.setUsername("citizen-user");
        user.setFullName("Citizen Scientist");
        user.setEmail("citizen@example.com");
        app.memory().setUser(app, user);
    }

    @After
    public void tearDown() {
        TestAccountActivity.recordServiceSupplier = null;
    }

    @Test
    public void refresh_populatesUserInformation_andSummary() {
        DataSummary summary = new DataSummary();
        summary.setUploaded(7);
        summary.setVerified(4);
        TestAccountActivity.recordServiceSupplier = () -> createRecordService(summary);

        TestAccountActivity activity = Robolectric.buildActivity(TestAccountActivity.class)
                .setup()
                .get();

        TextView username = activity.findViewById(R.id.tv_username);
        TextView name = activity.findViewById(R.id.tv_name);
        TextView email = activity.findViewById(R.id.tv_email);
        TextView records = activity.findViewById(R.id.tv_records);

        assertEquals(user.getUsername(), username.getText().toString());
        assertEquals(user.getFullName(), name.getText().toString());
        assertEquals(user.getEmail(), email.getText().toString());
        assertEquals("7/4", records.getText().toString());
    }

    @Test
    public void clickingSubmitted_opensDataMapActivity() {
        DataSummary summary = new DataSummary();
        summary.setUploaded(1);
        summary.setVerified(1);
        TestAccountActivity.recordServiceSupplier = () -> createRecordService(summary);

        TestAccountActivity activity = Robolectric.buildActivity(TestAccountActivity.class)
                .setup()
                .get();

        activity.findViewById(R.id.ll_records).performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(DataMapActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    private static RecordService createRecordService(DataSummary summary) {
        Response response = new Response();
        response.setData(summary);
        response.setMessage("ok");
        FakeRecordService.CallFactory factory = () -> ImmediateCall.success(response);
        return new FakeRecordService(factory);
    }

    public static class TestAccountActivity extends AccountActivity {
        static Supplier<RecordService> recordServiceSupplier;

        @Override
        RecordService createRecordService() {
            if (recordServiceSupplier != null) {
                return recordServiceSupplier.get();
            }
            return super.createRecordService();
        }
    }

    private static class FakeRecordService implements RecordService {

        interface CallFactory {
            Call<Response> create();
        }

        private final CallFactory summaryCallFactory;

        FakeRecordService(CallFactory summaryCallFactory) {
            this.summaryCallFactory = summaryCallFactory;
        }

        @Override
        public Call<Response> upload(okhttp3.MultipartBody.Part model, okhttp3.MultipartBody.Part[] images, okhttp3.MultipartBody.Part survey) {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public Call<Response> listByUser(java.util.Map<String, Integer> map) {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public Call<Response> listByProject(java.util.Map<String, Integer> map) {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public Call<Response> summary() {
            return summaryCallFactory.create();
        }
    }

    private static class ImmediateCall implements Call<Response> {

        private final retrofit2.Response<Response> response;
        private final Throwable error;
        private boolean executed;
        private boolean canceled;

        private ImmediateCall(retrofit2.Response<Response> response, Throwable error) {
            this.response = response;
            this.error = error;
        }

        static ImmediateCall success(Response response) {
            return new ImmediateCall(retrofit2.Response.success(response), null);
        }

        @Override
        public retrofit2.Response<Response> execute() throws IOException {
            executed = true;
            if (error != null) {
                throw new IOException(error);
            }
            return response;
        }

        @Override
        public void enqueue(Callback<Response> callback) {
            executed = true;
            if (error != null) {
                callback.onFailure(this, error);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override
        public boolean isExecuted() {
            return executed;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<Response> clone() {
            return new ImmediateCall(response, error);
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }

        @Override
        public Timeout timeout() {
            return new Timeout();
        }
    }
}
