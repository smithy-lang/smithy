$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
    properties: { size: Integer, color: String }
    create: StdCreate
}

operation StdCreate {
    input: StdCreateInput
    output: StdCreateOutput
}

@input
structure StdCreateInput {
    size: Integer
    color: String
}

structure StdCreateOutput {
    @required
    widgetId: String
}

@createsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        propertiesFrom: "spec"
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
    size: Integer
    color: String
}

structure MakeWidgetOutput {
    widgetId: String
}
