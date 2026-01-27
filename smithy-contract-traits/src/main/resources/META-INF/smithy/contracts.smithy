$version: "2.0"

namespace smithy.contracts

/// Restricts shape values to those that satisfy one or more JMESPath expressions.
/// Each expression must produce `true`.
@trait(selector: ":not(:test(service, operation, resource))")
map conditions {
    /// Name of the condition
    key: ConditionName

    /// Definition of the condition
    value: Condition
}

@pattern("^[A-Z]+[A-Za-z0-9]*$")
string ConditionName

/// Defines an individual condition.
structure Condition {
    /// JMESPath expression that must evaluate to `true`.
    @required
    expression: String

    /// Description of the condition. Used in error messages when violated.
    @required
    description: String
}
