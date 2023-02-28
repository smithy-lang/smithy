$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02",
    resources: [Forecast]
}

@cfnResource()
resource Forecast {
    identifiers: {
        forecastId: String
    }
    properties: {
        chanceOfRain: Float
    }
    read: GetForecast
    put: PutForecast
    create: CreateForecast
    update: UpdateForecast
}

@readonly
operation GetForecast {
    input := {
        @required
        forecastId: String
    }
    output := {
        @nestedProperties
        forecastData: ForecastData
    }
}

@idempotent
operation PutForecast {
    input := {
        @required
        forecastId: String
        @nestedProperties
        forecastData: ForecastData
    }
}

operation CreateForecast {
    input := {
        @nestedProperties
        forecastData: ForecastData
    }
}

operation UpdateForecast {
    input := {
        @required
        forecastId: String
        @nestedProperties
        forecastData: ForecastData
    }
}

structure ForecastData for Forecast {
    $chanceOfRain
}
