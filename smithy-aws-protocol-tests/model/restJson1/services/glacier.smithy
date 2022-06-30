$version: "2.0"

metadata suppressions = [{
    id: "HttpMethodSemantics",
    namespace: "com.amazonaws.glacier",
}]

namespace com.amazonaws.glacier

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests

@service(
    sdkId: "Glacier",
    arnNamespace: "glacier",
    cloudFormationName: "Glacier",
    cloudTrailEventSource: "glacier.amazonaws.com",
    endpointPrefix: "glacier",
)
@sigv4(
    name: "glacier",
)
@restJson1
@title("Amazon Glacier")
@xmlNamespace(
    uri: "http://glacier.amazonaws.com/doc/2012-06-01/",
)
service Glacier {
    version: "2012-06-01",
    operations: [
        UploadArchive,
        UploadMultipartPart,
    ],
}

@httpRequestTests([
    {
        id: "GlacierVersionHeader",
        documentation: "Glacier requires that a version header be set on all requests.",
        protocol: restJson1,
        method: "POST",
        uri: "/foo/vaults/bar/archives",
        headers: {
            "X-Amz-Glacier-Version": "2012-06-01",
        },
        body: "",
        params: {
            accountId: "foo",
            vaultName: "bar",
        },
    },
    {
        id: "GlacierChecksums",
        documentation: "Glacier requires checksum headers that are cumbersome to provide.",
        protocol: restJson1,
        method: "POST",
        uri: "/foo/vaults/bar/archives",
        headers: {
            "X-Amz-Glacier-Version": "2012-06-01",
            "X-Amz-Content-Sha256": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            "X-Amz-Sha256-Tree-Hash": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
        },
        body: "hello world",
        params: {
            accountId: "foo",
            vaultName: "bar",
            body: "hello world"
        },
        appliesTo: "client",
    },
    {
        id: "GlacierAccountId",
        documentation: """
            Glacier requires that the account id be set, but you can just use a
            hyphen (-) to indicate the current account. This should be default
            behavior if the customer provides a null or empty string.""",
        protocol: restJson1,
        method: "POST",
        uri: "/-/vaults/bar/archives",
        headers: {
            "X-Amz-Glacier-Version": "2012-06-01",
        },
        body: "",
        params: {
            accountId: "",
            vaultName: "bar",
        },
        appliesTo: "client",
    }
])
@http(
    method: "POST",
    uri: "/{accountId}/vaults/{vaultName}/archives",
    code: 201,
)
operation UploadArchive {
    input: UploadArchiveInput,
    output: ArchiveCreationOutput,
    errors: [
        InvalidParameterValueException,
        MissingParameterValueException,
        RequestTimeoutException,
        ResourceNotFoundException,
        ServiceUnavailableException,
    ],
}

@httpRequestTests([
    {
        id: "GlacierMultipartChecksums",
        documentation: "Glacier requires checksum headers that are cumbersome to provide.",
        protocol: restJson1,
        method: "PUT",
        uri: "/foo/vaults/bar/multipart-uploads/baz",
        headers: {
            "X-Amz-Glacier-Version": "2012-06-01",
            "X-Amz-Content-Sha256": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            "X-Amz-Sha256-Tree-Hash": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
        },
        body: "hello world",
        params: {
            accountId: "foo",
            vaultName: "bar",
            uploadId: "baz",
            body: "hello world"
        },
        appliesTo: "client",
    }
])
@http(
    method: "PUT",
    uri: "/{accountId}/vaults/{vaultName}/multipart-uploads/{uploadId}",
    code: 204,
)
operation UploadMultipartPart {
    input: UploadMultipartPartInput,
    output: UploadMultipartPartOutput,
    errors: [
        InvalidParameterValueException,
        MissingParameterValueException,
        RequestTimeoutException,
        ResourceNotFoundException,
        ServiceUnavailableException,
    ],
}

structure ArchiveCreationOutput {
    @httpHeader("Location")
    location: string,
    @httpHeader("x-amz-sha256-tree-hash")
    checksum: string,
    @httpHeader("x-amz-archive-id")
    archiveId: string,
}

@error("client")
@httpError(400)
structure InvalidParameterValueException {
    type: string,
    code: string,
    message: string,
}

@error("client")
@httpError(400)
structure MissingParameterValueException {
    type: string,
    code: string,
    message: string,
}

@error("client")
@httpError(408)
structure RequestTimeoutException {
    type: string,
    code: string,
    message: string,
}

@error("client")
@httpError(404)
structure ResourceNotFoundException {
    type: string,
    code: string,
    message: string,
}

@error("server")
@httpError(500)
structure ServiceUnavailableException {
    type: string,
    code: string,
    message: string,
}

structure UploadArchiveInput {
    @httpLabel
    @required
    vaultName: string,
    @httpLabel
    @required
    accountId: string,
    @httpHeader("x-amz-archive-description")
    archiveDescription: string,
    @httpHeader("x-amz-sha256-tree-hash")
    checksum: string,
    @httpPayload
    body: Stream = "",
}

structure UploadMultipartPartInput {
    @httpLabel
    @required
    accountId: string,
    @httpLabel
    @required
    vaultName: string,
    @httpLabel
    @required
    uploadId: string,
    @httpHeader("x-amz-sha256-tree-hash")
    checksum: string,
    @httpHeader("Content-Range")
    range: string,
    @httpPayload
    body: Stream = "",
}

structure UploadMultipartPartOutput {
    @httpHeader("x-amz-sha256-tree-hash")
    checksum: string,
}

@streaming
blob Stream

string string
