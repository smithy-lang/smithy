$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    Good: {
        expression: "length(string) < length(string2)"
    }
    WrongType: {
        expression: "string"
    }
    WrongType2: {
        expression: "int"
    }
    MissingField: {
        expression: "foo"
    }
})
structure BadConditions {
    string: String
    string2: String
    int: Integer
}
