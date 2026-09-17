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

// The marker targets a structure rather than a list of structures, so it is
// reported instead of falling back to automatic detection of `tasks`.
structure ListTasksOutput {
    @nestedProperties
    summary: ResultSummary

    tasks: TaskSummaries
}

structure ResultSummary {
    total: Integer
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
