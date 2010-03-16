#! /bin/sh

if [ -n "$1" ] ; then
	p=$1
else
	p=src/
fi

for f in $(find "${p}" -name \*java) ; do
	sed -e 's:///*Log.v:Log.v:' -i $f
	sed -e 's:///*Log.d:Log.d:' -i $f
done

for f in $(find . -name AndroidManifest.xml) ; do
	sed -e 's/android:debuggable="false"/android:debuggable="true"/' -i $f
done
