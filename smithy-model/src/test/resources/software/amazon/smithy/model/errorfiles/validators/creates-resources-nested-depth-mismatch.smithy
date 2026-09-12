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

// clusterId iterates one list (clusters); nodeId iterates two (clusters then nodes). The cardinality
// signatures differ, so the identifiers cannot be correlated element-for-element. The old textual
// prefix heuristic compared the substring before the first `[*]` (both "clusters") and missed this.
@createsResources([
    {
        resource: Reservation
        identifiers: {
            clusterId: { path: "clusters[*].clusterId" }
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
    clusterId: String
    nodes: NodeList
}

list NodeList {
    member: Node
}

structure Node {
    nodeId: String
}
