from sphinx.writers.html import HTMLTranslator as SphinxHTMLTranslator
from docutils.writers.html4css1 import HTMLTranslator as BaseTranslator
from docutils import nodes

from sphinx.locale import _


def setup(app):
    app.set_translator('html', HTMLTranslator)

class HTMLTranslator(SphinxHTMLTranslator):

    # Make production lists use ABNF and not BNF notation.
    def visit_productionlist(self, node):
        # type: (nodes.Node) -> None
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
