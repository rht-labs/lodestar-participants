package com.redhat.labs.lodestar.participants.rest.client;

import com.redhat.labs.lodestar.participants.exception.ParticipantException;
import com.redhat.labs.lodestar.participants.model.GitLabCommit;
import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.service.ParticipantService;
import com.redhat.labs.lodestar.participants.utils.JsonMarshaller;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class GitlabApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabApiClient.class);

    @ConfigProperty(name = "default.branch")
    String branch;

    @ConfigProperty(name = "gitlab4j.api.url")
    String gitUrl;

    @ConfigProperty(name = "gitlab.personal.access.token")
    String pat;

    @ConfigProperty(name = "participant.file")
    String participantFile;

    @ConfigProperty(name = "user.management.file")
    String userManagementFormat;

    @Inject
    ParticipantService participantService;

    @Inject
    JsonMarshaller json;

    GitLabApi gitlabApi;

    @PostConstruct
    void setupGitlabClient() {
        gitlabApi = new GitLabApi(gitUrl, pat);
        gitlabApi.enableRequestResponseLogging();
    }

    public List<Participant> getParticipants(Integer projectId) {
        try {
            RepositoryFile file = gitlabApi.getRepositoryFileApi().getFile(projectId, participantFile, branch);
            return json.fromJsonList(file.getDecodedContentAsString());
        } catch (GitLabApiException e) {
            if(e.getHttpStatus() == 404) {
                LOGGER.debug("Could find not file {} for project {}", participantFile, projectId);
                return Collections.emptyList();
            }
            String message = String.format("Participant file not retrieved for project %s. Status %s, Reason %s",
                    projectId, e.getHttpStatus(), e.getReason());
            throw new ParticipantException(message, e);
        }
    }

    public void updateParticipants(GitLabCommit commit) {
        List<Participant> participants = participantService.getParticipants(commit.getEngagementUuid());

        String participantContent = json.toJson(participants);

        List<CommitAction> commitFiles = new ArrayList<>();

        CommitAction action = new CommitAction()
                .withAction(CommitAction.Action.UPDATE)
                .withFilePath(participantFile)
                .withContent(participantContent);

        commitFiles.add(action);

        if(!commit.getResetParticipants().isEmpty()) { //Count users who already have a reset pending (file already exists)
            String updateMessage = String.format("%s \n Reset %d user(s)", commit.getCommitMessage(),
                    commit.getResetParticipants().size());
            commit.setCommitMessage(updateMessage);
        }

        for(Participant reset : commit.getResetParticipants()) {
            this.createResetAction(reset, commit.getProjectId(), commitFiles);
        }

        commit(commitFiles, commit);
    }

    public void resetParticipants(GitLabCommit commit) {
        List<CommitAction> commitFiles = new ArrayList<>();
        String prefix = String.format("Reset Users (%s): ", commit.getResetParticipants().size());
        StringBuilder message = new StringBuilder(prefix);

        for(Participant reset : commit.getResetParticipants()) {
            this.createResetAction(reset, commit.getProjectId(), commitFiles);
            message.append(reset.getEmail());
            message.append("\n");
        }

        commit.setCommitMessage(message.toString());

        if(!commitFiles.isEmpty()) {
            commit(commitFiles, commit);
        }
    }

    private void commit(List<CommitAction> commitFiles, GitLabCommit commit) {
        CommitPayload payload = new CommitPayload()
                .withBranch(branch)
                .withCommitMessage(commit.getCommitMessage())
                .withAuthorEmail(commit.getAuthorEmail())
                .withAuthorName(commit.getAuthorName())
                .withActions(commitFiles);

        try {
            gitlabApi.getCommitsApi().createCommit(commit.getProjectId(), payload);
            LOGGER.debug("Participant commit successful {}", commit.getProjectId());
        } catch (GitLabApiException e) {
            throw new ParticipantException(String.format("status code %s reason %s", e.getHttpStatus(), e.getReason()), e);
        }
    }

    private void createResetAction(Participant reset, int projectId, List<CommitAction> commitFiles) {
        String filePath = String.format(userManagementFormat, reset.getUuid());
        try { //200 response means it already exists
            gitlabApi.getRepositoryFileApi().getFile(projectId, filePath, branch, false);
        } catch (GitLabApiException e) {
            if(e.getHttpStatus() == 404) {
                String resetContent = json.toJson(reset);
                CommitAction resetAction = new CommitAction().withAction(CommitAction.Action.CREATE)
                        .withFilePath(filePath).withContent(resetContent);
                commitFiles.add(resetAction);
            }
        }
    }
}
