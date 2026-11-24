$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: WidgetId }
    properties: { name: String, size: Integer }
    create: CreateWidget
}

string WidgetId

operation CreateWidget {
    input := {
        name: String
        size: Integer
    }
    output := {
        @required
        widgetId: WidgetId
    }
}

// The pointed-at structures carry both the identifier and the properties, which is valid.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        propertiesFrom: "widget"
    }
])
operation DescribeWidget {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        widget: WidgetDescription
    }
}

@createsResources([
    {
        resource: Widget
        identifiersFrom: "widget"
    }
])
operation MakeWidget {
    output := {
        widget: WidgetDescription
    }
}

structure WidgetDescription {
    widgetId: WidgetId
    name: String
    size: Integer
}

// A member of the other kind must still target the identifier's or property's shape.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        propertiesFrom: "widget"
    }
])
operation DescribeBadIdentifier {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        widget: BadIdentifierDescription
    }
}

structure BadIdentifierDescription {
    widgetId: Integer
    name: String
}

@createsResources([
    {
        resource: Widget
        identifiersFrom: "widget"
    }
])
operation MakeBadProperty {
    output := {
        widget: BadPropertyDescription
    }
}

structure BadPropertyDescription {
    widgetId: WidgetId
    size: String
}

// An explicit @resourceIdentifier wins over a member that only matches the identifier by name,
// so `widgetId` is neither kind and is reported.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        propertiesFrom: "widget"
    }
])
operation DescribeShadowedIdentifier {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        widget: ShadowedIdentifierDescription
    }
}

structure ShadowedIdentifierDescription {
    widgetId: Integer

    @required
    @resourceIdentifier("widgetId")
    id: WidgetId

    name: String
}

// Two members resolving to the same property conflict.
@createsResources([
    {
        resource: Widget
        identifiersFrom: "widget"
    }
])
operation MakeConflictingProperty {
    output := {
        widget: ConflictingPropertyDescription
    }
}

structure ConflictingPropertyDescription {
    widgetId: WidgetId
    size: String

    @property(name: "size")
    sz: Integer
}
