$version: "2.0"

namespace aws.customizations

/// Indicates the response body from S3 is not wrapped in the
/// aws-restxml-protocol operation-level XML node. Intended to only be used by
/// AWS S3.
@trait(selector: "operation")
structure s3UnwrappedXmlOutput {}
