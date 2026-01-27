$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    Good: {
        description: "Good"
        expression: "length(string) < length(string2)"
    }
    WrongType: {
        description: "WrongType"
        expression: "string"
    }
    WrongType2: {
        description: "WrongType2"
        expression: "int"
    }
    MissingField: {
        description: "MissingField"
        expression: "foo"
    }
})
structure BadConditions {
    string: String
    string2: String
    int: Integer
}
