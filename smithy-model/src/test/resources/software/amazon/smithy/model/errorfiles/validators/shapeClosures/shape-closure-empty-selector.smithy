$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#EmptySelector"
        includeBySelector: ""
    }
    {
        id: "com.example#SelectorMatchesNothing"
        includeBySelector: "[id = 'com.example#DoesNotExist']"
    }
    {
        id: "com.example#SelectorMatchesNothingOutsideNamespace"
        includeBySelector: "[id = 'com.example#DoesNotExist']"
        includeNamespaces: ["com.example"]
    }
]

namespace com.example

structure Foo {}
