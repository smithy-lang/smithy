/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.MediaType;
import software.amazon.smithy.utils.StringUtils;

/**
 * Shared validation utility functions for protocol tests.
 */
public final class ProtocolTestValidationUtils {
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    static {
        // Disallow loading DTDs and more for protocol test contents.
        try {
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DOCUMENT_BUILDER_FACTORY.setXIncludeAware(false);
            DOCUMENT_BUILDER_FACTORY.setExpandEntityReferences(false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private ProtocolTestValidationUtils() {}

    /**
     * Checks whether a body is well-formed according to its mediaType.
     *
     * <p>If the body is not well-formed, a exception with context will be returned.
     *
     * <p>Currently XML and JSON validation are supported.
     *
     * @param body         The body to validate.
     * @param rawMediaType The mediaType to validate the body with.
     * @return Returns an Optional Exception if the body is not valid.
     */
    public static Optional<Exception> validateMediaType(String body, String rawMediaType) {
        if (StringUtils.isEmpty(body) || StringUtils.isEmpty(rawMediaType)) {
            return Optional.empty();
        }

        MediaType mediaType = MediaType.from(rawMediaType);
        if (isXml(mediaType)) {
            return validateXml(body);
        } else if (isJson(mediaType)) {
            return validateJson(body);
        }

        return Optional.empty();
    }

    private static boolean isXml(MediaType mediaType) {
        return mediaType.getSubtype().equals("xml") || mediaType.getSuffix().orElse("").equals("xml");
    }

    private static Optional<Exception> validateXml(String body) {
        try {
            DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(body)));
            return Optional.empty();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return Optional.of(e);
        }
    }

    private static boolean isJson(MediaType mediaType) {
        return mediaType.getSubtype().equals("json") || mediaType.getSuffix().orElse("").equals("json");
    }

    private static Optional<Exception> validateJson(String body) {
        try {
            Node.parse(body);
            return Optional.empty();
        } catch (ModelSyntaxException e) {
            return Optional.of(e);
        }
    }
}
