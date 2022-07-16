from sphinx.locale import _
from sphinx.writers.html import HTMLTranslator as SphinxHTMLTranslator

from docutils import nodes

from .traitindex import setup_smithy_trait_index
from .redirects import setup as setup_redirects


def setup(app):
    app.set_translator('html', HTMLTranslator)
    setup_smithy_trait_index(app)
    setup_redirects(app)

class HTMLTranslator(SphinxHTMLTranslator):

    # Make production lists use ABNF and not BNF notation.
    def visit_productionlist(self, node):
        # type: (nodes.Node) -> None
        node.get('classes', []).append("productionlist")
        node.get('classes', []).append("highlight")
        self.body.append(self.starttag(node, 'pre'))
        lastname = None
        for production in node:
            if production['tokenname']:
                if lastname is not None:
                    self.body.append('\n')
                lastname = production['tokenname']
                self.body.append(self.starttag(production, 'strong', ''))
                self.body.append(lastname + "</strong> =\n    ")
            elif lastname is not None:
                self.body.append('  ')
            production.walkabout(self)
            self.body.append('\n')
        self.body.append('</pre>\n')
        raise nodes.SkipNode
