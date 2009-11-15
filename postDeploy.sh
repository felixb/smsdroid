#! /bin/sh

for f in $(find src/ -name \*java) ; do
	git checkout $f
done

git checkout AndroidManifest.xml
