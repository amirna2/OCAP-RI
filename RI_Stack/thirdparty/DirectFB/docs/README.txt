Use the following commands from a bash shell to build the documentation:

cd html
rm -f *.html
perl ../gendoc.pl < ../../include/directfb.h
