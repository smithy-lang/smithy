from smithy.lexer import SmithyLexer
import requests
import xml.etree.ElementTree as ET
import re

project = "Smithy"
copyright = "2022, Amazon Web Services"
author = "Amazon Web Services"

# -- General configuration ------------------------------------------------

extensions = [
    "myst_parser",
    "sphinx_copybutton",
    "sphinx_substitution_extensions",
    "smithy",
]
templates_path = ["../_templates", "../root"]

# Pygments highlight styles are set per light/dark mode via html_theme_options
# below (pydata/sphinx-book-theme convention), not the top-level options furo
# used.

todo_include_todos = False
smartquotes = False
nitpicky = True

# -- Markdown configuration -----------------------------------------------

myst_enable_extensions = [
    "colon_fence"
]

# -- Options for HTML output ----------------------------------------------

html_theme = "sphinx_book_theme"
language = "en"

html_static_path = ["../_static"]
html_css_files = ["custom.css"]
html_js_files = ["custom.js"]
html_favicon = "../_static/favicon.svg"

# Publish the raw page source with a .txt suffix (e.g. quickstart.rst.txt) so
# the "View source" header button opens it as plain text in the browser rather
# than triggering a download, which a bare .rst can do on static hosts.
html_copy_source = True
html_show_sourcelink = True
html_sourcelink_suffix = ".txt"

html_theme_options = {
    # Light/dark wordmarks, resolved from html_static_path. No "text" label is
    # set: the wordmark already says "Smithy", so adding text would be a second
    # mark (BRAND.md: never place a second mark next to the wordmark).
    "logo": {
        "image_light": "_static/smithy.svg",
        "image_dark": "_static/smithy-dark.svg",
    },
    # The wordmark links to the site root (the landing page), not the docs root.
    # A root-relative "/" resolves to the server origin, so it points at the
    # smithy.io landing page in production and the local landing page under
    # `make serve` (build/html/index.html) without hardcoding the domain.
    "logo_link": "/",
    # "Edit this page" / repository button. path_to_docs is overridden
    # per-version in the source-1.0/source-2.0 conf files.
    "repository_url": "https://github.com/smithy-lang/smithy",
    "repository_branch": "main",
    "path_to_docs": "docs/source-2.0",
    "use_edit_page_button": True,
    "use_repository_button": True,
    "use_issues_button": True,
    # We add our own "View source" button (see _add_local_source_button) that
    # opens the raw .rst served from the site itself, rather than the built-in
    # GitHub blob link, so leave use_source_button off.
    "use_source_button": False,
    "use_download_button": False,
    "use_fullscreen_button": False,
    # GitHub icon in the navbar.
    "icon_links": [
        {
            "name": "GitHub",
            "url": "https://github.com/smithy-lang/smithy",
            "icon": "fa-brands fa-github",
            "type": "fontawesome",
        },
    ],
    # Restore a horizontal top header that mirrors the smithy.io landing page
    # (the sphinx-book-theme base blanks these slots and puts nav in the left
    # sidebar). Logo on the left; the homepage's Documentation / Examples /
    # Awesome Smithy links in the center; theme switcher + GitHub icon at the
    # end. The center links live in _templates/components/smithy-navbar-nav.html
    # so we show just those three rather than the whole doc toctree.
    "navbar_start": ["navbar-logo"],
    "navbar_center": ["components/smithy-navbar-nav.html"],
    "navbar_end": ["search-button-field", "theme-switcher", "navbar-icon-links"],
    "navbar_align": "left",
    # sphinx-book-theme puts a "toggle primary sidebar" button in the article
    # header, but with our restored pydata top header there are now two
    # .primary-toggle buttons and the theme JS only wires the first (the one in
    # the top header). The article-header copy is visible on desktop but dead,
    # so remove it; the top-header toggle still handles the mobile sidebar.
    "article_header_start": [],
    "show_navbar_depth": 1,
    "show_toc_level": 2,
    # Dark mode uses the custom "Ember" syntax theme (smithy/ember_style.py),
    # which is tuned for a dark code panel. In light mode pydata renders the
    # code panel on a light surface, where Ember's light-on-dark tokens wash
    # out, so light mode uses the high-contrast built-in "xcode" style instead.
    "pygments_light_style": "xcode",
    "pygments_dark_style": "ember",
}

