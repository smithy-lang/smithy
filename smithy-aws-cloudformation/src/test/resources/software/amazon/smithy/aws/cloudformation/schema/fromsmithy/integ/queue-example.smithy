$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnAdditionalIdentifier
use aws.cloudformation#cfnName
use aws.cloudformation#cfnResource
use aws.cloudformation#cfnExcludeProperty
use aws.cloudformation#cfnMutability

service TestService {
    version: "2012-11-05",
    resources: [
        Queue,
    ]
}

/// Definition of Smithy::TestService::Queue Resource Type
@cfnResource(
    additionalSchemas: [
        GetQueueUrlResult,
        AttributeStructure,
    ])
resource Queue {
    // The QueueName is the literal identifier, but access
    // in other operations is handled through the QueueUrl.
    identifiers: {
        QueueName: String,
    },
    put: CreateQueue,
    operations: [
        GetQueueUrl,
    ],
}

// This structure is necessary to handle the way these queue
// attributes are handled as a map with an enum of allowed
// attributes.
@internal
structure AttributeStructure {
    @cfnMutability("read")
    @cfnAdditionalIdentifier
    Arn: String,
    ContentBasedDeduplication: Boolean,

    @range(min: 0, max: 900)
    DelaySeconds: Integer,

    @cfnMutability("create-and-read")
    FifoQueue: Boolean,
    KmsMasterKeyId: String,

    @range(min: 60, max: 86400)
    KmsDataKeyReusePeriodSeconds: Integer,

    @range(min: 1024, max: 262144)
    MaximumMessageSize: Integer,

    @range(min: 60, max: 1209600)
    MessageRetentionPeriod: Integer,

    @range(min: 0, max: 20)
    ReceiveMessageWaitTimeSeconds: Integer,
    RedrivePolicy: RedrivePolicy,

    @range(min: 0, max: 43200)
    VisibilityTimeout: Integer,
}

@internal
structure RedrivePolicy {
    deadLetterTargetArn: String,
    maxReceiveCount: Integer,
}

@idempotent
operation CreateQueue {
    input: CreateQueueRequest,
    output: CreateQueueResult,
    errors: [
        QueueDeletedRecently,
        QueueNameExists,
    ],
}

operation GetQueueUrl {
    input: GetQueueUrlRequest,
    output: GetQueueUrlResult,
    errors: [
        QueueDoesNotExist,
    ],
}

structure CreateQueueRequest {
    @cfnName("Tags")
    @cfnMutability("full")
    tags: TagMap,

    @required
    QueueName: String,

    // Exclude this property because we've modeled explicitly
    // as the AttributeStructure structure.
    @cfnExcludeProperty
    Attributes: QueueAttributeMap,
}

structure CreateQueueResult {
    QueueUrl: String,
}

structure GetQueueUrlRequest {
    QueueOwnerAWSAccountId: String,

    @required
    QueueName: String,
}

structure GetQueueUrlResult {
    @cfnAdditionalIdentifier
    @cfnMutability("read")
    @cfnName("URL")
    QueueUrl: String,
}

@error("client")
@httpError(400)
structure QueueDeletedRecently {}

@error("client")
@httpError(400)
structure QueueDoesNotExist {}

@error("client")
@httpError(400)
structure QueueNameExists {}

map QueueAttributeMap {
    key: QueueAttributeName,

    value: String,
}

map TagMap {
    key: TagKey,

    value: TagValue,
}

@enum([
    {
        value: "All",
    },
    {
        value: "Policy",
    },
    {
        value: "VisibilityTimeout",
    },
    {
        value: "MaximumMessageSize",
    },
    {
        value: "MessageRetentionPeriod",
    },
    {
        value: "ApproximateNumberOfMessages",
    },
    {
        value: "ApproximateNumberOfMessagesNotVisible",
    },
    {
        value: "CreatedTimestamp",
    },
    {
        value: "LastModifiedTimestamp",
    },
    {
        value: "QueueArn",
    },
    {
        value: "ApproximateNumberOfMessagesDelayed",
    },
    {
        value: "DelaySeconds",
    },
    {
        value: "ReceiveMessageWaitTimeSeconds",
    },
    {
        value: "RedrivePolicy",
    },
    {
        value: "FifoQueue",
    },
    {
        value: "ContentBasedDeduplication",
    },
    {
        value: "KmsMasterKeyId",
    },
    {
        value: "KmsDataKeyReusePeriodSeconds",
    },
])
string QueueAttributeName

string TagKey

string TagValue
