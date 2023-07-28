$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector"
        id: "NoteTheInteger"
        message: "Note the integer please"
        severity: "WARNING"
        configuration: {
            selector: "[id|namespace = smithy.example] integer"
        }
    }
]

metadata suppressions = [
    {
        namespace: "*"
        id: "NoteTheInteger"
    }
]

metadata severityOverrides = [
    {
        namespace: "smithy.example"
        id: "NoteTheInteger"
        severity: "DANGER"
    }
]

namespace smithy.example

integer IntegerTarget
