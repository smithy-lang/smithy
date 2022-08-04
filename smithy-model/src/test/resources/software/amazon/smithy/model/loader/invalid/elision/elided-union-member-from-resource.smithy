// Parse error at line 12, column 15 near `for`: Expected: '{', but found 'f'
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
