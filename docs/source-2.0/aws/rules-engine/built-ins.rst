.. _rules-engine-aws-built-ins:

==========================
AWS rules engine built-ins
==========================

AWS-specific :ref:`rules engine built-ins <rules-engine-parameters-built-ins>`
make it possible to integrate with AWS concepts like `Regions`_. An additional
dependency is required to access these functions:

The following example adds ``smithy-aws-endpoints`` as a Gradle dependency
to a Smithy project:

.. tab:: Gradle

    .. code-block:: kotlin

        dependencies {
            ...
            implementation("software.amazon.smithy:smithy-aws-endpoints:__smithy_version__")
            ...
        }

.. tab:: smithy-build.json

    .. code-block:: json

        {
            "maven": {
                "dependencies": [
                    "software.amazon.smithy:smithy-aws-endpoints:__smithy_version__"
                ]
            }
        }


.. _rules-engine-aws-built-ins-region:

``AWS::Region`` built-in
========================

Description
    The AWS region configured for the SDK client.
Type
    ``string``


.. _rules-engine-aws-built-ins-use-dualstack:

``AWS::UseDualStack`` built-in
==============================

Description
    If the SDK client is configured to use dual stack endpoints, defaults to
    ``false``.
Type
    ``boolean``


.. _rules-engine-aws-built-ins-use-fips:

``AWS::UseFIPS`` built-in
=========================

Description
    If the SDK client is configured to use FIPS-compliant endpoints, defaults
    to ``false``.
Type
    ``boolean``

.. _rules-engine-aws-built-ins-account-id:

``AWS::Auth::AccountId`` built-in
=================================

Description
    The AWS AccountId.
Type
    ``string``

.. _rules-engine-aws-built-ins-credential-scope:

``AWS::Auth::CredentialScope`` built-in
=======================================

Description
    The AWS Credential Scope.
Type
    ``string``

.. _rules-engine-aws-built-ins-s3-accelerate:

``AWS::S3::Accelerate`` built-in
================================

Description
    If the SDK client is configured to use S3 transfer acceleration, defaults
    to ``false``.
Type
    ``boolean``


.. _rules-engine-aws-built-ins-s3-disable-mrap:

``AWS::S3::DisableMultiRegionAccessPoints`` built-in
====================================================

Description
    If the SDK client is configured to not use S3's multi-region access points,
    defaults to ``false``.
Type
    ``boolean``


.. _rules-engine-aws-built-ins-s3-force-path-style:

``AWS::S3::ForcePathStyle`` built-in
====================================

Description
    If the SDK client is configured to use solely S3 path style routing,
    defaults to ``false``.
Type
    ``boolean``


.. _rules-engine-aws-built-ins-s3-use-arn-region:

``AWS::S3::UseArnRegion`` built-in
==================================

Description
    If the SDK client is configured to use S3 bucket ARN regions or raise an
    error when the bucket ARN and client region differ, defaults to ``true``.
Type
    ``boolean``

.. important::
    SDKs MUST raise an error when the **partitions** of an ARN and the
    partition of the configured region differ.


.. _rules-engine-aws-built-ins-s3-use-global-endpoint:

``AWS::S3::UseGlobalEndpoint`` built-in
=======================================

Description
    If the SDK client is configured to use S3's global endpoint instead of the
    regional ``us-east-1`` endpoint, defaults to ``false``.
Type
    ``boolean``


.. _rules-engine-aws-built-ins-s3-control-use-arn-region:

``AWS::S3Control::UseArnRegion`` built-in
=========================================

Description
    If the SDK client is configured to use S3 Control bucket ARN regions or
    raise an error when the bucket ARN and client region differ, defaults to
    ``true``.
Type
    ``boolean``

.. important::
    SDKs MUST raise an error when the **partitions** of an ARN and the
    partition of the configured region differ.


.. _rules-engine-aws-built-ins-sts-use-global-endpoint:

``AWS::STS::UseGlobalEndpoint`` built-in
========================================

Description
    If the SDK client is configured to use STS' global endpoint instead of the
    regional ``us-east-1`` endpoint, defaults to ``false``.
Type
    ``boolean``

.. _Regions: https://docs.aws.amazon.com/whitepapers/latest/get-started-documentdb/aws-regions-and-availability-zones.html
