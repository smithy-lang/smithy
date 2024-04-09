$version: "2.0"

namespace smithy.example

// Parse error here.
@externalDocumentation(foo:)
string MyString
// ^ Recover here
