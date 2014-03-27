package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.RAW_POSTER_FRAME;

public class ArtifactPosterFrame extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactPosterFrame.class);
    private final Graph graph;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;

    @Inject
    public ArtifactPosterFrame(
            final Graph graph,
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        String widthStr = getOptionalParameter(request, "width");
        int[] boundaryDims = new int[]{200, 200};

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        if (widthStr != null) {
            boundaryDims[0] = boundaryDims[1] = Integer.parseInt(widthStr);

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=thumnail" + boundaryDims[0] + ".jpg");

            byte[] thumbnailData = artifactThumbnailRepository.getThumbnailData(artifactVertex.getId(), "poster-frame", boundaryDims[0],
                    boundaryDims[1], user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        StreamingPropertyValue rawPosterFrameValue = RAW_POSTER_FRAME.getPropertyValue(artifactVertex);
        if (rawPosterFrameValue == null) {
            LOGGER.warn("Could not find raw poster from for artifact: %s", artifactVertex.getId().toString());
            respondWithNotFound(response);
            return;
        }
        InputStream in = rawPosterFrameValue.getInputStream();
        try {
            if (widthStr != null) {
                LOGGER.info("Cache miss for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                byte[] thumbnailData = artifactThumbnailRepository.createThumbnail(artifactVertex.getId(), "poster-frame", in,
                        boundaryDims, user).getMetadata().getData();
                ServletOutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
            } else {
                response.setContentType("image/png");
                IOUtils.copy(in, response.getOutputStream());
            }
        } finally {
            in.close();
        }
    }
}
