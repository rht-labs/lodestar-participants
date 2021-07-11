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
@Table(indexes = {
        @Index(columnList = "projectId"), @Index(columnList = "engagementUuid")
})
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

}
