$version: "2.0"

// Fixtures for combined mode, where a primary service is set together with a shape
// closure. The closure is the generated set and must contain the service.
metadata shapeClosures = [
    // Contains the Weather service (and therefore its operations and their data shapes),
    // plus an operation and a data shape that are not connected to the service. This makes
    // the closure's operation set differ from a top-down walk of the service alone.
    {
        id: "smithy.example#combinedClosure"
        includeBySelector: ":is([id = 'smithy.example#Weather'], [id = 'smithy.example#Standalone'], [id = 'smithy.example#ExtraType'])"
    }
    // Renames a shape so combined-mode renames can be verified.
    {
        id: "smithy.example#combinedRenamedClosure"
        includeBySelector: ":is([id = 'smithy.example#Weather'], [id = 'smithy.example#ExtraType'])"
        rename: {
            "smithy.example#ExtraType": "RenamedExtraType"
        }
    }
    // Does not contain the Weather service, so setting it together with the Weather service
    // is invalid combined mode.
    {
        id: "smithy.example#serviceNotIncluded"
        includeBySelector: "[id = 'smithy.example#ExtraType']"
    }
]

namespace smithy.example

service Weather {
    operations: [
        GetCity
        GetForecast
    ]
}

operation GetCity {
    input := {
        cityId: String
    }
    output := {
        city: City
    }
}

operation GetForecast {
    input := {
        cityId: String
    }
    output := {
        temperature: Integer
    }
}

// Not bound to the Weather service. It is pulled into the closure directly so the closure's
// operation set is a superset of the service's operations.
operation Standalone {
    input := {
        value: String
    }
    output := {
        result: String
    }
}

structure City {
    name: String
}

// Unconnected to any service; only reachable through the closure selector.
structure ExtraType {
    value: String
}
