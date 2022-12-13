$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "any"}])
structure exampleTrait {}

@exampleTrait
string Example
