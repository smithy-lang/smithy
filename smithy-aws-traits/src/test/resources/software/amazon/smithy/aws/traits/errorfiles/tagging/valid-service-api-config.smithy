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

@tagEnabled(apiConfig: {
    tagApi: AddTagsToResource
    untagApi: RemoveTagsFromResource
    listTagsApi: DescribeTagsForResource
})
service Weather {
    version: "2006-03-01"
    resources: [City]
    operations: [AddTagsToResource, RemoveTagsFromResource, DescribeTagsForResource]
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

operation RemoveTagsFromResource {
    input := {
        @required
        resourceArn: String
        @required
        tagKeys: TagKeys
    }
    output := { }
}

@readonly
operation DescribeTagsForResource {
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
