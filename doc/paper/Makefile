ifeq ($(OS),Windows_NT)
	pdfviewer="C:\Program Files\Tracker Software\PDF Viewer\PDFXCview.exe"
else
	pdfviewer=evince
endif

show: make bibtex
	$(pdfviewer) paper.pdf &

plot:
	cd plots && make

convert:
	cd plots && make convert

make:
	@pdflatex -interaction=nonstopmode -halt-on-error -output-directory .output paper.tex 1>&2 > .output/error
	@mv .output/paper.pdf .

bibtex:
	@cd .output && bibtex paper > /dev/null && cd ..

clean:
	rm .output/*.aux .output/*.log
