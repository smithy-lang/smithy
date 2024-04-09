$version: "2.0"

namespace aws.iam

/// Provides a custom IAM action name. By default, the action name is the same as the operation name.
@deprecated(since: "2023-11-10", message: "Use the `name` member of the `aws.iam#iamAction` trait instead.")
@trait(selector: "operation")
string actionName

/// A brief description of what granting the user permission to invoke an operation would entail.
/// This description should begin with something similar to 'Enables the user to...' or 'Grants permission to...'
@deprecated(since: "2023-11-10", message: "Use the `documentation` member of the `aws.iam#iamAction` trait instead.")
@trait(selector: "operation")
string actionPermissionDescription

/// Applies condition keys by name to a resource or operation.
@trait(selector: ":test(resource, operation)")
list conditionKeys {
    member: IamIdentifier
}

/// Uses the associated member’s value as this condition key’s value.
/// Needed when the member name doesn't match the condition key name.
@trait(selector: "member")
string conditionKeyValue

/// Defines the set of condition keys that appear within a service in addition to
/// inferred and global condition keys.
@trait(selector: "service")
map defineConditionKeys {
    key: IamIdentifier
    value: ConditionKeyDefinition
}

/// Disables the automatic inference of condition keys of service's resources or a specific resource.
@trait(selector: ":test(service, resource)")
structure disableConditionKeyInference {}

/// Indicates properties of a Smithy operation as an IAM action.
@trait(selector: "operation")
structure iamAction {
    /// The name of the action in AWS IAM.
    name: String

    /// A brief description of what granting the user permission to invoke an operation would entail.
    /// This description should begin with something similar to 'Enables the user to...' or 'Grants permission to...'
    documentation: String

    /// A relative URL path that defines more information about the action within a set of IAM-related documentation.
    relativeDocumentation: String

    /// Other actions that the invoker must be authorized to perform when executing the targeted operation.
    requiredActions: RequiredActionsList

    /// The resources an IAM action can be authorized against.
    resources: ActionResources

    /// The resources that performing this IAM action will create.
    createsResources: ResourceNameList
}

/// Indicates properties of a Smithy resource in AWS IAM.
@trait(selector: "resource")
structure iamResource {
    /// The name of the resource in AWS IAM.
    name: String

    /// A relative URL path that defines more information about the resource
    /// within a set of IAM-related documentation.
    relativeDocumentation: String

    /// When set to `true`, decouples this IAM resource's condition keys from
    /// those of its parent resource(s).
    disableConditionKeyInheritance: Boolean
}

/// Other actions that the invoker must be authorized to perform when executing the targeted operation.
@deprecated(since: "2023-11-10", message: "Use the `requiredActions` member of the `aws.iam#iamAction` trait instead.")
@trait(selector: "operation")
list requiredActions {
    member: IamIdentifier
}

/// Specifies the list of IAM condition keys which must be resolved by the service,
/// as opposed to being pulled from the request.
@trait(selector: "service")
list serviceResolvedConditionKeys {
    member: IamIdentifier
}

/// The principal types that can use the service or operation.
@trait(selector: ":test(service, operation)")
list supportedPrincipalTypes {
    member: PrincipalType
}

/// A container for information on the resources that an IAM action may be authorized against.
@private
structure ActionResources {
    /// Resources that will always be authorized against for functionality of the IAM action.
    required: ActionResourceMap

    /// Resources that will be authorized against based on optional behavior of the IAM action.
    optional: ActionResourceMap
}

/// A defined condition key to appear within a service in addition to inferred and global condition keys.
@private
structure ConditionKeyDefinition {
    @required
    type: ConditionKeyType

    /// Defines documentation about the condition key.
    documentation: String

    /// A valid URL that defines more information about the condition key.
    externalDocumentation: String

    /// A relative URL path that defines more information about the condition key
    /// within a set of IAM-related documentation.
    relativeDocumentation: String
}

/// Contains information about a resource an IAM action can be authorized against.
@private
structure ActionResource {
    /// The condition keys used for authorizing against this resource.
    conditionKeys: ConditionKeysList
}

@private
map ActionResourceMap {
    key: ResourceName
    value: ActionResource
}

@private
@uniqueItems
list ConditionKeysList {
    member: String
}

@private
@uniqueItems
list RequiredActionsList {
    member: IamIdentifier
}

@private
@uniqueItems
list ResourceNameList {
    member: ResourceName
}

/// The IAM policy type of the value that will supplied for this context key
@private
enum ConditionKeyType {
    /// A String type that contains an Amazon Resource Name (ARN).
    ARN

    /// An unordered list of ARN types.
    ARRAY_OF_ARN = "ArrayOfARN"

    /// A String type that contains base-64 encoded binary data.
    BINARY = "Binary"

    /// An unordered list of Binary types.
    ARRAY_OF_BINARY = "ArrayOfBinary"

    /// A general string type.
    STRING = "String"

    /// An unordered list of String types.
    ARRAY_OF_STRING = "ArrayOfString"

    /// A general type for integers and floats.
    NUMERIC = "Numeric"

    /// An unordered list of Numeric types.
    ARRAY_OF_NUMERIC = "ArrayOfNumeric"

    /// A String type that conforms to the datetime profile of ISO 8601.
    DATE = "Date"

    /// An unordered list of Date types.
    ARRAY_OF_DATE = "ArrayOfDate"

    /// A general boolean type.
    BOOL = "Bool"

    /// An unordered list of Bool types.
    ARRAY_OF_BOOL = "ArrayOfBool"

    /// A String type that conforms to RFC 4632.
    IP_ADDRESS = "IPAddress"

    /// An unordered list of IPAddress types.
    ARRAY_OF_IP_ADDRESS = "ArrayOfIPAddress"
}

@pattern("^([A-Za-z0-9][A-Za-z0-9-\\.]{0,62}:[^:]+)$")
@private
string IamIdentifier

@private
string ResourceName

/// An IAM policy principal type.
@private
enum PrincipalType {
    /// An AWS account.
    ROOT = "Root"

    /// An AWS IAM user.
    IAM_USER = "IAMUser"

    /// An AWS IAM role.
    IAM_ROLE = "IAMRole"

    /// A federated user session.
    FEDERATED_USER = "FederatedUser"
}
