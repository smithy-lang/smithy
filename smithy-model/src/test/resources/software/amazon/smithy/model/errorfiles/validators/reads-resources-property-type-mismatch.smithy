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

// reads resolves identifiers on the input and properties on the output. The `size` property targets
// Integer, but `label` on the output is a String.
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        properties: { size: { path: "label" } }
    }
])
operation GetWidget {
    input: GetWidgetInput
    output: GetWidgetOutput
}

@input
structure GetWidgetInput {
    widgetId: String
}

structure GetWidgetOutput {
    label: String
}
