package com.redhat.labs.lodestar.participants.rest.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.http.NoHttpResponseException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.participants.model.GitlabFile;

@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@Path("/api/v4")
@RegisterRestClient(configKey = "gitlab.api")
@RegisterProvider(value = RestClientResponseMapper.class, priority = 50)
@RegisterClientHeaders(GitlabTokenFactory.class)
@Produces("application/json")
@Consumes("application/json")
public interface GitlabRestClient {
    
    @PUT
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    GitlabFile updateFile(@PathParam("id") @Encoded String projectId, @PathParam("file_path") @Encoded String filePath,
            GitlabFile file);
    
    @GET
    @Path("/projects/{id}/repository/files/{file_path}")
    @Produces("application/json")
    GitlabFile getFile(@PathParam("id") @Encoded String projectId, @PathParam("file_path") @Encoded String filePath,
            @QueryParam("ref") @Encoded String ref);
}
