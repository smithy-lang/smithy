$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    BadSyntax: {
        documentation: "I have no idea what this would mean"
        expression: "||"
    }
})
structure FooBar {
    int: Integer
}
