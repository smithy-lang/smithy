$version: "2.0"

namespace smithy.example

// GA service hosting an operation-level preview, a member-level preview, and a resource-level preview. All
// three reference one featureId; they stay independent owners because each carries its own @unstable trait.
@unstableFeatures(
    FOO_PREVIEW: { message: "Preview feature.", reason: "PREVIEW" }
)
service Service {
    version: "2020-01-01"
    operations: [
        PreviewOperation
        GaOperation
    ]
    resources: [
        PreviewResource
    ]
}

// Service-level preview: its own service, so the whole closure is owned without affecting the GA service.
@unstableFeatures(
    SVC_PREVIEW: { message: "Preview service.", reason: "PREVIEW" }
)
@unstable(featureId: "SVC_PREVIEW")
service PreviewService {
    version: "2020-01-01"
    operations: [
        PreviewServiceOp
    ]
}

// Two services define the same featureId. A feature resolves from the service that encloses the shape, so
// MultiWrongService's entry (different message, no reason) must be ignored for shapes it does not enclose.
@unstableFeatures(
    MULTI_PREVIEW: { message: "Wrong service." }
)
service MultiWrongService {
    version: "2020-01-01"
    operations: [
        MultiWrongOperation
        MultiSharedOperation
    ]
}

@unstableFeatures(
    MULTI_PREVIEW: { message: "Enclosing service.", reason: "PREVIEW" }
)
service MultiRightService {
    version: "2020-01-01"
    operations: [
        MultiSharedOperation
    ]
}

// --- Resources ---
// Resource-level preview: the resource and its lifecycle-operation closure inherit FOO_PREVIEW.
@unstable(featureId: "FOO_PREVIEW")
resource PreviewResource {
    identifiers: {
        id: String
    }
    read: GetPreviewResource
}

// --- Operations ---
// Operation-level preview: its entire input / output closure inherits FOO_PREVIEW.
@unstable(featureId: "FOO_PREVIEW")
operation PreviewOperation {
    input := {
        previewOpInputMember: String

        // A nested owner: this member declares its own @unstable while already inside PreviewOperation's
        // closure. getFeatureOwners must report both this member and PreviewOperation as owners (nesting).
        @unstable(featureId: "FOO_PREVIEW")
        nestedOwnerMember: String

        // Only reachable through the preview operation.
        previewOnlyMember: PreviewOnlyStruct

        // Also reachable through the GA operation.
        sharedMember: SharedStr

        // Recursive shapes must not trip up the traversal.
        recursiveMember: RecursiveStruct

        // Aggregate shapes reachable only through the preview operation are owned via the list-member and
        // map-key / map-value edges, just like structure members.
        listMember: PreviewList

        mapMember: PreviewMap
    }

    output := {
        previewOpOutputMember: String
    }
}

// GA operation with a single preview member.
operation GaOperation {
    input := {
        gaMember: String

        @unstable(featureId: "FOO_PREVIEW")
        previewMember: String

        // Shared with the preview operation's input.
        gaSharedMember: SharedStr
    }
}

@readonly
operation GetPreviewResource {
    input := {
        @required
        id: String
    }

    output := {
        detail: String
    }
}

operation PreviewServiceOp {
    input := {
        data: PreviewServiceNested
    }
}

@unstable(featureId: "MULTI_PREVIEW")
operation MultiWrongOperation {}

// Bound to both MultiWrongService and MultiRightService, which both define MULTI_PREVIEW. The feature must
// resolve deterministically to the lowest enclosing service shape id (MultiRightService < MultiWrongService).
@unstable(featureId: "MULTI_PREVIEW")
operation MultiSharedOperation {}

structure PreviewOnlyStruct {
    detail: String
}

structure UnboundStruct {
    data: PreviewOnlyStruct
}

structure RecursiveStruct {
    self: RecursiveStruct
}

structure PreviewServiceNested {
    value: String
}

structure PreviewListElement {
    detail: String
}

structure PreviewMapValue {
    detail: String
}

list PreviewList {
    member: PreviewListElement
}

map PreviewMap {
    key: String
    value: PreviewMapValue
}

@length(min: 1)
string SharedStr
