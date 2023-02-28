$version: "2.0"

metadata selectorTests = [
    {
        selector: "[trait|(keys)|namespace = 'smithy.example']"
        matches: [
            smithy.example#ServiceA
        ]
    }
    {
        selector: "[trait|(values)|description]"
        matches: [
            smithy.example#ServiceA
        ]
    }
    {
        selector: "[trait|(length) > 1]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Exception
            smithy.example#ServiceB
        ]
    }
    {
        selector: "[trait|smithy.api#deprecated]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Exception
            smithy.example#ServiceB
        ]
    }
    {
        selector: "[trait|deprecated]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Exception
            smithy.example#ServiceB
        ]
    }
    {
        selector: "[trait|error = client]"
        matches: [
            smithy.example#Exception
        ]
    }
    {
        selector: "[trait|error != client]"
        matches: [
        ]
    }
    {
        selector: "[trait|documentation *= TODO, FIXME]"
        matches: [
            smithy.example#ServiceB
            smithy.example#Exception
        ]
    }
]

namespace smithy.example

@deprecated
@unstable
@error("client")
@documentation("FIXME")
structure Exception {
    message: String
}

@ServiceProperty(description: "This is ServiceA")
service ServiceA {
    version: "2019-06-17"
}

@deprecated
@documentation("TODO")
service ServiceB {
    version: "2018-06-17"
}

@trait(selector: "service")
structure ServiceProperty {
    description: String
}
