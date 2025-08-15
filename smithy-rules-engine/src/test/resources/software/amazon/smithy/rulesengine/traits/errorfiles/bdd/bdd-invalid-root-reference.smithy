$version: "2.0"

namespace smithy.example

use smithy.rules#endpointBdd

@endpointBdd({
    version: "1.1"
    parameters: {}
    conditions: []
    results: []
    nodes: ""  // Base64 encoded empty node array
    root: -5  // Invalid negative root reference (only -1 is allowed for FALSE)
    nodeCount: 0
})
service InvalidRootRefService {
    version: "2022-01-01"
    operations: [GetThing]
}

@readonly
operation GetThing {}
