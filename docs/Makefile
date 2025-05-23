# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = python3 -msphinx
SPHINXPROJ    = Smithy
SHELL         := /bin/bash

install: requirements.txt
	python3 -m venv build/venv
	source build/venv/bin/activate && pip3 install -r requirements.txt . && pip3 install -e .
	(cd landing-page && npm install)

clean:
	-rm -rf build/* install

html1:
	@source build/venv/bin/activate && $(SPHINXBUILD) -M html "source-1.0" "build/1.0" $(SPHINXOPTS) -W --keep-going -n $(O)

html2:
	@source build/venv/bin/activate && $(SPHINXBUILD) -M html "source-2.0" "build/2.0" $(SPHINXOPTS) -W --keep-going -n $(O)

build-landing-page:
	cd landing-page && npm run build

html: html2 html1 build-landing-page merge-versions 

serve: 
	npx http-server build/html

merge-versions:
	mkdir -p build/html
	cp -R "build/1.0/html" "build/html/1.0"
	cp -R "build/2.0/html" "build/html/2.0"
	cp -R root/* "build/html"
	cp -R landing-page/dist/* "build/html/"

openhtml:
	open "build/html/index.html"

dev: install
	@source build/venv/bin/activate && sphinx-autobuild "source-2.0" "build/2.0" $(SPHINXOPTS) $(O)

.PHONY: Makefile clean
