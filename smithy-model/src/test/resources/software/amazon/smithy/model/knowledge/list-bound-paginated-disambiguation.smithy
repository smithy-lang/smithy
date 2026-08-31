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

// Two list-of-structure members: only the @paginated items member carries the
// resource elements; the other must not be selected.
structure ListTasksOutput {
    nextToken: String
    tasks: TaskSummaries
    relatedResources: RelatedResourceSummaries
}

list TaskSummaries {
    member: TaskSummary
}

list RelatedResourceSummaries {
    member: RelatedResourceSummary
}

structure TaskSummary {
    taskName: TaskName
    name: Name
    status: Status
}

structure RelatedResourceSummary {
    arn: String
    relationType: String
}

string TaskName
string Name
string Status
