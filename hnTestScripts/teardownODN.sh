#
# Revert all files after running ODN 
# so that you can run CTP tests
#
echo revert all files after running ODN 
echo
echo ... reverting fdc-files.txt
cd $RICOMMONROOT/resources
svn revert fdc-files.txt
#
echo
echo ... reverting platform.cfg
cd $PLATFORMROOT
svn revert platform.cfg
#
echo

