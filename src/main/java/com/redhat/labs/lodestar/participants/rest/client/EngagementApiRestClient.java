package com.redhat.labs.lodestar.participants.rest.client;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.participants.model.Engagement;
import com.redhat.labs.lodestar.participants.model.GitlabProject;

@RegisterRestClient(configKey = "engagement.api")
@Produces("application/json")
public interface EngagementApiRestClient {

    @GET
    @Path("/api/v1/engagements")
    List<Engagement> getAllEngagements(@QueryParam("includeCommits") boolean includeCommits, @QueryParam("includeStatus") boolean includeStatus, @QueryParam("pagination") boolean pagination);
    
    @GET
    @Path("/api/v1/engagements/projects")
    List<Engagement> getAllEngagementProjects();
    
    @GET
    @Path("/api/v1/engagements/projects/{engagementUuid}")
    GitlabProject getProject(@PathParam("engagementUuid") String engagementUuid);
}
