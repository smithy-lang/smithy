$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait",
        namespace: "example.weather"
    }
]

namespace example.weather

use aws.api#taggable
use aws.api#tagEnabled

@tagEnabled
service Weather {
    version: "2006-03-01",
    resources: [City]
}

structure Tag {
    key: String
    value: String
}

list TagList {
    member: Tag
}

resource City {
    identifiers: { cityId: CityId },
    properties: {
        name: String
        coordinates: CityCoordinates
        tags: TagList
    }
    create: CreateCity
}

operation CreateCity {
    input := {
        name: String
        coordinates: CityCoordinates
        tags: TagList
    }
    output := {
        @required
        cityId: CityId
    }
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId

// This structure is nested within GetCityOutput.
structure CityCoordinates {
    @required
    latitude: Float,

    @required
    longitude: Float,
}

