$version: "2.0"

metadata selectorTests = [
    {
        selector: "[@trait|range: @{min} = @{max}]"
        matches: [
            smithy.example#Ten
        ]
    }
    {
        selector: "[@trait|enum|(values): @{deprecated} = true && @{tags|(values)} = \"deprecated\"]"
        matches: [
            smithy.example#GoodEnum
        ]
    }
    {
        selector: "[@trait|idRef: @{failWhenMissing} = true && @{errorMessage} ?= false]"
        skipPreludeShapes: true
        matches: [
            smithy.example#integerRef
        ]
    }
    {
        selector: "[@trait|httpApiKeyAuth: @{name} = header && @{in} != 'x-api-token', 'authorization']"
        matches: [
            smithy.example#WeatherService
        ]
    }
    {
        selector: "[@trait|deprecated: @{message} = DePrEcAtEd]"
        matches: [
        ]
    }
    {
        selector: "[@trait|deprecated: @{message} = DePrEcAtEd i]"
        matches: [
            smithy.example#DeprecatedString
        ]
    }
]

namespace smithy.example

@range(min: 1, max: 10)
integer OneToTen

@range(min: 1)
integer GreaterThanOne

@range(min: 10, max: 10)
integer Ten

@enum([
    {value: "a"}
    {value: "b", tags: ["deprecated"], deprecated: true}
    {value: "c", tags: ["internal"]}
])
string GoodEnum

@trait
@idRef(failWhenMissing: true, selector: "integer")
string integerRef

@httpApiKeyAuth(name: "header", in: "header")
service WeatherService {
    version: "2017-02-11"
}

@deprecated(
    message: "deprecated"
)
string DeprecatedString
