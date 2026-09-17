$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
]

namespace com.example

resource Attachment {
    identifiers: {
        instanceId: String
        volumeId: String
    }
}

@createsResources([
    {
        resource: "com.example#Attachment"
        identifiers: {
            instanceId: { path: "instances[*].instanceId" }
            volumeId: { path: "volumes[*].volumeId" }
        }
    }
])
operation AttachVolumes {
    input: AttachVolumesInput
    output: AttachVolumesOutput
}

@input
structure AttachVolumesInput {}

structure AttachVolumesOutput {
    instances: InstanceList
    volumes: VolumeList
}

list InstanceList {
    member: InstanceData
}

list VolumeList {
    member: VolumeData
}

structure InstanceData {
    instanceId: String
}

structure VolumeData {
    volumeId: String
}
