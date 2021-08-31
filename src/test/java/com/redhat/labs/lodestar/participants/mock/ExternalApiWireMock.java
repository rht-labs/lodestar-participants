package com.redhat.labs.lodestar.participants.mock;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer; 
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        
        String body = ResourceLoader.load("seed-engagement.json");
        
        stubFor(get(urlEqualTo("/api/v1/engagements/projects")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-project-13065.json");
        
        stubFor(get(urlEqualTo("/api/v1/engagements/projects/cb570945-a209-40ba-9e42-63a7993baf4d")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-project-20962.json");
        
        stubFor(get(urlEqualTo("/api/v1/engagements/projects/second")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-file-users-13065.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/13065/repository/files/engagement%2Fparticipants%2Ejson?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        
        body = ResourceLoader.load("gitlab-file-users-20962.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/20962/repository/files/engagement%2Fparticipants%2Ejson?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        stubFor(post(urlEqualTo("/api/v4/projects/13065/repository/commits")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody("{}")
                ));

        stubFor(get(urlEqualTo("/api/v4/projects/99/repository/files/engagement%2Fparticipants%2Ejson?ref=master")).willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 500 Something bad happened\"}")
                ));
        
        stubFor(get(urlEqualTo("/api/v4/projects/30/repository/files/engagement%2Fparticipants%2Ejson?ref=master")).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 404 No file found \"}")
                ));

        stubFor(headUserManagement(13065, "uuid", 404));
        stubFor(headUserManagement(13065, "3634ce4%2D71c7%2D4509%2D8f69%2D980e399f5ce8", 404));
        stubFor(headUserManagement(13065, "9975ea17%2Db305%2D4424%2Da9c7%2Dd452faa6e5d4", 404));

        Map<String, String> config = new HashMap<>();
        config.put("gitlab4j.api.url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        
        return config;
    }

    @Override
    public void stop() {
        if(null != wireMockServer) {
           wireMockServer.stop();
        }
        
    }

    private MappingBuilder headUserManagement(int projectId, String uuid, int status) {
        String format = "/api/v4/projects/%d/repository/files/queue%%2Fuser%%2Dmanagement%%2D%s%%2Ejson?ref=master";
        String url = String.format(format, projectId, uuid);

        return head(urlEqualTo(url)).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 404 No file found \"}"));
    }


}
