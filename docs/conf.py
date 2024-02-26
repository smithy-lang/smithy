from smithy.lexer import SmithyLexer
import requests

project = u'Smithy'
copyright = u'2022, Amazon Web Services'
author = u'Amazon Web Services'

# -- General configuration ------------------------------------------------

extensions = ['sphinx_copybutton', 'sphinx_substitution_extensions', 'smithy']
templates_path = ['../_templates', '../root']

pygments_style = "default"
pygments_dark_style = "gruvbox-dark"

todo_include_todos = False
smartquotes = False
nitpicky = True

# -- Options for HTML output ----------------------------------------------

html_theme = 'furo'
language = "en"
html_logo = "../_static/smithy.svg"

html_static_path = ["../_static"]
html_css_files = ["custom.css"]
html_favicon = "../_static/favicon.png"

html_theme_options = {
    "light_css_variables": {
        "admonition-font-size": "100%",
        "admonition-title-font-size": "100%",
        "color-brand-primary": "#C44536",
        "color-brand-content": "#00808b",
        "color-announcement-background": "#711818de",
        "color-announcement-text": "#fff"
    },
    "dark_css_variables": {
        "color-brand-primary": "#ed9d13",
        "color-brand-content": "#58d3ff",
        "color-announcement-background": "#711818de",
        "color-announcement-text": "#fff"
    },
    "footer_icons": [
        {
            "name": "GitHub",
            "url": "https://github.com/awslabs/smithy",
            "html": """
                <svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"></path>
                </svg>
            """,
            "class": "",
        }
    ],
    "source_repository": "https://github.com/awslabs/smithy/",
    "source_branch": "main",
    "sidebar_hide_name": True
}

# Disable the copy button on code blocks using the "no-copybutton" class.
copybutton_selector = "div:not(.no-copybutton) > div.highlight > pre"

# Load the version number from ../VERSION
def __load_version():
    with open('../../VERSION', 'r') as file:
        return file.read().replace('\n', '')

# We use the __smithy_version__ placeholder in documentation to represent
# the current Smithy library version number. This is found and replaced
# using a source-read pre-processor so that the generated documentation
# always references the current VERSION.
smithy_version = __load_version()
smithy_version_placeholder = "__smithy_version__"

## Find the latest version of the gradle plugin from github
def __load_gradle_version():
    return requests.get('https://api.github.com/repos/smithy-lang/smithy-gradle-plugin/tags').json()[0]['name']

# We use the __smithy_gradle_version__ placeholder in documentation to represent
# the current gradle plugin version number. This is found and replaced
# using a source-read pre-processor so that the generated documentation
# always references the latest release of the gradle plugin
smithy_gradle_plugin_version = __load_gradle_version()
smithy_gradle_version_placeholder = "__smithy_gradle_version__"

def setup(sphinx):
    sphinx.add_lexer("smithy", SmithyLexer)
    sphinx.connect('source-read', source_read_handler)
    print("Finding and replacing '" + smithy_version_placeholder + "' with '" + smithy_version + "'")
    print("Finding and replacing '" + smithy_gradle_version_placeholder + "' with '" + smithy_gradle_plugin_version + "'")

# Rewrites __smithy_version__ to the version found in ../VERSION and
# rewrites __smithy_gradle_version__ to the latest version found on Github
def source_read_handler(app, docname, source):
    source[0] = source[0].replace(smithy_version_placeholder, smithy_version)
    source[0] = source[0].replace(smithy_gradle_version_placeholder, smithy_gradle_plugin_version)
