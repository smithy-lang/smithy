$version: "2.0"

metadata selectorTests = [
    {
        selector: "[trait|enum]"
        matches: [
            smithy.example#SimpleEnum
            smithy.example#EnumWithTags
        ]
    },
    {
        selector: "[trait|enum|(values)|tags|(values)]"
        matches: [
            smithy.example#EnumWithTags
        ]
    }
]

namespace smithy.example

@deprecated
string NoMatch

@enum([
    {name: "foo", value: "foo"}
    {name: "baz", value: "baz"}
])
string SimpleEnum

@enum([
    {name: "foo", value: "foo", tags: ["a"]}
    {name: "baz", value: "baz"}
    {name: "spam", value: "spam", tags: []}
])
string EnumWithTags
