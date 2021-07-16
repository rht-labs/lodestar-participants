package com.redhat.labs.lodestar.participants.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = { @Index(columnList = "projectId"), @Index(columnList = "engagementUuid") })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Participant {

    @Id
    String uuid;
    @Column(nullable = false)
    Long projectId;
    @Column(nullable = false)
    String engagementUuid;
    @Column(nullable = false)
    @JsonProperty("first_name")
    String firstName;
    @Column(nullable = false)
    @JsonProperty("last_name")
    String lastName;
    @Column(nullable = false)
    String email;
    @Column(nullable = false)
    String role;
    @Column(nullable = false)
    String organization;

    /**
     * Inverse of isSame
     * @param other
     * @return
     */
    public boolean isDifferent(Participant other) {
        return !isSame(other);
    }

    /**
     * Checks for matching firstName, lastName, email and role. If both fields are null that is a match
     * @param other
     * @return
     */
    public boolean isSame(Participant other) {
        return equalsOrNull(this.firstName, other.firstName) && equalsOrNull(this.lastName, other.lastName)
                && equalsOrNull(this.email, other.email) && equalsOrNull(this.role, other.role);

    }

    /**
     * Checks if the fields are equal (either both null or both string equals). 
     * @param thiz this object
     * @param that comparison field
     * @return
     */
    private boolean equalsOrNull(String thiz, String that) {
        // both null
        if (thiz == null && that == null) {
            return true;
        }

        // if thiz null -> that must be not null otherwise compare
        return thiz == null || that.contentEquals(thiz);

    }

}
