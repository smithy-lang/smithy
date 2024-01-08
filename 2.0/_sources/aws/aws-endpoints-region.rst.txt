.. _aws-endpoints:

===============================
AWS Declarative Endpoint Traits
===============================

This document defines AWS declarative endpoint traits.

.. _aws-endpoints-overview:

----------------------
AWS Endpoints Overview
----------------------

An endpoint is used to connect programmatically to an AWS service. An endpoint is the URL of the
entry point for an AWS web service.

Most AWS services are regional: they offer regional endpoints and the service's resources are independent
of similar resources in other regions.

.. _aws-endpoints-region:

Region
    Each `region <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/regions.html>`_
    consists of multiple availability zones within a single geographic area. Regions themselves are isolated
    and independent from other regions.

.. _aws-partition:

Partition
    AWS groups regions into
    `partitions <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/partitions.html>`_.
    Every region is in exactly one partition and each partition has one or more regions.
    AWS commercial Regions are in the ``aws`` partition, Regions in China are in the ``aws-cn`` partition,
    and AWS GovCloud Regions are in the ``aws-us-gov`` partition.

.. _fips-endpoints:

FIPS Endpoints
    Some AWS services have endpoints that support Federal Information Processing Standard (FIPS) 140-2.
    For a list of FIPS endpoints, see
    `FIPS endpoints by Service
    <http://aws.amazon.com/compliance/fips/#FIPS_Endpoints_by_Service>`_.

.. _dualstack-endpoints:

Dual stack Endpoints
    Some AWS services offer dual stack endpoints, so that you can access them using either IPv4 or IPv6 requests.
    For a list of services that support dual stack endpoints, see `AWS services that support IPv6
    <https://docs.aws.amazon.com/vpc/latest/userguide/aws-ipv6-support.html>`_.

.. smithy-trait:: aws.endpoints#endpointsModifier
.. _aws.endpoints#endpointsModifier-trait:

-----------------------------------------
``aws.endpoints#endpointsModifier`` trait
-----------------------------------------

Summary
    A meta-trait that marks a trait as an endpoint modifier.  Traits that are marked with this trait are
    applied to service shapes or operation shapes to indicate how a client can resolve
    endpoints for that service or operation.
Trait selector
    ``[trait|trait]``
Value type
    Annotation trait

The following example defines a service with ``standardRegionalEndpoints`` modified by
the hypothetical ``fooExample`` endpoint modifier.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#endpointsModifier
    use aws.endpoints#standardRegionalEndpoints

    @endpointsModifier
    @trait(selector: "service")
    structure fooExample {}

    @standardRegionalEndpoints
    @fooExample
    service MyService {
        version: "2020-04-02"
    }

Because endpoint modification definitions are just specialized shapes, they
can also support configuration settings.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use aws.endpoints#endpointsModifier
    use aws.endpoints#standardRegionalEndpoints

    @endpointsModifier
    @trait(selector: "service")
    structure endpointSuffix {
        suffix: String
    }

    @standardRegionalEndpoints
    @endpointSuffix(suffix="-suffix")
    service MyService {
        version: "2020-04-02"
    }



.. smithy-trait:: aws.endpoints#standardRegionalEndpoints
.. _aws.endpoints#standardRegionalEndpoints-trait:

-------------------------------------------------
``aws.endpoints#standardRegionalEndpoints`` trait
-------------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service's endpoints should be resolved using the standard AWS regional
    patterns:

    - Default: ``https://{service}.{region}.{dnsSuffix}``
    - Fips: ``https://{service}-fips.{region}.{dnsSuffix}``
    - Dualstack: ``https://{service}.{region}.{dualStackDnsSuffix}``
    - Fips/Dualstack: ``https://{service}-fips.{region}.{dualStackDnsSuffix}``

Trait selector
    ``service``
Trait value
    A ``structure`` with the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - partitionSpecialCases
      - ``map`` of partition to `PartitionSpecialCase object`_
      - A map of partition to partition special cases - endpoints for a partition that do not follow the
        standard patterns.
    * - regionSpecialCases
      - ``map`` of region to `RegionSpecialCase object`_
      - A map of region to regional special cases - endpoints for a region that do not follow the
        standard patterns.

Conflicts with
    :ref:`aws.endpoints#standardPartitionalEndpoints-trait`

