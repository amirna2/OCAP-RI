#
#     Are the environment variables set up?
#

if [ -f ~/setEnv ]; then
   echo "Resetting environment variables"
   source ~/setEnv
   echo reset OCAPROOT $OCAPROOT
   echo reset PLATFORMROOT  $PLATFORMROOT
   echo    ... OCAP environment variables set
   echo
fi

# Was a repository specified?
if [ $# == 0  ]
then
  echo No repository specified
else 
  repository=$1
fi

svn checkout https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/Rx_Results

#if [ "$?" -ne 0 ]; then
#echo "mf"

### Add a blank line as a separator from the svn info
echo >> $PLATFORMROOT/runRxResults.txt

### Capture the revision and date/time information  
### Note: If there are no differences between local and repository then
###   svn will not commit a 'new' version.  Date/time guarantees a difference.
svn info | grep Revision >> $PLATFORMROOT/runRxResults.txt
date >> $PLATFORMROOT/runRxResults.txt
svn info | grep Revision >> $PLATFORMROOT/rxLogFiles.zip
date >> $PLATFORMROOT/rxLogFiles.zip
echo "These results were run on $repository on $(uname -n) which is running $(uname -s)" >> $PLATFORMROOT/runRxResults.txt

### finally, commit the new file to svn 
cp $PLATFORMROOT/runRxResults.txt Rx_Results
cp $PLATFORMROOT/rxLogFiles.zip Rx_Results

cd Rx_Results
svn info rxLogFiles.zip 1>/dev/null 2>&1
rc=`echo $?`
# rxLogFiles.zip needs to be added to svn
if [ "$rc" -eq 1 ]
then
   svn add rxLogFiles.zip
fi

#TODO: Find a way of getting the current revision.
# svn info doesnt work as the entire OCAPRI dir is not a working copy.
#REV=svn info | grep '^Revision:' | sed -e 's/^Revision: //'
#echo "SVN Revision: ($REV+1)" >> runRxResults.txt
svn commit -m "Automated: Committed reports for runRx.sh for $(uname -n) which is running $(uname -s)"
cat runRxResults.txt
