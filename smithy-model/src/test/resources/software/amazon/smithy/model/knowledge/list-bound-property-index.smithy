$version: "2.0"

namespace com.example

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
    list: ListTasks
}

@readonly
@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "tasks")
operation ListTasks {
    input: ListTasksInput
    output: ListTasksOutput
}

structure ListTasksInput {
    nextToken: String
    maxResults: Integer
}

structure ListTasksOutput {
    nextToken: String
    tasks: TaskSummaries
}

list TaskSummaries {
    member: TaskSummary
}

structure TaskSummary {
    taskName: TaskName
    name: Name
    status: Status
    lastUpdatedAt: Timestamp
}

string TaskName
string Name
string Status
