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

@tagEnabled(disableDefaultOperations: true)
service Weather {
    version: "2006-03-01",
    resources: [City]
    operations: [GetCurrentTime]
}

service Weather2 {
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

list TagKeys {
    member: String
}

operation TagCity {
    input := {
        @required
        cityId: CityId
        @length(max: 128)
        @property(name: "tagz")
        tags: TagList
    }
    output := { }
}

operation UntagCity {
    input := {
        @required
        cityId: CityId
        @required
        @notProperty
        tagKeys: TagKeys
    }
    output := { }
}

operation ListTagsForCity {
    input := {
        @required
        cityId: CityId
    }
    output := { 
        @length(max: 128)
        @property(name: "tagz")
        tags: TagList
    }
}

@taggable(property: "tagz", apiConfig: {tagApi: TagCity, untagApi: UntagCity, listTagsApi: ListTagsForCity})
resource City {
    identifiers: { cityId: CityId },
    properties: {
        name: String
        coordinates: CityCoordinates
        tagz: TagList
    }
    create: CreateCity
    read: GetCity,
    update: UpdateCity
    operations: [TagCity, UntagCity, ListTagsForCity],
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

@pattern("^[A-Za-z0-9 ]+$")
string CityId

operation UpdateCity {
    input := {
        @required
        cityId: CityId
        tagz: TagList
    }
    output := {}
}

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
