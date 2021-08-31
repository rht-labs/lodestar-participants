package com.redhat.labs.lodestar.participants.resource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import javax.inject.Inject;

import com.redhat.labs.lodestar.participants.model.GitLabCommit;
import com.redhat.labs.lodestar.participants.rest.client.GitlabApiClient;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.mockito.Mockito;

@QuarkusTest
@TestHTTPEndpoint(ParticipantResource.class)
class ParticipantResourceTest {

    @InjectSpy
    GitlabApiClient gitlabApiClient;

    @Inject
    ParticipantService participantService;

    @BeforeEach
    void init() {
        participantService.refresh();
    }

    @Test
    void testRefresh() {
        when().put("refresh").then().statusCode(200);
        assertEquals(6, participantService.getParticipantCount());
    }

    @Test
    void testGetEnabledBreakdown() {
        when().get("/enabled/breakdown").then().statusCode(200).body("size()", is(3));
    }
    
    @Test
    void testGetEnabled() {
        when().get("/enabled").then().statusCode(200).body("size()", is(3)).body("All", equalTo(6))
                .body("'Red Hat'", equalTo(1)).body("Others", equalTo(5));
    }

    @Test
    void testGetParticipant() {
        when().get().then().statusCode(200).body("size()", is(6)).header("x-total-participants", equalTo("6"));
    }
    
    @Test
    void testGetParticipantBadRequest() {
        given().queryParam("engagementUuids", "1").queryParam("region", "a").when().get().then().statusCode(400);
    }
    
    @Test
    void testGetParticipantForRegion() {
        given().queryParam("page", "0").queryParam("pageSize", "2").queryParam("region", "na")
                .queryParam("region", "apac").get().then().statusCode(200)
                .body("size()", is(2)).header("x-total-participants", equalTo("3"));
    }

    @Test
    void testGetParticipantForUuids() {
        given().queryParam("page", "1").queryParam("pageSize", "5").queryParam("engagementUuids", "second")
                .queryParam("engagementUuids", "cb570945-a209-40ba-9e42-63a7993baf4d").get().then().statusCode(200)
                .body("size()", is(1)).header("x-total-participants", equalTo("6"));
    }

    @Test
    void testGetParticipantsByEngagementId() {
        given().pathParam("engagementUuid", "cb570945-a209-40ba-9e42-63a7993baf4d")
                .get("engagements/uuid/{engagementUuid}").then().statusCode(200)
                .header("x-total-participants", equalTo("3")).body("size()", is(3)).body("uuid", hasItems("uuid",
                        "9975ea17-b305-4424-a9c7-d452faa6e5d4", "3634ce4-71c7-4509-8f69-980e399f5ce8"));
    }

    @Test
    void testUpdateParticipants() {

        String engagementUuid = "cb570945-a209-40ba-9e42-63a7993baf4d";
        List<Participant> participants = participantService.getParticipants(engagementUuid);
        participants.add(Participant.builder().email("mac4@riot.com").firstName("Bonald").lastName("MacDonald").role("security").build());
        participants.remove(0);
        participants.get(0).setFirstName("Update");
        participants.get(0).setReset(true);

        given().header("Content-Type", "application/json").pathParam("engagementUuid", engagementUuid).pathParam("region", "na").body(participants)
                .put("engagements/uuid/{engagementUuid}/{region}").then().statusCode(200);

        Assertions.assertEquals(3, participantService.getParticipantsCount(engagementUuid));

        given().pathParam("engagementUuid", engagementUuid).get("engagements/uuid/{engagementUuid}").then()
                .statusCode(200).header("x-total-participants", equalTo("3")).body("size()", is(3)).body("email", hasItems("mac4@riot.com"))
                .body("first_name", hasItems("Update"));

        verify(gitlabApiClient, times(1)).updateParticipants(any(GitLabCommit.class));
    }
    
    @Test
    void testNoUpdateParticipants() {

        String engagementUuid = "cb570945-a209-40ba-9e42-63a7993baf4d";
        List<Participant> participants = participantService.getParticipants(engagementUuid);

        given().header("Content-Type", "application/json").pathParam("engagementUuid", engagementUuid).pathParam("region", "na").body(participants)
                .put("engagements/uuid/{engagementUuid}/{region}").then().statusCode(200);

        verify(gitlabApiClient, times(0)).updateParticipants(any(GitLabCommit.class));

    }

    @Test
    void testRestOnly() {
        String engagementUuid = "cb570945-a209-40ba-9e42-63a7993baf4d";
        List<Participant> participants = participantService.getParticipants(engagementUuid);
        participants.forEach(p -> p.setReset(true));

        given().header("Content-Type", "application/json").pathParam("engagementUuid", engagementUuid).pathParam("region", "na").body(participants)
                .put("engagements/uuid/{engagementUuid}/{region}").then().statusCode(200);

        verify(gitlabApiClient, times(1)).resetParticipants(any(GitLabCommit.class));
    }


}
