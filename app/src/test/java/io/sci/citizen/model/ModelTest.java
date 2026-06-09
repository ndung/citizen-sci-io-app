package io.sci.citizen.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelTest {

    @Test
    public void data_roundTripsEveryProperty() {
        Date start = new Date(1_000L);
        Date finish = new Date(2_000L);
        Date created = new Date(3_000L);
        Project project = new Project();
        project.setId(10L);
        project.setName("Water Watch");
        Image image = new Image();
        SurveyResponse response = new SurveyResponse();
        QuestionParameter parameter = new QuestionParameter();
        parameter.setId(5L);
        Map<Long, List<String>> imagePaths = new LinkedHashMap<>();
        imagePaths.put(10L, Arrays.asList("file:///tmp/a.jpg"));
        Map<Long, List<QuestionParameter>> survey = new LinkedHashMap<>();
        survey.put(20L, Arrays.asList(parameter));

        Data data = new Data();
        data.setId(1L);
        data.setUuid("uuid-1");
        data.setProjectId(10L);
        data.setLatitude(-6.2d);
        data.setLongitude(106.8d);
        data.setAccuracy(3.5d);
        data.setStartDate(start);
        data.setFinishDate(finish);
        data.setEditMode(true);
        data.setUploading(true);
        data.setUploaded(true);
        data.setCreatedAt(created);
        data.setImagePaths(imagePaths);
        data.setSurvey(survey);
        data.setImages(Arrays.asList(image));
        data.setSurveyResponses(Arrays.asList(response));
        data.setProject(project);
        data.setDetails("clear water");

        assertEquals(Long.valueOf(1L), data.getId());
        assertEquals("uuid-1", data.getUuid());
        assertEquals(Long.valueOf(10L), data.getProjectId());
        assertEquals(Double.valueOf(-6.2d), data.getLatitude());
        assertEquals(Double.valueOf(106.8d), data.getLongitude());
        assertEquals(Double.valueOf(3.5d), data.getAccuracy());
        assertSame(start, data.getStartDate());
        assertSame(finish, data.getFinishDate());
        assertTrue(data.isEditMode());
        assertTrue(data.isUploading());
        assertTrue(data.isUploaded());
        assertSame(created, data.getCreatedAt());
        assertSame(imagePaths, data.getImagePaths());
        assertSame(survey, data.getSurvey());
        assertSame(image, data.getImages().get(0));
        assertSame(response, data.getSurveyResponses().get(0));
        assertSame(project, data.getProject());
        assertEquals("clear water", data.getDetails());
        assertTrue(data.toString().contains("Record{"));
        assertTrue(data.toString().contains("latitude=-6.2"));
    }

    @Test
    public void simpleModels_roundTripProperties() {
        Date created = new Date(5_000L);
        Section section = new Section();
        section.setId(7L);
        section.setSequence(2);
        section.setType("survey");
        section.setName("Basics");
        section.setStatus(1);

        SurveyQuestion question = new SurveyQuestion();
        question.setId(11L);
        question.setAttribute("color");
        question.setSection(section);
        question.setQuestion("Water color?");
        question.setType(2);
        question.setRequired(true);
        section.setQuestions(Arrays.asList(question));

        Project project = new Project();
        project.setId(3L);
        project.setName("River");
        project.setIconUrl("https://example.test/icon.png");
        project.setDescription("River observations");
        project.setCreatedAt(created);
        project.setSections(Arrays.asList(section));

        Image image = new Image();
        image.setId(4L);
        image.setUuid("img-1");
        image.setSection(section);
        image.setCreateAt(created);

        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setId(6L);
        surveyResponse.setQuestion(question);
        surveyResponse.setResponse("brown");
        surveyResponse.setResponseDateTime(created);

        DataSummary summary = new DataSummary();
        summary.setRank("gold");
        summary.setUploaded(8);
        summary.setVerified(5);
        summary.setTotal(13);

        User user = new User();
        user.setId(99L);
        user.setUsername("citizen");
        user.setFullName("Citizen Scientist");
        user.setStatus(1);
        user.setEmail("citizen@example.test");
        user.setGender("Female");
        user.setAddress("Jakarta");
        user.setPostalCode("12345");
        user.setProfession("Researcher");
        user.setEducation("S1");

        Map<Long, Project> projects = new LinkedHashMap<>();
        projects.put(project.getId(), project);
        LoginDetails details = new LoginDetails();
        details.setUser(user);
        details.setProjects(projects);

        assertEquals(Long.valueOf(7L), section.getId());
        assertEquals(2, section.getSequence());
        assertEquals("survey", section.getType());
        assertEquals("Basics", section.getName());
        assertEquals(Integer.valueOf(1), section.getStatus());
        assertSame(question, section.getQuestions().get(0));
        assertEquals(Long.valueOf(3L), project.getId());
        assertEquals("River", project.getName());
        assertEquals("https://example.test/icon.png", project.getIconUrl());
        assertEquals("River observations", project.getDescription());
        assertSame(created, project.getCreatedAt());
        assertSame(section, project.getSections().get(0));
        assertEquals(Long.valueOf(4L), image.getId());
        assertEquals("img-1", image.getUuid());
        assertSame(section, image.getSection());
        assertSame(created, image.getCreateAt());
        assertEquals(Long.valueOf(6L), surveyResponse.getId());
        assertSame(question, surveyResponse.getQuestion());
        assertEquals("brown", surveyResponse.getResponse());
        assertSame(created, surveyResponse.getResponseDateTime());
        assertEquals("gold", summary.getRank());
        assertEquals(8, summary.getUploaded());
        assertEquals(5, summary.getVerified());
        assertEquals(13, summary.getTotal());
        assertEquals(99L, user.getId());
        assertEquals("citizen", user.getUsername());
        assertEquals("Citizen Scientist", user.getFullName());
        assertEquals(1, user.getStatus());
        assertEquals("citizen@example.test", user.getEmail());
        assertEquals("Female", user.getGender());
        assertEquals("Jakarta", user.getAddress());
        assertEquals("12345", user.getPostalCode());
        assertEquals("Researcher", user.getProfession());
        assertEquals("S1", user.getEducation());
        assertTrue(user.toString().contains("citizen"));
        assertSame(user, details.getUser());
        assertSame(projects, details.getProjects());
    }

    @Test
    public void questionParameter_equalityAndHashCodeUseId() {
        QuestionParameter first = new QuestionParameter();
        first.setId(1L);
        first.setSequence(1);
        first.setDescription("yes");
        QuestionParameter sameId = new QuestionParameter();
        sameId.setId(1L);
        sameId.setSequence(2);
        sameId.setDescription("no");
        QuestionParameter different = new QuestionParameter();
        different.setId(2L);

        assertEquals(first, sameId);
        assertEquals(first.hashCode(), sameId.hashCode());
        assertFalse(first.equals(different));
        assertFalse(first.equals("not a parameter"));
        assertTrue(first.toString().contains("yes"));
    }

    @Test
    public void surveyQuestion_compareToUsesId() {
        SurveyQuestion low = new SurveyQuestion();
        low.setId(1L);
        low.setAttribute("low");
        low.setQuestion("Low?");
        low.setType(3);
        low.setRequired(false);
        QuestionParameter option = new QuestionParameter();
        option.setId(7L);
        low.setOptions(Arrays.asList(option));

        SurveyQuestion high = new SurveyQuestion();
        high.setId(2L);

        assertTrue(low.compareTo(high) < 0);
        assertTrue(high.compareTo(low) > 0);
        assertEquals(0, low.compareTo(low));
        assertEquals("low", low.getAttribute());
        assertEquals("Low?", low.getQuestion());
        assertEquals(Integer.valueOf(3), low.getType());
        assertFalse(low.isRequired());
        assertSame(option, low.getOptions().get(0));
        assertTrue(low.toString().contains("Low?"));
    }
}
