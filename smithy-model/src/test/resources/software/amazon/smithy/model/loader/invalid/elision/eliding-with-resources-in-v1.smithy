// Structures can only be bound to resources with Smithy version 2 or later. Attempted to bind a structure to a resource with version `1.0`.
$version: "1.0"

namespace smithy.example

resource MyResource {
    identifiers: {
        id: String
    }
}

structure Invalid for MyResource {
    $id
}
