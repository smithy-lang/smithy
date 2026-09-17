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
        SearchTasks
    ]
}

@readonly
operation SearchTasks {
    input: SearchTasksInput
    output: SearchTasksOutput
}

structure SearchTasksInput {}

// A single unambiguous list-of-structures member: the list lifecycle would
// auto-detect this, but plain collection operations never do.
structure SearchTasksOutput {
    tasks: TaskSummaries
}

list TaskSummaries {
    member: TaskSummary
}

structure TaskSummary {
    taskName: TaskName
    name: Name
}

string TaskName
string Name
