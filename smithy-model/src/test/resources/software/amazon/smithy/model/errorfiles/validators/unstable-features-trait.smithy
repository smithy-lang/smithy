$version: "2.0"

namespace smithy.example

@unstableFeatures(
    MYSERVICE_FIRST_PREVIEW: { message: "This is my preview operation and is subject to change!", reason: "PREVIEW" }
)
service MyService {
    version: "2020-01-01"
    operations: [
        DefinedFeatureOperation
        UndefinedFeatureOperation
        MemberFeatureOperation
        CrossServiceOperation
        InvalidMultiServiceOperation
        NestedMemberOperation
        BareUnstableOperation
        MixinPreviewOperation
        MixinGaOperation
    ]
    resources: [
        MyResource
    ]
}

service MySecondService {
    version: "2020-01-01"
    operations: [
        InvalidMultiServiceOperation
    ]
}

@unstableFeatures(
    MYSERVICE_FIRST_PREVIEW: { reason: "PREVIEW" }
    OTHERSERVICE_PREVIEW: { reason: "PREVIEW" }
)
service MyOtherService {
    version: "2020-01-01"
    operations: [
        ValidMultiServiceOperation
    ]
}

@unstableFeatures(
    BADSERVICE_PREVIEW: { reason: "PREVIEW" }
)
@unstable(featureId: "BADSERVICE_UNKNOWN")
service BadPreviewService { version: "2020-01-01" }

service NoFeaturesService {
    version: "2020-01-01"
    operations: [
        NoFeaturesOperation
    ]
}

@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
resource MyResource {
    identifiers: {
        id: String
    }
}

// featureId resolves to the enclosing service's @unstableFeatures entry.
@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
operation DefinedFeatureOperation {}

// featureId is not defined in the enclosing service's @unstableFeatures trait.
@unstable(featureId: "MYSERVICE_UNKNOWN_PREVIEW")
operation UndefinedFeatureOperation {}

// featureId is defined, but only on OtherService, which does not enclose this operation.
@unstable(featureId: "OTHERSERVICE_PREVIEW")
operation CrossServiceOperation {}

operation MemberFeatureOperation {
    input := {
        // Resolves to a defined feature.
        @unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
        goodMember: String

        // References an undefined feature.
        @unstable(featureId: "MYSERVICE_MISSING_MEMBER_PREVIEW")
        badMember: String
    }
}

@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
operation InvalidMultiServiceOperation {}

@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
operation ValidMultiServiceOperation {}

@unstable(featureId: "NOFEATURES_PREVIEW")
operation NoFeaturesOperation {}

// Bound to no service, so its featureId cannot be resolved against any @unstableFeatures trait.
@unstable(featureId: "UNBOUND_PREVIEW")
operation UnboundOperation {}

// An operation-level preview whose input member ALSO carries @unstable.
@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
operation NestedMemberOperation {
    input := {
        @unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
        nestedMember: String
    }
}

@unstable
operation BareUnstableOperation {}

@unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
operation MixinPreviewOperation {
    input: MixinPreviewInput
}

operation MixinGaOperation {
    input: MixinGaInput
}

@mixin
structure NestedMixin {
    @unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
    nestedMixinMember: String
}

structure MixinPreviewInput with [NestedMixin] {}

@mixin
structure GaMixin {
    @unstable(featureId: "MYSERVICE_FIRST_PREVIEW")
    gaMixinMember: String
}

structure MixinGaInput with [GaMixin] {}
