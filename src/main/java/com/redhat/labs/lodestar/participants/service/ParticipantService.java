package com.redhat.labs.lodestar.participants.service;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.participants.model.Engagement;
import com.redhat.labs.lodestar.participants.model.GitlabFile;
import com.redhat.labs.lodestar.participants.model.GitlabProject;
import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.participants.rest.client.GitlabRestClient;
import com.redhat.labs.lodestar.participants.utils.JsonMarshaller;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class ParticipantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

    private static final String ENGAGEMENT_UUID = "engagementUuid";
    private static final String updateEvent = "updateEvent";

    @Inject
    @RestClient
    GitlabRestClient gitlabRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    ParticipantRepository participantRepository;

    @Inject
    JsonMarshaller json;

    @Inject
    EventBus bus;

    @ConfigProperty(name = "branch")
    String branch;

    @ConfigProperty(name = "participant.file")
    String participantFile;

    String commitMessage = "Participants updated";

    void onStart(@Observes StartupEvent ev) {
        long count = participantRepository.count();
        LOGGER.debug("There are {} participant in the participant db.", count);

        if (count == 0) {
            refresh();
            count = participantRepository.count();
            LOGGER.debug("There are now {} participants in the participant db.", count);
        }
    }

    @Transactional
    public void refresh() {

        List<Engagement> engagements = engagementRestClient.getAllEngagementProjects();

        LOGGER.debug("Engagement count {}", engagements.size());
        engagements.parallelStream().forEach(this::reloadEngagement);
        
        LOGGER.debug("refresh complete");
    }
    
    @Transactional
    public long purge() {
        LOGGER.info("Purging participants db");
        return participantRepository.deleteAll();
    }

    @Transactional
    public void reloadEngagement(Engagement e) {
        LOGGER.debug("Reloading {}", e);
        
        if(e.getUuid() == null) {
            LOGGER.error("PROJECT {} DOES NOT HAVE AN ENGAGEMENT UUID ON THE DESCRIPTION", e.getProjectId());
        } else {

            List<Participant> participants = getParticipantsFromGitlab(String.valueOf(e.getProjectId()));
    
            for (Participant participant : participants) {
                LOGGER.trace("u {}", participant);
                participant.setProjectId(e.getProjectId());
                participant.setEngagementUuid(e.getUuid());
            }
    
            deleteParticipants(e.getUuid());
            participantRepository.persist(participants);
        }
    }

    public long getParticipantsCount(String engagementUuid) {
        return participantRepository.count(ENGAGEMENT_UUID, engagementUuid);
    }

    public List<Participant> getParticipants(String engagementUuid) {
        return participantRepository.list(ENGAGEMENT_UUID, Sort.by("uuid", Direction.Descending), engagementUuid);
    }
    
    public List<Participant> getParticipantsAcrossEngagements(int page, int pageSize, List<String> engagementUuids) {
        return participantRepository.find(ENGAGEMENT_UUID + " IN (?1)", Sort.by("uuid", Direction.Descending), engagementUuids).page(Page.of(page, pageSize)).list();
    }
    
    public long getParticipantsAcrossEngagementsCount(List<String> engagementUuids) {
        return participantRepository.count(ENGAGEMENT_UUID + " IN (?1)", engagementUuids);
    }

    public List<Participant> getParticipantsPage(int page, int pageSize) {
        return participantRepository.findAll(Sort.by("uuid", Direction.Descending)).page(Page.of(page, pageSize)).list();
    }

    public long getParticipantCount() {
        return participantRepository.count();
    }
    
    public void updateParticipants(List<Participant> participants, String engagementUuid, String authorEmail, String authorName) {
        long projectId = getProjectIdFromUuid(engagementUuid);
        updateParticipantsDB(participants, engagementUuid, projectId, authorEmail, authorName);
        
        String message = String.format("%s,%d,%s,%s", engagementUuid, projectId, authorEmail, authorName);
        bus.sendAndForget(updateEvent, message);
        LOGGER.debug("Updated {} participants for {}", participants.size(), engagementUuid);
    }

    @Transactional
    public void updateParticipantsDB(List<Participant> participants, String engagementUuid, long projectId, String authorEmail, String authorName) {
        
        for (Participant participant : participants) {
            participant.setEngagementUuid(engagementUuid);
            participant.setProjectId(projectId);
        }

        deleteParticipants(engagementUuid);
        participantRepository.persist(participants);
    }

    private long getProjectIdFromUuid(String engagementUuid) {
        GitlabProject p = engagementRestClient.getProject(engagementUuid);
        return p.getId();
    }
    
    private long deleteParticipants(String engagementUuid) {
        long deletedRows = participantRepository.delete(ENGAGEMENT_UUID, engagementUuid);
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, engagementUuid);
        
        return deletedRows;
    }

    private List<Participant> getParticipantsFromGitlab(String projectIdOrPath) {

        try {
            GitlabFile file = gitlabRestClient.getFile(projectIdOrPath, participantFile, branch);
            file.decodeFileAttributes();
            return json.fromJson(file.getContent());
        } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() != 404) {
                throw ex;
            }
            LOGGER.error("No participant file found for {} {} {}", projectIdOrPath, ex.getClass(), ex.getMessage());
            return Collections.emptyList();
        }        
    }

    /**
     * 
     * @param message 3 part method - uuid,authorEmail,authorName
     */
    @ConsumeEvent(value = updateEvent, blocking = true)
    @Transactional
    public void updateParticipantsInGitlab(String message) {
        LOGGER.debug("Gitlabbing participants - {}", message);
        
        String[] messageFields = message.split(",");

        List<Participant> participants = getParticipants(messageFields[0]);

        String content = json.toJson(participants);
        GitlabFile file = GitlabFile.builder().filePath(participantFile).content(content).commitMessage(commitMessage)
                .branch(branch).authorEmail(messageFields[2]).authorName(messageFields[3]).build();
        file.encodeFileAttributes();

        gitlabRestClient.updateFile(messageFields[1], participantFile, file);

    }

}
