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

// `propertiesFrom` points at a projection of structures. It resolves to the element structure
// `WidgetSpec`, whose members are inferred by name against the resource's properties.
@createsResources([
    {
        resource: Widget
        propertiesFrom: "specs[*]"
    }
])
operation MakeWidgets {
    input: MakeWidgetsInput
    output: MakeWidgetsOutput
}

@input
structure MakeWidgetsInput {
    specs: SpecList
}

list SpecList {
    member: WidgetSpec
}

structure WidgetSpec {
    size: Integer
}

structure MakeWidgetsOutput {
    widgetIds: WidgetIdList
}

list WidgetIdList {
    member: String
}
