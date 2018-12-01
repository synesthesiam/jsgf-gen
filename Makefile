dist:
	gradle installDist

release: dist
	tar -C build/install -czf jsgf-gen.tar.gz jsgf-gen/
