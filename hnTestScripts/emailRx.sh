
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

# Was an e-mail recipient specified?
if [ $# == 0  ]
then
  echo No recipients specified
  exit
fi
recipient=$1
echo Recipient $recipient
subject=$2
echo Subject: $subject
repository=$3
echo Repository: $repository
svnRevNo=$4
echo SVN revision number of the RI code: $svnRevNo

# Reformat the results so they are easier to read
svn checkout https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/Rx_Results
cd Rx_Results
pass=`grep -i ^pass runRxResults.txt | wc -l`
echo "These tests were run on ${repository} and SVN revision number of the RI code was $svnRevNo:" > reformat.txt
echo $pass Pass >> reformat.txt
fail=`grep -i scripts runRxResults.txt | grep "^FAIL" | wc -l`
echo $fail Fail >> reformat.txt
timedOut=`grep -i TimedOut runRxResults.txt | wc -l`
echo $timedOut TimedOut >> reformat.txt
echo " " >> reformat.txt

grep -i "^pass" runRxResults.txt >> reformat.txt
grep -i scripts runRxResults.txt | grep "^FAIL" | cut -f 1-2 >> reformat.txt
grep -i TimedOut runRxResults.txt >> reformat.txt
echo " " >> reformat.txt

reRun=`grep -i scripts runRxResults.txt | grep "Re-run results:*" | wc -l`
# Only display re-run results, if any of the tests were re-run coz of failures.
if [ $reRun -gt 0 ]
then
    echo "After re-running failing tests, Results are:" >> reformat.txt
    echo Number of tests re-run: $reRun >> reformat.txt
    echo " " >> reformat.txt
    rerunPass=`grep -i scripts runRxResults.txt | grep "Re-run results:Pass" | wc -l`
    echo $rerunPass Pass after re-run >> reformat.txt
    rerunFail=`grep -i scripts runRxResults.txt | grep "Re-run results:FAIL" | grep "FAIL$" | wc -l`
    echo $rerunFail Fail after re-run >> reformat.txt
    rerunTO=`(grep -i scripts runRxResults.txt | grep "Re-run results:.*TimedOut" | wc -l)`
    echo $rerunTO TimedOut after re-run >> reformat.txt

    grep -i "Re-run results:Pass" runRxResults.txt >> reformat.txt
    grep -i scripts runRxResults.txt | grep "^Re-run results:FAIL" | grep "FAIL$" | cut -f 1-2 >> reformat.txt
    grep -i scripts runRxResults.txt | grep "^Re-run results:.*TimedOut" | cut -f 1-2 >> reformat.txt
    echo " " >> reformat.txt
fi

excludedTests=`wc -l $PLATFORMROOT/../hnTestScripts/exclude_list.txt | cut -c1`
echo "Number of excluded tests: $excludedTests" >> reformat.txt
cat $PLATFORMROOT/../hnTestScripts/exclude_list.txt >> reformat.txt
echo " " >> reformat.txt
echo "--------------------------------------------------------------------------------------------------- " >> reformat.txt
echo "--------------------------------------------------------------------------------------------------- " >> reformat.txt

# Emailing the svn rev. checked in, so that pulling down logs 
# from svn for that rev. is easy for later use.
resultsSvnNo=`(svn info https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/Rx_Results | grep "^Revision: " | cut -c11-)`
echo "For full results, see revision number $resultsSvnNo of the test results at: " >> reformat.txt 
echo "https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/Rx_Results" >> reformat.txt

# email if possible
if [ -f /usr/bin/email ]
then
   cat reformat.txt | email -s $subject  $recipient
   echo Results sent via e-mail to $recipient
fi

# list the results in the log
echo "Contents of reformat.txt"
echo " "
cat reformat.txt
echo "-------------------------------------------------------------------------------------------------------"
echo "Contents of runRxResults.txt"
echo " "
cat runRxResults.txt 

# cleanup
rm reformat.txt

