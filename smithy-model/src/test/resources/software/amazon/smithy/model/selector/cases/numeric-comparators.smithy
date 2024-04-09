$version: "2.0"

metadata selectorTests = [
    {
        selector: "[trait|length|min > 1]"
        matches: [
            smithy.example#AtLeastTen
        ]
    },
    {
        selector: "[trait|length|min >= 1]"
        skipPreludeShapes: true
        matches: [
            smithy.example#AtLeastOne
            smithy.example#AtLeastTen
        ]
    },
    {
        selector: "[trait|length|min < 2]"
        skipPreludeShapes: true
        matches: [
            smithy.example#AtLeastOne
        ]
    },
    {
        selector: "[trait|length|max <= 5]"
        matches: [
            smithy.example#AtMostFive
        ]
    },
]

namespace smithy.example

@length(min: 1)
string AtLeastOne

@length(max: 5)
string AtMostFive

@length(min: 10)
string AtLeastTen
