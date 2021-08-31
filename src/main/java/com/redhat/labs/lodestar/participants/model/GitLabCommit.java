package com.redhat.labs.lodestar.participants.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitLabCommit {

    private String authorEmail;
    private String authorName;
    private String engagementUuid;
    private int projectId;
    private String commitMessage;
    private boolean updateRequired;

    @Builder.Default
    private Set<Participant> resetParticipants = new HashSet<>();

}
