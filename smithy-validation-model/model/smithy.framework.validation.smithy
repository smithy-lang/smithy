$version: "2.0"

namespace smithy.framework

/// A standard error for input validation failures.
/// This should be thrown by services when a member of the input structure
/// falls outside of the modeled or documented constraints.
@error("client")
structure ValidationException {
    /// A summary of the validation failure.
    @required
    message: String

    /// A list of specific failures encountered while validating the input.
    /// A member can appear in this list more than once if it failed to satisfy multiple constraints.
    fieldList: ValidationExceptionFieldList
}

/// Describes one specific validation failure for an input member.
structure ValidationExceptionField {
    /// A JSONPointer expression to the structure member whose value failed to satisfy the modeled constraints.
    @required
    path: String

    /// A detailed description of the validation failure.
    @required
    message: String
}

list ValidationExceptionFieldList {
    member: ValidationExceptionField
}
