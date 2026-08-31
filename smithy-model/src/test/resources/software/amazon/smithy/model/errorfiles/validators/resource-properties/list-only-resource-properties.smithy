$version: "2.0"

namespace smithy.example

service NamespaceStore {
    version: "2022-04-01"
    resources: [
        Namespace
    ]
}

resource Namespace {
    identifiers: {
        namespaceName: NamespaceName
    }
    resources: [
        Task
    ]
}

resource Task {
    identifiers: {
        namespaceName: NamespaceName
        taskName: TaskName
    }
    properties: {
        name: Name
        status: Status
    }
    list: ListTasks
}

@readonly
operation ListTasks {
    input: ListTasksInput
    output: ListTasksOutput
}

structure ListTasksInput {
    @required
    namespaceName: NamespaceName
}

structure ListTasksOutput {
    tasks: TaskSummaries
}

list TaskSummaries {
    member: TaskSummary
}

structure TaskSummary {
    taskName: TaskName
    name: Name
    status: Status
}

string NamespaceName
string TaskName
string Name
string Status
