package com.redhat.labs.lodestar.participants.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

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
        
        stubFor(get(urlEqualTo("/api/v4/projects/13065/repository/files/users.json?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        stubFor(put(urlEqualTo("/api/v4/projects/13065/repository/files/users.json")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("gitlab-file-users-20962.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/20962/repository/files/users.json?ref=master")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        stubFor(put(urlEqualTo("/api/v4/projects/20962/repository/files/users.json")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        stubFor(get(urlEqualTo("/api/v4/projects/99/repository/files/users.json?ref=master")).willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type",  "application/json")
                .withBody("{\"msg\": \" 500 Something bad happened\"}")
                ));
        
        Map<String, String> config = new HashMap<>();
        config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        
        return config;
    }

    @Override
    public void stop() {
        if(null != wireMockServer) {
           wireMockServer.stop();
        }
        
    }


}
