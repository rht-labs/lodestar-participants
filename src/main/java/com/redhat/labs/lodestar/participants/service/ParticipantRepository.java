package com.redhat.labs.lodestar.participants.service;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.lodestar.participants.model.Participant;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class ParticipantRepository implements PanacheRepository<Participant> {

}
