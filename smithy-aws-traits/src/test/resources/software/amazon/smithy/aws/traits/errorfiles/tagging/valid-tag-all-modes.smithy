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
    operations: [GetCurrentTime, TagResource, UntagResource, ListTagsForResource]
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

operation TagResource {
    input := {
        @required
        arn: String
        @length(max: 128)
        tags: TagList
    }
    output := { }
}

operation UntagResource {
    input := {
        @required
        arn: String
        @required
        tagKeys: TagKeys
    }
    output := { }
}

operation ListTagsForResource {
    input := {
        @required
        arn: String
    }
    output := {
        @length(max: 128)
        tags: TagList
    }
}

operation TagCity {
    input := {
        @required
        cityId: CityId
        @length(max: 128)
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
        tags: TagList
    }
}

@taggable(property: "tags", apiConfig: {tagApi: TagCity, untagApi: UntagCity, listTagsApi: ListTagsForCity})
resource City {
    identifiers: { cityId: CityId },
    properties: {
        name: String
        coordinates: CityCoordinates
        tags: TagList
    }
    read: GetCity,
    operations: [TagCity, UntagCity, ListTagsForCity],
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

