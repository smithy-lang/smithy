$version: "2.0"

metadata suppressions = [
    {
        id: "HttpMethodSemantics",
        namespace: "com.amazonaws.s3",
    },
    {
        id: "EnumTrait",
        namespace: "com.amazonaws.s3",
    }
]

namespace com.amazonaws.s3

use aws.api#service
use aws.auth#sigv4
use aws.customizations#s3UnwrappedXmlOutput
use aws.protocols#restXml
use aws.protocols#httpChecksum
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(
    sdkId: "S3",
    arnNamespace: "s3",
    cloudFormationName: "S3",
    cloudTrailEventSource: "s3.amazonaws.com",
    endpointPrefix: "s3",
)
@sigv4(
    name: "s3",
)
@restXml(
    noErrorWrapping: true
)
@title("Amazon Simple Storage Service")
@xmlNamespace(
    uri: "http://s3.amazonaws.com/doc/2006-03-01/",
)
service AmazonS3 {
    version: "2006-03-01",
    operations: [
        ListObjectsV2,
        GetBucketLocation,
        DeleteObjectTagging
    ],
}


// TODO This needs more test cases to enforce the setting
// resolution of config options, ARN based addressing, and more.
@httpRequestTests([
    {
        id: "S3DefaultAddressing",
        documentation: "S3 clients should map the default addressing style to virtual host.",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                },
            },
        },
    },
    {
        id: "S3VirtualHostAddressing",
        documentation: "S3 clients should support the explicit virtual host addressing style.",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "virtual",
                    },
                },
            },
        },
    },
    {
        id: "S3PathAddressing",
        documentation: "S3 clients should support the explicit path addressing style.",
        protocol: restXml,
        method: "GET",
        uri: "/mybucket",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "path",
                    },
                },
            },
        },
    },
    {
        id: "S3VirtualHostDualstackAddressing",
        documentation: """
            S3 clients should support the explicit virtual host
            addressing style with Dualstack.""",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.dualstack.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "virtual",
                        use_dualstack_endpoint: true,
                    },
                },
            },
        },
    },
    {
        id: "S3VirtualHostAccelerateAddressing",
        documentation: """
            S3 clients should support the explicit virtual host
            addressing style with S3 Accelerate.""",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3-accelerate.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "virtual",
                        use_accelerate_endpoint: true,
                    },
                },
            },
        },
    },
    {
        id: "S3VirtualHostDualstackAccelerateAddressing",
        documentation: """
            S3 clients should support the explicit virtual host
            addressing style with Dualstack and S3 Accelerate.""",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3-accelerate.dualstack.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "virtual",
                        use_dualstack_endpoint: true,
                        use_accelerate_endpoint: true,
                    },
                },
            },
        },
    },
    {
        id: "S3OperationAddressingPreferred",
        documentation: """
            S3 clients should resolve to the addressing style of the
            operation if defined on both the client and operation.""",
        protocol: restXml,
        method: "GET",
        uri: "/",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "list-type=2",
        ],
        params: {
            Bucket: "mybucket",
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                    s3: {
                        addressing_style: "path",
                    },
                },
                operation: {
                    s3: {
                        addressing_style: "virtual",
                    },
                },
            },
        },
    },
])
@http(
    method: "GET",
    uri: "/{Bucket}?list-type=2",
    code: 200,
)
@paginated(
    inputToken: "ContinuationToken",
    outputToken: "NextContinuationToken",
    pageSize: "MaxKeys",
)
operation ListObjectsV2 {
    input: ListObjectsV2Request,
    output: ListObjectsV2Output,
    errors: [
        NoSuchBucket,
    ],
}


@httpRequestTests([
    {
        id: "S3EscapeObjectKeyInUriLabel",
        documentation: """
            S3 clients should escape special characters in Object Keys
            when the Object Key is used as a URI label binding.
        """,
        protocol: restXml,
        method: "DELETE",
        uri: "/my%20key.txt",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "tagging"
        ],
        params: {
            Bucket: "mybucket",
            Key: "my key.txt"
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                },
            },
        },
    },
    {
        id: "S3EscapePathObjectKeyInUriLabel",
        documentation: """
            S3 clients should preserve an Object Key representing a path
            when the Object Key is used as a URI label binding, but still
            escape special characters.
        """,
        protocol: restXml,
        method: "DELETE",
        uri: "/foo/bar/my%20key.txt",
        host: "s3.us-west-2.amazonaws.com",
        resolvedHost: "mybucket.s3.us-west-2.amazonaws.com",
        body: "",
        queryParams: [
            "tagging"
        ],
        params: {
            Bucket: "mybucket",
            Key: "foo/bar/my key.txt"
        },
        vendorParamsShape: aws.protocoltests.config#AwsConfig,
        vendorParams: {
            scopedConfig: {
                client: {
                    region: "us-west-2",
                },
            },
        },
    }
])
@http(
    method: "DELETE",
    uri: "/{Bucket}/{Key+}?tagging",
    code: 204
)
operation DeleteObjectTagging {
    input: DeleteObjectTaggingRequest
    output: DeleteObjectTaggingOutput
}


