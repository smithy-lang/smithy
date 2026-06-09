$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#NoOpRename"
        includeNamespaces: ["com.example"]
        rename: { "com.example#Foo": "Foo" }
    }
    {
        id: "com.example#OperationRename"
        includeNamespaces: ["com.example"]
        rename: { "com.example#GetFoo": "Renamed" }
    }
    {
        id: "com.example#ResourceRename"
        includeNamespaces: ["com.example"]
        rename: { "com.example#WidgetResource": "Renamed" }
    }
    {
        id: "com.example#ServiceRename"
        includeNamespaces: ["com.example"]
        rename: { "com.example#WidgetService": "Renamed" }
    }
    {
        id: "com.example#NonRenamedConflict"
        includeNamespaces: ["com.example"]
        rename: { "com.example#Foo": "BAR" }
    }
    {
        id: "com.example#RenameRenameConflict"
        includeNamespaces: ["com.example"]
        rename: {
            "com.example#Foo": "Conflict"
            "com.example#Bar": "CONFLICT"
        }
    }
    {
        id: "com.example#InvalidIdentifier"
        includeNamespaces: ["com.example"]
        rename: { "com.example#Foo": "123Foo" }
    }
]

namespace com.example

structure Foo {
    bar: Bar
}

structure Bar {
    value: String
}

operation GetFoo {
    input: Foo
    output: Foo
}

resource WidgetResource {
    identifiers: {
        id: String
    }
}

service WidgetService {
    version: "2024-01-01"
}
