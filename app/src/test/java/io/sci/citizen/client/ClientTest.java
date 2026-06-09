package io.sci.citizen.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class ClientTest {

    private App app;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
        PreferenceManager.getDefaultSharedPreferences(app).edit().clear().commit();
    }

    @Test
    public void response_roundTripsDataAndMessage() {
        Response response = new Response();
        Object data = new Object();

        response.setData(data);
        response.setMessage("ok");

        assertSame(data, response.getData());
        assertEquals("ok", response.getMessage());
    }

    @Test
    public void addCookiesInterceptor_addsStoredCookiesToRequest() throws Exception {
        HashSet<String> cookies = new HashSet<>(Arrays.asList("session=abc", "theme=dark"));
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putStringSet(AddCookiesInterceptor.PREF_COOKIES, cookies)
                .commit();
        FakeChain chain = new FakeChain(new Request.Builder().url("https://example.test/").build());

        new AddCookiesInterceptor(app).intercept(chain);

        assertTrue(chain.proceededRequest.headers("Cookie").contains("session=abc"));
        assertTrue(chain.proceededRequest.headers("Cookie").contains("theme=dark"));
    }

    @Test
    public void receivedCookiesInterceptor_persistsSetCookieHeaders() throws Exception {
        FakeChain chain = new FakeChain(new Request.Builder().url("https://example.test/").build());
        chain.responseBuilder
                .addHeader("Set-Cookie", "session=abc")
                .addHeader("Set-Cookie", "theme=dark");

        new ReceivedCookiesInterceptor(app).intercept(chain);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        assertTrue(preferences.getStringSet("PREF_COOKIES", new HashSet<>()).contains("session=abc"));
        assertTrue(preferences.getStringSet("PREF_COOKIES", new HashSet<>()).contains("theme=dark"));
    }

    @Test
    public void apiUtils_createRetrofitServicesAndSetCommonErrorMessage() {
        app.memory().setLoginFlag(app, false);

        assertNotNull(ApiUtils.AuthService(app));
        assertEquals(app.getString(R.string.failed_check_your_internet_connection), ApiUtils.SOMETHING_WRONG);
        assertNotNull(ApiUtils.RecordService(app));
        assertNotNull(ApiUtils.SurveyService(app));
        assertNotNull(ApiUtils.ProjectService(app));
        assertEquals(App.API_URL, RetrofitClient.retrofit.baseUrl().toString());
    }

    @Test
    public void retrofitServiceAnnotations_buildExpectedRequests() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://example.test/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService auth = retrofit.create(AuthService.class);
        RecordService records = retrofit.create(RecordService.class);
        SurveyService surveys = retrofit.create(SurveyService.class);
        ProjectService projects = retrofit.create(ProjectService.class);

        RequestBody text = RequestBody.create(MediaType.parse("text/plain"), "citizen");
        MultipartBody.Part part = MultipartBody.Part.createFormData("model", "{}");
        MultipartBody.Part[] images = new MultipartBody.Part[]{MultipartBody.Part.createFormData("images", "a")};

        assertRequest(auth.checkPhoneNumber(text).request(), "POST", "/api/auth/check-credential-id");
        assertRequest(auth.createPassword(new java.util.HashMap<String, String>()).request(), "POST", "/api/auth/create-password");
        assertRequest(auth.signIn(new java.util.HashMap<String, String>()).request(), "POST", "/api/auth/sign-in");
        assertRequest(auth.signUp(new java.util.HashMap<String, String>()).request(), "POST", "/api/auth/sign-up");
        assertRequest(auth.updateProfile(new java.util.HashMap<String, String>()).request(), "POST", "/api/user/update-profile");
        assertRequest(auth.changePassword(new java.util.HashMap<String, String>()).request(), "POST", "/api/user/change-pwd");
        assertRequest(records.upload(part, images, part).request(), "POST", "/api/record/upload");
        assertRequest(records.listByUser(new java.util.HashMap<String, Integer>()).request(), "POST", "/api/record/list-by-user");
        assertRequest(records.listByProject(new java.util.HashMap<String, Integer>()).request(), "POST", "/api/record/list-by-project");
        assertRequest(records.summary().request(), "GET", "/api/record/user-summary");
        assertEquals("3", surveys.questionnaires("3").request().url().queryParameter("version"));
        assertRequest(surveys.questionnaires("3").request(), "GET", "/api/survey/list/");
        assertRequest(surveys.upload(part).request(), "POST", "/api/survey/upload/");
        assertRequest(projects.projects().request(), "GET", "/api/projects");
    }

    private static void assertRequest(Request request, String method, String encodedPath) {
        assertEquals(method, request.method());
        assertEquals(encodedPath, request.url().encodedPath());
    }

    private static class FakeChain implements Interceptor.Chain {
        private final Request request;
        private final okhttp3.Call call;
        private Request proceededRequest;
        private okhttp3.Response.Builder responseBuilder;

        FakeChain(Request request) {
            this.request = request;
            this.call = new FakeOkHttpCall(request);
            this.responseBuilder = new okhttp3.Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create("", MediaType.parse("text/plain")));
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public okhttp3.Response proceed(Request request) {
            proceededRequest = request;
            return responseBuilder.request(request).build();
        }

        @Override
        public Connection connection() {
            return null;
        }

        @Override
        public Call call() {
            return call;
        }

        @Override
        public int connectTimeoutMillis() {
            return 1;
        }

        @Override
        public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int readTimeoutMillis() {
            return 1;
        }

        @Override
        public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int writeTimeoutMillis() {
            return 1;
        }

        @Override
        public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
            return this;
        }
    }

    private static class FakeOkHttpCall implements okhttp3.Call {
        private final Request request;
        private boolean canceled;

        FakeOkHttpCall(Request request) {
            this.request = request;
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public okhttp3.Response execute() throws IOException {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public void enqueue(Callback responseCallback) {
            throw new UnsupportedOperationException("Not used in tests");
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Timeout timeout() {
            return new Timeout();
        }

        @Override
        public okhttp3.Call clone() {
            return new FakeOkHttpCall(request);
        }
    }
}
