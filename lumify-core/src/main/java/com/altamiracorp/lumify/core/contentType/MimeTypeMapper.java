package com.altamiracorp.lumify.core.contentType;

import java.io.InputStream;
import java.util.Map;

public interface MimeTypeMapper {
    public String guessMimeType(InputStream in, String fileExt) throws Exception;

    void init(Map map);
}
