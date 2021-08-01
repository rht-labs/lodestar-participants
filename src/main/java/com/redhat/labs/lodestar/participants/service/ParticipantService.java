package com.redhat.labs.lodestar.participants.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class ParticipantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

    private static final String ENGAGEMENT_UUID = "engagementUuid";
    public static final String UPDATE_EVENT = "updateEvent";
    public static final String NO_UPDATE = "noUpdate";

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

    @ConfigProperty(name = "branch")
    String branch;

    @ConfigProperty(name = "participant.file")
    String participantFile;

    @ConfigProperty(name = "commit.message.prefix")
    String commitMessagePrefix;

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
        engagements.stream().forEach(this::reloadEngagement);

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

        if (e.getUuid() == null) {
            LOGGER.error("PROJECT {} DOES NOT HAVE AN ENGAGEMENT UUID ON THE DESCRIPTION", e.getProjectId());
        } else {

            List<Participant> participants = getParticipantsFromGitlab(String.valueOf(e.getProjectId()));

            for (Participant participant : participants) {
                LOGGER.trace("u {}", participant);
                fillOutParticipant(participant, e.getUuid(), e.getProjectId());
            }

            deleteParticipants(e.getUuid());
            participantRepository.persist(participants);
        }
    }

    public long getParticipantsCount(String engagementUuid) {
        return participantRepository.count(ENGAGEMENT_UUID, engagementUuid);
    }

    public List<Participant> getParticipants(String engagementUuid) {
        return participantRepository.list(ENGAGEMENT_UUID, Sort.by("email").and("uuid"), engagementUuid);
    }

    public List<Participant> getParticipantsAcrossEngagements(int page, int pageSize, List<String> engagementUuids) {
        return participantRepository
                .find(ENGAGEMENT_UUID + " IN (?1)", Sort.by(ENGAGEMENT_UUID).and("email").and("uuid"), engagementUuids)
                .page(Page.of(page, pageSize)).list();
    }

    public long getParticipantsAcrossEngagementsCount(List<String> engagementUuids) {
        return participantRepository.count(ENGAGEMENT_UUID + " IN (?1)", engagementUuids);
    }

    public List<Participant> getParticipantsPage(int page, int pageSize) {
        return participantRepository.findAll(Sort.by(ENGAGEMENT_UUID).and("email").and("uuid")).page(Page.of(page, pageSize))
                .list();
    }
    
    public List<Participant> getParticipantsByRegion(List<String> region, int page, int pageSize) {
        return participantRepository.getParticipantsByRegion(region, Page.of(page, pageSize));
    }
    
    public long countParticipantsByRegion(List<String> region) {
        return participantRepository.countParticipantsByRegion(region);
    }

    public long getParticipantCount() {
        return participantRepository.count();
    }

    public Map<String, Long> getParticipantRollup(List<String> region) {
        if(region.isEmpty()) {
            return participantRepository.getParticipantRollup();
        }
            
        return participantRepository.getParticipantRollup(region);
    }
    
    public Map<String, Map<String, Long>> getParticipantRollupAllRegions() {
        Map<String, Map<String, Long>> rollup = participantRepository.getParticipantRollupAllRegions();
        return rollup;
    }

    @Transactional
    public String updateParticipants(List<Participant> participants, String engagementUuid, String region, String authorEmail,
            String authorName) {

        long projectId = getProjectIdFromUuid(engagementUuid);

        List<Participant> existingParticipants = getParticipants(engagementUuid);
        
        Set<String> added = new HashSet<>();
        Set<String> updated = new HashSet<>();
        Set<String> unchanged = new HashSet<>();
        
        Set<String> deleted = existingParticipants.stream().map(Participant::getEmail).collect(Collectors.toSet());

        for (Participant participant : participants) {
            fillOutParticipant(participant, engagementUuid, region, projectId);

            Optional<Participant> optionalParticipant = (existingParticipants.isEmpty()) ? Optional.empty()
                    : existingParticipants.stream().filter(current -> current.getEmail().equals(participant.getEmail()))
                            .findFirst();

            if (optionalParticipant.isPresent()) { //Found matching email in the db. 
                participant.setUuid(optionalParticipant.get().getUuid());
                if(participant.isDifferent(optionalParticipant.get())) {
                    LOGGER.debug("changed {}", participant.getEmail());
                    updated.add(participant.getEmail());
                } else {
                    LOGGER.debug("unchanged {}", participant.getEmail());
                    unchanged.add(participant.getEmail());
                }
            } else {
                LOGGER.debug("added {}", participant.getEmail());
                added.add(participant.getEmail());
                if (participant.getUuid() == null) {
                    participant.setUuid(UUID.randomUUID().toString());
                }
            }
        }
    
        deleted.removeAll(updated);
        deleted.removeAll(unchanged);
        
        if(added.isEmpty() && updated.isEmpty() && deleted.isEmpty()) {
            return NO_UPDATE;
        }

        deleteParticipants(engagementUuid);
        participantRepository.persist(participants);
        
        StringBuilder commitMessage = new StringBuilder();
        commitMessage.append(projectId);
        commitMessage.append(",");
        commitMessage.append(commitMessagePrefix);
        commitMessage.append(" ");
        updateCommitMessage(commitMessage, "added", added);
        updateCommitMessage(commitMessage, "updated", updated);
        updateCommitMessage(commitMessage, "deleted", deleted);

        return commitMessage.toString().trim();
    }
    
    private void updateCommitMessage(StringBuilder commitMessage, String type, Collection<String> changes) {
        
        if(changes.size() > 2) {
            commitMessage.append(changes.size());
            commitMessage.append(" ");
            commitMessage.append(type);
            commitMessage.append(". ");
        } else if(!changes.isEmpty()) {
            changes.stream().forEach(ch -> commitMessage.append(ch + " ")); 
            commitMessage.append(type);
            commitMessage.append(". ");
        }
        
    }
    
    /**
     * No need to set region here since it's data from gitlab and should already have it
     * @param participant
     * @param engagementUuid
     * @param projectId
     */
    private void fillOutParticipant(Participant participant, String engagementUuid, long projectId) {
        fillOutParticipant(participant, engagementUuid, participant.getRegion(), projectId);
    }

    /**
     * Region should be sent into the api payload and added to the participant. Region is the same
     * per engagement id.
     * @param participant
     * @param engagementUuid
     * @param region
     * @param projectId
     */
    private void fillOutParticipant(Participant participant, String engagementUuid, String region, long projectId) {
        String org = participant.getEmail().toLowerCase().endsWith("@redhat.com") ? "Red Hat" : "Others";
        participant.setOrganization(org);
        participant.setProjectId(projectId);
        participant.setEngagementUuid(engagementUuid);
        participant.setRegion(region);
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
                LOGGER.error("Error ({}) fetching participant file for project {}", ex.getResponse().getStatus(), projectIdOrPath);
                throw ex;
            }
            LOGGER.error("No participant file found for {} {}", projectIdOrPath, ex.getMessage());
            return Collections.emptyList();
        } catch (RuntimeException ex) {
            LOGGER.error("Failure retrieving file {} {}", projectIdOrPath, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * 
     * @param message 3 part method - uuid,authorEmail,authorName
     */
    @ConsumeEvent(value = UPDATE_EVENT, blocking = true)
    @Transactional
    public void updateParticipantsInGitlab(String message) {
        LOGGER.debug("Gitlabbing participants - {}", message);

        String[] uuidProjectMessageEmailName = message.split(",");

        List<Participant> participants = getParticipants(uuidProjectMessageEmailName[0]);

        String content = json.toJson(participants);
        GitlabFile file = GitlabFile.builder().filePath(participantFile).content(content).commitMessage(uuidProjectMessageEmailName[2])
                .branch(branch).authorEmail(uuidProjectMessageEmailName[3]).authorName(uuidProjectMessageEmailName[4]).build();
        file.encodeFileAttributes();

        gitlabRestClient.updateFile(uuidProjectMessageEmailName[1], participantFile, file);

    }

}
