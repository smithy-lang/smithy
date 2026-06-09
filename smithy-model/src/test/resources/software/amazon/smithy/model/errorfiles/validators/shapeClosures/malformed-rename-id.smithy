$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#Good"
        includeNamespaces: ["com.example"]
    }
    {
        id: "com.example#Bad"
        includeNamespaces: ["com.example"]
        rename: { BadKey: "Renamed" }
    }
]

namespace com.example

structure Foo {}
