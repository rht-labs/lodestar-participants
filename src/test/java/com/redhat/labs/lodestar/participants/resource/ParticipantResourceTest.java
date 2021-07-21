package com.redhat.labs.lodestar.participants.resource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.model.Participant;
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
        when().put("refresh").then().statusCode(200);
        assertEquals(6, participantService.getParticipantCount());
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
        participants.add(Participant.builder().email("mac4@riot.com").firstName("Bonald").lastName("MacDonald")
                .role("security").build());
        participants.remove(0);
        participants.get(0).setFirstName("Update");

        given().header("Content-Type", "application/json").pathParam("engagementUuid", engagementUuid)
                .body(participants).put("engagements/uuid/{engagementUuid}").then().statusCode(200);

        Assertions.assertEquals(3, participantService.getParticipantsCount(engagementUuid));

        given().pathParam("engagementUuid", "cb570945-a209-40ba-9e42-63a7993baf4d")
                .get("engagements/uuid/{engagementUuid}").then().statusCode(200)
                .header("x-total-participants", equalTo("3")).body("size()", is(3))
                .body("email", hasItems("mac4@riot.com")).body("first_name", hasItems("Update"));
    }

}
