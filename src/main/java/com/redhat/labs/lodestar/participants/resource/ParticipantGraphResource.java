package com.redhat.labs.lodestar.participants.resource;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

@GraphQLApi
public class ParticipantGraphResource {
    
    @Inject
    ParticipantService participantService;
    
    @Query("totalParticipants")
    @Description("a count of all commits")
    public long getCommitCount() {
        return participantService.getParticipantCount();
    }
    
    @Query("allParticipants")
    @Description("Get all commits from all projects")
    public List<Participant> getAllActivity(@Name("page") int page, @Name("pageSize") int pageSize) {
        return participantService.getParticipantsPage(page, pageSize);
    }

    @Query("totalParticipantsForUuid")
    @Description("a count of all participants for uuid")
    public long getParticipantCountForUuid(@Name("uuid") String engagementUuid) {
        return participantService.getParticipantsCount(engagementUuid);
    }
    
    @Query("participantForEngagement")
    @Description("Get a list of participants from a single project by uuid")
    public List<Participant> getActivityForEngagement(@Name("uuid") String engagementUuid) {
        return participantService.getParticipants(engagementUuid);
    }

}
