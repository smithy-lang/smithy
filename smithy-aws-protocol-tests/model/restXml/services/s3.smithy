$version: "1.0"

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
use aws.protocols#restXml
use smithy.test#httpRequestTests

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
    ],
}

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

structure CommonPrefix {
    Prefix: Prefix,
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

@enum([
    {
        value: "url",
        name: "url",
    },
])
string EncodingType

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

@enum([
    {
        value: "STANDARD",
        name: "STANDARD",
    },
    {
        value: "REDUCED_REDUNDANCY",
        name: "REDUCED_REDUNDANCY",
    },
    {
        value: "GLACIER",
        name: "GLACIER",
    },
    {
        value: "STANDARD_IA",
        name: "STANDARD_IA",
    },
    {
        value: "ONEZONE_IA",
        name: "ONEZONE_IA",
    },
    {
        value: "INTELLIGENT_TIERING",
        name: "INTELLIGENT_TIERING",
    },
    {
        value: "DEEP_ARCHIVE",
        name: "DEEP_ARCHIVE",
    },
    {
        value: "OUTPOSTS",
        name: "OUTPOSTS",
    },
])
string ObjectStorageClass

string Prefix

@enum([
    {
        value: "requester",
        name: "requester",
    },
])
string RequestPayer

integer Size

string StartAfter

string Token
