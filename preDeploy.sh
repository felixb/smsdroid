#! /bin/sh

for f in $(find src/ -name \*java) ; do
	sed -e 's:Log.v://Log.v:' -i $f
	sed -e 's:Log.d://Log.d:' -i $f
done

sed -e 's/android:debuggable="true"/android:debuggable="false"/' -i AndroidManifest.xml
