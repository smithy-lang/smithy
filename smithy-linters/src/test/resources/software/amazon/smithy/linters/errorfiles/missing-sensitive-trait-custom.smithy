$version: "2.0"

metadata validators = [{
    name: "MissingSensitiveTrait",
    id: "DefaultMissingSensitiveTrait",
    configuration: {
        excludeDefaults: true,
        terms: [
            "bank",
            "foo",
            "string",
            "second member",
            "bill inginfo",
            "da ta"
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

// should get flagged
structure FooOperationRequest {
    firstMember: CabAnkle,
    // should get flagged
    secondMember: BillingInfo,
    thirdMember: SafeBillingInfo
}

// should get flagged
structure FooOperationResponse {
}

structure CabAnkle {
    myMember: MyString
}

//should not get flagged
structure BillingInfo {
    // should get flagged
    bank: MyString,
    data: MyString,
    // should not get flagged
    safeBank: MySensitiveString,
    firstName: FirstName,
    // should not get flagged
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

string FirstName

@sensitive
string LastName
