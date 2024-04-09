.. _cli_installation:

.. |release-uri| replace:: https://github.com/smithy-lang/smithy/releases/download/__smithy_version__
.. |mac| replace:: smithy-cli-darwin-x86_64
.. |mac-arm| replace:: smithy-cli-darwin-aarch64
.. |linux| replace:: smithy-cli-linux-x86_64
.. |linux-arm| replace:: smithy-cli-linux-aarch64
.. |windows| replace:: smithy-cli-windows-x64
.. |verification-note| replace::
        Please refer to :ref:`cli_verification` for instructions
        on how to verify downloaded artifacts.

.. |install-note| replace::
        If you've already installed the CLI and want to update to the
        latest version, you can use the ``--update`` flag.
        You may also choose where to install the CLI - for more information,
        run the installer using the ``--help`` flag.
.. |sudo-note| replace::
        The following command typically requires using ``sudo``
        to install the CLI in the default location (``/usr/local/smithy``).
        Alternatively, you may set your own install path, which should
        mitigate permissions issues when not running with ``sudo``.


============
Installation
============

The Smithy CLI is supported on the following platforms:

- MacOS (x86, ARM)
- Linux (x86, ARM)
- Windows (x64)

Releases of the Smithy CLI can be found on the `Smithy GitHub releases`_ page.

