package com.altamiracorp.lumify.web.routes.graph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.altamiracorp.lumify.core.ingest.ArtifactExtractedInfo;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactMetadata;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactType;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.lumify.web.routes.artifact.ArtifactThumbnailByRowKey;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class GraphVertexUploadImage extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphVertexUploadImage.class);

    private static final String ATTR_GRAPH_VERTEX_ID = "graphVertexId";
    private static final String DEFAULT_MIME_TYPE = "image";
    private static final String SOURCE_UPLOAD = "User Upload";

    private final ArtifactRepository artifactRepository;
    private final GraphRepository graphRepository;

    @Inject
    public GraphVertexUploadImage(final ArtifactRepository artifactRepo, final GraphRepository graphRepo) {
        artifactRepository = artifactRepo;
        graphRepository = graphRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, ATTR_GRAPH_VERTEX_ID);
        final List<Part> files = Lists.newArrayList(request.getParts());

        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        final User user = getUser(request);
        final Part file = files.get(0);

        final GraphVertex entityVertex = graphRepository.findVertex(graphVertexId, user);
        if (entityVertex == null) {
            LOGGER.warn("Could not find associated entity vertex for id: " + graphVertexId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Artifact artifact = convertToArtifact(file);
        artifactRepository.save(artifact, user.getModelUserContext());

        ArtifactExtractedInfo artifactDetails = new ArtifactExtractedInfo();
        artifactDetails.setArtifactType(ArtifactType.IMAGE.toString());
        artifactDetails.setTitle("Image of " + entityVertex.getProperty(PropertyName.TITLE));
        artifactDetails.setSource(SOURCE_UPLOAD);

        GraphVertex artifactVertex = null;
        if (artifact.getMetadata().getGraphVertexId() != null) {
            artifactVertex = graphRepository.findVertex(artifact.getMetadata().getGraphVertexId(), user);
        }
        if (artifactVertex == null) {
            artifactVertex = artifactRepository.saveToGraph(artifact, artifactDetails, user);
        }

        entityVertex.setProperty(PropertyName.GLYPH_ICON, ArtifactThumbnailByRowKey.getUrl(artifact.getRowKey()));
        graphRepository.commit();

        graphRepository.findOrAddRelationship(entityVertex.getId(), artifactVertex.getId(), LabelName.HAS_IMAGE, user);
        graphRepository.commit();

        respondWithJson(response, entityVertex.toJson());
    }

    private Artifact convertToArtifact(final Part file) throws IOException {
        final InputStream fileInputStream = file.getInputStream();
        final byte[] rawContent = IOUtils.toByteArray(fileInputStream);
        LOGGER.debug("Uploaded file raw content byte length: " + rawContent.length);

        final String fileName = file.getName();

        String mimeType = DEFAULT_MIME_TYPE;
        if (file.getContentType() != null) {
            mimeType = file.getContentType();
        }

        final String fileRowKey = RowKeyHelper.buildSHA256KeyString(rawContent);
        LOGGER.debug("Generated row key: " + fileRowKey);

        Artifact artifact = new Artifact(fileRowKey);
        ArtifactMetadata metadata = artifact.getMetadata();
        metadata.setCreateDate(new Date());
        metadata.setRaw(rawContent);
        metadata.setFileName(fileName);
        metadata.setFileExtension(FilenameUtils.getExtension(fileName));
        metadata.setMimeType(mimeType);

        return artifact;
    }
}