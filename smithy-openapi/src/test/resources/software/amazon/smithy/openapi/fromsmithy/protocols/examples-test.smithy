namespace smithy.examplestrait

use aws.protocols#restJson1

@restJson1
service Banking {
    version: "2022-06-26",
    operations: [Deposit, Withdraw]
}

@idempotent
@http(method: "PUT", uri: "/account/{username}", code: 200)
operation Deposit {
    input: DepositInput,
    output: DepositOutput,
    errors: [InvalidUsername, InvalidAmount]
}

@idempotent
@http(method: "PATCH", uri: "/account/withdraw", code: 200)
operation Withdraw {
    input: WithdrawInput,
    output: WithdrawOutput,
    errors: [InvalidUsername]
}

@input
structure DepositInput {
    @httpHeader("accountNumber")
    accountNumber: String,

    @required
    @httpLabel
    username: String,

    @httpQuery("accountHistory")
    accountHistory: ExampleList,

    @httpPayload
    depositAmount: String
}

@input
structure WithdrawInput {
    @httpHeader("accountNumber")
    accountNumber: String,

    @httpHeader("username")
    username: String,

    @httpQueryParams()
    withdrawParams: ExampleMap,

    time: date,
    withdrawAmount: String,
    withdrawOption: String
}

list ExampleList {
    member: String
}

map ExampleMap {
    key: String,
    value: String
}

@mediaType("video/quicktime")
blob exampleVideo

@timestampFormat("http-date")
timestamp date

@output
structure DepositOutput {
    @httpHeader("username")
    username: String,

    @httpHeader("authenticationResult")
    authenticationResult: ExampleList,

    textMessage: String,
    emailMessage: String
}

@output
structure WithdrawOutput {
    @httpHeader("branch")
    branch: String,

    @httpHeader("result")
    accountHistory: ExampleList,

    location: String,
    bankName: String,
    atmRecording: exampleVideo
}

@error("client")
structure InvalidUsername {
    @httpHeader("internalErrorCode")
    internalErrorCode: String,

    @httpPayload
    errorMessage: String
}

@error("server")
structure InvalidAmount {
    errorMessage1: String,
    errorMessage2: String,
    errorMessage3: String
}

apply Deposit @examples(
    [
        {
            title: "Deposit valid example",
            documentation: "depositTestDoc",
            input: {
                accountNumber: "102935",
                username: "sichanyoo",
                accountHistory: ["10", "-25", "50"],
                depositAmount: "200"
            },
            output: {
                username: "sichanyoo",
                authenticationResult: ["pass1", "pass2", "pass3"],
                textMessage: "You deposited 200-text",
                emailMessage: "You deposited 200-email"
            },
        },

        {
            title: "Deposit invalid username example",
            documentation: "depositTestDoc2",
            input: {
                username: "sichanyoo",
                accountHistory: ["-200", "200", "10"],
                depositAmount: "-200"
            },
            error: {
                shapeId: InvalidUsername,
                content: {
                    internalErrorCode: "4gsw2-34",
                    errorMessage: "ERROR: Invalid username."
                }
            },
        },

        {
            title: "Deposit invalid amount example",
            documentation: "depositTestDoc3",
            input: {
                accountNumber: "203952",
                username: "obidos",
                accountHistory: ["2000", "50000", "100"],
                depositAmount: "-100"
            },
            error: {
                shapeId: InvalidAmount,
                content: {
                    errorMessage1: "ERROR: Invalid amount.",
                    errorMessage2: "2gdx4-34",
                    errorMessage3: "2gcbe-98"
                }
            },
        }
    ]
)

apply Withdraw @examples(
    [
        {
            title: "Withdraw valid example",
            documentation: "withdrawTestDoc",
            input: {
                accountNumber: "124634",
                username: "amazon",
                withdrawParams: {"location" : "Denver", "bankName" : "Chase"},
                time: "Tue, 29 Apr 2014 18:30:38 GMT",
                withdrawAmount: "-35",
                withdrawOption: "ATM"
            },
            output: {
                branch: "Denver-203",
                accountHistory: ["34", "5", "-250"],
                location: "Denver",
                bankName: "Chase",
                atmRecording: "dGVzdHZpZGVv"
            },
        },

        {
            title: "Withdraw invalid username example",
            documentation: "withdrawTestDoc2",
            input: {
                accountNumber: "231565",
                username: "peccy",
                withdrawParams: {"location" : "Seoul", "bankName" : "Chase"},
                withdrawAmount: "-450",
                withdrawOption: "Venmo"
            },
            error: {
                shapeId: InvalidUsername,
                content: {
                    internalErrorCode: "8dfws-21",
                    errorMessage: "ERROR: Invalid username."
                }
            },
        }
    ]
)
