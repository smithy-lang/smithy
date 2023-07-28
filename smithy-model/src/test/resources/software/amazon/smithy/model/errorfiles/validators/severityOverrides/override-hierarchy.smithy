$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector"
        id: "NoteTheShape.Integer"
        message: "Note the shape"
        severity: "NOTE"
        configuration: {
            selector: "[id|namespace = smithy.example] integer"
        }
    }
    {
        name: "EmitEachSelector"
        id: "NoteTheShape.List"
        message: "Note the list please"
        severity: "NOTE"
        configuration: {
            selector: "[id|namespace = smithy.example] list"
        }
    }
]

metadata severityOverrides = [
    {
        namespace: "*"
        id: "NoteTheShape"
        severity: "WARNING"
    }
]

namespace smithy.example

integer IntegerTarget

list ListTarget {
    member: String
}
