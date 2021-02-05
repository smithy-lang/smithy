========================
Amazon S3 Customizations
========================

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none

S3 Bucket Addressing
====================

Clients for Amazon S3 SHOULD expose multiple levels of configuration for bucket
addressing: environment, file, client, and operation. Settings for bucket
addressing should be resolved closest to the operation. Most bucket addressing
configuration settings compose together. The following table is a
non-exhaustive list of combinations showing the expected precedence orders for
the :ref:`s3-bucket-virtual-hosting` settings:

.. list-table::
    :header-rows: 1
    :widths: 20 20 20 20 20

    * - Environment
      - File
      - Client
      - Operation
      - Resolved
    * - virtual-host
      - unset
      - unset
      - unset
      - *virtual-host*
    * - path
      - virtual-host
      - unset
      - unset
      - *virtual-host*
    * - unset
      - unset
      - virtual-host
      - path
      - *path*
    * - path
      - virtual-host
      - unset
      - unset
      - *virtual-host*
    * - unset
      - virtual-host
      - path
      - unset
      - *path*

.. _s3-bucket-virtual-hosting:

S3 Bucket Virtual Hosting
-------------------------

A client for Amazon S3 MUST expose the option to use `virtual hosting`_ for
addressing a bucket. Configurations MUST support the default of using virtual
hosting, explicitly configuring virtual hosting, and explicitly configuring the
`path-style requests`_. When set to virtual hosting, clients MUST remove the
bucket name from the request URI and MUST prepend it to the request host. When
set to path-style, clients MUST NOT perform this action and MUST use the
modeled HTTP bindings for the request.

.. list-table::
    :header-rows: 1
    :widths: 20 60 20

    * - Style
      - Host
      - URI
    * - virtual
      - bucketname.s3.us-west-2.amazonaws.com
      - /
    * - path
      - s3.us-west-2.amazonaws.com
      - /bucketname


S3 Dual-Stack Endpoints
-----------------------

A client for Amazon S3 MUST expose the option to use `dual-stack endpoints`_
for addressing a bucket. Configurations MUST default this setting to being
disabled. When enabled, the string literal ".dualstack" is placed after S3's
:ref:`service-endpoint-prefix` of "s3" and before the region in the host for
the request. Clients MUST have the :ref:`s3-bucket-virtual-hosting` setting
resolved to "virtual" to enable this setting.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - DualStack Setting
      - Host
    * - Disabled
      - bucketname.s3.us-west-2.amazonaws.com
    * - Enabled
      - bucketname.s3.dualstack.us-west-2.amazonaws.com


S3 Transfer Acceleration Endpoints
----------------------------------

A client for Amazon S3 MUST expose the option to use S3 `transfer acceleration`_
for addressing a bucket. Configurations MUST default this setting to being
disabled. When enabled, the string literal "s3-accelerate" MUST replace the
S3's :ref:`service-endpoint-prefix` of "s3" and MUST remove the resolved region
from the host. Clients MUST have the :ref:`s3-bucket-virtual-hosting` setting
resolved to "virtual" to enable this setting.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Transfer Acceleration Setting
      - Host
    * - Disabled
      - bucketname.s3.us-west-2.amazonaws.com
    * - Enabled
      - bucketname.s3-accelerate.us-west-2.amazonaws.com

*TODO: Add the other bucket addressing customizations and more.*


.. _virtual hosting: https://docs.aws.amazon.com/AmazonS3/latest/dev/VirtualHosting.html
.. _path-style requests: https://docs.aws.amazon.com/AmazonS3/latest/dev/VirtualHosting.html#path-style-access
.. _dual-stack endpoints: https://docs.aws.amazon.com/AmazonS3/latest/dev/dual-stack-endpoints.html
.. _transfer acceleration: https://docs.aws.amazon.com/AmazonS3/latest/dev/transfer-acceleration.html
