package com.redhat.labs.lodestar.participants.resource;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.participants.model.Participant;
import com.redhat.labs.lodestar.participants.service.ParticipantService;

@RequestScoped
@Path("api/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Participants", description = "Participants for an Engagement")
public class ParticipantResource {
    private static final int DEFAULT_PAGE_SIZE = 100;

    @Inject
    ParticipantService participantService;

    @GET
    public Response getParticipants(@QueryParam("engagementUuids") List<String> engagementUuids,
            @QueryParam(value = "page") Integer page, @QueryParam(value = "pageSize") Integer pageSize) {

        List<Participant> participants;
        long participantCount;

        if (page == null) {
            page = 0;
        }

        if (pageSize == null) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (!engagementUuids.isEmpty()) {
            participantCount = participantService.getParticipantsAcrossEngagementsCount(engagementUuids);
            participants = participantService.getParticipantsAcrossEngagements(page, pageSize, engagementUuids);
        } else {
            participantCount = participantService.getParticipantCount();
            participants = participantService.getParticipantsPage(page, pageSize);
        }
        
        return Response.ok(participants).header("x-page", page).header("x-per-page", pageSize)
                .header("x-total-participants", participantCount).header("x-total-pages", (participantCount / pageSize) + 1).build();
    }
    
    @PUT
    @Path("/refresh")
    @APIResponses(value = { @APIResponse(responseCode = "202", description = "The request was accepted and will be processed.") })
    @Operation(summary = "Refreshes database with data in git, purging first")
    public Response refreshData() {
        participantService.purge();
        participantService.refresh();
        
        return Response.accepted().build();
    }

    @GET
    @Path("/engagements/uuid/{engagementUuid}")
    public Response getParticipantsByEngagementUuid(@PathParam(value = "engagementUuid") String uuid) {
        List<Participant> participants = participantService.getParticipants(uuid);
        long participantCount = participantService.getParticipantsCount(uuid);
        return Response.ok(participants).header("x-total-participants", participantCount).build();
    }

    @PUT
    @Path("/engagements/uuid/{engagementUuid}")
    public Response updateParticipants(@PathParam(value = "engagementUuid") String uuid, List<Participant> participants,
            @QueryParam(value = "authorEmail") String authorEmail,
            @QueryParam(value = "authorName") String authorName) {

        participantService.updateParticipants(participants, uuid, authorEmail, authorName);

        return Response.ok().build();
    }

}
