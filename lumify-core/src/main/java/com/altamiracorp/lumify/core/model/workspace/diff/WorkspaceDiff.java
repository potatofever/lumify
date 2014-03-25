package com.altamiracorp.lumify.core.model.workspace.diff;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceEntity;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class WorkspaceDiff {
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public WorkspaceDiff(
            final Graph graph,
            final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    public List<DiffItem> diff(Workspace workspace, List<WorkspaceEntity> workspaceEntities, List<Edge> workspaceEdges, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspace.getId());

        List<DiffItem> result = new ArrayList<DiffItem>();
        for (WorkspaceEntity workspaceEntity : workspaceEntities) {
            List<DiffItem> entityDiffs = diffWorkspaceEntity(workspace, workspaceEntity, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        for (Edge workspaceEdge : workspaceEdges) {
            List<DiffItem> entityDiffs = diffEdge(workspace, workspaceEdge, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        return result;
    }

    private List<DiffItem> diffEdge(Workspace workspace, Edge edge, Authorizations authorizations) {
        List<DiffItem> result = new ArrayList<DiffItem>();

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(edge, workspace.getId());
        if (sandboxStatus != SandboxStatus.PUBLIC) {
            result.add(new EdgeDiffItem(edge, sandboxStatus));
        }

        diffProperties(workspace, edge, result);

        return result;
    }

    public List<DiffItem> diffWorkspaceEntity(Workspace workspace, WorkspaceEntity workspaceEntity, Authorizations authorizations) {
        List<DiffItem> result = new ArrayList<DiffItem>();

        Vertex entityVertex = this.graph.getVertex(workspaceEntity.getEntityVertexId(), authorizations);

        // vertex can be null if the user doesn't have access to the entity
        if (entityVertex == null) {
            return null;
        }

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(entityVertex, workspace.getId());
        if (sandboxStatus != SandboxStatus.PUBLIC) {
            result.add(new VertexDiffItem(entityVertex, sandboxStatus, workspaceEntity.isVisible()));
        }

        diffProperties(workspace, entityVertex, result);

        return result;
    }

    private void diffProperties(Workspace workspace, Element element, List<DiffItem> result) {
        List<Property> properties = toList(element.getProperties());
        SandboxStatus[] propertyStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspace.getId());
        for (int i = 0; i < properties.size(); i++) {
            if (propertyStatuses[i] != SandboxStatus.PUBLIC) {
                Property property = properties.get(i);
                Property existingProperty = findExistingProperty(properties, property);
                result.add(new PropertyDiffItem(element, property, existingProperty, propertyStatuses[i]));
            }
        }
    }

    private Property findExistingProperty(List<Property> properties, Property workspaceProperty) {
        for (Property property : properties) {
            if (property.getName().equals(workspaceProperty.getName())) {
                if (property.getKey().equals(workspaceProperty.getKey())) {
                    continue;
                }
                return property;
            }
        }
        return null;
    }
}
