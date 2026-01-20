$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions([
    {
        id: "Good",
        expression: "length(string) < length(string2)"
    }
    {
        id: "WrongType",
        expression: "string"
    }
    {
        id: "WrongType2",
        expression: "int"
    }
    {
        id: "MissingField",
        expression: "foo"
    }

])
structure BadConditions {
    string: String
    string2: String
    int: Integer
}
