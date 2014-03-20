package com.altamiracorp.lumify.web.routes.relationship;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.security.VisibilityTranslator;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Edge;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RelationshipSetVisibility extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RelationshipSetVisibility.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public RelationshipSetVisibility(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final VisibilityTranslator visibilityTranslator) {
        super(userRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphEdgeId = getAttributeString(request, "graphEdgeId");
        final String visibilitySource = getRequiredParameter(request, "visibilitySource");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getWorkspaceId(request);

        Edge graphEdge = graph.getEdge(graphEdgeId, authorizations);
        if (graphEdge == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getUsername());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "not a valid visibility");
            chain.next(request, response);
            return;
        }

        LOGGER.info("changing edge (%s) visibility source to %s", graphEdge.getId().toString(), visibilitySource);

        GraphUtil.updateVisibilitySource(this.graph, visibilityTranslator, graphEdge, visibilitySource, workspaceId);

        this.graph.flush();

        JSONObject json = GraphUtil.toJson(graphEdge, workspaceId);
        respondWithJson(response, json);
    }
}
