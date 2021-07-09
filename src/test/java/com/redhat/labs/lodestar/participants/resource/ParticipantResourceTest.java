package com.redhat.labs.lodestar.participants.resource;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.resource.ParticipantResource;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ParticipantResource.class)
class ParticipantResourceTest {

    @Inject
    ParticipantService participantService;

    @BeforeEach
    void init() {
        participantService.refresh();
    }
    
    @Test
    void testRefresh() {
        when().put("refresh").then().statusCode(202);
        assertEquals(6, participantService.getParticipantCount());
    }

    @Test
    void testGetParticipant() {
        when().get().then().statusCode(200).body("size()", is(6)).header("x-total-participants", equalTo("6"));
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
                .header("x-total-participants", equalTo("3")).body("size()", is(3))
                .body("uuid", hasItems("f3634ce4-71c7-4509-8f69-980e399f5ce8", "cf00bf8a-5359-4ee9-b57e-5b13c5107687",
                        "40b1a03a-cc08-4142-b9c1-1321da5ab927"));
    }

    @Test
    void testUpdateParticipants() {
        String engagementUuid = "cb570945-a209-40ba-9e42-63a7993baf4d";
        List<Participant> participants = new ArrayList<>();
        participants.add(Participant.builder().uuid("uuid").email("mac@ocularpatdown.com").firstName("Ronald")
                .lastName("MacDonald").role("security").build());

        given().header("Content-Type", "application/json").pathParam("engagementUuid", engagementUuid)
                .body(participants).put("engagements/uuid/{engagementUuid}").then().statusCode(200);

        Assertions.assertEquals(1, participantService.getParticipantsCount(engagementUuid));
    }

}