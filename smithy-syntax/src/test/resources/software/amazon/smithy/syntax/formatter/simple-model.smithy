$version: "2.0"

metadata foo = "hello"

namespace smithy.example

/// Documentation
@sensitive
@length(min: 10, max: 100)
string MyString
