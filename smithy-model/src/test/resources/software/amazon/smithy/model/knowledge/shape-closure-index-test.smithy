$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#Namespaced"
        includeNamespaces: ["com.example"]
    }
    {
        id: "com.example#BySelector"
        includeBySelector: "[id|namespace = 'com.other']"
    }
    {
        id: "com.example#Renamed"
        includeNamespaces: ["com.example"]
        rename: {
            "com.example#Foo": "RenamedFoo"
        }
    }
]

namespace com.example

use com.other#Other

structure Foo {
    bar: Bar
    other: Other
}

structure Bar {
    value: String
}