@httpResponseTests([{
        id: "GetBucketLocationUnwrappedOutput",
        documentation: """
            S3 clients should use the @s3UnwrappedXmlOutput trait to determine
            that the response shape is not wrapped in a restxml operation-level XML node.
        """,
        code: 200,
        body: "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">us-west-2</LocationConstraint>",
        params: {
            "LocationConstraint": "us-west-2"
        },
        protocol: restXml
}])
@http(uri: "/{Bucket}?location", method: "GET")
@s3UnwrappedXmlOutput
operation GetBucketLocation {
    input: GetBucketLocationRequest,
    output: GetBucketLocationOutput,
}


structure CommonPrefix {
    Prefix: Prefix,
}

structure GetBucketLocationRequest {
    @httpLabel
    @required
    Bucket: BucketName,
}

@xmlName("LocationConstraint")
structure GetBucketLocationOutput {
    LocationConstraint: BucketLocationConstraint,
}

structure ListObjectsV2Request {
    @httpLabel
    @required
    Bucket: BucketName,

    @httpQuery("delimiter")
    Delimiter: Delimiter,

    @httpQuery("encoding-type")
    EncodingType: EncodingType,

    @httpQuery("max-keys")
    MaxKeys: MaxKeys,

    @httpQuery("prefix")
    Prefix: Prefix,

    @httpQuery("continuation-token")
    ContinuationToken: Token,

    @httpQuery("fetch-owner")
    FetchOwner: FetchOwner,

    @httpQuery("start-after")
    StartAfter: StartAfter,

    @httpHeader("x-amz-request-payer")
    RequestPayer: RequestPayer,

    @httpHeader("x-amz-expected-bucket-owner")
    ExpectedBucketOwner: AccountId,
}

structure ListObjectsV2Output {
    IsTruncated: IsTruncated,

    @xmlFlattened
    Contents: ObjectList,

    Name: BucketName,

    Prefix: Prefix,

    Delimiter: Delimiter,

    MaxKeys: MaxKeys,

    @xmlFlattened
    CommonPrefixes: CommonPrefixList,

    EncodingType: EncodingType,

    KeyCount: KeyCount,

    ContinuationToken: Token,

    NextContinuationToken: NextToken,

    StartAfter: StartAfter,
}

@input
structure DeleteObjectTaggingRequest {
    @httpLabel
    @required
    Bucket: BucketName

    @httpLabel
    @required
    Key: ObjectKey

    @httpQuery("versionId")
    VersionId: ObjectVersionId

    @httpHeader("x-amz-expected-bucket-owner")
    ExpectedBucketOwner: AccountId
}

@output
structure DeleteObjectTaggingOutput {
    @httpHeader("x-amz-version-id")
    VersionId: ObjectVersionId
}

@error("client")
structure NoSuchBucket {}

structure Object {
    Key: ObjectKey,

    LastModified: LastModified,

    ETag: ETag,

    Size: Size,

    StorageClass: ObjectStorageClass,

    Owner: Owner,
}

structure Owner {
    DisplayName: DisplayName,

    ID: ID,
}

list CommonPrefixList {
    member: CommonPrefix,
}

list ObjectList {
    member: Object,
}

string AccountId

string BucketName

string Delimiter

string DisplayName

enum EncodingType {
    @suppress(["EnumShape"])
    url
}

string ETag

boolean FetchOwner

string ID

boolean IsTruncated

integer KeyCount

timestamp LastModified

integer MaxKeys

string NextToken

@length(
    min: 1,
)
string ObjectKey

enum ObjectStorageClass {
    STANDARD
    REDUCED_REDUNDANCY
    GLACIER
    STANDARD_IA
    ONEZONE_IA
    INTELLIGENT_TIERING
    DEEP_ARCHIVE
    OUTPOSTS
}

string Prefix

enum RequestPayer {
    @suppress(["EnumShape"])
    requester
}

integer Size

string StartAfter

string Token

enum BucketLocationConstraint {
    @suppress(["EnumShape"])
    us_west_2 = "us-west-2"
}

string ObjectVersionId

