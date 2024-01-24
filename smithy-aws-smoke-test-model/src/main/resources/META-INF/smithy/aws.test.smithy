$version: "2.0"

namespace aws.test

/// Base vendor params for all aws services.
@mixin
@suppress(["UnreferencedShape"])
structure BaseAwsVendorParams {
    /// The AWS region to sign the request for and to resolve the default
    /// endpoint with.
    region: String = "us-west-2"

    /// The set of regions to sign a sigv4a request with.
    sigv4aRegionSet: NonEmptyStringList

    /// A static endpoint to send the request to.
    uri: String

    /// Whether to resolve a FIPS compliant endpoint or not.
    useFips: Boolean = false

    /// Whether to resolve a dualstack endpoint or not.
    useDualstack: Boolean = false

    /// Whether to use account ID based routing where applicable.
    useAccountIdRouting: Boolean = true
}

@private
@length(min: 1)
@suppress(["UnreferencedShape"])
list NonEmptyStringList {
    member: String
}

/// Concrete vendor params to apply to AWS services by default.
@suppress(["UnreferencedShape"])
structure AwsVendorParams with [BaseAwsVendorParams] {}

/// Vendor params for S3.
@suppress(["UnreferencedShape"])
structure S3VendorParams with [BaseAwsVendorParams] {
    /// Whether to resolve an accelerate endpoint or not.
    useAccelerate: Boolean = false

    /// Whether to use the global endpoint for us-east-1.
    useGlobalEndpoint: Boolean = false

    /// Whether to force path-style addressing.
    forcePathStyle: Boolean = false

    /// Whether to use the region in the bucket arn to override the set
    /// region.
    useArnRegion: Boolean = true

    /// Whether to use S3's multi-region access points.
    useMultiRegionAccessPoints: Boolean = true
}
