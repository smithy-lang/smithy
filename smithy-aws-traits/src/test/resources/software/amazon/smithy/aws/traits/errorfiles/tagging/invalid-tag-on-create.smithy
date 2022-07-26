$version: "2.0"

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

/// Tag-on-create, not defined as a resource property
@taggable(property: "tags")
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
    output := {
        @required
        cityId: CityId
    }
    input := {
        name: String
        coordinates: CityCoordinates
        tags: TagList
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

