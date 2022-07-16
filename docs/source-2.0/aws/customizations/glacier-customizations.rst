=============================
Amazon Glacier Customizations
=============================

--------------------------------
``X-Amz-Glacier-Version`` header
--------------------------------

A client for Amazon Glacier MUST set the ``X-Amz-Glacier-Version`` header to
the value of the service shape's ``version`` property for all requests.


---------------------------
Default value for accountId
---------------------------

Many operations in Amazon Glacier have an ``accountId`` member that is bound
to the URI. Customers can specify the string "-" to indicate that the
account making the request should be used. Since this is what customers
usually want, clients SHOULD set this value by default.


---------------------------
Default checksum generation
---------------------------

When uploading an archive as part of the `UploadArchive`_ or `UploadMultipartPart`_
operations, the ``X-Amz-Content-Sha256`` and ``X-Amz-Sha256-Tree-Hash``
headers MUST be set. Since the logic for computing these headers is static,
clients SHOULD populate them by default. See `computing checksums`_ for details
on how to calculate the values for these headers.


.. _UploadArchive: https://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-post.html
.. _UploadMultipartPart: https://docs.aws.amazon.com/amazonglacier/latest/dev/api-upload-part.html
.. _computing checksums: https://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html
