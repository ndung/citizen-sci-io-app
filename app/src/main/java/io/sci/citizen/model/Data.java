package io.sci.citizen.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Data implements Serializable {

    private Long id;
    private String uuid;

    private Long projectId;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Date startDate;
    private Date finishDate;
    private boolean editMode;
    private boolean uploading;
    private boolean uploaded;
    private Date createdAt;
    private Map<Long,List<String>> imagePaths;
    private List<Image> images;
    private Map<Long, List<QuestionParameter>> survey;
    private List<SurveyResponse> surveyResponses;

    private Project project;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracy() { return accuracy; }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean isUploading() { return uploading; }

    public void setUploading(boolean uploading) { this.uploading = uploading; }

    public boolean isUploaded() { return uploaded; }

    public void setUploaded(boolean uploaded) { this.uploaded = uploaded; }

    public Date getCreatedAt() { return createdAt; }

    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Map<Long,List<String>> getImagePaths() { return imagePaths; }

    public void setImagePaths(Map<Long,List<String>> imagePaths) { this.imagePaths = imagePaths; }

    public Map<Long, List<QuestionParameter>> getSurvey() { return survey; }

    public void setSurvey(Map<Long, List<QuestionParameter>> survey) { this.survey = survey; }

    public List<Image> getImages() { return images; }

    public void setImages(List<Image> images) { this.images = images; }

    public List<SurveyResponse> getSurveyResponses() { return surveyResponses; }

    public void setSurveyResponses(List<SurveyResponse> surveyResponses) { this.surveyResponses = surveyResponses; }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public String toString() {
        return "Record{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", startDate=" + startDate +
                ", finishDate=" + finishDate +
                ", editMode=" + editMode +
                ", imagePaths=" + imagePaths +
                ", images=" + images +
                '}';
    }

}
