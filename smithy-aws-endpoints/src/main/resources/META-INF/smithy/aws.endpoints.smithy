$version: "2.0"

namespace aws.endpoints

/// Marks a trait as an endpoints modifier defining trait.
///
/// The targeted trait must only be applied to service shapes,
/// must be a structure, and must have the `trait` trait.
@trait(
    selector: "structure[trait|trait]",
    breakingChanges: [{change: "presence"}]
)
structure endpointsModifier { }

/// Marks that a services endpoints should be resolved using
/// standard regional endpoint patterns.
@trait(
    selector: "service",
    conflicts: [standardPartitionalEndpoints],
    breakingChanges: [{change: "remove"}]
)
@endpointsModifier
@unstable
structure standardRegionalEndpoints {
    /// A list of partition special cases - endpoints for a partition that do not follow the standard patterns.
    partitionSpecialCases: PartitionSpecialCaseMap,
    /// A list of regional special cases - endpoints for a region that do not follow the standard patterns.
    regionSpecialCases: RegionSpecialCaseMap
}

@private
map PartitionSpecialCaseMap {
    key: String,
    value: PartitionSpecialCaseList
}

@private
list PartitionSpecialCaseList {
    member: PartitionSpecialCase
}

@private
structure PartitionSpecialCase {
    @required
    endpoint: String,

    dualStack: Boolean,
    fips: Boolean
}

@private
map RegionSpecialCaseMap {
    key: String,
    value: RegionSpecialCaseList
}

@private
list RegionSpecialCaseList {
    member: RegionSpecialCase
}

@private
structure RegionSpecialCase {
    @required
    endpoint: String,

    dualStack: Boolean,
    fips: Boolean,
    signingRegion: String
}

/// Marks that a services is non-regionalized and has
/// a single endpoint in each partition.
@trait(
    selector: "service",
    conflicts: [standardRegionalEndpoints],
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure standardPartitionalEndpoints {
    /// The pattern type to use for the partition endpoint.
    @required
    endpointPatternType: PartitionEndpointPattern,

    /// A list of partition endpoint special cases - partitions that do not follow the services standard patterns
    /// or are located in a region other than the partition's defaultGlobalRegion.
    partitionEndpointSpecialCases: PartitionEndpointSpecialCaseMap,
}

@private
enum PartitionEndpointPattern {
    SERVICE_DNSSUFFIX = "service_dnsSuffix"
    SERVICE_REGION_DNSSUFFIX = "service_region_dnsSuffix"
}

@private
map PartitionEndpointSpecialCaseMap {
    key: String,
    value: PartitionEndpointSpecialCaseList
}

@private
list PartitionEndpointSpecialCaseList {
    member: PartitionEndpointSpecialCase
}

@private
structure PartitionEndpointSpecialCase {
    endpoint: String,
    region: String,
    dualStack: Boolean,
    fips: Boolean,
}

/// Marks that a services has only dualStack endpoints.
@trait(
    selector: "service",
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure dualStackOnlyEndpoints { }

/// Marks that a services has hand written endpoint rules.
@trait(
    selector: "service",
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure rulesBasedEndpoints { }
