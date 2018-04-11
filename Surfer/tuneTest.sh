#!/bin/sh

ipAddr=$1
iter=1
sleepVal=$2

logMsg()
{
    if [ -n "$1" ]
    then
        echo $1
        test_if4ri --host=$ipAddr log "$1"

        if [ $? -ne 0 ]
        then
            echo "logMsg failed!?"
            exit 2
        fi
    fi
}

signal_handler()
{
    logMsg "killed... pid was: $$";
    sleep $sleepVal
    exit 1
}

sendKey()
{
    if [ -n "$1" ]
    then
        test_if4ri --host=$ipAddr key $1

        if [ $? -ne 0 ]
        then
            echo "sendKey failed!?"
            exit 2
        fi

        if [ -n "$2" ]
        then
            sleep $2
        fi
    fi
}

main()
{
    if [ $ipAddr ]; then
        logMsg "RI Platform IP address: $ipAddr"
    else
        ipAddr=127.0.0.1
        logMsg "default IP address: $ipAddr"
    fi

    if [ $sleepVal ]; then
        logMsg "interstitial sleep: $sleepVal"
    else
        sleepVal=3
        logMsg "default sleep: $sleepVal"
    fi

    logMsg "powering on..."
    sendKey 69 $sleepVal  # power key (turn power on)

    while [ true ]
    do
        logMsg "iteration $iter <----"
        logMsg "tuner 0 channel up..."
        sendKey 65 $sleepVal  # channel up
        logMsg "tuner 0 channel up..."
        sendKey 65 $sleepVal  # channel up
        logMsg "swap to tuner 1..."
        sendKey 72 $sleepVal  # live key (swap tuners)
        logMsg "tuner 1 channel up..."
        sendKey 65 $sleepVal  # channel up
        logMsg "swap to tuner 0..."
        sendKey 72 $sleepVal  # live key (swap tuners)
        logMsg "tuner 0 channel down..."
        sendKey 66 $sleepVal  # channel down
        logMsg "power off..."
        sendKey 69 $sleepVal  # power key (turn power off)
        logMsg "power on..."
        sendKey 69 $sleepVal  # power key (turn power on)
        logMsg "swap to tuner 1..."
        sendKey 72 $sleepVal  # live key (swap tuners)
        logMsg "tuner 1 channel down..."
        sendKey 66 $sleepVal  # channel down
        let iter=iter+1
    done
}

trap signal_handler INT KILL
main

