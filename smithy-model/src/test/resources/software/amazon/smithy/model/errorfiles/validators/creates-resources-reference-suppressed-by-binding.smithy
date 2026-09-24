$version: "2"

// Intentionally NO MemberShouldReferenceResource suppression. Each operation below declares, via a
// resource lifecycle trait, that an identifier-named member refers to the bound resource. That is a
// declaration analogous to `@references`, so the linter must not warn about any of these members.

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
}

// 1. Bare-field identifier path: `widgetId` on the output is exempt.
@createsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
    }
])
operation CreateWidget {
    input: CreateWidgetInput
    output: CreateWidgetOutput
}

@input
structure CreateWidgetInput {}

structure CreateWidgetOutput {
    widgetId: String
}

// 2. Nested field-chain identifier path: `output.widgetId` resolves to `ResultData$widgetId`,
// which must be exempt even though it is not a top-level output member.
@createsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "output.widgetId" } }
    }
])
operation CreateWidgetNested {
    input: CreateWidgetNestedInput
    output: CreateWidgetNestedOutput
}

@input
structure CreateWidgetNestedInput {}

structure CreateWidgetNestedOutput {
    output: ResultData
}

structure ResultData {
    widgetId: String
}

// 3. No identifier locator at all: naming the resource is enough to exempt the identifier-named
// member `widgetId` on the operation's own output.
@createsResources([
    {
        resource: Widget
    }
])
operation CreateWidgetImplicit {
    input: CreateWidgetImplicitInput
    output: CreateWidgetImplicitOutput
}

@input
structure CreateWidgetImplicitInput {}

structure CreateWidgetImplicitOutput {
    widgetId: String
}
