$version: "1.0"

metadata validators = [{
    name: "MissingSensitiveTrait",
    id: "DefaultMissingSensitiveTrait",
    configuration: {
        excludeDefaults: true,
        phrases: [
            "caBANKle",
        ],
        words: [
            "foo",
            "string",
            "secondMember"
        ]
    }
}]

namespace smithy.example

service FooService {
    version: "2020-09-21",
    operations: [FooOperation],
}

operation FooOperation {
    input: FooOperationRequest,
    output: FooOperationResponse,
    errors: [],
}

structure FooOperationRequest {
    firstMember: CabAnkle,
    secondMember: BillingInfo,
    thirdMember: SafeBillingInfo
}

structure FooOperationResponse {
}

structure CabAnkle {
    myMember: MyString
}

// should get flagged
structure BillingInfo {
    // should get flagged
    bank: MyString,
    data: MyString,
    safeBank: MySensitiveString,
    // should get flagged
    firstName: FirstName,
    lastName: LastName
}

@sensitive
structure SafeBillingInfo {
    bank: MyString,
    data: MyString,
    safeBank: MySensitiveString,
    firstName: MyString,
    lastName: MySensitiveString
}

string MyString

@sensitive
string MySensitiveString

// should get flagged
string FirstName

@sensitive
string LastName
