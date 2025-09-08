package io.sci.citizen.model;

import com.google.android.gms.common.internal.Objects;

import java.io.Serializable;

public class QuestionParameter implements Serializable {

    private Long id;
    private Integer sequence;
    private String description;

    public QuestionParameter(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() { return sequence; }

    public void setSequence(Integer sequence) { this.sequence = sequence; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "QuestionParameter{" +
                "id=" + id +
                ", sequence='" + sequence +
                ", description='" + description +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionParameter parameter = (QuestionParameter) o;
        return Objects.equal(id, parameter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
