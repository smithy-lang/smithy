$version: "2.0"

namespace smithy.example

// Parse error here.
@externalDocumentation(foo:)

// Then recover on the line below.
/// Hello
@unknown
integer MyInteger
