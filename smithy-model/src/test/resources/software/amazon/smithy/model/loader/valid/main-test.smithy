namespace example.weather

service Weather {
    version: "2006-03-01"
    resources: [City]
    operations: [GetCurrentTime]
}

resource City {
    identifiers: {
        cityId: CityId
    }
    read: GetCity
    list: ListCities
    resources: [Forecast]
}

resource Forecast {
    identifiers: {
        cityId: CityId
    }
    read: GetForecast
}

// Pattern is a trait.
@pattern("^[A-Za-z0-9 ]+$")
string CityId

@readonly
operation GetCurrentTime {
    output: GetCurrentTimeOutput
}

structure GetCurrentTimeOutput {
    time: smithy.api#Timestamp
}

@readonly
operation GetForecast {
    input: GetForecastInput
    output: GetForecastOutput
    errors: [NoSuchResource]
}

structure GetForecastInput {
    @required
    cityId: CityId
}

structure GetForecastOutput {
    @required
    chanceOfRain: smithy.api#Float
    @required
    low: smithy.api#Integer
    @required
    high: smithy.api#Integer
}

@readonly
operation GetCity {
    input: GetCityInput
    output: GetCityOutput
    errors: [NoSuchResource]
}

structure GetCityInput {
    @required
    cityId: CityId
}

structure GetCityOutput {
    @required
    name: smithy.api#String
    @required
    coordinates: CityCoordinates
}

@readonly
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "pageSize", items: "items")
operation ListCities {
    input: ListCitiesInput
    output: ListCitiesOutput
}

structure ListCitiesInput {
    nextToken: smithy.api#String
    pageSize: smithy.api#Integer
}

// Traits can be applied outside of the definition.
apply ListCitiesInput @documentation("hello!")
structure ListCitiesOutput {
    nextToken: smithy.api#String
    @required
    items: CitySummaries
}

structure CityCoordinates {
    @required
    latitude: smithy.api#Float
    @required
    longitude: smithy.api#Float
}

@error("client")
structure NoSuchResource {
    @required
    resourceType: smithy.api#String
}

list CitySummaries {
    member: CitySummary
}

@references([{
    resource: City
}])
structure CitySummary {
    @required
    cityId: CityId
    @required
    name: smithy.api#String
}

