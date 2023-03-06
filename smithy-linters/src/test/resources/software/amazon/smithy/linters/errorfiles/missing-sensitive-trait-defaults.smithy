$version: "2.0"

metadata validators = [
        {name: "MissingSensitiveTrait",
        id: "DefaultMissingSensitiveTrait"}
]

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
    secondMember: BillingAddress,
    thirdMember: SafeBillingAddress
}

structure FooOperationResponse {
}

structure CabAnkle {
    myMember: MyString,
    // should get flagged
    myBirthday: MyString
}

// should get flagged
structure BillingAddress {
    // should get flagged
    bank: MyString,
    data: MyString,
    safeBank: MySensitiveString,
    // should get flagged
    firstName: FirstName,
    lastName: LastName,
    someEnum: MyEnum
}

@sensitive
structure SafeBillingAddress {
    bank: MyString,
    data: MyString,
    safeBank: MySensitiveString,
    firstName: MyString,
    lastName: MySensitiveString
}

enum MyEnum {
    // should not be flagged
    IP_ADDRESS
}

string MyString

@sensitive
string MySensitiveString

// should get flagged
string FirstName

@sensitive
string LastName
