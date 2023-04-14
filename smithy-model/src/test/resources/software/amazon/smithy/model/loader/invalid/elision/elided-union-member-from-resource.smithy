// Syntax error at line 12, column 15: Expected LBRACE('{') but found IDENTIFIER('for') | Model
$version: "2.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
}

union MyUnion for MyResource {
    $id
}
