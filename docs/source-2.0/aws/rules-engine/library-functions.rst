.. _rules-engine-aws-library-functions:

==================================
AWS rules engine library functions
==================================

AWS-specific rules engine library :ref:`functions <rules-engine-endpoint-rule-set-function>`
make it possible to integrate AWS concepts like `Amazon Resource Names (ARNs)`_
and `Partitions`_. An additional dependency is required to access these
functions:

The following example adds ``smithy-aws-endpoints`` as a dependency
to a Smithy project:

.. tab:: Smithy CLI

    .. code-block:: json
        :caption: smithy-build.json

        {
            "maven": {
                "dependencies": [
                    "software.amazon.smithy:smithy-aws-endpoints:__smithy_version__"
                ]
            }
        }

.. tab:: Gradle

    .. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            dependencies {
                ...
                implementation("software.amazon.smithy:smithy-aws-endpoints:__smithy_version__")
                ...
            }

    .. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            dependencies {
                ...
                implementation 'software.amazon.smithy:smithy-aws-endpoints:__smithy_version__'
                ...
            }

.. _rules-engine-aws-library-awsPartition:

``aws.partition`` function
==========================

Summary
    Provides a `Partition structure`_ of content for the
Argument type
    * region: ``string``
Return type
    ``option<Partition>``

    *Contains the region's partition, or an empty optional if the region is
    unknown.*

The following example uses ``aws.partition`` to provide `partition`_
information from the region in the value of the ``foo`` parameter:

.. code-block:: json

    {
        "fn": "aws.partition",
        "argv": [
            {"ref": "foo"}
        ]
    }


.. _rules-engine-aws-library-awsPartition-Partition:

-----------------------
``Partition`` structure
-----------------------

The ``Partition`` structure is returned from the `aws.partition function`_ when
its input is a known region. The ``Partition`` object contains the following
properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - The partition's name.
    * - dnsSuffix
      - ``string``
      - The partition's default DNS suffix.
    * - dualStackDnsSuffix
      - ``string``
      - The partition's dual-stack specific DNS suffix.
    * - supportsFIPS
      - ``bool``
      - Indicates whether the partition supports a FIPS compliance mode.
    * - supportsDualStack
      - ``bool``
      - Indicates whether the partition supports dual-stack endpoints.
    * - implicitGlobalRegion
      - ``string``
      - The region used by partitional (non-regionalized/global) services for signing.


.. _rules-engine-aws-library-awsParseArn:

``aws.parseArn`` function
=========================

Summary
    Computes an `ARN structure`_ given an input ``string``.
Argument type
    * value: ``string``
Return type
    ``option<ARN>``

    *Contains the parsed ARN, or an empty optional if the ARN could not be
    parsed*

The following example uses ``aws.parseArn`` to parse the value of the ``foo``
parameter into its component parts:

.. code-block:: json

    {
        "fn": "aws.parseArn",
        "argv": [
            {"ref": "foo"}
        ]
    }


.. _rules-engine-aws-library-parseArn-Arn:

-----------------
``ARN`` structure
-----------------

The ``ARN`` structure is returned from the `aws.parseArn function`_ when its
input is a valid `ARN`_. The ``ARN`` object
contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - partition
      - ``string``
      - The partition where the resource is located.
    * - service
      - ``string``
      - The service namespace where the resource is located.
    * - region
      - ``string``
      - The region where the resource is located. May be an empty length
        value if the resource is not region-based.
    * - accountId
      - ``string``
      - The account that the resource is managed by. May be an empty length
        value if the resource is not account-based.
    * - resourceId
      - ``array<string>``
      - An array of resourceId components, where the final segment of the
        ARN is split on ``:`` and ``/`` characters.


.. _rules-engine-aws-library-parseArn-examples:

--------
Examples
--------

The following table shows valid and invalid values for an input to the
`aws.parseArn function`_:

.. list-table::
    :header-rows: 1
    :widths: 25 10 10 15 15 15 10

    * - Input
      - Valid?
      - partition
      - service
      - region
      - accountId
      - resourceId
    * - ``arn:aws:sns:us-west-2:012345678910:example-sns-topic-name``
      - ``true``
      - ``aws``
      - ``sns``
      - ``us-west-2``
      - ``012345678910``
      - ``example-sns-topic-name``
    * - ``11111111-2222-3333-4444-555555555555``
      - ``false``
      -
      -
      -
      -
      -
    * - ``arn:aws:ec2:us-east-1:012345678910:vpc/vpc-0e9801d129EXAMPLE``
      - ``true``
      - ``aws``
      - ``ec2``
      - ``us-east-1``
      - ``012345678910``
      - ``[vpc, vpc-0e9801d129EXAMPLE]``
    * - ``arn:aws:iam::012345678910:user/johndoe``
      - ``true``
      - ``aws``
      - ``iam``
      - An empty string.
      - ``012345678910``
      - ``[user, johndoe]``
    * - ``arn:aws:s3:::bucket_name``
      - ``true``
      - ``aws``
      - ``s3``
      - An empty string.
      - An empty string.
      - ``bucket_name``


.. _rules-engine-aws-library-isVirtualHostableS3Bucket:

``aws.isVirtualHostableS3Bucket`` function
==========================================

Summary
    Evaluates whether the input string is a compliant :rfc:`1123` host segment
    and contains a segment that is a valid bucket name. When ``allowSubDomains``
    is true, evaluates whether the input string is composed of values that are
    each compliant values joined by dot (``.``) characters.
Argument type
    * value: ``string``
    * allowSubDomains: ``bool``
Return type
    ``bool``

The following example uses ``aws.isVirtualHostableS3Bucket`` to check if the
value of the ``foo`` parameter is an :rfc:`1123` compliant host segment and a
valid bucket name.

.. code-block:: json

    {
        "fn": "aws.isVirtualHostableS3Bucket",
        "argv": [
            {"ref": "foo"},
            false
        ]
    }

.. _ARN: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
.. _Amazon Resource Names (ARNs): https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
.. _partition: https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/partitions.html
.. _Partitions: https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/partitions.html
