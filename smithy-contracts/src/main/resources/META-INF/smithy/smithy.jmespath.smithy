$version: "2"

namespace smithy.jmespath

@trait(selector: "*")
@documentation("These expressions must produce 'true'")
map constraints {
    key: String
    value: Constraint
}

structure Constraint {
    /// JMESPath expression that must evaluate to true.
    @required
    path: String

    /// Description of the constraint. Used in error messages when violated.
    description: String
}