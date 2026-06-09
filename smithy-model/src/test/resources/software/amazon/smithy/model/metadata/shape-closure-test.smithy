$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#primary"
        includeNamespaces: ["com.example"]
        includeBySelector: "string"
        rename: {
            "com.example#Foo": "RenamedFoo"
        }
    }
    {
        id: "com.example#secondary"
        includeNamespaces: ["com.example"]
    }
]

namespace com.example

structure Foo {
    bar: Bar
}

structure Bar {
    value: String
}
