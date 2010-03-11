#! /bin/sh

if [ "$(basename $0)" == "bump-connector.sh" ] ; then
	echo -n "bump connector: "
	basename $PWD
	n=$(fgrep app_name res/values/*.xml | cut -d\> -f2 | cut -d\< -f1 | tr ' ' '-' | tr -d ':')
else
	echo "bump websms"
	n=$(fgrep app_name res/values/*.xml | cut -d\> -f2 | cut -d\< -f1 | cut -d\  -f1 | tr -d \ )
fi

v=${1}
vv=$(echo ${v}0000 | tr -d . | head -c4 | sed -e 's:^0::g')

if [ -n "$2" ] ; then
	vn="$v $2"
else
	vn=$v
fi

echo v    $v
echo vn   $vn
echo vv   $vv
echo n    $n
echo tag "${n}-${vn/ /-}"
echo tagm  "$(echo ${n} | tr '-' ' ' | sed -e 's/Connector /Connector: /') v${vn}"

sed -i -e "s/android:versionName=\"[^\"]*/android:versionName=\"${vn}/" AndroidManifest.xml
sed -i -e "s/android:versionCode=\"[^\"]*/android:versionCode=\"${vv}/" AndroidManifest.xml
vfile=$(grep -l app_version res/values/*.xml)
sed -i -e "s/app_version\">[^<]*/app_version\">${vn}/" "${vfile}"

git diff

ant debug || exit -1

mv bin/*-debug.apk ~/public_html/h/flx/ 2> /dev/null

echo "enter for commit+tag"
read a
git commit -am "bump to ${n} v${vn}"
git tag -a "${n}-${vn/ /-}" -m "${n} v${vn}" 

