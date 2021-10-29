package com.redhat.labs.lodestar.participants.service;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.redhat.labs.lodestar.participants.exception.ParticipantException;
import com.redhat.labs.lodestar.participants.model.GitLabCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.participants.model.Engagement;
import com.redhat.labs.lodestar.participants.model.Participant;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ParticipantServiceTest {

    @Inject
    ParticipantService participantService;
    
    @BeforeEach
    void init() {
        participantService.purge();
        participantService.refresh();
    }
    
    @Test
    void testRollup() {
        Map<String, Long> rollup = participantService.getParticipantRollup(Collections.emptyList());
        assertEquals(6, rollup.get("All"));
        assertEquals(5, rollup.get("Others"));
        assertEquals(1, rollup.get("Red Hat"));
    }
    @Test
    void testRegionRollup() {
        Map<String, Long> rollup = participantService.getParticipantRollup(Collections.singletonList("latam"));
        assertEquals(3, rollup.get("All"));
        assertEquals(2, rollup.get("Others"));
        assertEquals(1, rollup.get("Red Hat"));
    }
    
    @Test
    void testAllRegionRollup() {
        Map<String, Map<String, Long>> rollup = participantService.getParticipantRollupAllRegions();
        
        assertEquals(3, rollup.size());
        
        assertTrue(rollup.containsKey("All"));
        assertTrue(rollup.containsKey("na"));
        assertTrue(rollup.containsKey("latam"));
        
        Map<String, Long> all = rollup.get("All");
        Map<String, Long> na = rollup.get("na");
        Map<String, Long> latam = rollup.get("latam");
        
        assertEquals(6, all.get("All"));
        assertEquals(5, all.get("Others"));
        assertEquals(1, all.get("Red Hat"));
        
        assertEquals(3, na.get("All"));
        assertEquals(3, na.get("Others"));
        assertNull(na.get("Red Hat"));
        
        assertEquals(3, latam.get("All"));
        assertEquals(2, latam.get("Others"));
        assertEquals(1, latam.get("Red Hat"));
    }
    
    @Test
    void testRegion() {
        List<String> region = Collections.singletonList("latam");
        
        long regionCount = participantService.countParticipantsByRegion(region);
        assertEquals(3, regionCount);
        
        List<Participant> participants = participantService.getParticipantsByRegion(region, 0, 6);
        
        assertEquals(3, participants.size());
    }
    
    @Test
    void testRefresh() {
        participantService.purge();
        participantService.refresh();
        
        assertEquals(6, participantService.getParticipantCount());
    }

    @Test
    void testCheckDBPopTwice() {
        participantService.purge();
        participantService.checkDBPopulation();
        assertEquals(6, participantService.getParticipantCount());

        participantService.checkDBPopulation();
        assertEquals(6, participantService.getParticipantCount());
    }
    
    @Test
    void testReloadEngagementFailure() {
        Engagement engagement = Engagement.builder().projectId(99).uuid("99uuid99").build();
        ParticipantException ex = assertThrows(ParticipantException.class, () -> participantService.reloadEngagement(engagement));
        
        assertEquals("File engagement/participants.json not retrieved for project 99. Status 500, Reason Server Error", ex.getMessage());
    }
    
    @Test
    void testGetParticipantTotal() {
        long participantCount = participantService.getParticipantCount();
        assertEquals(6, participantCount);
    }
    
    @Test
    void testGetParticipantPage() {
        List<Participant> participants = participantService.getParticipantsPage(0, 4);
        assertEquals(4, participants.size());
        assertEquals("3634ce4-71c7-4509-8f69-980e399f5ce8", participants.get(0).getUuid());
    }
    
    @Test
    void testGetParticipantByEngagementUuid() {
        assertEquals(3, participantService.getParticipantsCount("cb570945-a209-40ba-9e42-63a7993baf4d"));
        
        List<Participant> participants = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
        assertEquals(3, participants.size());
        assertEquals("3634ce4-71c7-4509-8f69-980e399f5ce8", participants.get(0).getUuid());
    }
    
    @Test
    void testGetParticipantsAcrossEngagements() {
        List<String> uuids = new ArrayList<>();
        uuids.add("second");
        uuids.add("cb570945-a209-40ba-9e42-63a7993baf4d");
        
        long participantCount = participantService.getParticipantsAcrossEngagementsCount(uuids);
        assertEquals(6, participantCount);
        
        List<Participant> participants = participantService.getParticipantsAcrossEngagements(1, 5, uuids);
        assertEquals(1, participants.size());
        assertEquals("b9322287-bc88-4b39-9796-2299ab073887", participants.get(0).getUuid());
    }
    
    @Test
    void testUpdateParticipants() {
       List<Participant> participants = new ArrayList<>();
       participants.add(Participant.builder().uuid("uuid").email("joe@schmo.com").firstName("Joe").lastName("Schmo").role("schmo").build());
       GitLabCommit update = participantService.updateParticipants(participants, "cb570945-a209-40ba-9e42-63a7993baf4d", "na", "bo@bo.com", "Bo Bichette");

       assertNotNull(update);
       assertEquals("cb570945-a209-40ba-9e42-63a7993baf4d", update.getEngagementUuid());
       assertEquals(13065, update.getProjectId());
       assertTrue(update.getCommitMessage().startsWith("Participants: joe@schmo.com added."));
       assertTrue(update.getCommitMessage().contains("3 deleted."));
       assertEquals(0, update.getResetParticipants().size());
       assertEquals("bo@bo.com", update.getAuthorEmail());
       assertEquals("Bo Bichette", update.getAuthorName());
       
       List<Participant> result = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
       
       assertEquals(1, result.size());
       assertEquals("cb570945-a209-40ba-9e42-63a7993baf4d", result.get(0).getEngagementUuid());
    }
    
    @Test
    void testUpdateParticipantsNoUpdate() {
       List<Participant> participants = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");

       GitLabCommit update = participantService.updateParticipants(participants, "cb570945-a209-40ba-9e42-63a7993baf4d","na", "bo@bo.com", "Bo Bichette");

       assertFalse(update.isUpdateRequired());
       
       List<Participant> result = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
       
       assertEquals(3, result.size());
       assertEquals(participants.get(0), result.get(0));
       assertEquals(participants.get(1), result.get(1));
       assertEquals(participants.get(2), result.get(2));
    }

    @Test
    void testResetOnly() {
        List<Participant> participants = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
        participants.forEach(p -> p.setReset(true));
    }
    
}
