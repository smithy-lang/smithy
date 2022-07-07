$version: "1.0"

namespace aws.cloudformation

/// Indicates that the CloudFormation property generated from this member is an
/// additional identifier for the resource.
@unstable
@trait(
    selector: "structure > :test(member > string)",
    conflicts: [cfnExcludeProperty],
    breakingChanges: [{change: "remove"}]
)
structure cfnAdditionalIdentifier {}

/// The cloudFormationName trait allows a CloudFormation resource property name
/// to differ from a structure member name used in the model.
@unstable
@trait(
    selector: "structure > member",
    breakingChanges: [{change: "any"}]
)
string cfnName

/// Indicates that a structure member should not be included in generated
/// CloudFormation resource definitions.
@unstable
@trait(
    selector: "structure > member",
    conflicts: [
        cfnAdditionalIdentifier,
        cfnMutability,
    ],
    breakingChanges: [{change: "add"}]
)
structure cfnExcludeProperty {}

/// Indicates that a structure member is the default value to be used
/// for the CloudFormation property.
@unstable
@trait(
    selector: "structure > member",
    breakingChanges: [{change: "presence"}]
)
structure cfnDefaultValue {}

/// Indicates an explicit CloudFormation mutability of the structure member
/// when part of a CloudFormation resource.
@unstable
@trait(
    selector: "structure > member",
    conflicts: [cfnExcludeProperty]
)
@enum([
    {
        value: "full",
        name: "FULL",
        documentation: """
            Indicates that the CloudFormation property generated from this
            member does not have any mutability restrictions, meaning that it
            can be specified by the user and returned in a `read` or `list`
            request.""",
    },
    {
        value: "create-and-read",
        name: "CREATE_AND_READ",
        documentation: """
            Indicates that the CloudFormation property generated from this
            member can be specified only during resource creation and can be
            returned in a `read` or `list` request.""",
    },
    {
        value: "create",
        name: "CREATE",
        documentation: """
            Indicates that the CloudFormation property generated from this
            member can be specified only during resource creation and cannot
            be returned in a `read` or `list` request. MUST NOT be set if the
            member is also marked with the `@additionalIdentifier` trait.""",
    },
    {
        value: "read",
        name: "READ",
        documentation: """
            Indicates that the CloudFormation property generated from this
            member can be returned by a `read` or `list` request, but
            cannot be set by the user.""",
    },
    {
        value: "write",
        name: "WRITE",
        documentation: """
            Indicates that the CloudFormation property generated from this
            member can be specified by the user, but cannot be returned by a
            `read` or `list` request. MUST NOT be set if the member is also
            marked with the `@additionalIdentifier` trait.""",
    }
])
string cfnMutability

/// Indicates that a Smithy resource is a CloudFormation resource.
@unstable
@trait(
    selector: "resource",
    breakingChanges: [{change: "presence"}]
)
structure cfnResource {
    /// Provides a custom CloudFormation resource name.
    name: String,

    /// A list of additional shape IDs of structures that will have their
    /// properties added to the CloudFormation resource.
    additionalSchemas: StructureIdList,
}

@private
list StructureIdList {
    @idRef(failWhenMissing: true, selector: "structure")
    member: String
}
