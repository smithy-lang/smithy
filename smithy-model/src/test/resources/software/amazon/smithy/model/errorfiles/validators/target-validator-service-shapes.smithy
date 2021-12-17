namespace ns.foo

service InvalidService1 {
    version: "2020-07-02",
    shapes: [NotFound]
}

service InvalidService2 {
    version: "2020-07-02",
    shapes: [InvalidService1]
}

service InvalidService3 {
    version: "2020-07-02",
    shapes: [Resource]
}

service InvalidService4 {
    version: "2020-07-02",
    shapes: [CreateResource]
}

service InvalidService5 {
    version: "2020-07-02",
    shapes: [CreateResourceInput$name]
}

service InvalidService6 {
    version: "2020-07-02",
    shapes: [smithy.api#sensitive]
}

resource Resource {
    identifiers: {
        id: String,
    },
    create: CreateResource,
}

operation CreateResource {
    input: CreateResourceInput,
}

structure CreateResourceInput {
    name: String,
}
