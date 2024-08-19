$version: "2.0"

namespace aws.endpoints

/// Marks a trait as an endpoints modifier defining trait.
///
/// The targeted trait must only be applied to service shapes,
/// must be a structure, and must have the `trait` trait.
@trait(
    selector: "structure[trait|trait]"
    breakingChanges: [{change: "presence"}]
)
structure endpointsModifier { }

/// Marks that a services endpoints should be resolved using
/// standard regional endpoint patterns.
@trait(
    selector: "service"
    conflicts: [standardPartitionalEndpoints]
    breakingChanges: [{change: "remove"}]
)
@endpointsModifier
@unstable
structure standardRegionalEndpoints {
    /// A map of partition to partition special cases -
    /// endpoints for a partition that do not follow the standard patterns.
    partitionSpecialCases: PartitionSpecialCaseMap

    /// A map of region to regional special cases -
    /// endpoints for a region that do not follow the standard patterns.
    regionSpecialCases: RegionSpecialCaseMap
}

@private
map PartitionSpecialCaseMap {
    key: String
    value: PartitionSpecialCaseList
}

@private
list PartitionSpecialCaseList {
    member: PartitionSpecialCase
}

/// Defines the endpoint pattern to apply for all regional endpoints in the given partition.
@private
structure PartitionSpecialCase {
    /// The special-cased endpoint pattern.
    @required
    endpoint: String

    /// When true, the special case will apply to dualstack endpoint variants.
    dualStack: Boolean

    /// When true, the special case will apply to fips endpoint variants.
    fips: Boolean
}

@private
map RegionSpecialCaseMap {
    key: String
    value: RegionSpecialCaseList
}

@private
list RegionSpecialCaseList {
    member: RegionSpecialCase
}

/// Defines the endpoint pattern to apply for a region.
@private
structure RegionSpecialCase {
    /// The special-cased endpoint pattern.
    @required
    endpoint: String

    /// When true, the special case will apply to dualstack endpoint variants.
    dualStack: Boolean

    /// When true, the special case will apply to fips endpoint variants.
    fips: Boolean

    /// Overrides the signingRegion used for this region.
    signingRegion: String
}

/// Marks that a services is non-regionalized and has
/// a single endpoint in each partition.
@trait(
    selector: "service"
    conflicts: [standardRegionalEndpoints]
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure standardPartitionalEndpoints {
    /// The pattern type to use for the partition endpoint.
    @required
    endpointPatternType: PartitionEndpointPattern

    /// A map of partition to a list of partition endpoint special cases -
    /// partitions that do not follow the services standard patterns or are
    /// located in a region other than the partition's defaultGlobalRegion.
    partitionEndpointSpecialCases: PartitionEndpointSpecialCaseMap
}

@private
enum PartitionEndpointPattern {
    SERVICE_DNSSUFFIX = "service_dnsSuffix"
    SERVICE_REGION_DNSSUFFIX = "service_region_dnsSuffix"
}

@private
map PartitionEndpointSpecialCaseMap {
    key: String
    value: PartitionEndpointSpecialCaseList
}

@private
list PartitionEndpointSpecialCaseList {
    member: PartitionEndpointSpecialCase
}

/// Defines the endpoint pattern to apply for a partitional endpoint.
@private
structure PartitionEndpointSpecialCase {
    /// The special-cased endpoint pattern.
    endpoint: String

    /// The region to override the defaultGlobalRegion used in this partition.
    region: String

    /// When true, the special case will apply to dualstack endpoint variants.
    dualStack: Boolean

    /// When true, the special case will apply to fips endpoint variants.
    fips: Boolean
}

/// Marks that a services has only dualStack endpoints.
@trait(
    selector: "service"
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure dualStackOnlyEndpoints { }

/// Marks that a services has hand written endpoint rules.
@trait(
    selector: "service"
    breakingChanges: [{change: "any"}]
)
@endpointsModifier
@unstable
structure rulesBasedEndpoints { }
