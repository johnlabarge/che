/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;

import static org.eclipse.che.api.project.server.DtoConverter.asDto;

/**
 * For caching and proxy-ing Workspace Configuration.
 *
 * @author gazarenkov
 */
@Singleton
public class WorkspaceHolder extends WorkspaceProjectsSyncer {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceHolder.class);

    private String apiEndpoint;

    private String workspaceId;

    private final String userToken;

    private WorkspaceDto workspace;

    private HttpJsonRequestFactory httpJsonRequestFactory;

    @Inject
    public WorkspaceHolder(@Named("che.api") String apiEndpoint,
    //                       @Named("workspace.id") String workspaceId,
                           HttpJsonRequestFactory httpJsonRequestFactory) throws ServerException {

        this.apiEndpoint = apiEndpoint;
        this.httpJsonRequestFactory = httpJsonRequestFactory;

        this.workspaceId = System.getenv("CHE_WORKSPACE_ID");
        this.userToken = System.getenv("USER_TOKEN");

        LOG.info("Workspace ID: " + workspaceId);
        LOG.info("API Endpoint: " + apiEndpoint);
        LOG.info("User Token  : " + (userToken != null));

        // check connection
        try {
            this.workspace = workspaceDto();
        } catch (ServerException e) {
            LOG.error(e.getLocalizedMessage());
            System.exit(1);
        }


        // TODO - invent mechanism to recognize workspace ID
        // for Docker container name of this property is defined in
        // org.eclipse.che.plugin.docker.machine.DockerInstanceMetadata.CHE_WORKSPACE_ID
        // it resides on Workspace Master side so not accessible from agent code
        //if (workspaceId == null) {
        //    throw new ServerException("Workspace ID is not defined for Workspace Agent");
        //}

    }


    @Override
    public List<? extends ProjectConfig> getProjects() throws ServerException {

        return workspaceDto().getConfig().getProjects();
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }


    /**
     * Add project on WS-master side.
     *
     * @param project
     *         project to add
     * @throws ServerException
     */
    protected void addProject(ProjectConfig project) throws ServerException {

        final UriBuilder builder = UriBuilder.fromUri(apiEndpoint).path(WorkspaceService.class)
                                             .path(WorkspaceService.class, "addProject");
        if(userToken != null)
            builder.queryParam("token", userToken);
        final String href = builder.build(workspaceId).toString();


//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class)
//                                      .path(WorkspaceService.class, "addProject")
//                                      .build(workspaceId).toString();
        try {
            httpJsonRequestFactory.fromUrl(href).usePostMethod().setBody(asDto(project)).request();
        } catch (IOException | ApiException e) {
            throw new ServerException(e.getMessage());
        }

    }

    /**
     * Updates project on WS-master side.
     *
     * @param project
     *         project to update
     * @throws ServerException
     */
    protected void updateProject(ProjectConfig project) throws ServerException {


        final UriBuilder builder = UriBuilder.fromUri(apiEndpoint).path(WorkspaceService.class)
                                             .path(WorkspaceService.class, "updateProject");
        if(userToken != null)
            builder.queryParam("token", userToken);
        final String href = builder.build(new String[]{workspaceId, project.getPath()}, false).toString();


//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class)
//                                      .path(WorkspaceService.class, "updateProject")
//                                      .build(new String[]{workspaceId, project.getPath()}, false).toString();
        try {
            httpJsonRequestFactory.fromUrl(href).usePutMethod().setBody(asDto(project)).request();
        } catch (IOException | ApiException e) {
            throw new ServerException(e.getMessage());
        }

    }


    protected void removeProject(ProjectConfig project) throws ServerException {

        final UriBuilder builder = UriBuilder.fromUri(apiEndpoint).path(WorkspaceService.class)
                                             .path(WorkspaceService.class, "deleteProject");
        if(userToken != null)
            builder.queryParam("token", userToken);
        final String href = builder.build(new String[]{workspaceId, project.getPath()}, false).toString();

//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class)
//                                      .path(WorkspaceService.class, "deleteProject")
//                                      .build(new String[]{workspaceId, project.getPath()}, false).toString();
        try {
            httpJsonRequestFactory.fromUrl(href).useDeleteMethod().request();
        } catch (IOException | ApiException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * @return WorkspaceDto
     * @throws ServerException
     */
    private WorkspaceDto workspaceDto() throws ServerException {

        final UriBuilder builder = UriBuilder.fromUri(apiEndpoint).path(WorkspaceService.class)
                                             .path(WorkspaceService.class, "getByKey");
        if(userToken != null)
            builder.queryParam("token", userToken);
        final String href = builder.build(workspaceId).toString();

//        final String href = UriBuilder.fromUri(apiEndpoint).queryParam("token", userToken)
//                                      .path(WorkspaceService.class).path(WorkspaceService.class, "getByKey")
//                                      .build(workspaceId).toString();
        try {
            return httpJsonRequestFactory.fromUrl(href).useGetMethod().request().asDto(WorkspaceDto.class);
        } catch (IOException | ApiException e) {
            throw new ServerException(e);
        }
    }

}
