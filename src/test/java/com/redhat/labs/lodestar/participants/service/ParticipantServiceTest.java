package com.redhat.labs.lodestar.participants.service;


import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.participants.model.Engagement;
import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

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
        Map<String, Long> rollup = participantService.getParticipantRollup();
        assertEquals(4, rollup.get("All"));
        assertEquals(2, rollup.get("Others"));
        assertEquals(2, rollup.get("Red Hat"));
    }
    
    @Test
    void testRefresh() {
        participantService.purge();
        participantService.refresh();
        
        for(Participant p : participantService.getParticipantsPage(0, 10)) {
            System.out.println(p);
        }
        
        assertEquals(6, participantService.getParticipantCount());
    }
    
    @Test
    void testReloadEngagementFailure() {
        Engagement engagement = Engagement.builder().projectId(99).uuid("99uuid99").build();
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> {
            participantService.reloadEngagement(engagement);
        });
        
        assertEquals(500, ex.getResponse().getStatus());
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
        assertEquals("f3634ce4-71c7-4509-8f69-980e399f5ce8", participants.get(0).getUuid());
    }
    
    @Test
    void testGetParticipantByEngagementUuid() {
        assertEquals(3, participantService.getParticipantsCount("cb570945-a209-40ba-9e42-63a7993baf4d"));
        
        List<Participant> participants = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
        assertEquals(3, participants.size());
        assertEquals("f3634ce4-71c7-4509-8f69-980e399f5ce8", participants.get(0).getUuid());
    }
    
    @Test
    void testGetParticipantsAcrossEngagements() {
        List<String> uuids = new ArrayList<String>();
        uuids.add("second");
        uuids.add("cb570945-a209-40ba-9e42-63a7993baf4d");
        
        long participantCount = participantService.getParticipantsAcrossEngagementsCount(uuids);
        assertEquals(6, participantCount);
        
        List<Participant> participants = participantService.getParticipantsAcrossEngagements(1, 5, uuids);
        assertEquals(1, participants.size());
        assertEquals("40b1a03a-cc08-4142-b9c1-1321da5ab927", participants.get(0).getUuid());
    }
    
    @Test
    void testUpdateParticipants() {
       List<Participant> participants = new ArrayList<>();
       participants.add(Participant.builder().uuid("uuid").email("joe@schmo.com").firstName("Joe").lastName("Schmo").role("schmo").build());
       participantService.updateParticipants(participants, "cb570945-a209-40ba-9e42-63a7993baf4d", "bo@bo.com", "Bo Bichette"); 
       
       List<Participant> result = participantService.getParticipants("cb570945-a209-40ba-9e42-63a7993baf4d");
       
       assertEquals(1, result.size());
       assertEquals("cb570945-a209-40ba-9e42-63a7993baf4d", result.get(0).getEngagementUuid());
    }
    
}
