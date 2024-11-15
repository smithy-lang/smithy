$version: "2.0"

namespace smithy.example

operation IdempotencyTokenRequired {
    input := {
        @idempotencyToken
        @required
        token: String
    }
}
