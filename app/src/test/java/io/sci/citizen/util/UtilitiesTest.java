package io.sci.citizen.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.User;
import okhttp3.MultipartBody;
import okio.Buffer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class UtilitiesTest {

    private App app;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void stringUtils_validatesEmptyEmailAndPasswordValues() {
        assertTrue(StringUtils.isNullOrEmpty(null));
        assertTrue(StringUtils.isNullOrEmpty(""));
        assertTrue(StringUtils.isNullOrEmpty("-"));
        assertFalse(StringUtils.isNullOrEmpty("value"));

        assertTrue(StringUtils.isEmailValid("citizen@example.com"));
        assertFalse(StringUtils.isEmailValid("not-an-email"));
        assertTrue(StringUtils.isPasswordValid("123456"));
        assertFalse(StringUtils.isPasswordValid("12345"));
    }

    @Test
    public void sequence_returnsIncreasingValues() {
        int first = Sequence.nextValue();
        int second = Sequence.nextValue();

        assertEquals(first + 1, second);
    }

    @Test
    public void chipperUtils_hashesMd5AndPublicKey() {
        String passwordHash = ChipperUtils.getMD5Hash("secret");
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String expectedPublicKey = ChipperUtils.getMD5Hash("card-1" + passwordHash + today + "hpc-lab");

        assertEquals("5ebe2294ecd0e0f08eab7690d2a6ee69", passwordHash);
        assertEquals(expectedPublicKey, ChipperUtils.getPublicKey("card-1", "secret"));
        assertEquals(32, expectedPublicKey.length());
    }

    @Test
    public void gsonDeserializer_parsesIsoDateWithColonOffset() throws Exception {
        GsonDeserializer deserializer = new GsonDeserializer();
        Date expected = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .parse("2025-09-08T10:15:30.000+0700");

        Date actual = deserializer.deserialize(
                new com.google.gson.JsonPrimitive("2025-09-08T10:15:30.000+07:00"),
                Date.class,
                null);

        assertEquals(expected, actual);
    }

    @Test
    public void memory_persistsUserProjectDataAndSurveyState() {
        Memory memory = app.memory();
        Project project = project(9L);
        User user = new User();
        user.setUsername("citizen");

        Map<Long, Project> projects = new LinkedHashMap<>();
        projects.put(project.getId(), project);
        memory.setUser(app, user);
        memory.setToken(app, "token-1");
        memory.setLoginFlag(app, true);
        memory.setProjects(projects);
        memory.setProject(project);

        QuestionParameter answer = new QuestionParameter();
        answer.setId(1L);
        answer.setDescription("yes");
        memory.putSurveyAnswers(50L, Arrays.asList(answer));

        Data current = new Data();
        current.setUuid("data-1");
        current.setProjectId(project.getId());
        current.setStartDate(new Date(1_000L));
        memory.setData(current);
        memory.putData("stored-1", current);

        assertEquals("citizen", memory.getUser(app).getUsername());
        assertEquals("token-1", memory.getToken(app));
        assertTrue(memory.getLoginFlag(app));
        assertEquals("River", memory.project().getName());
        assertEquals("River", memory.getProjects().get(9L).getName());
        assertEquals("yes", memory.getSurveyAnswers(50L).get(0).getDescription());
        assertEquals("data-1", memory.getData().getUuid());
        assertEquals("data-1", memory.getData("stored-1").getUuid());

        Data saved = memory.saveData();

        assertNotNull(saved.getFinishDate());
        assertNull(memory.getData());
        assertTrue(memory.getSurveyAnswers().isEmpty());
        assertEquals("data-1", memory.getData("data-1").getUuid());

        memory.deleteData("data-1");
        assertNull(memory.getData("data-1"));
        assertNotNull(memory.removeData());
        assertTrue(memory.getDataMap().isEmpty());
        memory.setProject(null);
        assertNull(memory.project());
    }

    @Test
    public void uriAndMultipartUtils_handleFileUris() throws Exception {
        File file = new File(app.getCacheDir(), "photo.txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("hello".getBytes());
        }
        Uri uri = Uri.fromFile(file);

        assertEquals(file.getAbsoluteFile(), UriUtils.copyToCache(app, uri).getAbsoluteFile());

        MultipartBody.Part part = MultipartUtils.uriToPart(app, "section-a", uri, "images");
        Buffer buffer = new Buffer();
        part.body().writeTo(buffer);

        assertTrue(part.headers().get("Content-Disposition").contains("name=\"images\""));
        assertTrue(part.headers().get("Content-Disposition").contains("section-a-photo.txt"));
        assertEquals("hello", buffer.readUtf8());

        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("note", "clear");
        MultipartBody body = MultipartUtils.buildBody(app, "section-a", uri, "images", extra);
        assertEquals(2, body.parts().size());
    }

    @Test
    public void notificationUtil_handlesEmptyMessagesAndTimestamps() throws Exception {
        long expected = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .parse("2025-09-08 10:15:30").getTime();
        NotificationUtil notificationUtil = new NotificationUtil(app);

        assertEquals(expected, NotificationUtil.getTimeMilliSec("2025-09-08 10:15:30"));
        assertEquals(0L, NotificationUtil.getTimeMilliSec("bad timestamp"));
        assertNull(notificationUtil.showNotificationMessage("Title", "", "2025-09-08 10:15:30", new android.content.Intent()));
        assertNull(notificationUtil.getBitmapFromURL("not a url"));
        NotificationUtil.clearNotifications();
    }

    @Test
    public void locationAssistant_acceptsRealLocationsAndReportsThemToListener() {
        RecordingLocationListener listener = new RecordingLocationListener();
        LocationAssistant assistant = new LocationAssistant(app, listener, LocationAssistant.Accuracy.LOW, 1_000L, true);
        Location location = new Location("gps");
        location.setLatitude(-6.2d);
        location.setLongitude(106.8d);
        location.setAccuracy(5f);

        assistant.setQuiet(true);
        assistant.setVerbose(true);
        assertFalse(assistant.onPermissionsUpdated(999, new int[0]));
        assistant.onLocationChanged(location);
        assistant.stop();
        assistant.unregister();

        assertSame(location, assistant.getBestLocation());
        assertSame(location, listener.location);
        assertFalse(assistant.checkLocationAvailability());
    }

    private static Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("River");
        project.setIconUrl("");
        return project;
    }

    private static class RecordingLocationListener implements LocationAssistant.Listener {
        Location location;

        @Override public void onNeedLocationPermission() { }
        @Override public void onExplainLocationPermission() { }
        @Override public void onLocationPermissionPermanentlyDeclined(android.view.View.OnClickListener fromView, android.content.DialogInterface.OnClickListener fromDialog) { }
        @Override public void onNeedLocationSettingsChange() { }
        @Override public void onFallBackToSystemSettings(android.view.View.OnClickListener fromView, android.content.DialogInterface.OnClickListener fromDialog) { }
        @Override public void onNewLocationAvailable(Location location) { this.location = location; }
        @Override public void onMockLocationsDetected(android.view.View.OnClickListener fromView, android.content.DialogInterface.OnClickListener fromDialog) { }
        @Override public void onError(LocationAssistant.ErrorType type, String message) { }
    }
}
