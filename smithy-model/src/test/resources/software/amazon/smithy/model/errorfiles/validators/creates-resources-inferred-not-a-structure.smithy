$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
    properties: { size: Integer }
    create: StdCreate
}

operation StdCreate {
    input: StdCreateInput
    output: StdCreateOutput
}

@input
structure StdCreateInput {
    size: Integer
}

structure StdCreateOutput {
    @required
    widgetId: String
}

// `propertiesFrom` must point at a nested structure or a projection of structures. Here it resolves
// to a string, which cannot have its members inferred.
@createsResources([
    {
        resource: Widget
        propertiesFrom: "label"
    }
])
operation MakeWidget {
    input: MakeWidgetInput
    output: MakeWidgetOutput
}

@input
structure MakeWidgetInput {
    label: String
}

structure MakeWidgetOutput {
    widgetId: String
}
