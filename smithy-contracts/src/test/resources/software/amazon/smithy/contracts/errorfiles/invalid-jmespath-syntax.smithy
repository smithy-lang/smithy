$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    BadSyntax: {
        expression: "||"
    }
})
structure FooBar {
    int: Integer
}
