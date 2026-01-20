$version: "2.0"

namespace smithy.contracts

/// Restricts shape values to those that satisfy one or more JMESPath expressions.
/// Each expression must produce 'true'.
@trait(selector: ":not(:test(service, operation, resource))")
list conditions {
    member: Condition
}

structure Condition {
    /// The identifier of the conditions.
    /// The provided `id` MUST match Smithy's `IDENTIFIER` ABNF.
    /// No two conditions on a single shape can share the same ID.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String

    /// JMESPath expression that must evaluate to true.
    @required
    expression: String

    /// Description of the condition. Used in error messages when violated.
    description: String
}
