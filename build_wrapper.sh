#!/bin/sh
if [ -a ../setEnv ]; then
        . ../setEnv
else
        . ../../setEnv
fi

case $1 in
	-s)
	dir=$OCAPROOT
	cmd=ant
	;;
	-sj)
	dir=$OCAPROOT/java
	cmd=ant
	;;
	-p)
	dir=$PLATFORMROOT
	cmd=make
	;;
	*)
	echo "invalid identifier - must be -s (for stack), -sj (stack/java), or -p (platform)"
	exit -1
	;;
esac
shift;
cd $dir;$cmd $@
