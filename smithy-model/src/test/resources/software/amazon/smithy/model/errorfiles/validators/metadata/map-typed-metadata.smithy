$version: "2.0"

metadata tags = {
    "env": "prod"
    "team": "core"
}

namespace smithy.example

@metadata(key: "tags")
map Tags {
    key: String
    value: String
}
