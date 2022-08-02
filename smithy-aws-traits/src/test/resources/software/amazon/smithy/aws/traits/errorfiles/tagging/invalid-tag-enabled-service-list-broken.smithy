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
    operations: [TagResource, UntagResource, ListTagsForResource]
}

structure Tag {
    key: String
    value: String
}

@uniqueItems
list TagKeys {
    member: String
}

list TagList {
    member: Tag
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
    input: ListTagsForResourceInput
    output: ListTagsForResourceOutput
}

structure ListTagsForResourceInput {
    @required
    zarn: String
}

structure ListTagsForResourceOutput {
    @length(max: 128)
    tags: TagList
}

@taggable
resource City {
    identifiers: { cityId: CityId },
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId

