$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "remove"}])
structure exampleTrait {}

@exampleTrait
string Example
