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
        status: Status
    }
    collectionOperations: [
        BatchDescribeTasks
    ]
}

@readonly
operation BatchDescribeTasks {
    input: BatchDescribeTasksInput
    output: BatchDescribeTasksOutput
}

structure BatchDescribeTasksInput {}

structure BatchDescribeTasksOutput {
    @nestedProperties
    tasks: TaskSummaries
}

list TaskSummaries {
    member: TaskSummary
}

structure TaskSummary {
    @required
    taskName: TaskName

    name: Name
    status: Status
}

string TaskName
string Name
string Status
