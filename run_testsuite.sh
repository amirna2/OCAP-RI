appendToFile=0
testcount=`cat $1 | grep tset | wc -l`
resultLabel=`echo $1 | sed 's|.*/\(.*\).scenario|\1|' | sed 's|\(.*\).scenario|\1|'`

if [ -n "$2" -a "$2" != "-retry" ]; then
    outputFile=$2
    appendToFile=1
else
    outputFile=${resultLabel}_results.txt
    appendToFile=0
fi

if [ -a ../setEnv ]; then
        . ../setEnv
else
        . ../../setEnv
fi

myDir=`pwd`
nohup start_daemon 2>&1 > /dev/null &
cd $PLATFORMROOT;./runRI.sh -ate -setup -deletelog -deletestorage -capall &
runRI_pid=$!

cd ~/atelite/results;rm -f a*
cd $myDir;loadtps $1

sleep 10s
RETV="0"
while [ "$RETV" -eq "0" ]; do
	atec stat | grep nobody > /dev/null
	RETV="$?"
	sleep 2s
done

kill -9 ${runRI_pid}
if [ `uname` == "Linux" ]
then
   killall -9 ri
   killall -9 vlc.exe
   killall -9 ate_if
else
   taskkill /F /IM ri.exe & > /dev/null
   taskkill /F /IM vlc.exe & > /dev/null
   taskkill /F /IM ate_if.exe & > /dev/null
fi

sleep 10s

if [ ${appendToFile} -eq 0 -a -e $PLATFORMROOT/../${outputFile} ]; then
    rm $PLATFORMROOT/../${outputFile}
fi

if [ ${appendToFile} -eq 0 ]; then
    date > $PLATFORMROOT/../${outputFile}
else
    date >> $PLATFORMROOT/../${outputFile}
fi

echo "" >> $PLATFORMROOT/../${outputFile}

cd ~/atelite/results;make_summary RESULT-TPID | tee -a $PLATFORMROOT/../${outputFile}
passcount=`make_summary RESULT-TPID | grep PASS | wc -l`
cd ${PLATFORMROOT}/../

echo "Pass count is ${passcount} of total tests: ${testcount} (${resultLabel})"
echo "" >> ${resultLabel}_results.txt
echo "Pass count is ${passcount} of total tests: ${testcount} (${resultLabel})" >> ${outputFile}
if [ ${passcount} -ne ${testcount} ]; then
	echo "TEST FAILED!" | tee -a $PLATFORMROOT/../${outputFile}
    date >>  ${outputFile}
    if [ "$2" = "-retry" ]; then
        cat ${outputFile} | grep -v PASS | grep tset | cut -f2 | sed 's/-/\t/g' > ${resultLabel}_retry.scenario
        echo "~~~~~~ Retrying non-passing tests... ~~~~~~" >> ${outputFile}
        ./$0 ${resultLabel}_retry.scenario ${resultLabel}_results.txt
        if [ "$?" -eq 0 ]; then
            exit 0
        fi
    fi
	exit -1
fi

exit 0
