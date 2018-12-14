.PHONY: docker docker-armhf dist release deb manifest

dist:
	gradle installDist

docker: dist
	docker build . -t synesthesiam/jsgf-gen:1.0-amd64

docker-armhf: dist
	docker build . -f Dockerfile.armhf -t synesthesiam/jsgf-gen:1.0-armhf

release: dist
	rm -f jsgf-gen.tar.gz
	tar -C build/install -czf jsgf-gen.tar.gz jsgf-gen/

deb: dist
	rm -rf deb/jsgf-gen-1.0/usr/lib/jsgf-gen
	mkdir -p deb/jsgf-gen-1.0/usr/lib/jsgf-gen
	cp -R build/install/jsgf-gen deb/jsgf-gen-1.0/usr/lib/
	fakeroot dpkg -b deb/jsgf-gen-1.0 deb/jsgf-gen-1.0_all.deb

manifest:
	docker manifest push --purge synesthesiam/jsgf-gen:1.0
	docker manifest create --amend synesthesiam/jsgf-gen:1.0 synesthesiam/jsgf-gen:1.0-amd64 synesthesiam/jsgf-gen:1.0-armhf
	docker manifest annotate synesthesiam/jsgf-gen:1.0 synesthesiam/jsgf-gen:1.0-armhf --os linux --arch arm
	docker manifest push synesthesiam/jsgf-gen:1.0
