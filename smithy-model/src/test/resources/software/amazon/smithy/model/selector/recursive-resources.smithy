$version: "2"

// This model is broken. It's just used to test whether the topdown function
// blows up or not.

namespace smithy.example

resource A {
    resources: [B],
}

resource B {
    resources: [A],
}
