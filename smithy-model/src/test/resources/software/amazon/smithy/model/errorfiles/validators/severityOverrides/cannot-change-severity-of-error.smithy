$version: "2.0"

metadata severityOverrides = [
    {
        namespace: "*"
        id: "LengthTrait"
        severity: "WARNING"
    }
]

namespace smithy.example

// This emits an error which the above severityOverride attempts, but fails, to lower the severity of.
@length(min: -1)
string Invalid
