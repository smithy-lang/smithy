$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "presence", severity: "WARNING"}])
structure exampleTrait {}

@exampleTrait
string Example
