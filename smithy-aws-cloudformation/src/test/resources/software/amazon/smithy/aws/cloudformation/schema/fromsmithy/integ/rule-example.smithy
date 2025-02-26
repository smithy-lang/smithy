$version: "2.0"

namespace smithy.example

use aws.api#arn
use aws.api#tagEnabled
use aws.api#taggable
use aws.cloudformation#cfnName
use aws.cloudformation#cfnResource

@tagEnabled
service TestService {
    version: "2024-10-29"
    operations: [
        ListTagsForResource
        TagResource
        UntagResource
    ]
    resources: [
        Rule
    ]
}

/// Represents a Recycle Bin retention rule that governs the retention of specified resources
@arn(template: "rule/{ResourceName}")
@taggable(property: "Tags")
@cfnResource(primaryIdentifier: "RuleArn")
resource Rule {
    identifiers: {
        Identifier: RuleIdentifier
    }
    properties: {
        Status: RuleStatus
        Description: Description
        ResourceTags: ResourceTags
        LockConfiguration: LockConfiguration
        ExcludeResourceTags: ExcludeResourceTags
        ResourceType: ResourceType
        LockState: LockState
        RetentionPeriod: RetentionPeriod
        Tags: TagList
        RuleArn: RuleArn
    }
    create: CreateRule
    read: GetRule
    update: UpdateRule
    delete: DeleteRule
    list: ListRules
    operations: [
        LockRule
        UnlockRule
    ]
}

operation CreateRule {
    input: CreateRuleRequest
    output: CreateRuleResponse
    errors: [
        InternalServerException
        ServiceQuotaExceededException
        ValidationException
    ]
}

