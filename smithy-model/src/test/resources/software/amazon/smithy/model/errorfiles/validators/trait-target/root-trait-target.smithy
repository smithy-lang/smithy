$version: "2.0"

namespace smithy.example

// Invalid: @root can only be applied to trait definitions.
@root
string NotATrait
