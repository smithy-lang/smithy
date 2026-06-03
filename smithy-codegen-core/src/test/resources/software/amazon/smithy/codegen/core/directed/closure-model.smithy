$version: "2.0"

metadata shapeClosures = [
    {
        id: "smithy.example#cityData"
        includeBySelector: "[id = 'smithy.example#City']"
    }
    {
        id: "smithy.example#getCityClosure"
        includeBySelector: "[id = 'smithy.example#GetCity']"
    }
    {
        id: "smithy.example#renamedCityData"
        includeBySelector: "[id = 'smithy.example#City']"
        rename: {
            "smithy.example#City": "ZanyCity"
        }
    }
    {
        id: "smithy.example#getForecastClosure"
        includeBySelector: "[id = 'smithy.example#GetForecast']"
    }
    {
        id: "smithy.example#serviceClosure"
        includeBySelector: "[id = 'smithy.example#Weather']"
    }
    {
        id: "smithy.example#cityDirectoryClosure"
        includeBySelector: "[id = 'smithy.example#CityDirectory']"
    }
    {
        id: "smithy.example#mixinUserClosure"
        includeBySelector: "[id = 'smithy.example#MixinUser']"
    }
]

namespace smithy.example

service Weather {
    operations: [
        GetCity
        GetForecast
    ]
    resources: [
        Forecast
    ]
}

resource Forecast {
    identifiers: { forecastId: String }
}

// A separate service that defines service-level pagination defaults, used to verify
// that flattenPaginationInfoIntoOperations works when a closure (rooted at an
// operation, not this service) is driving code generation.
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults")
service CityDirectory {
    operations: [
        ListCities
    ]
}

@readonly
@paginated(items: "items")
operation ListCities {
    input := {
        maxResults: Integer
        nextToken: String
    }
    output := {
        items: CityNames
        nextToken: String
    }
}

list CityNames {
    member: String
}

@mixin
structure CommonFields {
    id: String
}

// Uses a mixin so that flattenAndRemoveMixins (run by performDefaultCodegenTransforms)
// can be verified in closure-based generation. The mixin member is flattened in and the
// mixin shape itself is removed from the model.
structure MixinUser with [CommonFields] {
    name: String
}

operation GetCity {
    input := {
        cityId: String
    }
    output := {
        city: City
    }
}

// Shares a single structure for input and output so that
// createDedicatedInputsAndOutputs must synthesize dedicated shapes.
operation GetForecast {
    input: ForecastData
    output: ForecastData
}

structure ForecastData {
    temperature: Integer
}

structure City {
    name: String
    conditions: Conditions
    coordinates: Coordinates
}

structure Coordinates {
    latitude: Float
    longitude: Float
}

enum Conditions {
    SUNNY
    RAINY
}
