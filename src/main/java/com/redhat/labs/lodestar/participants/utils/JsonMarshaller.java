package com.redhat.labs.lodestar.participants.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.redhat.labs.lodestar.participants.exception.ParticipantExcpetion;
import com.redhat.labs.lodestar.participants.model.Participant;

import io.quarkus.runtime.StartupEvent;

/**
 * Used converting String to Objects (non-request, non-response)
 * 
 * @author mcanoy
 *
 */
@ApplicationScoped
public class JsonMarshaller {
    public static final Logger LOGGER = LoggerFactory.getLogger(JsonMarshaller.class);

    private ObjectMapper om = new ObjectMapper();

    void onStart(@Observes StartupEvent ev) {
        om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public Participant fromJson(String json) {
        return fromJson(json, new TypeReference<Participant>() {
            });

    }

    public List<Participant> fromJsonList(String json) {
        return fromJson(json, new TypeReference<List<Participant>>() {
            });
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return om.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ParticipantExcpetion("Error translating participant json data", e);
        }
    }

    public String toJson(Object participants) {
        try {
            return om.writeValueAsString(participants);
        } catch (JsonProcessingException e) {
            throw new ParticipantExcpetion("Error translating participant data to json", e);
        }
    }

}
