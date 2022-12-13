$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "presence", message: "This is bad!"}])
structure exampleTrait {}

@exampleTrait
string Example
