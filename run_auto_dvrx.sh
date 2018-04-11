if [ -a ../setEnv ]; then
        . ../setEnv
else
        . ../../setEnv
fi
myDir=`pwd`
myCmd=$1

cd $PLATFORMROOT;./runRI.sh -setup -deletestorage -autodvr $myCmd &
runRI_pid=$!

sleep 8s
RETV="-1"
while [ "$RETV" -ne "0" ]; do
    sleep 2s
	grep -q "Result for $myCmd" RILog.txt
	RETV="$?"
    if [ "$RETV" -ne "0" ]; then
        ps -p $runRI_pid | grep $runRI_pid
        if [ "$?" -eq "0" ]; then
            RETV="-1"
        else
            echo ">>> ri execution interrupted... <<<"
            RETV="0"
        fi
    fi
done

kill -9 ${runRI_pid}
if [ `uname` == "Linux" ]
then
   killall -9 ri
   killall -9 vlc.exe
   killall -9 ate_if
else
   taskkill /F /IM ri.exe
   taskkill /F /IM vlc.exe
   taskkill /F /IM ate_if.exe
fi

grep "Result for $myCmd" RILog.txt | grep -q "PASSED"
if [ "$?" -eq "0" ]; then
    echo "$myCmd PASSED"
    exit 0
else
    echo "$myCmd FAILED"
    exit 1
fi 
