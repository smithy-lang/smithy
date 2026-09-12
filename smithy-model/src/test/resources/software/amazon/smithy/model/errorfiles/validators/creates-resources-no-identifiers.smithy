$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource AsyncJob {
    identifiers: { jobId: String }
}

@createsResources([
    {
        resource: "com.example#AsyncJob"
    }
])
operation StartJob {
    input: StartJobInput
    output: StartJobOutput
}

@input
structure StartJobInput {}

structure StartJobOutput {}
