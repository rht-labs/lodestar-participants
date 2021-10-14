package com.redhat.labs.lodestar.participants.rest.client;

import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.http.NoHttpResponseException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.participants.model.Engagement;

@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@RegisterRestClient(configKey = "engagement.api")
@Produces("application/json")
@Path("/api/v2/engagements")
public interface EngagementApiRestClient {

    @GET
    List<Engagement> getAllEngagements();
    
    @GET
    @Path("{uuid}")
    Engagement getEngagement(@PathParam("uuid") String engagementUuid);

    @PUT
    @Path("{uuid}/participants/{count}")
    Response updateParticipantCount(@PathParam("uuid") String uuid, @PathParam("count") int count);
}
