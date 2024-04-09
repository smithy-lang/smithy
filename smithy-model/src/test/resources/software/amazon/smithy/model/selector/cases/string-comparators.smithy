$version: "2.0"

metadata selectorTests = [
    {
        selector: "[id|name = 'Name']"
        matches: [
            smithy.example#Name
        ]
    },
    {
        selector: "[id|namespace != 'smithy.api']"
        matches: [
            smithy.example#Name
            smithy.example#Age
            smithy.example#_Boolean
        ]
    },
    {
        selector: "[id|name ^= '_']"
        matches: [
            smithy.example#_Boolean
        ]
    },
    {
        selector: "[trait|documentation $= 'ge']"
        matches: [
            smithy.example#Age
        ]
    },
    {
        selector: "[id|name *= 'Boo']"
        skipPreludeShapes: true
        matches: [
            smithy.example#_Boolean
        ]
    },
    {
        selector: "[trait|documentation ?= true]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Age
        ]
    },
]

namespace smithy.example

@length(min: 1)
string Name

@documentation("Age")
integer Age

boolean _Boolean
