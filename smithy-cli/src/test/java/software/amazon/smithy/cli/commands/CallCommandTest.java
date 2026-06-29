/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CallCommandTest {

    @Test
    void redactsSigv4CredentialButKeepsScheme() {
        // sigv4 carries the access key id (Credential=) and the signature after the scheme; only the
        // scheme token may be shown.
        String header = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20260101/us-east-1/s3/aws4_request, "
                + "SignedHeaders=host;x-amz-date, Signature=deadbeefcafe";
        String redacted = CallCommand.redactAuthorization(header);

        assertEquals("AWS4-HMAC-SHA256 <redacted>", redacted);
    }

    @Test
    void keepsBasicSchemeAndRedactsCredentials() {
        assertEquals("Basic <redacted>", CallCommand.redactAuthorization("Basic dXNlcjpwYXNz"));
    }

    @Test
    void keepsBearerSchemeAndRedactsToken() {
        assertEquals("Bearer <redacted>", CallCommand.redactAuthorization("Bearer abc.def.ghi"));
    }

    @Test
    void schemeMatchIsCaseInsensitive() {
        assertEquals("bAsIc <redacted>", CallCommand.redactAuthorization("bAsIc dXNlcjpwYXNz"));
    }

    @Test
    void redactsUnknownSchemeWhole() {
        // An unrecognized scheme is fully redacted: the first token might itself be the secret.
        assertEquals("<redacted>", CallCommand.redactAuthorization("Custom super-secret-token"));
    }

    @Test
    void redactsValueWithNoSchemeTokenWhole() {
        // No space: the whole value is one token. If it isn't a known scheme, redact it entirely rather
        // than echo what could be a raw credential.
        assertEquals("<redacted>", CallCommand.redactAuthorization("rawtokenwithnospace"));
    }

    @Test
    void redactsEmptyValueWhole() {
        assertEquals("<redacted>", CallCommand.redactAuthorization(""));
    }
}
