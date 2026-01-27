$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    BadSyntax: {
        description: "I have no idea what this would mean"
        expression: "||"
    }
})
structure FooBar {
    int: Integer
}
