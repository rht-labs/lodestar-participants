package com.redhat.labs.lodestar.participants.rest.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.Map;

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

    @ConfigProperty(name = "legacy.engagement.file")
    String engagementFile;

    @ConfigProperty(name = "user.management.file")
    String userManagementFormat;

    @Inject
    ParticipantService participantService;

    @Inject
    JsonMarshaller json;

    GitLabApi gitlabApi;

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @PostConstruct
    void setupGitlabClient() {
        gitUrl = gitUrl.trim();
        pat = pat.trim();

        LOGGER.info("Using git {}", gitUrl);

        gitlabApi = new GitLabApi(gitUrl, pat);
        gitlabApi.enableRequestResponseLogging();
    }

    public List<Participant> getParticipants(Integer projectId) {
        RepositoryFile file = getFile(projectId, participantFile, true);

        if(file == null) {
            return Collections.emptyList();
        }

        return json.fromJsonList(file.getDecodedContentAsString());
    }

    public void updateParticipants(GitLabCommit commit, List<Participant> participants) {
        String participantContent = json.toJson(participants);

        List<CommitAction> commitFiles = new ArrayList<>();

        CommitAction action = new CommitAction()
                .withAction(CommitAction.Action.UPDATE)
                .withFilePath(participantFile)
                .withContent(participantContent);

        commitFiles.add(action);

        commitFiles.add(createLegacyJson(commit.getProjectId(), participantContent));

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
        if(getFile(projectId, filePath, false) == null) {

            String resetContent = json.toJson(reset);
            CommitAction resetAction = new CommitAction().withAction(CommitAction.Action.CREATE)
                    .withFilePath(filePath).withContent(resetContent);
            commitFiles.add(resetAction);
        }
    }

    private CommitAction createLegacyJson(int projectId, String partipantsJson) {

        RepositoryFile legacyEngagementFile = getFile(projectId, "engagement.json", true);

        JsonElement element = gson.fromJson(legacyEngagementFile.getDecodedContentAsString(), JsonElement.class);
        JsonObject engagement = element.getAsJsonObject();

        element = gson.fromJson(partipantsJson, JsonElement.class);

        engagement.add("engagement_users", element);
        JsonObject sorted = new JsonObject();
        engagement.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(es -> sorted.add(es.getKey(), es.getValue()));

        String legacyContent = gson.toJson(sorted);

        return new CommitAction()
                .withAction(CommitAction.Action.UPDATE)
                .withFilePath(engagementFile)
                .withContent(legacyContent);
    }

    private RepositoryFile getFile(int projectId, String filePath, boolean includeContent) {
        try {
            return gitlabApi.getRepositoryFileApi().getFile(projectId, filePath, branch, includeContent);
        } catch (GitLabApiException e) {
            if(e.getHttpStatus() == 404) {
                LOGGER.debug("Could find not file {} for project {}", filePath, projectId);
                return null;
            }
            String message = String.format("File %s not retrieved for project %s. Status %s, Reason %s", filePath,
                    projectId, e.getHttpStatus(), e.getReason());
            throw new ParticipantException(message, e);
        }
    }

}
