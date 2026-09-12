$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
]

namespace com.example

resource Snapshot {
    identifiers: {
        dbInstanceId: String
        snapshotId: String
    }
}

@createsResources([
    {
        resource: "com.example#Snapshot"
        identifiers: {
            dbInstanceId: { path: "snapshots[*].dbInstanceId" }
            snapshotId: { path: "snapshots[*].snapshotId" }
        }
    }
])
operation BatchCreateSnapshots {
    input: BatchCreateSnapshotsInput
    output: BatchCreateSnapshotsOutput
}

@input
structure BatchCreateSnapshotsInput {}

structure BatchCreateSnapshotsOutput {
    snapshots: SnapshotList
}

list SnapshotList {
    member: SnapshotData
}

structure SnapshotData {
    dbInstanceId: String
    snapshotId: String
}