Most AWS services are regionalized and are strongly encouraged to follow
the standard endpoint patterns defined above for consistency, and to
ensure that endpoints are forwards compatible, and that SDK updates are
not required when the service launches in a new region or partition.

The following example defines a service that uses the standard regional endpoints:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#standardRegionalEndpoints

    @standardRegionalEndpoints
    service MyService {
        version: "2020-04-02"
    }

While services are strongly encouraged to follow standard endpoint patterns,
there are occasional exceptions and special cases.  The following example defines
a service that uses standard regional endpoints, but uses a non-standard pattern for
FIPS endpoints in US GovCloud:

.. code-block:: smithy

    @standardRegionalEndpoints(
        partitionSpecialCases: {
            aws-us-gov: [
                {
                    endpoint: "https://myservice.{region}.{dnsSuffix}",
                    fips: true
                }
            ]
        }
    )
    service MyService {
        version: "2020-04-02"
    }

``PartitionSpecialCase`` object
-------------------------------

A ``PartitionSpecialCase`` defines the endpoint pattern to apply for all regional endpoints
in the given partition. A PartitionSpecialCase object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - endpoint
      - ``string``
      - **Required**. The special-cased :ref:`endpoint pattern <aws.endpoints#endpoint-pattern>`
    * - dualStack
      - ``boolean``
      - When ``true`` the special case will apply to dualstack endpoint variants.
    * - fips
      - ``boolean``
      - When ``true`` the special case will apply to fips endpoint variants.


``RegionSpecialCase`` object
----------------------------

A ``RegionSpecialCase`` object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - endpoint
      - ``string``
      - **Required**. The special-cased :ref:`endpoint pattern <aws.endpoints#endpoint-pattern>`.
    * - dualStack
      - ``boolean``
      - When ``true`` the special case will apply to dualstack endpoint variants.
    * - fips
      - ``boolean``
      - When ``true`` the special case will apply to fips endpoint variants.
    * - signingRegion
      - ``string``
      - Overrides the signingRegion used for this region.

.. smithy-trait:: aws.endpoints#standardPartitionalEndpoints
.. _aws.endpoints#standardPartitionalEndpoints-trait:

----------------------------------------------------
``aws.endpoints#standardPartitionalEndpoints`` trait
----------------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service is
    `partitional <https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/global-services.html#global-services-that-are-unique-by-partition>`_
    and a single endpoint should be resolved per partition.
Trait selector
    ``service``
Trait value
    A ``structure`` with the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - endpointPatternType
      - ``string``
      - **Required** The pattern type to use for the partition endpoint.  This value can be set to ``service_dnsSuffix`` to
        use the ``https://{service}.{dnsSuffix}`` pattern or ``service_region_dnsSuffix`` to use
        ``https://{service}.{region}.{dnsSuffix}``.
    * - partitionEndpointSpecialCases
      - ``map`` of partition to `PartitionEndpointSpecialCase object`_
      - A map of partition to partition endpoint special cases - partitions that do not follow the
        service's standard patterns or are located in a region other than the partition's
        ``defaultGlobalRegion``.

Conflicts with
    :ref:`aws.endpoints#standardRegionalEndpoints-trait`

Partitional services (also known as "global" services) resolve a single endpoint per partition.
That single endpoint is located in the partition's ``defaultGlobalRegion``. Partitional
services should follow one of two standard patterns:

- ``service_dnsSuffix``: ``https://{service}.{dnsSuffix}``
- ``service_region_dnsSuffix``: ``https://{service}.{region}.{dnsSuffix}``

The following example defines a partitional service that uses ``{service}.{dnsSuffix}``:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.endpoints#standardPartitionalEndpoints

    @standardPartitionalEndpoints(endpointPatternType: "service_dnsSuffix")
    service MyService {
        version: "2020-04-02"
    }

Services should follow the standard patterns; however, occasionally there are special cases.
The following example defines a partitional service that uses a special case pattern in
the ``aws`` partition and uses a non-standard global region in the ``aws-cn`` partition:

.. code-block:: smithy

    @standardPartitionalEndpoints(
        endpointPatternType: "service_dnsSuffix",
        partitionEndpointSpecialCases: {
            aws: [{endpoint: "https://myservice.global.amazonaws.com"}],
            aws-cn: [{region: "cn-north-1"}]
        }
    )
    service MyService {
        version: "2020-04-02"
    }

