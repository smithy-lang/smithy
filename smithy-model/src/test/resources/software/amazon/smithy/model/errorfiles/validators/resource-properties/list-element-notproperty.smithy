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
    list: ListTasks
}

@readonly
operation ListTasks {
    input: ListTasksInput
    output: ListTasksOutput
}

structure ListTasksInput {}

structure ListTasksOutput {
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

    @notProperty
    derivedDisplayLabel: Name
}

string TaskName
string Name
