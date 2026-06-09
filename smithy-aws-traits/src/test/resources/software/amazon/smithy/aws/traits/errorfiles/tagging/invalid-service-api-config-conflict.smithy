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

// `apiConfig.tagApi` is set to `AddTagsToResource`, but the service also binds an
// operation named `TagResource`. The default-named op is dead code in this model
// and the validator should warn about the ambiguity.
@tagEnabled(apiConfig: {
    tagApi: AddTagsToResource
})
service Weather {
    version: "2006-03-01"
    resources: [City]
    operations: [AddTagsToResource, TagResource, UntagResource, ListTagsForResource]
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

operation AddTagsToResource {
    input := {
        @required
        resourceArn: String
        @length(max: 128)
        tags: TagList
    }
    output := { }
}

operation TagResource {
    input := {
        @required
        resourceArn: String
        @length(max: 128)
        tags: TagList
    }
    output := { }
}

operation UntagResource {
    input := {
        @required
        resourceArn: String
        @required
        tagKeys: TagKeys
    }
    output := { }
}

@readonly
operation ListTagsForResource {
    input := {
        @required
        resourceArn: String
    }
    output := {
        @length(max: 128)
        tags: TagList
    }
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
