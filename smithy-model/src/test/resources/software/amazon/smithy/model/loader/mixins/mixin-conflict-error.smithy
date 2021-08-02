// This should fail when also loaded with mixin-conflict-acceptable.smithy
$version: "1.1"

namespace smithy.example

@mixin
structure A with B {
    a: Integer
}
