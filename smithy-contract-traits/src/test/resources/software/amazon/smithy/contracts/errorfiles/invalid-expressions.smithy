$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    Good: {
        documentation: "Good"
        expression: "length(string) < length(string2)"
    }
    WrongType: {
        documentation: "WrongType"
        expression: "string"
    }
    WrongType2: {
        documentation: "WrongType2"
        expression: "int"
    }
    MissingField: {
        documentation: "MissingField"
        expression: "foo"
    }
})
structure BadConditions {
    string: String
    string2: String
    int: Integer
}
