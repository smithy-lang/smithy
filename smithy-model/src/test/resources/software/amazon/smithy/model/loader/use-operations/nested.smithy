namespace smithy.example.nested

use smithy.example.other#Hello2
use smithy.example.other#X
use smithy.example.other#GetHello2

operation Hello {}

resource Resource {
    // A "use" identifier.
    identifiers: {
        x: X,
        s: String,
    },

    // A "use" lifecycle operation.
    read: GetHello2,

    // A "use" instance operation.
    operations: [Hello2]
}

structure A {}

structure B {}

@error("client")
structure C {}
