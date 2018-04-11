#!/bin/sh
CURRDIR="`pwd`"; export CURRDIR
mkdir Output
rm -rf "/tmp/nanoxml-build-$$"
rm -rf "/tmp/nanoxml-sax-build-$$"
rm -rf "/tmp/nanoxml-lite-build-$$"

echo "Compiling NanoXML/Lite 2.2.3"
mkdir "/tmp/nanoxml-lite-build-$$"
(cd Sources/Lite; javac -deprecation -target 1.1 -g:none -d "/tmp/nanoxml-lite-build-$$" nanoxml/*.java) || exit 1
(cd /tmp/nanoxml-lite-build-$$; jar cMf "$CURRDIR/Output/nanoxml-lite.jar" nanoxml/*.class) || exit 1
(cd Test/Lite; javac -classpath ":$CURRDIR/Output/nanoxml-lite.jar:." DumpXML_Lite.java) || exit 1

echo "Compiling NanoXML/Java 2.2.3"
mkdir "/tmp/nanoxml-build-$$"
(cd Sources/Java; javac -deprecation -g:none -d "/tmp/nanoxml-build-$$" net/n3/nanoxml/*.java) || exit 1
(cd /tmp/nanoxml-build-$$; jar cMf "$CURRDIR/Output/nanoxml.jar" net/n3/nanoxml/*.class) || exit 1
(cd Test/Java; javac -classpath ":$CURRDIR/Output/nanoxml.jar:." DumpXML.java) || exit 1

echo "Compiling NanoXML/SAX 2.2.3"
CLASSPATH=":$CURRDIR/ThirdParty/SAX/sax.jar:/tmp/nanoxml-build-$$:."; export CLASSPATH
mkdir "/tmp/nanoxml-sax-build-$$"
mkdir "/tmp/nanoxml-sax-build-$$/net"
mkdir "/tmp/nanoxml-sax-build-$$/net/n3"
mkdir "/tmp/nanoxml-sax-build-$$/net/n3/nanoxml"
for a in ContentReader.class IXMLBuilder.class IXMLParser.class IXMLReader.class IXMLValidator.class NonValidator.class StdXMLParser.class StdXMLReader.class XMLEntityResolver.class XMLParseException.class XMLParserFactory.class XMLUtil.class XMLValidationException.class
    do cp "/tmp/nanoxml-build-$$/net/n3/nanoxml/$a" "/tmp/nanoxml-sax-build-$$/net/n3/nanoxml/"
  done
(cd Sources/SAX; javac -g:none -d "/tmp/nanoxml-build-$$" net/n3/nanoxml/sax/*.java) || exit 1
(cd /tmp/nanoxml-build-$$; jar cMf "$CURRDIR/Output/nanoxml-sax.jar" net/n3/nanoxml/sax/*.class)

echo "Generating JavaDoc"
mkdir "$CURRDIR/Documentation/JavaDoc"
javadoc -protected -sourcepath "$CURRDIR/Sources/Lite:$CURRDIR/Sources/Java:$CURRDIR/Sources/SAX"         -classpath "/tmp/nanoxml-lite-build-$$:/tmp/nanoxml-build-$$:/tmp/nanoxml-sax-build-$$:$CURRDIR/Thirdparty/SAX/sax.jar"         -d "$CURRDIR/Documentation/JavaDoc" -version -author -windowtitle "NanoXML 2.2.3"         net.n3.nanoxml         net.n3.nanoxml.sax         nanoxml >"/tmp/javadoc.log" 2>&1 || (cat /tmp/javadoc.log; exit 1) || exit 1

rm -rf "/tmp/nanoxml-lite-build-$$"
rm -rf "/tmp/nanoxml-sax-build-$$"
rm -rf "/tmp/nanoxml-build-$$"

echo "Done"
echo "Output is in $CURRDIR/Output/"

