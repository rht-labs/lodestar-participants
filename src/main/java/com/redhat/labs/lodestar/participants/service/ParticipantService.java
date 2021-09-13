package com.redhat.labs.lodestar.participants.service;

import java.util.Collection;
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

import com.redhat.labs.lodestar.participants.model.*;
import com.redhat.labs.lodestar.participants.rest.client.GitlabApiClient;
import io.quarkus.vertx.ConsumeEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.participants.rest.client.EngagementApiRestClient;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ParticipantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

    private static final String ENGAGEMENT_UUID = "engagementUuid";
    public static final String UPDATE_EVENT = "updateEvent";
    public static final String RESET_EVENT = "resetEvent";

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    ParticipantRepository participantRepository;

    @Inject
    GitlabApiClient gitlabApiClient;

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

        List<Engagement> engagements = engagementRestClient.getAllEngagements();

        LOGGER.debug("Engagement count {}", engagements.size());
        engagements.forEach(this::reloadEngagement);

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

            List<Participant> participants = gitlabApiClient.getParticipants(e.getProjectId());

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

    public Map<String, Long> getEngagementCounts() {
        return participantRepository.getEngagementRollup();
    }

    public Map<String, Long> getParticipantRollup(List<String> region) {
        if(region.isEmpty()) {
            return participantRepository.getParticipantRollup();
        }
            
        return participantRepository.getParticipantRollup(region);
    }
    
    public Map<String, Map<String, Long>> getParticipantRollupAllRegions() {
        return participantRepository.getParticipantRollupAllRegions();
    }

    //consider diffing with javers
    @Transactional
    public GitLabCommit updateParticipants(List<Participant> participants, String engagementUuid, String region, String authorEmail,
                                           String authorName) {

        int projectId = getProjectIdFromUuid(engagementUuid);

        List<Participant> existingParticipants = getParticipants(engagementUuid);
        
        Set<String> added = new HashSet<>();
        Set<String> updated = new HashSet<>();
        Set<String> unchanged = new HashSet<>();
        Set<Participant> reset = new HashSet<>();
        
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

            if(participant.needsReset()) {
                LOGGER.debug("Reset {}", participant.getEmail());
                reset.add(participant);
            }
            participant.setReset(null);
        }
    
        deleted.removeAll(updated);
        deleted.removeAll(unchanged);

        GitLabCommit gitLabCommit = GitLabCommit.builder().projectId(projectId)
                .resetParticipants(reset).authorEmail(authorEmail).authorName(authorName)
                .engagementUuid(engagementUuid).build();

        //Return here if no changes to participants
        if(added.isEmpty() && updated.isEmpty() && deleted.isEmpty()) {
            return gitLabCommit;
        }

        deleteParticipants(engagementUuid);
        participantRepository.persist(participants);
        
        StringBuilder commitMessage = new StringBuilder(commitMessagePrefix);
        commitMessage.append(" ");
        updateCommitMessage(commitMessage, "added", added);
        updateCommitMessage(commitMessage, "updated", updated);
        updateCommitMessage(commitMessage, "deleted", deleted);

        gitLabCommit.setCommitMessage(commitMessage.toString().trim());
        gitLabCommit.setUpdateRequired(true);

        return gitLabCommit;
    }
    
    private void updateCommitMessage(StringBuilder commitMessage, String type, Collection<String> changes) {
        
        if(changes.size() > 2) {
            commitMessage.append(String.format("%d %s. %s ", changes.size(), type, getEmoji()));
        } else if(!changes.isEmpty()) {
            changes.forEach(ch -> commitMessage.append(String.format("%s %s. %s", ch, type, getEmoji())));
        }
        
    }
    
    /**
     * No need to set region here since it's data from gitlab and should already have it
     * @param participant will be modified
     * @param engagementUuid uuid
     * @param projectId id of project
     */
    private void fillOutParticipant(Participant participant, String engagementUuid, long projectId) {
        fillOutParticipant(participant, engagementUuid, participant.getRegion(), projectId);
    }

    /**
     * Region should be sent into the api payload and added to the participant. Region is the same
     * per engagement id.
     * @param participant will be modified
     * @param engagementUuid uuid
     * @param region region of engagement
     * @param projectId id of project
     */
    private void fillOutParticipant(Participant participant, String engagementUuid, String region, long projectId) {
        String org = participant.getEmail().toLowerCase().endsWith("@redhat.com") ? "Red Hat" : "Others";
        participant.setOrganization(org);
        participant.setProjectId(projectId);
        participant.setEngagementUuid(engagementUuid);
        participant.setRegion(region);
    }

    private int getProjectIdFromUuid(String engagementUuid) {
        return engagementRestClient.getEngagement(engagementUuid).getProjectId();
    }

    private long deleteParticipants(String engagementUuid) {
        long deletedRows = participantRepository.delete(ENGAGEMENT_UUID, engagementUuid);
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, engagementUuid);

        return deletedRows;
    }

    /**
     * This will process resets with participant update
     * @param commit 5 part method - uuid,projectId,message,authorEmail,authorName
     */
    @ConsumeEvent(value = ParticipantService.UPDATE_EVENT, blocking = true)
    @Transactional
    public void updateParticipants(GitLabCommit commit) {
        List<Participant> participants = getParticipants(commit.getEngagementUuid());

        gitlabApiClient.updateParticipants(commit, participants);
        engagementRestClient.updateParticipantCount(commit.getEngagementUuid(), participants.size());

    }

    /**
     * Reset only - no participant updates
     * @param commit The changes
     */
    @ConsumeEvent(value = ParticipantService.RESET_EVENT, blocking = true)
    @Transactional
    public void resetParticipants(GitLabCommit commit) {
        gitlabApiClient.resetParticipants(commit);
    }

    private String getEmoji() {
        String bear = "\ud83d\udc3b";

        int bearCodePoint = bear.codePointAt(bear.offsetByCodePoints(0, 0));
        int mysteryAnimalCodePoint = bearCodePoint + new java.security.SecureRandom().nextInt(144);
        char[] mysteryEmoji = { Character.highSurrogate(mysteryAnimalCodePoint),
                Character.lowSurrogate(mysteryAnimalCodePoint) };

        return String.valueOf(mysteryEmoji);
    }

}