.. tab:: MacOS

    .. tab:: Homebrew (Recommended)

        You can get the CLI via Homebrew by first tapping the official
        `Smithy Homebrew Tap`_, and then installing the ``smithy-cli`` formula.

        .. code-block:: sh
            :caption: /bin/sh

            brew tap smithy-lang/tap && brew install smithy-cli

        After the install completes, you may run ``smithy --help`` to verify
        your installation.

    .. tab:: Manual (x86)

        First, retrieve the latest smithy installation from the
        `Smithy GitHub releases`_.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            mkdir -p smithy-install/smithy && \
                curl -L |release-uri|/|mac|.zip -o smithy-install/|mac|.zip && \
                unzip -qo smithy-install/|mac|.zip -d smithy-install && \
                mv smithy-install/|mac|/* smithy-install/smithy

        .. seealso::
            |verification-note|

        Now, run the installer (``./install``) located in the newly created
        directory (``smithy-install/smithy``).

        .. important::
            |sudo-note|

        .. code-block:: sh
            :caption: /bin/sh

            sudo smithy-install/smithy/install

        .. note::
            |install-note|

        After the install completes, you may run ``smithy --help`` to verify
        your installation. You may also delete all the files downloaded for
        performing the installation (under ``smithy-install/``):

        .. code-block:: sh
            :caption: /bin/sh

            rm -rf smithy-install/


    .. tab:: Manual (ARM)

        First, retrieve the latest smithy installation from the
        `Smithy GitHub releases`_.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            mkdir -p smithy-install/smithy && \
                curl -L |release-uri|/|mac-arm|.zip -o smithy-install/|mac-arm|.zip && \
                unzip -qo smithy-install/|mac-arm|.zip -d smithy-install && \
                mv smithy-install/|mac-arm|/* smithy-install/smithy

        .. seealso::
            |verification-note|

        Now, run the installer (``./install``) located in the newly created
        directory (``smithy-install/smithy``).

        .. important::
            |sudo-note|

        .. code-block:: sh
            :caption: /bin/sh

            sudo smithy-install/smithy/install

        .. note::
            |install-note|

        After the install completes, you may run ``smithy --help`` to verify
        your installation. You may also delete all the files downloaded for
        performing the installation (under ``smithy-install/``):

        .. code-block:: sh
            :caption: /bin/sh

            rm -rf smithy-install/


.. tab:: Linux

    .. tab:: Manual (x86)

        First, retrieve the latest smithy installation from the
        `Smithy GitHub releases`_.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            mkdir -p smithy-install/smithy && \
                curl -L |release-uri|/|linux|.zip -o smithy-install/|linux|.zip && \
                unzip -qo smithy-install/|linux|.zip -d smithy-install && \
                mv smithy-install/|linux|/* smithy-install/smithy

        .. seealso::
            |verification-note|

        Now, run the installer (``./install``) located in the newly created
        directory (``smithy-install/smithy``).

        .. important::
            |sudo-note|

        .. code-block:: sh
            :caption: /bin/sh

            sudo smithy-install/smithy/install

        .. note::
            |install-note|

        After the install completes, you may run ``smithy --help`` to verify
        your installation. You may also delete all the files downloaded for
        performing the installation (under ``smithy-install/``):

        .. code-block:: sh
            :caption: /bin/sh

            rm -rf smithy-install/

    .. tab:: Manual (ARM)

        First, retrieve the latest smithy installation from the
        `Smithy GitHub releases`_.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            mkdir -p smithy-install/smithy && \
                curl -L |release-uri|/|linux-arm|.zip -o smithy-install/|linux-arm|.zip && \
                unzip -qo smithy-install/|linux-arm|.zip -d smithy-install && \
                mv smithy-install/|linux-arm|/* smithy-install/smithy

        .. seealso::
            |verification-note|

        Now, run the installer (``./install``) located in the newly created
        directory (``smithy-install/smithy``).

        .. important::
            |sudo-note|

        .. code-block:: sh
            :caption: /bin/sh

            sudo smithy-install/smithy/install

        .. note::
            |install-note|

        After the install completes, you may run ``smithy --help`` to verify
        your installation. You may also delete all the files downloaded for
        performing the installation (under ``smithy-install/``):

        .. code-block:: sh
            :caption: /bin/sh

            rm -rf smithy-install/


.. tab:: Windows

    .. tab:: Scoop (Recommended)

        You can get the CLI via Scoop by first adding the official
        `Smithy Scoop Bucket`_, and then installing from the ``smithy-cli`` manifest.

        .. code-block:: powershell
            :caption: powershell

            scoop bucket add smithy-lang https://github.com/smithy-lang/scoop-bucket; `
                scoop install smithy-lang/smithy-cli

        After the install completes, you may run ``smithy --help`` to verify
        your installation.

    .. tab:: Manual (x64)

        First, retrieve the latest smithy installation from the
        `Smithy GitHub releases`_.

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            New-Item -Type Directory -Path smithy-install\smithy -Force; `
                Invoke-WebRequest -Uri |release-uri|/|windows|.zip `
                                  -OutFile smithy-install\|windows|.zip

        .. seealso::
            |verification-note|

        Now, unzip the installation in the newly created directory and run
        the installer (``install.bat``).

        .. important:: The following command typically requires running
            powershell with administrator access in order to install the
            CLI into the default location (``<Letter>:\Program Files\Smithy``).
            Alternatively, you may specify your own path, which can mitigate
            permissions issues when not running as administrator.

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            Expand-Archive smithy-install\|windows|.zip -DestinationPath smithy-install\; `
                Move-Item -Path smithy-install\|windows|\* -Destination smithy-install\smithy; `
                smithy-install\smithy\install

        Follow the installer prompts accordingly to complete the installation.

        After the install completes, you may run ``smithy --help`` to verify
        your installation. You may also delete all the files downloaded for
        performing the installation (under ``smithy-install\``):

        .. code-block:: powershell
            :caption: powershell

            rm -r -force smithy-install\

.. important:: If the location where the CLI is installed is not captured in
    your `PATH` environment variable, you will have to run the executable with
    the full path.


.. _cli_verification:

Verification (Optional)
-----------------------

The Smithy CLI distributables are PGP-signed and hashed before they are
released. You should verify the signatures and hashes of the archives to
ensure the validity and integrity of what you are downloading. You can follow
the steps below to do so for your given platform.

Before proceeding, please make sure you have both the GnuPG utility (``gpg``)
and a sha256-checksum utility (``sha256sum``) installed on your system.
We'll be using these to perform the verification.

.. note:: This is only applicable to manual installations - Homebrew and Scoop verify
    checksums before install.

.. tab:: MacOS

    Download the detached signature file (``.asc``) and the sha256-checksum
    file (``.sha256``) corresponding to the zip file (which you already
    downloaded) from the `Smithy GitHub releases`_.

    .. tab:: Manual (x86)

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L '|release-uri|/|mac|.zip.{asc,sha256}' -o 'smithy-install/|mac|.zip.#1'

        Verify the checksum of the zip file using the `sha256sum` utility.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            (cd smithy-install && sha256sum -c |mac|.zip.sha256)

        Now, retrieve the public PGP key from the `release`_, and import
        it into your key-chain.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L |release-uri|/smithy.asc -o smithy-install/smithy.asc && \
                gpg --import smithy-install/smithy.asc

        Finally, after importing the key, verify the signature of the
        zip file with gpg.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            gpg --verify smithy-install/|mac|.zip.asc smithy-install/|mac|.zip


    .. tab:: Manual (ARM)

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L '|release-uri|/|mac-arm|.zip.{asc,sha256}' -o 'smithy-install/|mac-arm|.zip.#1'

        Verify the checksum of the zip file using the `sha256sum` utility.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            (cd smithy-install && sha256sum -c |mac-arm|.zip.sha256)

        Now, retrieve the public PGP key from the `release`_, and import
        it into your key-chain.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L |release-uri|/smithy.asc -o smithy-install/smithy.asc && \
                gpg --import smithy-install/smithy.asc

        Finally, after importing the key, verify the signature of the
        zip file with gpg.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            gpg --verify smithy-install/|mac-arm|.zip.asc smithy-install/|mac-arm|.zip


.. tab:: Linux

    Download the detached signature file (``.asc``) and the sha256-checksum
    file (``.sha256``) corresponding to the zip file (which you already
    downloaded) from the `Smithy GitHub releases`_.

    .. tab:: Manual (x86)

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L '|release-uri|/|linux|.zip.{asc,sha256}' -o 'smithy-install/|linux|.zip.#1'

        Verify the checksum of the zip file using the `sha256sum` utility.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            (cd smithy-install && sha256sum -c |linux|.zip.sha256)

        Now, retrieve the public PGP key from the `release`_, and import
        it into your key-chain.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L |release-uri|/smithy.asc -o smithy-install/smithy.asc && \
                gpg --import smithy-install/smithy.asc

        Finally, after importing the key, verify the signature of the
        zip file with gpg.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            gpg --verify smithy-install/|linux|.zip.asc smithy-install/|linux|.zip


    .. tab:: Manual (ARM)

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L '|release-uri|/|linux-arm|.zip.{asc,sha256}' -o 'smithy-install/|linux-arm|.zip.#1'

        Verify the checksum of the zip file using the `sha256sum` utility.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            (cd smithy-install && sha256sum -c |linux-arm|.zip.sha256)

        Now, retrieve the public PGP key from the `release`_, and import
        it into your key-chain.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            curl -L |release-uri|/smithy.asc -o smithy-install/smithy.asc && \
                gpg --import smithy-install/smithy.asc

        Finally, after importing the key, verify the signature of the
        zip file with gpg.

        .. code-block:: sh
            :caption: /bin/sh
            :substitutions:

            gpg --verify smithy-install/|linux-arm|.zip.asc smithy-install/|linux-arm|.zip


.. tab:: Windows

    Download the detached signature file (``.asc``) and the sha256-checksum
    file (``.sha256``) corresponding to the zip file (which you already
    downloaded) from the `Smithy GitHub releases`_.

    .. tab:: Manual (x64)

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            Invoke-WebRequest -Uri |release-uri|/|windows|.zip.asc `
                -OutFile smithy-install\|windows|.zip.asc
            Invoke-WebRequest -Uri |release-uri|/|windows|.zip.sha256 `
                -OutFile smithy-install\|windows|.zip.sha256

        Compute the actual checksum of the zip file using ``certutil``.

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            certutil -hashfile smithy-install\|windows|.zip SHA256

        Now, print out the expected checksum from the file that you
        downloaded (``.sha256``).

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            cat smithy-install\|windows|.zip.sha256

        Verify that the output from the two commands matches. There may be a
        file-path appended to the latter command output - you
        may safely ignore that.

        Now, retrieve the public PGP key from the `release`_, and import it into your key-chain.

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            Invoke-WebRequest -Uri |release-uri|/smithy.asc `
                -OutFile smithy-install\smithy.asc; ` 
                gpg --import smithy-install\smithy.asc

        Finally, after importing the key, verify the signature of the
        zip file with gpg.

        .. code-block:: powershell
            :caption: powershell
            :substitutions:

            gpg --verify smithy-install\|windows|.zip.asc smithy-install\|windows|.zip

.. warning:: Upon verifying the signature, you will receive a
    ``WARNING`` message:

    .. code-block::

        gpg: WARNING: This key is not certified with a trusted signature!
        gpg:          There is no indication that the signature belongs to the owner.

    This is expected since there is no established chain of trust between
    you and the smithy key. For more information on this topic, see the
    `key validation`_ section from the GNU Privacy Handbook.


.. _Smithy GitHub releases: https://github.com/smithy-lang/smithy/releases
.. _Smithy Homebrew Tap: https://github.com/smithy-lang/homebrew-tap
.. _Smithy Scoop Bucket: https://github.com/smithy-lang/scoop-bucket
.. _release: https://github.com/smithy-lang/smithy/releases/download/__smithy_version__
.. _key validation: https://www.gnupg.org/gph/en/manual/x334.html
