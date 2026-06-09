$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#Shapes"
        includeNamespaces: ["com.example"]
        rename: {
            "com.example#Foo": "RenamedFoo"
        }
    }
]

namespace com.example

structure Foo {
    bar: Bar
}

structure Bar {
    value: String
}
