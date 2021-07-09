package com.redhat.labs.lodestar.participants.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

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
                .body("data.totalParticipants", equalTo(6)).body("data.allParticipants.uuid", hasItem("f3634ce4-71c7-4509-8f69-980e399f5ce8"));
    }

}