# The sphinx-book-theme default left sidebar repeats the logo, icon links, and
# search box at the top (navbar-logo / icon-links / search-button-field). Those
# now live in the top header (see navbar_* above), so drop them here and keep
# the sidebar to just the navigation tree.
html_sidebars = {
    "**": ["sbt-sidebar-nav.html"],
}

# Disable the copy button on code blocks using the "no-copybutton" class.
copybutton_selector = "div:not(.no-copybutton) > div.highlight > pre"


# Load the version number from ../VERSION
def __load_version():
    with open("../../VERSION", "r") as file:
        return file.read().replace("\n", "")


# Find the latest version of the gradle plugin from github
def __load_gradle_version():
    return requests.get(
        "https://api.github.com/repos/smithy-lang/smithy-gradle-plugin/tags"
    ).json()[0]["name"]


# Find the latest version of the typescript codegen plugin from maven repo
def __load_typescript_codegen_version():
    response = requests.get(
        "https://repo1.maven.org/maven2/software/amazon/smithy/typescript/smithy-typescript-codegen/maven-metadata.xml"
    )
    response.raise_for_status()
    root = ET.fromstring(response.text)
    version = root.find(".//latest").text
    if version is None:
        raise Exception("Unable to find latest version of smithy-typescript-codegen")
    return version


# Find the latest version of smithy-java from github
def __load_java_version():
    tags = requests.get(
        "https://api.github.com/repos/smithy-lang/smithy-java/tags"
    ).json()
    return next(
        tag["name"] for tag in tags if re.match(r"^\d+\.\d+\.\d+$", tag["name"])
    )


# We use this list of replacements to replace placeholder values in the documentation
# with computed values. These are found and replaced
# using a source-read pre-processor so that the generated documentation
# always uses the latest computed value for the placeholder.
replacements = [
    ("__smithy_version__", __load_version()),
    ("__smithy_gradle_version__", __load_gradle_version()),
    ("__smithy_typescript_version__", __load_typescript_codegen_version()),
    ("__smithy_java_version__", __load_java_version()),
]


def setup(sphinx):
    sphinx.add_lexer("smithy", SmithyLexer)
    sphinx.connect("source-read", source_read_handler)
    _harden_pydata_short_link()
    # Runs after sphinx-book-theme populates header_buttons (priority 501).
    sphinx.connect("html-page-context", _add_local_source_button, priority=502)
    for placeholder, replacement in replacements:
        print("Finding and replacing '" + placeholder + "' with '" + replacement + "'")


# Adds a "View source" button to the article header that opens the page's raw
# reStructuredText served from the site itself (the _sources/ copy Sphinx emits
# when html_copy_source is on), rather than linking out to GitHub. This relies
# on sphinx-book-theme's header_buttons context list.
def _add_local_source_button(app, pagename, templatename, context, doctree):
    if not context.get("show_source") or not context.get("has_source"):
        return
    sourcename = context.get("sourcename")
    header_buttons = context.get("header_buttons")
    if not sourcename or header_buttons is None:
        return
    source_url = context["pathto"]("_sources/" + sourcename, resource=True)
    header_buttons.append(
        {
            "type": "link",
            "url": source_url,
            "tooltip": "View page source",
            # No text: show just the icon (the tooltip names it on hover).
            "text": "",
            "icon": "fas fa-code",
            "label": "source-file-button",
        }
    )


# The pydata-sphinx-theme "ShortenLinkTransform" (which shortens bare
# github/gitlab links) runs urlparse() unguarded on every bare link in the
# docs. Our rules-engine spec autolinks an IPv6 example URL ("https://[fe80::1]")
# whose trailing whitespace makes urlparse raise "Invalid IPv6 URL", aborting
# the whole build. The transform only ever cares about github.com/gitlab.com
# hosts, so we wrap its run() to swallow unparseable URIs and leave them as-is.
def _harden_pydata_short_link():
    try:
        from pydata_sphinx_theme.short_link import ShortenLinkTransform
    except ImportError:
        return

    original_run = ShortenLinkTransform.run

    def safe_run(self, **kwargs):
        try:
            return original_run(self, **kwargs)
        except ValueError:
            # An unparseable bare URL (e.g. an IPv6 literal example) is not a
            # link we'd shorten anyway; leave the document untouched.
            return None

    ShortenLinkTransform.run = safe_run


# Rewrites placeholders with computed value
def source_read_handler(app, docname, source):
    for placeholder, replacement in replacements:
        source[0] = source[0].replace(placeholder, replacement)
