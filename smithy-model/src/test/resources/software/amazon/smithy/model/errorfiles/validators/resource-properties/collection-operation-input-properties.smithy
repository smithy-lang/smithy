$version: "2.0"

namespace smithy.example

service TaskStore {
    version: "2022-04-01"
    resources: [
        Task
    ]
}

resource Task {
    identifiers: {
        taskName: TaskName
    }
    properties: {
        name: Name
    }
    collectionOperations: [
        BatchPutTasks
    ]
}

@idempotent
operation BatchPutTasks {
    input: BatchPutTasksInput
    output: BatchPutTasksOutput
}

structure BatchPutTasksInput {
    @nestedProperties
    tasks: TaskItems
}

structure BatchPutTasksOutput {}

list TaskItems {
    member: TaskItem
}

structure TaskItem {
    @required
    taskName: TaskName

    name: Name
}

string TaskName
string Name
