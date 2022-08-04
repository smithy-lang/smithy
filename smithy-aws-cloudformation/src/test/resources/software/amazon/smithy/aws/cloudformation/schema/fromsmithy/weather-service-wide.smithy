$version: "2.0"

namespace example.weather

use aws.cloudformation#cfnResource
use aws.api#arn
use aws.api#taggable
use aws.api#tagEnabled

/// Provides weather forecasts.
@paginated(inputToken: "nextToken", outputToken: "nextToken",
           pageSize: "pageSize")
@tagEnabled
service Weather {
    version: "2006-03-01",
    resources: [City]
    operations: [GetCurrentTime, example.tagging#TagResource, example.tagging#UntagResource, example.tagging#ListTagsForResource]
}

@cfnResource
@taggable
@arn(template: "city/{CityId}")
resource City {
    identifiers: { cityId: CityId },
    properties: {
        name: String
        coordinates: CityCoordinates
    }
    create: CreateCity
    read: GetCity,
    update: UpdateCity
    list: ListCities,
    resources: [Forecast]
}

operation CreateCity {
    input := {
        name: String
        coordinates: CityCoordinates
    }
    output := {
        @required
        cityId: CityId
    }
}
operation UpdateCity {
    input := {
        @required
        cityId: CityId
        name: String
        coordinates: CityCoordinates
    }
    output := {}
}

/// @cfnResource
resource Forecast {
    identifiers: { cityId: CityId },
    read: GetForecast,
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId

@readonly
operation GetCity {
    input: GetCityInput,
    output: GetCityOutput,
    errors: [NoSuchResource]
}

@input
structure GetCityInput {
    // "cityId" provides the identifier for the resource and
    // has to be marked as required.
    @required
    cityId: CityId
}

@output
structure GetCityOutput {
    // "required" is used on output to indicate if the service
    // will always provide a value for the member.
    @required
    name: String,

    @required
    coordinates: CityCoordinates,
}

// This structure is nested within GetCityOutput.
structure CityCoordinates {
    @required
    latitude: Float,

    @required
    longitude: Float,
}

// "error" is a trait that is used to specialize
// a structure as an error.
@error("client")
structure NoSuchResource {
    @required
    resourceType: String
}

// The paginated trait indicates that the operation may
// return truncated results.
@readonly
@paginated(items: "items")
operation ListCities {
    input: ListCitiesInput,
    output: ListCitiesOutput
}

@input
structure ListCitiesInput {
    nextToken: String,
    pageSize: Integer
}

@output
structure ListCitiesOutput {
    nextToken: String,

    @required
    items: CitySummaries,
}

// CitySummaries is a list of CitySummary structures.
list CitySummaries {
    member: CitySummary
}

// CitySummary contains a reference to a City.
@references([{resource: City}])
structure CitySummary {
    @required
    cityId: CityId,

    @required
    name: String,
}

@readonly
operation GetCurrentTime {
    input: GetCurrentTimeInput,
    output: GetCurrentTimeOutput
}

@input
structure GetCurrentTimeInput {}

@output
structure GetCurrentTimeOutput {
    @required
    time: Timestamp
}

@readonly
operation GetForecast {
    input: GetForecastInput,
    output: GetForecastOutput
}

// "cityId" provides the only identifier for the resource since
// a Forecast doesn't have its own.
@input
structure GetForecastInput {
    @required
    cityId: CityId,
}

@output
structure GetForecastOutput {
    chanceOfRain: Float
}
