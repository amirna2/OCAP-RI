#!/bin/bash
#
#  Install the emma jar to capture coverage data on the nightly runs.
#  Note that emma is NOT checked in with the other generic tools
#  but is expected to be in the Downloads directory.
#  Details for emma are on the public Wiki page. 
#
emmaDir=/cygdrive/c/Downloads/emma-stable-2.1.5320-lib
if [ ! -d $emmaDir ]; then
   echo "Emma directory not found where expected:"
   echo $emmaDir
   exit
fi
if [ ! -d "$OCAPROOT" ]; then
   echo "OCAPROOT not a directory"
   exit
fi

echo copying emma jar into tools/generic
mkdir $OCAPROOT/tools/generic/emma
cp $emmaDir/*.jar $OCAPROOT/tools/generic/emma
ls -la $OCAPROOT/tools/generic/emma

cd $OCAPROOT/java
sed -e 's|build.test.CLASSPATH=|build.test.CLASSPATH=tools/generic/emma/emma.jar|'  OCAP-debug.properties

ant build

#
# To verify it works, attempt to instrument the stack
# cd $PLATFORMROOT
# ./runRI.sh -coverage
