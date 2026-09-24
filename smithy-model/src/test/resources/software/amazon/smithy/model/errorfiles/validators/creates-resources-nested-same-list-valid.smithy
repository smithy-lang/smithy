$version: "2"

metadata suppressions = [
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Reservation {
    identifiers: {
        clusterId: String
        nodeId: String
    }
}

// Both identifiers project through the same two lists in the same order, so each element of the
// nested iteration yields a complete identifier tuple. This is unambiguous and valid.
@createsResources([
    {
        resource: Reservation
        identifiers: {
            clusterId: { path: "clusters[*].nodes[*].clusterId" }
            nodeId: { path: "clusters[*].nodes[*].nodeId" }
        }
    }
])
operation ReserveNodes {
    input: ReserveNodesInput
    output: ReserveNodesOutput
}

@input
structure ReserveNodesInput {}

structure ReserveNodesOutput {
    clusters: ClusterList
}

list ClusterList {
    member: Cluster
}

structure Cluster {
    nodes: NodeList
}

list NodeList {
    member: Node
}

structure Node {
    clusterId: String
    nodeId: String
}
