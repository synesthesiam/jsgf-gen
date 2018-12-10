.PHONY: docker dist release deb

dist:
	gradle installDist

docker: dist
	docker build . -t synesthesiam/jsgf-gen:1.0

release: dist
	rm -f jsgf-gen.tar.gz
	tar -C build/install -czf jsgf-gen.tar.gz jsgf-gen/

deb: dist
	rm -rf deb/jsgf-gen-1.0/usr/lib/jsgf-gen
	mkdir -p deb/jsgf-gen-1.0/usr/lib/jsgf-gen
	cp -R build/install/jsgf-gen deb/jsgf-gen-1.0/usr/lib/
	fakeroot dpkg -b deb/jsgf-gen-1.0 deb/jsgf-gen-1.0_all.deb
