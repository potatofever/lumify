package com.altamiracorp.lumify.web.routes.entity;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.security.LumifyVisibilityProperties;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;

public class ResolveDetectedObject extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ResolveDetectedObject.class);
    private final Graph graph;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final DetectedObjectRepository detectedObjectRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public ResolveDetectedObject(
            final Graph graphRepository,
            final AuditRepository auditRepository,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration,
            final DetectedObjectRepository detectedObjectRepository,
            final VisibilityTranslator visibilityTranslator) {
        super(userRepository, configuration);
        this.graph = graphRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.detectedObjectRepository = detectedObjectRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String artifactId = getRequiredParameter(request, "artifactId");
        final String title = getRequiredParameter(request, "title");
        final String conceptId = getRequiredParameter(request, "conceptId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");
        final String graphVertexId = getOptionalParameter(request, "graphVertexId");
        final String justificationText = getOptionalParameter(request, "justificationText");
        final String sourceInfo = getOptionalParameter(request, "sourceInfo");
        String rowKey = getOptionalParameter(request, "rowKey");
        String x1 = getRequiredParameter(request, "x1");
        String x2 = getRequiredParameter(request, "x2");
        String y1 = getRequiredParameter(request, "y1");
        String y2 = getRequiredParameter(request, "y2");

        String workspaceId = getWorkspaceId(request);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getUsername());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "not a valid visibility");
            chain.next(request, response);
            return;
        }

        JSONObject visibilityJson = GraphUtil.updateVisibilitySourceAndAddWorkspaceId(null, visibilitySource, workspaceId);
        LumifyVisibility lumifyVisibility = visibilityTranslator.toVisibility(visibilityJson);

        Concept concept = ontologyRepository.getConceptById(conceptId);
        Vertex artifactVertex = graph.getVertex(artifactId, authorizations);
        ElementMutation<Vertex> resolvedVertexMutation;
        DetectedObjectModel detectedObjectModel;
        Object id = graphVertexId != null && !graphVertexId.equals("") ? graphVertexId : graph.getIdGenerator().nextId();

        if (rowKey != null) {
            detectedObjectModel = detectedObjectRepository.findByRowKey(rowKey, user.getModelUserContext());
            detectedObjectModel.getMetadata().setResolvedId(id, lumifyVisibility.getVisibility());
            detectedObjectModel.getMetadata().setX1(Double.parseDouble(x1), lumifyVisibility.getVisibility());
            detectedObjectModel.getMetadata().setX2(Double.parseDouble(x2), lumifyVisibility.getVisibility());
            detectedObjectModel.getMetadata().setY1(Double.parseDouble(y1), lumifyVisibility.getVisibility());
            detectedObjectModel.getMetadata().setY2(Double.parseDouble(y2), lumifyVisibility.getVisibility());
            detectedObjectRepository.save(detectedObjectModel);
        } else {
            detectedObjectModel = detectedObjectRepository.saveDetectedObject(artifactId, id, conceptId, Double.parseDouble(x1), Double.parseDouble(y1), Double.parseDouble(x2), Double.parseDouble(y2), true, null, lumifyVisibility.getVisibility());
            rowKey = detectedObjectModel.getRowKey().getRowKey();
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString());

        Vertex resolvedVertex;
        if (graphVertexId == null || graphVertexId.equals("")) {
            resolvedVertexMutation = graph.prepareVertex(id, lumifyVisibility.getVisibility(), authorizations);
            CONCEPT_TYPE.setProperty(resolvedVertexMutation, concept.getId(), lumifyVisibility.getVisibility());
            TITLE.setProperty(resolvedVertexMutation, title, lumifyVisibility.getVisibility());

            resolvedVertex = resolvedVertexMutation.save();
            auditRepository.auditVertexElementMutation(AuditAction.UPDATE, resolvedVertexMutation, resolvedVertex, "", user, lumifyVisibility.getVisibility());

        } else {
            resolvedVertexMutation = graph.getVertex(id, authorizations).prepareMutation();
        }

        GraphUtil.addJustificationToMutation(resolvedVertexMutation, justificationText, sourceInfo, lumifyVisibility);

        resolvedVertexMutation.addPropertyValue(graph.getIdGenerator().nextId().toString(), "_rowKey", rowKey, metadata, lumifyVisibility.getVisibility());
        resolvedVertexMutation.setProperty(LumifyVisibilityProperties.VISIBILITY_PROPERTY.toString(), visibilitySource, metadata, lumifyVisibility.getVisibility());
        resolvedVertex = resolvedVertexMutation.save();
        auditRepository.auditVertexElementMutation(AuditAction.UPDATE, resolvedVertexMutation, resolvedVertex, "", user, lumifyVisibility.getVisibility());

        JSONObject result = detectedObjectModel.toJson();
        result.put("entityVertex", GraphUtil.toJson(resolvedVertex, workspaceId));

        Edge edge = graph.addEdge(artifactVertex, resolvedVertex, LabelName.RAW_CONTAINS_IMAGE_OF_ENTITY.toString(), lumifyVisibility.getVisibility(), authorizations);
        edge.setProperty(LumifyVisibilityProperties.VISIBILITY_JSON_PROPERTY.toString(), visibilityJson.toString(), lumifyVisibility.getVisibility());
        // TODO: replace second "" when we implement commenting on ui
        auditRepository.auditRelationship(AuditAction.CREATE, artifactVertex, resolvedVertex, edge, "", "", user, lumifyVisibility.getVisibility());

        // TODO: index the new vertex

        graph.flush();

        respondWithJson(response, result);
    }
}
