$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector"
        id: "NoteTheList"
        message: "Note the list please"
        severity: "NOTE"
        configuration: {
            selector: "[id|namespace = smithy.example] list"
        }
    }
    {
        name: "EmitEachSelector"
        id: "NoteTheInteger"
        message: "Note the integer please"
        severity: "WARNING"
        configuration: {
            selector: "[id|namespace = smithy.example] integer"
        }
    }
    // This event is left alone.
    {
        name: "EmitEachSelector"
        id: "NoteTheString"
        message: "Note the string please"
        severity: "WARNING"
        configuration: {
            selector: "[id|namespace = smithy.example] string"
        }
    }
]

metadata severityOverrides = [
    {
        namespace: "*"
        id: "NoteTheList"
        severity: "DANGER"
    }
    {
        namespace: "smithy.example"
        id: "NoteTheInteger"
        severity: "DANGER"
    }
    // This one is ignored because it has a severity < DANGER.
    {
        namespace: "smithy.example"
        id: "NoteTheList"
        severity: "WARNING"
    }
]

namespace smithy.example

string MyString

integer IntegerTarget

list ListTarget {
    member: String
}
