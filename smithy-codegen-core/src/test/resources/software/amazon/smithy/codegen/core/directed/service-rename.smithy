$version: "2.0"

namespace smithy.example

// A service that disambiguates a cross-namespace name conflict with a rename, used to
// verify Directive.getRenames() returns the service's renames in service mode.
service Renaming {
    operations: [
        GetThing
    ]
    rename: {
        "foo.example#Widget": "FooWidget"
    }
}

operation GetThing {
    output := {
        widget: Widget
        fooWidget: foo.example#Widget
    }
}

structure Widget {}
