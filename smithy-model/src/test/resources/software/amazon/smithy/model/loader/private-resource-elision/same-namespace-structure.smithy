$version: "2.0"

namespace smithy.private

@private
resource PrivateResource {
    identifiers: { id: String }
    properties: { value: String }
}

structure InternalElidedStructure for PrivateResource {
    $id
}