``PartitionEndpointSpecialCase`` object
---------------------------------------

A ``PartitionEndpointSpecialCase`` object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - endpoint
      - ``string``
      - The special-cased :ref:`endpoint pattern <aws.endpoints#endpoint-pattern>`.
    * - region
      - ``string``
      - Override the ``defaultGlobalRegion`` used in this partition.
    * - dualStack
      - ``boolean``
      - When ``true`` the special case will apply to dualstack endpoint variants.
    * - fips
      - ``boolean``
      - When ``true`` the special case will apply to fips endpoint variants.

.. smithy-trait:: aws.endpoints#dualStackOnlyEndpoints
.. _aws.endpoints#dualStackOnlyEndpoints-trait:

----------------------------------------------
``aws.endpoints#dualStackOnlyEndpoints`` trait
----------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service has only
    `dual stack endpoints <https://docs.aws.amazon.com/general/latest/gr/rande.html#dual-stack-endpoints>`_,
    does not support IPV4 only endpoints, and should not have the ``useDualStackEndpoint`` endpoint parameter.
    Dual stack endpoints support IPV4 and IPV6.
Trait selector
    ``service``
Trait value
    Annotation trait

Adding the ``dualStackOnlyEndpoints`` trait to a service modifies the generation of endpoints from
:ref:`aws.endpoints#standardRegionalEndpoints-trait` or :ref:`aws.endpoints#standardPartitionalEndpoints-trait`,
removes the ``useDualStackEndpoint`` parameter, and defaults the behavior to dual stack for
all partitions that support it.

The following example specifies a service that uses standard regional endpoint patterns and
is dual stack only:

.. code-block:: smithy

     @standardRegionalEndpoints
     @dualStackOnlyEndpoints
     service MyService {
         version: "2020-04-02"
     }

.. smithy-trait:: aws.endpoints#rulesBasedEndpoints
.. _aws.endpoints#rulesBasedEndpoints-trait:

-------------------------------------------
``aws.endpoints#rulesBasedEndpoints`` trait
-------------------------------------------

Summary
    An :ref:`endpoints modifier trait <aws.endpoints#endpointsModifier-trait>`
    that indicates that a service has hand written endpoint rules.
Trait selector
    ``service``
Trait value
    Annotation trait

Services marked with the ``rulesBasedEndpoints`` trait have hand written endpoint rules that
extend or replace their standard generated endpoint rules through an external mechanism.
This trait marks the presence of hand written rules, which are added to the model by a transformer,
but does not specify their behavior.

A service with ``rulesBasedEndpoints`` may extend the functionality of
endpoint behavior described in the model through other
:ref:`endpoints modifier traits <aws.endpoints#endpointsModifier-trait>`
by modifying the generated :ref:`EndpointRuleSet <smithy.rules#endpointRuleSet-trait>`.
The following example specifies a service that has standard regional endpoints extended with hand written rules:

.. code-block:: smithy

     @standardRegionalEndpoints
     @rulesBasedEndpoints
     service MyService {
         version: "2020-04-02"
     }

.. _aws.endpoints#endpoint-pattern:

----------------
Endpoint Pattern
----------------

Endpoint Patterns SHOULD begin with a scheme of either `http` or `https`.  When specifying special case endpoints in
:ref:`StandardRegionalEndpoints <aws.endpoints#standardRegionalEndpoints-trait>` or
:ref:`StandardPartitionalEndpoints <aws.endpoints#standardPartitionalEndpoints-trait>` you may use
an endpoint pattern such as ``https://{service}.{region}.{dnsSuffix}`` with the following pattern substitutions:

.. list-table::
    :header-rows: 1
    :widths: 10 60

    * - Pattern
      - Description
    * - ``{region}``
      - The region from the :ref:`AWS::Region built-in <rules-engine-aws-built-ins-region>`.
    * - ``{service}``
      - The endpoint prefix from the :ref:`service's endpointPrefix <service-endpoint-prefix>`.
    * - ``{dnsSuffix}``
      - The dns suffix from the :ref:`resolved partition's properties <rules-engine-aws-library-awsPartition-Partition>`.
    * - ``{dualStackDnsSuffix}``
      - The dualStack dns suffix from the :ref:`resolved partition's properties <rules-engine-aws-library-awsPartition-Partition>`.

