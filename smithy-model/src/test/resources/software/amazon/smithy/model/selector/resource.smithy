$version: "2.0"

namespace example.weather

service Weather {
    version: "2006-03-01"
    resources: [City]
}

resource City {
    identifiers: { cityId: CityId }
    properties: {
        name: String
        coordinates: CityCoordinates
    }
    read: GetCity
    resources: [Forecast]
}

resource Forecast {
    identifiers: { cityId: CityId }
    properties: { chanceOfRain: Float, name: String }
    read: GetForecast
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId

@readonly
operation GetCity {
    input: GetCityInput
    output: GetCityOutput
}

@input
structure GetCityInput {
    @required
    cityId: CityId
}

@output
structure GetCityOutput {
    @required
    name: String

    @required
    coordinates: CityCoordinates
}

structure CityCoordinates {
    @required
    latitude: Float

    @required
    longitude: Float
}

@readonly
operation GetForecast {
    input: GetForecastInput
    output: GetForecastOutput
}

@input
structure GetForecastInput {
    @required
    cityId: CityId
}

@output
structure GetForecastOutput {
    name: String
    chanceOfRain: Float
}

