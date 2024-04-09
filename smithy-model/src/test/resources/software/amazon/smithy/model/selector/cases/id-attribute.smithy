$version: "2.0"

metadata selectorTests = [
    {
        selector: "[id = smithy.example#Exception]"
        matches: [
            smithy.example#Exception
        ]
    }
    {
        selector: "[id|namespace = 'smithy.example']"
        matches: [
            smithy.example#Exception
            smithy.example#Exception$message
        ]
    }
    {
        selector: "[id|(length) <= 24]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Exception
        ]
    }
    {
        selector: "[id|(length) > 24]"
        skipPreludeShapes: true
        matches: [
            smithy.example#Exception$message
        ]
    }
]

namespace smithy.example

structure Exception {
    message: String
}
