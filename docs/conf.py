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
    "sphinx_markdown_builder",
    "smithy",
]
templates_path = ["../_templates", "../root"]

pygments_style = "default"
pygments_dark_style = "gruvbox-dark"

todo_include_todos = False
smartquotes = False
nitpicky = True

# -- Markdown configuration -----------------------------------------------

myst_enable_extensions = [
    "colon_fence"
]

# -- Options for HTML output ----------------------------------------------

html_theme = "furo"
language = "en"

html_static_path = ["../_static"]
html_css_files = ["custom.css"]
html_favicon = "../_static/favicon.svg"

html_theme_options = {
    "light_logo": "smithy.svg",
    "dark_logo": "smithy-dark.svg",
    "light_css_variables": {
        "admonition-font-size": "100%",
        "admonition-title-font-size": "100%",
        "color-brand-primary": "#C44536",
        "color-brand-content": "#00808b",
        "color-announcement-background": "#f8f8f8",
        "color-announcement-text": "#383838",
    },
    "dark_css_variables": {
        "color-brand-primary": "#ed9d13",
        "color-brand-content": "#58d3ff",
        "color-announcement-background": "##1a1c1e;",
    },
    "footer_icons": [
        {
            "name": "GitHub",
            "url": "https://github.com/smithy-lang/smithy",
            "html": """
                <svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"></path>
                </svg>
            """,
            "class": "",
        }
    ],
    "source_repository": "https://github.com/smithy-lang/smithy/",
    "source_branch": "main",
    "sidebar_hide_name": True,
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
    sphinx.connect("builder-inited", _builder_inited_handler)
    for placeholder, replacement in replacements:
        print("Finding and replacing '" + placeholder + "' with '" + replacement + "'")


# Rewrites placeholders with computed value
def source_read_handler(app, docname, source):
    for placeholder, replacement in replacements:
        source[0] = source[0].replace(placeholder, replacement)


# -- Markdown builder: register missing node handlers -----------------------

def _patch_markdown_translator(app):
    """Add handlers for node types not supported by sphinx-markdown-builder."""
    try:
        from sphinx_markdown_builder.translator import MarkdownTranslator
    except ImportError:
        return
    from docutils import nodes as _nodes

    def _make_admonition_visit(label):
        def visit(self, node):
            self._push_box(label)
        return visit

    def _admonition_depart(self, node):
        self._pop_context(node)

    for node_type, label in [
        ("tip", "TIP"),
        ("danger", "DANGER"),
        ("caution", "CAUTION"),
        ("admonition", "NOTE"),
    ]:
        visit_name = f"visit_{node_type}"
        depart_name = f"depart_{node_type}"
        if not hasattr(MarkdownTranslator, visit_name):
            setattr(MarkdownTranslator, visit_name, _make_admonition_visit(label))
            setattr(MarkdownTranslator, depart_name, _admonition_depart)

    # productionlist: render grammar rules as a code block
    def visit_productionlist(self, node):
        self._push_status(escape_text=False)
        self.add("```", prefix_eol=2, suffix_eol=1)
        for production in node.children:
            token_name = production.get("tokenname", "")
            rule_text = production.astext()
            if token_name:
                self.add(f"{token_name} ::= {rule_text}", prefix_eol=1)
            else:
                self.add(f"         {rule_text}", prefix_eol=1)
        self.add("```", prefix_eol=1, suffix_eol=2)
        self._pop_status()
        raise _nodes.SkipNode

    if not hasattr(MarkdownTranslator, "visit_productionlist"):
        MarkdownTranslator.visit_productionlist = visit_productionlist
        MarkdownTranslator.depart_productionlist = lambda self, node: None

    # caption: render as bold text
    def visit_caption(self, node):
        self.add("**", prefix_eol=2)

    def depart_caption(self, node):
        self.add("**", suffix_eol=2)

    if not hasattr(MarkdownTranslator, "visit_caption"):
        MarkdownTranslator.visit_caption = visit_caption
        MarkdownTranslator.depart_caption = depart_caption

    # hlist / hlistcol: pass through to children (they contain bullet_lists)
    def _pass(self, node):
        pass

    if not hasattr(MarkdownTranslator, "visit_hlist"):
        MarkdownTranslator.visit_hlist = _pass
        MarkdownTranslator.depart_hlist = _pass

    if not hasattr(MarkdownTranslator, "visit_hlistcol"):
        MarkdownTranslator.visit_hlistcol = _pass
        MarkdownTranslator.depart_hlistcol = _pass

    # label (non-footnote, e.g. tab labels from sphinx_inline_tabs): skip
    from sphinx_markdown_builder.contexts import FootNoteContext as _FootNoteContext

    def visit_label(self, node):
        if isinstance(self.ctx, _FootNoteContext):
            self.footnote_ctx.visit_label()
        else:
            raise _nodes.SkipNode

    def depart_label(self, node):
        if isinstance(self.ctx, _FootNoteContext):
            self.footnote_ctx.depart_label()

    MarkdownTranslator.visit_label = visit_label
    MarkdownTranslator.depart_label = depart_label


def _builder_inited_handler(app):
    if app.builder.name == "markdown":
        _patch_markdown_translator(app)
