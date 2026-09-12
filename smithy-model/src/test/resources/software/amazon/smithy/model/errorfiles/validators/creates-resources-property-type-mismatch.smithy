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

@createsResources([
    {
        resource: Widget
        properties: { size: { path: "spec.name" } }
    }
])
operation MakeWidget {
    input: MakeWidgetInput
    output: MakeWidgetOutput
}

@input
structure MakeWidgetInput {
    spec: WidgetSpec
}

structure WidgetSpec {
    name: String
}

structure MakeWidgetOutput {
    widgetId: String
}
