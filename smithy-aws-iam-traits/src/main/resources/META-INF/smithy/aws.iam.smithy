$version: "2.0"

namespace aws.iam

/// Provides a custom IAM action name. By default, the action name is the same as the operation name.
@trait(selector: "operation")
string actionName

/// A brief description of what granting the user permission to invoke an operation would entail.
/// This description should begin with something similar to 'Enables the user to...' or 'Grants permission to...'
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
///  inferred and global condition keys.
@trait(selector: "service")
map defineConditionKeys {
    key: IamIdentifier
    value: ConditionKeyDefinition
}

/// Disables the automatic inference of condition keys of service's resources or a specific resource.
@trait(selector: ":test(service, resource)")
structure disableConditionKeyInference {}

/// Indicates properties of a Smithy resource in AWS IAM.
@trait(selector: "resource")
structure iamResource {
    /// The name of the resource in AWS IAM.
    name: String

    ///  A relative URL path that defines more information about the resource
    ///  within a set of IAM-related documentation.
    relativeDocumentation: String
}

/// Other actions that the invoker must be authorized to perform when executing the targeted operation.
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

/// A defined condition key to appear within a service in addition to inferred and global condition keys.
@private
structure ConditionKeyDefinition {
    @required
    type: ConditionKeyType

    /// Defines documentation about the condition key.
    documentation: String

    /// A valid URL that defines more information about the condition key.
    externalDocumentation: String

    ///  A relative URL path that defines more information about the condition key
    ///  within a set of IAM-related documentation.
    relativeDocumentation: String
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
