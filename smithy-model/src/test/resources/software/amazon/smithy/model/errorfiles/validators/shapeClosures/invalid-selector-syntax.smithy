$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#EmptySelector"
        includeBySelector: ""
    }
    {
        id: "com.example#BadSelector"
        includeBySelector: "][not a selector"
    }
]

namespace com.example
