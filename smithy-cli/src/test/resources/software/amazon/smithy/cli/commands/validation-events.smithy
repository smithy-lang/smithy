$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "EmitSuppressed",
        severity: "NOTE",
        configuration: {
            selector: "[id = smithy.example#Suppressed]"
        }
    },
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
    },
    {
        name: "EmitEachSelector",
        id: "EmitDangers",
        severity: "DANGER",
        configuration: {
            selector: "[id = smithy.example#Danger]"
        }
    }
]

namespace smithy.example

@suppress(["EmitSuppressed"])
string Suppressed

string Note

string Warning

string Danger

@required // this trait is invalid and causes an error.
string Error
