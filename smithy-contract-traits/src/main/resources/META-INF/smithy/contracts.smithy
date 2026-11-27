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

/// Constrains the calls of an operation to those that satisfy one or more JMESPath
/// expressions. Each expression must produce `true` for every call, evaluated over
/// the operation's `{input, output, error, before, after}` call
/// (for example `input.start < input.end`).
@trait(selector: "operation")
map contracts {
    /// Name of the contract
    key: ConditionName

    /// Definition of the contract
    value: Condition
}

/// A name of a condition.
@pattern("^[A-Z]+[A-Za-z0-9]*$")
string ConditionName

/// Defines an individual condition.
structure Condition {
    /// JMESPath expression that must evaluate to `true`.
    @required
    expression: String

    /// Documentation about the condition. Can use CommonMark.
    @required
    documentation: String
}
