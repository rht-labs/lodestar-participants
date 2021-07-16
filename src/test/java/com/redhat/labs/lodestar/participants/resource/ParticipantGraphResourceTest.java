package com.redhat.labs.lodestar.participants.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.participants.mock.ResourceLoader;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ParticipantGraphResourceTest {

    @Inject
    ParticipantService participantService;

    @BeforeEach
    void init() {
        participantService.refresh();
    }

    @Test
    void testGetActivityForEngagement() {
        String body = ResourceLoader.load("graphql-participant.json");
        given().contentType(ContentType.JSON).body(body).when().post("/graphql").then().statusCode(200).assertThat()
                .body("data.totalParticipants", equalTo(6)).body("data.allParticipants.uuid",
                        hasItems("3634ce4-71c7-4509-8f69-980e399f5ce8", "9975ea17-b305-4424-a9c7-d452faa6e5d4", "uuid",
                                "4fb1e022-ce1f-41ee-b717-ccbf4a927baa", "8340945e-da36-42d3-b81f-a9e7787a33b0",
                                "b9322287-bc88-4b39-9796-2299ab073887"));
    }

}
