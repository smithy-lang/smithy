$version: "2.1"

namespace com.example

operation MixedWithCustomA {
    input: MixedWithCustomARequest
    output: MixedWithCustomAResponse
}

operation MixedWithCustomB {
    input: MixedWithCustomBGiven
    output: MixedWithCustomBTaken
}

operation MixedWithDefault {
    input := {}
    output := {}
}

@input
structure MixedWithCustomARequest {}

@output
structure MixedWithCustomAResponse {}

@input
structure MixedWithCustomBGiven {}

@output
structure MixedWithCustomBTaken {}
