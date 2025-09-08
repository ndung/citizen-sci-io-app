package io.sci.citizen.model;

import java.io.Serializable;
import java.util.Date;

public class Image implements Serializable {

    private Long id;
    private String uuid;
    private Section section;
    private Date createAt;

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

    public Section getSection() { return section; }

    public void setSection(Section section) { this.section = section; }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }
}
