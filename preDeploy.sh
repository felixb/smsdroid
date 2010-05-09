#! /bin/sh

if [ -n "$1" ] ; then
	p=$1
else
	p=src/
fi

for f in $(find "${p}" -name \*java) ; do
	sed \
		-e 's: Log.v: //Log.v:' \
		-e 's: Log.d: //Log.d:' \
		-e 's:\tLog.v:\t//Log.v:' \
		-e 's:\tLog.d:\t//Log.d:' \
		-i $f
done

sed -e 's/android:debuggable="true"/android:debuggable="false"/' -i AndroidManifest.xml
