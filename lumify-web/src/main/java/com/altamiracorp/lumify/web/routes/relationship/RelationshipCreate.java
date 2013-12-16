package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRelationship;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipCreate extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipCreate.class);

    private final GraphRepository graphRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public RelationshipCreate(final GraphRepository graphRepository,
                              final AuditRepository auditRepository,
                              final OntologyRepository ontologyRepository) {
        this.graphRepository = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        // validate parameters
        final String sourceGraphVertexId = getRequiredParameter(request, "sourceGraphVertexId");
        final String destGraphVertexId = getRequiredParameter(request, "destGraphVertexId");
        final String predicateLabel = getRequiredParameter(request, "predicateLabel");
        final String artifactId = getOptionalParameter(request, "artifactId");
        final String workspaceRowKey = getOptionalParameter(request, "workspaceRowKey");

        User user = getUser(request);
        GraphRelationship relationship = graphRepository.saveRelationship(sourceGraphVertexId, destGraphVertexId, predicateLabel, user);

        String relationshipDisplayName = ontologyRepository.getDisplayNameForLabel(predicateLabel, user);
        Object destTitle = graphRepository.findVertex(destGraphVertexId, user).getProperty(PropertyName.TITLE.toString());
        Object sourceTitle = graphRepository.findVertex(sourceGraphVertexId, user).getProperty(PropertyName.TITLE.toString());

        String locationOfCreation = null;
        if (artifactId != null) {
            auditRepository.audit(artifactId, auditRepository.relationshipAuditMessageOnArtifact(sourceTitle, destTitle, relationshipDisplayName), user);
            locationOfCreation = (String)graphRepository.findVertex(artifactId, user).getProperty(PropertyName.TITLE.toString());
        }

        auditRepository.audit(sourceGraphVertexId, auditRepository.relationshipAuditMessageOnSource(relationshipDisplayName, destTitle, locationOfCreation), user);
        auditRepository.audit(destGraphVertexId, auditRepository.relationshipAuditMessageOnDest(relationshipDisplayName, sourceTitle, locationOfCreation), user);

        LOGGER.info("Statement created:\n" + relationship.toJson().toString(2));

        respondWithJson(response, relationship.toJson());
    }
}