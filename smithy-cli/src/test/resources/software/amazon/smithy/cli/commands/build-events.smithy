$version: "1.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "EmitNotes",
        severity: "NOTE",
        configuration: {
            selector: "[id = smithy.example#Note]"
        }
    },
    {
        name: "EmitEachSelector",
        id: "EmitWarnings",
        severity: "WARNING",
        configuration: {
            selector: "[id = smithy.example#Warning]"
        }
    }
]

namespace smithy.example

string Note

string Warning
