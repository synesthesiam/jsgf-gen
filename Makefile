.PHONY: docker dist release

docker: dist
	docker build . -t synesthesiam/jsgf-gen:1.0

dist:
	gradle installDist

release: dist
	tar -C build/install -czf jsgf-gen.tar.gz jsgf-gen/