@idempotent
operation DeleteRule {
    input: DeleteRuleRequest
    output: DeleteRuleResponse
    errors: [
        ConflictException
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

@readonly
operation GetRule {
    input: GetRuleRequest
    output: GetRuleResponse
    errors: [
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

@paginated(inputToken: "NextToken", outputToken: "NextToken", items: "Rules", pageSize: "MaxResults")
@readonly
operation ListRules {
    input: ListRulesRequest
    output: ListRulesResponse
    errors: [
        InternalServerException
        ValidationException
    ]
}

@readonly
operation ListTagsForResource {
    input: ListTagsForResourceRequest
    output: ListTagsForResourceResponse
    errors: [
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

operation LockRule {
    input: LockRuleRequest
    output: LockRuleResponse
    errors: [
        ConflictException
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

operation TagResource {
    input: TagResourceRequest
    output: TagResourceResponse
    errors: [
        InternalServerException
        ResourceNotFoundException
        ServiceQuotaExceededException
        ValidationException
    ]
}

operation UnlockRule {
    input: UnlockRuleRequest
    output: UnlockRuleResponse
    errors: [
        ConflictException
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

operation UntagResource {
    input: UntagResourceRequest
    output: UntagResourceResponse
    errors: [
        InternalServerException
        ResourceNotFoundException
        ValidationException
    ]
}

operation UpdateRule {
    input: UpdateRuleRequest
    output: UpdateRuleResponse
    errors: [
        ConflictException
        InternalServerException
        ResourceNotFoundException
        ServiceQuotaExceededException
        ValidationException
    ]
}

@error("client")
structure ConflictException {
    Message: ErrorMessage
    Reason: ConflictExceptionReason
}

structure CreateRuleRequest {
    @required
    RetentionPeriod: RetentionPeriod

    Description: Description

    Tags: TagList

    @required
    ResourceType: ResourceType

    ResourceTags: ResourceTags

    LockConfiguration: LockConfiguration

    ExcludeResourceTags: ExcludeResourceTags
}

structure CreateRuleResponse {
    @required
    Identifier: RuleIdentifier

    RetentionPeriod: RetentionPeriod

    Description: Description

    Tags: TagList

    ResourceType: ResourceType

    ResourceTags: ResourceTags

    Status: RuleStatus

    LockConfiguration: LockConfiguration

    LockState: LockState

    RuleArn: RuleArn

    ExcludeResourceTags: ExcludeResourceTags
}

structure DeleteRuleRequest {
    @required
    Identifier: RuleIdentifier
}

structure DeleteRuleResponse {}

structure GetRuleRequest {
    @required
    Identifier: RuleIdentifier
}

structure GetRuleResponse {
    @required
    Identifier: RuleIdentifier

    Description: Description

    ResourceType: ResourceType

    RetentionPeriod: RetentionPeriod

    ResourceTags: ResourceTags

    Status: RuleStatus

    LockConfiguration: LockConfiguration

    LockState: LockState

    @notProperty
    LockEndTime: TimeStamp

    @cfnName("Arn")
    RuleArn: RuleArn

    ExcludeResourceTags: ExcludeResourceTags
}

@error("server")
structure InternalServerException {
    Message: ErrorMessage
}

structure ListRulesRequest {
    MaxResults: MaxResults

    NextToken: NextToken

    @required
    ResourceType: ResourceType

    ResourceTags: ResourceTags

    LockState: LockState

    ExcludeResourceTags: ExcludeResourceTags
}

structure ListRulesResponse {
    Rules: RuleSummaryList
    NextToken: NextToken
}

structure ListTagsForResourceRequest {
    @required
    ResourceArn: RuleArn
}

structure ListTagsForResourceResponse {
    Tags: TagList
}

structure LockConfiguration {
    @required
    UnlockDelay: UnlockDelay
}

structure LockRuleRequest {
    @required
    Identifier: RuleIdentifier

    @required
    LockConfiguration: LockConfiguration
}

structure LockRuleResponse {
    @required
    Identifier: RuleIdentifier

    Description: Description

    ResourceType: ResourceType

    RetentionPeriod: RetentionPeriod

    ResourceTags: ResourceTags

    Status: RuleStatus

    LockConfiguration: LockConfiguration

    LockState: LockState

    RuleArn: RuleArn

    ExcludeResourceTags: ExcludeResourceTags
}

@error("client")
structure ResourceNotFoundException {
    Message: ErrorMessage
    Reason: ResourceNotFoundExceptionReason
}

structure ResourceTag {
    @required
    ResourceTagKey: ResourceTagKey

    ResourceTagValue: ResourceTagValue
}

structure RetentionPeriod {
    @required
    RetentionPeriodValue: RetentionPeriodValue

    @required
    RetentionPeriodUnit: RetentionPeriodUnit
}

structure RuleSummary {
    Identifier: RuleIdentifier
    Description: Description
    RetentionPeriod: RetentionPeriod
    LockState: LockState
    RuleArn: RuleArn
}

@error("client")
structure ServiceQuotaExceededException {
    Message: ErrorMessage
    Reason: ServiceQuotaExceededExceptionReason
}

structure Tag {
    @required
    Key: TagKey

    @required
    Value: TagValue
}

structure TagResourceRequest {
    @required
    ResourceArn: RuleArn

    @required
    Tags: TagList
}

structure TagResourceResponse {}

structure UnlockDelay {
    @required
    UnlockDelayValue: UnlockDelayValue

    @required
    UnlockDelayUnit: UnlockDelayUnit
}

structure UnlockRuleRequest {
    @required
    Identifier: RuleIdentifier
}

structure UnlockRuleResponse {
    @required
    Identifier: RuleIdentifier

    Description: Description

    ResourceType: ResourceType

    RetentionPeriod: RetentionPeriod

    ResourceTags: ResourceTags

    Status: RuleStatus

    LockConfiguration: LockConfiguration

    LockState: LockState

    @notProperty
    LockEndTime: TimeStamp

    RuleArn: RuleArn

    ExcludeResourceTags: ExcludeResourceTags
}

structure UntagResourceRequest {
    @required
    ResourceArn: RuleArn

    @required
    TagKeys: TagKeyList
}

structure UntagResourceResponse {}

structure UpdateRuleRequest {
    @required
    Identifier: RuleIdentifier

    RetentionPeriod: RetentionPeriod

    Description: Description

    ResourceType: ResourceType

    ResourceTags: ResourceTags

    ExcludeResourceTags: ExcludeResourceTags
}

structure UpdateRuleResponse {
    @required
    Identifier: RuleIdentifier

    RetentionPeriod: RetentionPeriod

    Description: Description

    ResourceType: ResourceType

    ResourceTags: ResourceTags

    Status: RuleStatus

    LockState: LockState

    @notProperty
    LockEndTime: TimeStamp

    RuleArn: RuleArn

    ExcludeResourceTags: ExcludeResourceTags
}

@error("client")
structure ValidationException {
    Message: ErrorMessage
    Reason: ValidationExceptionReason
}

@length(min: 0, max: 5)
list ExcludeResourceTags {
    member: ResourceTag
}

@length(min: 0, max: 50)
list ResourceTags {
    member: ResourceTag
}

list RuleSummaryList {
    member: RuleSummary
}

@length(min: 0, max: 200)
list TagKeyList {
    member: TagKey
}

@length(min: 0, max: 200)
list TagList {
    member: Tag
}

enum ConflictExceptionReason {
    INVALID_RULE_STATE
}

@pattern("^[\\S ]{0,255}$")
string Description

string ErrorMessage

enum LockState {
    LOCKED = "locked"
    PENDING_UNLOCK = "pending_unlock"
    UNLOCKED = "unlocked"
}

@range(min: 1, max: 1000)
integer MaxResults

@pattern("^[A-Za-z0-9+/=]{1,2048}$")
string NextToken

enum ResourceNotFoundExceptionReason {
    RULE_NOT_FOUND
}

@pattern("^[\\S\\s]{1,128}$")
string ResourceTagKey

@pattern("^[\\S\\s]{0,256}$")
string ResourceTagValue

enum ResourceType {
    EBS_SNAPSHOT
    EC2_IMAGE
}

enum RetentionPeriodUnit {
    DAYS
}

@range(min: 1, max: 3650)
integer RetentionPeriodValue

@length(min: 0, max: 1011)
@pattern("^arn:aws(-[a-z]{1,3}){0,2}:ruler:[a-z\\-0-9]{0,63}:[0-9]{12}:rule/[0-9a-zA-Z]{11}{0,1011}$")
string RuleArn

@pattern("^[0-9a-zA-Z]{11}$")
string RuleIdentifier

enum RuleStatus {
    PENDING = "pending"
    AVAILABLE = "available"
}

enum ServiceQuotaExceededExceptionReason {
    SERVICE_QUOTA_EXCEEDED
}

@length(min: 1, max: 128)
@pattern("^([\\p{L}\\p{Z}\\p{N}_.:/=+\\-@]*)$")
string TagKey

@length(min: 0, max: 256)
@pattern("^([\\p{L}\\p{Z}\\p{N}_.:/=+\\-@]*)$")
string TagValue

timestamp TimeStamp

enum UnlockDelayUnit {
    DAYS
}

@range(min: 7, max: 30)
integer UnlockDelayValue

enum ValidationExceptionReason {
    INVALID_PAGE_TOKEN
    INVALID_PARAMETER_VALUE
}
