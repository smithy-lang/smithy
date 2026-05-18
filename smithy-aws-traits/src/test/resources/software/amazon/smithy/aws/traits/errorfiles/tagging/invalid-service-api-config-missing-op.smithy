$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait",
        namespace: "example.weather"
    }
]

namespace example.weather

use aws.api#arn
use aws.api#taggable
use aws.api#tagEnabled

// Service overrides only TagResource via apiConfig; UntagResource and ListTagsForResource
// are missing entirely. Expect ServiceTagging warnings for the missing slots and a
// TagEnabledService aggregate warning.
@tagEnabled(apiConfig: {
    tagApi: AddTagsToResource
})
service Weather {
    version: "2006-03-01"
    resources: [City]
    operations: [AddTagsToResource]
}

structure Tag {
    key: String
    value: String
}

list TagList {
    member: Tag
}

operation AddTagsToResource {
    input := {
        @required
        resourceArn: String
        @length(max: 128)
        tags: TagList
    }
    output := { }
}

@arn(template: "city/{cityId}")
@taggable(property: "tags")
resource City {
    identifiers: { cityId: String }
    properties: {
        name: String
        tags: TagList
    }
    create: CreateCity
}

operation CreateCity {
    input := for City {
        $name
        $tags
    }
    output := for City {
        @required
        $cityId
    }
}
