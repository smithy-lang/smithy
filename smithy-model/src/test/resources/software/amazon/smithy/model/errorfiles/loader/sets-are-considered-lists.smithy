$version: "1.0"

namespace smithy.example

// Even though sets aren't called out by the selector, it should
// be allowed anywhere a list is allowed.
@trait
set someSet {
    member: String
}
