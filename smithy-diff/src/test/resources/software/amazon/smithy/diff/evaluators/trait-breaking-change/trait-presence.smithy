$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "presence"}])
structure exampleTrait {}

@exampleTrait
string Example
