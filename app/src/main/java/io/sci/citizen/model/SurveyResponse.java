package io.sci.citizen.model;

import java.util.Date;

public class SurveyResponse {
    private Long id;
    private SurveyQuestion question;
    private String response;
    private Date responseDateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SurveyQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SurveyQuestion question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Date getResponseDateTime() {
        return responseDateTime;
    }

    public void setResponseDateTime(Date responseDateTime) {
        this.responseDateTime = responseDateTime;
    }
}
