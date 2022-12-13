$version: "2.0"

namespace example.tagging

structure Tag {
    key: String
    value: String
}

list TagList {
    member: example.tagging#Tag
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
