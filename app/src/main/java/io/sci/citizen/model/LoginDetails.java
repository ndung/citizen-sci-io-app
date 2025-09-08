package io.sci.citizen.model;

import java.util.Map;

public class LoginDetails {
    private User user;
    private Map<Long,Project> projects;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Map<Long,Project> getProjects() {
        return projects;
    }

    public void setProjects(Map<Long,Project> projects) {
        this.projects = projects;
    }
}
