$version: "2.0"

metadata selectorTests = [
    {
        selector: "service $authTraits(-[trait]-> [trait|authDefinition])"
        matches: [
            smithy.example#MyService
        ]
    }
    {
        selector: """
        service
        $authTraits(-[trait]-> [trait|authDefinition])
        ~>
        operation [trait|auth]
        """
        matches: [
            smithy.example#HasBasicAuth
            smithy.example#HasDigestAuth
        ]
    }
    {
        selector: """
        service
        $authTraits(-[trait]-> [trait|authDefinition])
        ~>
        operation
        [trait|auth]
        :not([@: @{trait|auth|(values)} {<} @{var|authTraits|id}]))
        """
        matches: [
            smithy.example#HasDigestAuth
        ]
    }
]

namespace smithy.example


@httpBasicAuth
@httpBearerAuth
service MyService {
    version: "2020-04-21"
    operations: [HasDigestAuth, HasBasicAuth, NoAuth]
}

@suppress(["AuthTrait"])
@auth([httpDigestAuth])
operation HasDigestAuth {}

@auth([httpBasicAuth])
operation HasBasicAuth {}

operation NoAuth {}
