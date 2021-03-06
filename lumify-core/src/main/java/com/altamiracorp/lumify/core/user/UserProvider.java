package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Vertex;

public interface UserProvider {
    User createFromVertex(Vertex user);

    User getSystemUser();

    ModelUserContext getModelUserContext(Authorizations authorizations, String... additionalAuthorizations);
}
