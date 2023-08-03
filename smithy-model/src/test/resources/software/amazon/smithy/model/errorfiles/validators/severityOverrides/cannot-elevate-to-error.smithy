$version: "2.0"

// Cannot override a severity to ERROR.
metadata severityOverrides = [
    {
        namespace: "*"
        id: "Foo"
        severity: "ERROR"
    }
]
