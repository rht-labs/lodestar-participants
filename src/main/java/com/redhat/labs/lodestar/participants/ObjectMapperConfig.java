package com.redhat.labs.lodestar.participants;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class ObjectMapperConfig implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
         objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
      // To suppress serializing properties with null values
         objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
