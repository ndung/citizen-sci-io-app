package io.sci.citizen.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Date;

import io.sci.citizen.ActivitySmokeTest;
import io.sci.citizen.App;
import io.sci.citizen.BuildConfig;
import io.sci.citizen.R;
import io.sci.citizen.model.Data;
import io.sci.citizen.model.Project;
import io.sci.citizen.model.QuestionParameter;
import io.sci.citizen.model.SurveyQuestion;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = App.class)
public class AdapterTest {

    private App app;
    private ActivitySmokeTest.TestBaseActivity activity;

    @Before
    public void setUp() {
        app = (App) ApplicationProvider.getApplicationContext();
        app.onCreate();
        app.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).edit().clear().commit();
        activity = Robolectric.buildActivity(ActivitySmokeTest.TestBaseActivity.class).setup().get();
    }

    @Test
    public void projectAdapter_addsClearsBindsAndDispatchesClicks() {
        final Project[] clicked = new Project[1];
        ProjectAdapter adapter = new ProjectAdapter(activity, project -> clicked[0] = project);
        Project project = new Project();
        project.setId(1L);
        project.setName("River");
        project.setDescription("River observations");
        project.setIconUrl("");
        project.setCreatedAt(new Date(1_000L));

        adapter.add(project, 0);
        ProjectAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent(), 0);
        adapter.onBindViewHolder(holder, 0);
        holder.cardView.performClick();

        assertEquals(1, adapter.getItemCount());
        assertEquals("River", holder.line1.getText().toString());
        assertEquals("River observations", holder.line2.getText().toString());
        assertTrue(holder.line3.getText().toString().contains("Created at:"));
        assertSame(project, clicked[0]);

        adapter.clear();
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void recordAdapter_bindsStatusAndDispatchesClicks() {
        final StringBuilder events = new StringBuilder();
        RecordAdapter adapter = new RecordAdapter(activity, new RecordAdapter.OnItemClickListener() {
            @Override public void onEdit(Data data) { events.append("edit:").append(data.getUuid()).append(";"); }
            @Override public void onUpload(Data data) { events.append("upload:").append(data.getUuid()).append(";"); }
            @Override public void onDelete(Data data) { events.append("delete:").append(data.getUuid()).append(";"); }
            @Override public void stopUploading(Data data) { events.append("stop:").append(data.getUuid()).append(";"); }
        });
        Data data = new Data();
        data.setUuid("data-1");
        data.setStartDate(new Date(1_000L));
        data.setImagePaths(new java.util.LinkedHashMap<>());

        adapter.add(data, 0);
        RecordAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent(), 0);
        adapter.onBindViewHolder(holder, 0);
        holder.editButton.performClick();
        holder.uploadButton.performClick();
        holder.deleteButton.performClick();

        assertEquals("uuid:data-1", holder.line1.getText().toString());
        assertTrue(holder.line3.getText().toString().contains(activity.getString(R.string.not_uploaded_yet)));
        assertEquals("edit:data-1;upload:data-1;delete:data-1;", events.toString());

        data.setUploading(true);
        adapter.onBindViewHolder(holder, 0);
        holder.editButton.performClick();
        holder.deleteButton.performClick();
        holder.uploadingPb.performClick();

        assertEquals(View.GONE, holder.uploadButton.getVisibility());
        assertEquals(View.VISIBLE, holder.uploadingPb.getVisibility());
        assertEquals("edit:data-1;upload:data-1;delete:data-1;stop:data-1;", events.toString());

        adapter.clear();
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void sliderImageAdapter_managesImageUris() {
        SliderImageAdapter adapter = new SliderImageAdapter(activity);
        Uri first = Uri.parse("file:///tmp/first.jpg");
        Uri second = Uri.parse("file:///tmp/second.jpg");

        adapter.add(first);
        adapter.add(second);
        adapter.removeAt(-1);
        adapter.removeAt(2);

        assertEquals(Arrays.asList(first, second), adapter.getData());
        assertEquals(2, adapter.getItemCount());

        adapter.removeAt(0);
        assertEquals(Arrays.asList(second), adapter.getData());

        adapter.removeAll();
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void surveyAdapter_textEntryStoresAnswerInMemory() {
        SurveyQuestion question = new SurveyQuestion();
        question.setId(50L);
        question.setAttribute("notes");
        question.setQuestion("Notes?");
        question.setType(3);
        QuestionParameter option = new QuestionParameter();
        option.setId(1L);
        option.setDescription("unused");
        question.setOptions(Arrays.asList(option));
        SurveyAdapter adapter = new SurveyAdapter(activity, Arrays.asList(question));
        SurveyAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent(), 0);

        adapter.onBindViewHolder(holder, 0);
        holder.getEditText().setText("murky");

        assertEquals(1, adapter.getItemCount());
        assertEquals("murky", app.memory().getSurveyAnswers(50L).get(0).getDescription());
    }

    private RecyclerView parent() {
        RecyclerView recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        return recyclerView;
    }
}
