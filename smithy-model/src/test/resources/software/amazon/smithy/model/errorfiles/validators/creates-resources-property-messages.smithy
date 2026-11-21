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
    input := {
        size: Integer
    }
    output := {
        @required
        widgetId: String
    }
}

// An explicit property name that the resource does not declare.
@createsResources([
    {
        resource: Widget
        properties: { weight: { path: "weight" } }
    }
])
operation MakeHeavyWidget {
    input := {
        weight: Integer
    }
}

// A propertiesFrom pointer outside the supported JMESPath subset.
@createsResources([
    {
        resource: Widget
        propertiesFrom: "specs[0]"
    }
])
operation MakeIndexedWidget {
    input := {
        specs: WidgetSpecs
    }
}

list WidgetSpecs {
    member: WidgetSpec
}

structure WidgetSpec {
    size: Integer
}
