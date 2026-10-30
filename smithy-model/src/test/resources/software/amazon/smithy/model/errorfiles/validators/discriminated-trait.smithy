$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait.smithy.api#discriminated"
        namespace: "*"
        reason: "The trait is unstable, which is not what this file tests."
    }
]

namespace smithy.example

/// Valid: members target structures. A structure may have members that target unions, which are nested objects.
@discriminated("type")
union Valid {
    circle: Circle
    holder: Holder
    renamed: Renamed
    caseDiffers: CaseDiffers
}

/// Not discriminated, so members may target anything.
union PlainUnion {
    text: String
    circle: Circle
}

/// Invalid: members that target something other than a structure. A bare trait uses `__type`.
@discriminated
union BadTargets {
    text: String
    items: Strings
    nothing: Unit
    standardUnion: PlainUnion
    discriminatedUnion: Valid
    enumTarget: MyEnum
    intEnumTarget: MyIntEnum
    mapTarget: MyMap
    documentTarget: Document
}

/// Invalid: the discriminator collides with a member of a targeted structure.
@discriminated("type")
union Collides {
    ok: Circle
    clash: HasType
}

/// Invalid: the default discriminator, `__type`, collides with a member of a targeted structure.
@discriminated
union DefaultCollides {
    clash: HasDefaultDiscriminator
}

/// Danger: the jsonName of a member of a targeted structure collides with the discriminator, which only matters in
/// protocols that use the jsonName as the wire name.
@discriminated("type")
union JsonNameCollides {
    clash: HasJsonName
}

/// Danger: a member is named like the discriminator but renamed by jsonName, which only matters in protocols that
/// ignore jsonName.
@discriminated("type")
union RenamedAway {
    clash: RenamedType
}

/// Invalid: the member name and the jsonName both match, so the wire name collides in every protocol.
@discriminated("type")
union SameJsonName {
    clash: SameJsonNameType
}

/// Invalid: a member inherited from a mixin does not target a structure.
@mixin
union BaseMembers {
    text: String
}

@discriminated("type")
union FromMixin with [BaseMembers] {
    circle: Circle
}

/// Warning: jsonName has no effect on the member of a discriminated union.
@discriminated("type")
union JsonNameOnMember {
    @jsonName("renamed")
    circle: Circle
}

/// Invalid: the colliding member is inherited from a mixin.
@discriminated("type")
union MixinCollides {
    clash: WithMixin
}

structure Circle {
    radius: Integer
}

structure Holder {
    plain: PlainUnion
    discriminated: Valid
}

structure HasType {
    type: String
}

structure HasJsonName {
    @jsonName("type")
    kind: String
}

structure RenamedType {
    @jsonName("x")
    type: String
}

structure SameJsonNameType {
    @jsonName("type")
    type: String
}

/// Valid: a jsonName that does not collide with the discriminator.
structure Renamed {
    @jsonName("Kind")
    kind: String
}

structure HasDefaultDiscriminator {
    __type: String
}

@mixin
structure TypeMixin {
    type: String
}

structure WithMixin with [TypeMixin] {
    other: String
}

list Strings {
    member: String
}

enum MyEnum {
    FOO
}

intEnum MyIntEnum {
    FOO = 1
}

map MyMap {
    key: String
    value: String
}

/// Valid: matching is case-sensitive, so `Type` does not collide with the discriminator `type`.
structure CaseDiffers {
    Type: String
}
