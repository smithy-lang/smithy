$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector"
        id: "MissingStringInputLengthValidation"
        severity: "DANGER"
        message: "This string is missing required length trait"
        configuration: {
            selector: """
operation -[input]-> structure > member
    :test(member > string:not([trait|enum]))
:test(member > string:not([trait|length]))
:test(member > string:not([trait|aws.api#arnReference]))
:test(member > string:not([trait|aws.api#providesPassRole]))
"""
        }
    }
]

namespace smithy.example
