#!/bin/bash

# DO NOT RUN THIS SCRIPT DIRECTLY. RUN IT ONLY THROUGH build.xml.

# TODO: finish "bashdoc" comments
# TODO: install atelite from scratch

# TODO: validate minMaturity for numeric (here or in build.xml)
# TODO: validate commitResult for Boolean (here or in build.xml)

# TODO: detect stream build failures, e.g.
#       [exec] Building 230 of 283: tsbuild dotm/tset.ocap.hn/org.ocap.hn/ContentServerNetModule.m 510
#       [exec] sending tar
#       [exec] tar: This does not look like a tar archive
#       [exec] tar: Skipping to next header
#       [exec] tar: Exiting with failure status due to previous errors

tsDir=~/atelite/testsuite
trDir=~/atelite/results

riRepositoryURL=https://community.cablelabs.com/svn/OCAPRI

##############################################################################
#
# function main
#
##############################################################################

function main
{
    local cmd=$1
    shift

    case $cmd in
      -ProcessResults )
        processResults "$@"
        ;;

      -EMailResults )
        emailResults "$@"
        ;;

      -RunTests )
        runTests "$@"
        ;;

      -UpdateDotM )
        updateDotM "$@"
        ;;

      -UpdateEnv )
        updateEnv "$@"
        ;;

      -UpdateRI )
        updateRI "$@"
        ;;

      * )
        echo "unrecognized arg: $cmd"
        exit 1
        ;;
    esac
}

##############################################################################
#
# function majorHeading
#
# Display a major heading.
#
# args: The text that is to appear in the heading.
#
##############################################################################

function majorHeading
{
    local bar='************************************************************************'

    local outerLen=${#bar}
    local innerLen=$(( outerLen - 2 ))

    local text=$(echo "$*" | tr [:lower:] [:upper:])
    local textLen=${#text}

    if [ $textLen -gt $innerLen ]
    then
        text=${text:0:$innerLen}
        textLen=${#text}
    fi

    local textLPad=$(( (innerLen - textLen) / 2 ))
    local textRPad=$(( innerLen - textLPad - textLen ))

    local date=$(date)
    local dateLen=${#date}

    if [ $dateLen -gt $innerLen ]
    then
        date=${date:0:$innerLen}
        dateLen=${#date}
    fi

    local dateLPad=$(( (innerLen - dateLen) / 2 ))
    local dateRPad=$(( innerLen - dateLPad - dateLen ))

    echo "$bar"
    echo "*$(printf %${innerLen}s)*"

    echo "*$(printf %${textLPad}s)$text$(printf %${textRPad}s)*"
    echo "*$(printf %${dateLPad}s)$date$(printf %${dateRPad}s)*"

    echo "*$(printf %${innerLen}s)*"
    echo "$bar"

    echo
}

##############################################################################
#
# function processResults
#
##############################################################################

function processResults
{
    local riRoot="$1"
    local scriptDir="$2"
    local label=$3
    local commitResult=$4
    local OCAPRI_root=$5
    local ocOcapTestRoot=$6

    setEnv "$riRoot"

    local RECORD_DIR=$trDir/home_networking
    local REVISION_FILE=$RECORD_DIR/revisions.txt
    local PLATFORM="$label run on $(uname -n) ($(uname -s))"

    cd $trDir

    # Check for results directory, if not installed, checkout.
    if [ ! -d $RECORD_DIR ]
    then
        svn checkout https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/CTP_results/home_networking
    else
        rm $RECORD_DIR/*
        svn update $RECORD_DIR
    fi

    # Capture revisions of code executed for this run.
    local riRevision=$(cat "$riRoot"/OCAPRI_root/revision.txt)
    local ctpRevision=$(cat ~/atelite/revision.txt)
    echo "# Code revisions and branches for $PLATFORM" > $REVISION_FILE
    echo "# RI  $riRevision ($OCAPRI_root)" >> $REVISION_FILE
    echo "# CTP $ctpRevision ($ocOcapTestRoot)" >> $REVISION_FILE

    # Record current results
    cd $trDir

    # If result summary from run_test are not available, attempt to create with current set of files.
    if [ ! -f $label.txt ]
    then
        make_summary JOBID-TPID-RESULT-SUBRESULTS | awk -f $scriptDir/process.result.awk > $label.txt
    fi

    cat $label.txt | cut -d " " -f2,3 | sed s/tset.// | sed s/builder.// | sed s/-/,/g | sed s/\\s/,/ | sort -u > $RECORD_DIR/results.csv
    cat $label.txt | grep NONPASS | cut -d " " -f 1 | xargs tar cfz $RECORD_DIR/non-pass-logs.tar.gz

    local numberOfTests=$(cat $label.txt | wc -l)
    local numberOfNonPass=$(grep NONPASS $label.txt | wc -l)
    local summaryResult="TOTAL=$numberOfTests NONPASS=$numberOfNonPass"

    # remove non-pass-logs.tar.gz so we don't check in old results
    # the tar failed since there wasn't anything to tar,,clean up
    # create a 0 size non-pass-logs.tar.gz
    if [ $numberOfNonPass -eq 0 ]
    then
        echo "Removing non pass results" 
        rm -f $RECORD_DIR/non-pass-logs.tar.gz
        rm -f $RECORD_DIR/non-pass-logs
        rm -f $RECORD_DIR/a*
        touch $RECORD_DIR/non-pass-logs.tar.gz 
    fi

    rm $label.txt

    if [ $commitResult = "true" ]
    then
        cd $RECORD_DIR
        svn commit -m "Automated: Committed reports for $PLATFORM $summaryResult"
        cd - >/dev/null
    fi
}

##############################################################################
#
# function EMailResults
#
##############################################################################

function emailResults
{
    local RECORD_DIR=$trDir/home_networking

    cd $trDir

    # Check for results directory, if not installed, checkout.
    if [ ! -d $RECORD_DIR ]
        then
            svn checkout https://community.cablelabs.com/svn/oc/ocap_ri/trunk/ri/QA/CTP_results/home_networking
    else
           rm $RECORD_DIR/*
           svn update $RECORD_DIR
    fi


    if [ -f mailFile.txt ]
    then
       rm mailFile.txt
    fi

    cd $RECORD_DIR


    local numberOfTests=$(cat results.csv | wc -l)
    local numberOfNonPass=$(grep NONPASS results.csv | wc -l)
    local summaryResult="TOTAL=$numberOfTests NONPASS=$numberOfNonPass"

    echo $summaryResult > mailFile.txt

    # process NONPASS results if there were any
    if [ $numberOfNonPass -ne 0 ]
    then
        gunzip non-pass-logs.tar.gz
        tar xf non-pass-logs.tar
        local numberOfFail=$(grep __RESULT=FAIL a* | wc -l)
        local numberOfUnresolved=$(grep __RESULT=UNRESOLVED a* | wc -l)
        local numberOfUntested=$(grep __RESULT=UNTESTED a* | wc -l)
        local numberOfMultiple=$(grep __RESULT=MULTIPLE a* | wc -l)
        local numberOfAteError=$(grep __RESULT=AteError a* | wc -l)

        echo "FAILS= $numberOfFail" >> mailFile.txt
        echo "UNRESOLVED= $numberOfUnresolved" >> mailFile.txt
        echo "MULTIPLES = $numberOfMultiple" >> mailFile.txt
        echo "UNTESTED = $numberOfUntested" >> mailFile.txt
        echo "ATE ERROR = $numberOfAteError" >> mailFile.txt
    fi

    ## email tsbuild errors if any occurred
    if [ -f $tsDir/tsbuild.err ]
    then
       echo >> mailFile.txt
       echo "!!!!BUILD ERRORS DETECTED!!!" >> mailFile.txt
       cat $tsDir/tsbuild.err >> mailFile.txt
       rm $tsDir/tsbuild.err
    fi

    ### Send the file "mailFile.txt" via e-mail
    ### mailx variables in .mailrc
    ### set from="LabD <ocaptst_build@cablelabs.com>"
    ### set smtp=smtp.cablelabs.com

    echo " "
    echo "SENDING E_MAIL !"
    if which mailx &> /dev/null;
    then
        agent=mailx
    elif which email &> /dev/null;
    then
        agent=email
    fi

    if [ $agent ]
    then
        cat mailFile.txt | $agent -s "Nightly HN Results" opencable-ri-hn@cablelabs.com 
    fi 

    ### show us the results
    cat mailFile.txt
    rm mailFile.txt
}

##############################################################################
#
# function runTests
#
##############################################################################

function runTests
{
    local riRoot="$1"
    local scriptDir="$2"
    local testFilter="$3"
    local excludeUnreliable="$4"
    local fileUnreliableTests="$5"

    setEnv "$riRoot"

    stopRI
    stop_daemon

    start_daemon >/dev/null

    majorHeading "Starting RI."

    startRI

    majorHeading "Deleting old results."

    rm -rf $trDir/a*

    majorHeading "Finding tests."

    cd $tsDir

    find tset.builder.* -type d -name \*.d | sort | sed 's|/| |g' |
        while read tset_builder_tCat tArea tSet tSet_tpNum_d
        do
            tpNum=$(echo $tSet_tpNum_d | sed "s|^$tSet||;s|\.d$||")
            echo $tset_builder_tCat-$tArea-$tSet-$tpNum
        done >tpids.txt

    majorHeading "Filtering tests."

    # Filter the TPIDs by testFilter.

    if [ -f "$scriptDir/$testFilter" ]
    then
        cat tpids.txt | grep -E -f "$scriptDir/$testFilter" >tpidsFiltered.txt
        mv tpidsFiltered.txt tpids.txt
    else
        cat tpids.txt | grep -E -e "$testFilter" >tpidsFiltered.txt
        mv tpidsFiltered.txt tpids.txt
    fi

    # Filter out the unreliable tests, if requested

    if [ $excludeUnreliable == "true" ]
    then
        majorHeading "Excluding Unreliable Tests"
        cat $scriptDir/$fileUnreliableTests | sed 's|,|-|g' > tmp.txt
        cat tpids.txt | 
            while read lineCTP
            do
               grep $lineCTP tmp.txt > NULL
               if [ "$?" -eq "0" ]
               then
                  echo Excluding: $lineCTP
               else
                  echo $lineCTP >> final.txt
               fi
            done
         echo
         rm tmp.txt
         mv final.txt tpids.txt
    else
        majorHeading "NOT excluding Unreliable Tests"
    fi

    cat tpids.txt | sed 's|-| |g' |
        while read tset_builder_tCat tArea tSet tpNum
        do
            echo $tset_builder_tCat/$tArea/$tSet/$tSet$tpNum.d
        done >hnTestDirs.txt

    rm tpids.txt

    majorHeading "Starting tests."

    toRun=0
    echo $toRun >toRun.txt

    maxLen=$(cat hnTestDirs.txt | awk -f $scriptDir/regress.maxLen.awk)

    cat hnTestDirs.txt |
        while read test
        do
            (( pad = maxLen - ${#test} ))
            atec adp $test |
            (
                read status rest

                echo -n $test
                while [ $pad -gt 0 ]
                do
                    echo -n ' '
                    (( -- pad ))
                done
                echo -n ' '
                echo $status $rest

                if [ "$status" = "+OK" ]
                then
                    toRun=$(cat toRun.txt)
                    (( ++ toRun ))
                    echo $toRun >toRun.txt
                fi
            )
        done

    rm hnTestDirs.txt

    echo

    waitForTests
    failureToLaunch
    waitForTests

    echo

    stopRI

#
#    Commenting out unreliable tests
#
#    specials[0]=tset.builder.upnp.cl-cds-TCOMetadata-60,4294967290
#    specials[1]=tset.builder.upnp.cl-cds-TCOMetadata-70,4294967290
    specials[2]=tset.builder.upnp.cl-cds-TCOMetadata-80,4294967292
#    specials[3]=tset.builder.upnp.cl-cds-TCOMetadata-120,4294967289

    for special in ${specials[*]}
    do
        echo $special
    done | sed 's|,| |g' |
        while read tpid suid
        do
            if [ -f "$scriptDir/$testFilter" ]
            then
                local pass=$(echo $tpid | grep -q -E -f "$scriptDir/$testFilter"; echo $?)
            else
                local pass=$(echo $tpid | grep -q -E -e "$testFilter"; echo $?)
            fi

            if [ $pass -ne 0 ]
            then
                continue
            fi

            t=(${tpid//-/ })
            u=(${t[0]//./ })
            testDir=${t[0]}/${t[1]}/${t[2]}/${t[2]}${t[3]}.d
            dotM=dotm/${u[0]}.${u[2]}.${u[3]}/${t[1]}/${t[2]}.m
            tpNum=${t[3]}

            rm -rf $testDir
            tsbuild $dotM $tpNum >/dev/null 2>&1

            cp $OCAPROOT/bin/$OCAPTC/env/final.properties /tmp/final.properties
            echo "OCAP.hn.SystemUpdateID=$suid" >> $OCAPROOT/bin/$OCAPTC/env/final.properties

            startRI

            atec adp $testDir |
            (
                read status rest

                echo $testDir $status $rest

                if [ "$status" = "+OK" ]
                then
                    toRun=$(cat toRun.txt)
                    (( ++ toRun ))
                    echo $toRun >toRun.txt
                fi
            )

            waitForTests

            stopRI

            mv /tmp/final.properties $OCAPROOT/bin/$OCAPTC/env/final.properties
            rm -rf $testDir
        done

    toRun=$(cat toRun.txt)
    rm toRun.txt

    if [ $toRun -eq 0 ]
    then
        echo No tests were run.
    else
        cd $trDir

        make_summary -$toRun TPID-RESULT-SUBRESULTS > hnTestResults.txt

        maxLen=$(cat hnTestResults.txt | awk -f $scriptDir/regress.maxLen.awk)

        cat hnTestResults.txt | awk -v maxLen=$maxLen -f $scriptDir/regress.print.awk

        rm hnTestResults.txt
    fi

    stop_daemon
}

##############################################################################
#
# function setEnv
#
##############################################################################

function setEnv
{
    local riRoot="$1"

    export TWB_TOOLROOT="$riRoot"/OCAPRI_root/ri/RI_Platform

    export RICOMMONROOT="$riRoot"/OCAPRI_root/common
    export PLATFORMROOT="$riRoot"/OCAPRI_root/ri/RI_Platform
    export OCAPROOT="$riRoot"/OCAPRI_root/ri/RI_Stack

    if [ "$(uname)" = "Linux" ]
    then
        export PLATFORMHOST=Linux
        export OCAPHOST=Linux

        export PLATFORMTC=Linux/debug
        export OCAPTC=CableLabs/simulator/Linux/debug
    else
        export PLATFORMHOST=Win32-Cygwin
        export OCAPHOST=Win32-Cygwin

        export PLATFORMTC=Win32/debug
        export OCAPTC=CableLabs/simulator/Win32/debug
    fi
}

##############################################################################
#
# function startRI
#
##############################################################################

function startRI
{
    cd "$PLATFORMROOT"

    ./runRI.sh -deletestorage -capall -setup -ate >/dev/null &
    riPid=$!
    riPidIsValid=true

    cd - >/dev/null
}

##############################################################################
#
# function stop_daemon
#
##############################################################################

function stop_daemon
{
    # TODO: this was copied from another script;
    #       is it necessary and sufficient?

    if [ "$(uname)" = "Linux" ]
    then
       killall -9 /usr/bin/perl    >/dev/null 2>&1
    else
       taskkill /F /IM perl.exe    >/dev/null 2>&1
    fi
}

##############################################################################
#
# function stopRI
#
##############################################################################

function stopRI
{
    if [ "$riPidIsValid" = "true" ]
    then
        exec 10>&2 2>/dev/null
        kill -9 $riPid
        wait $riPid
        exec 2>&10
        riPidIsValid=false
    fi

    # TODO: this was copied from another script;
    #       is it necessary and sufficient?

    if [ "$(uname)" = "Linux" ]
    then
       killall -9 ri               >/dev/null 2>&1
       killall -9 vlc.exe          >/dev/null 2>&1
       killall -9 ate_if           >/dev/null 2>&1
    else
       taskkill /F /IM ri.exe      >/dev/null 2>&1
       taskkill /F /IM vlc.exe     >/dev/null 2>&1
       taskkill /F /IM ate_if.exe  >/dev/null 2>&1
    fi
}

##############################################################################
#
# function updateDotM
#
##############################################################################

function updateDotM
{
    local scriptDir="$1"
    local minMaturity=$2
    local testFilter="$3"

    cd $tsDir

    majorHeading "Finding all unattended HN tests with maturity >= $minMaturity."

    # Find all .m files that potentially use any type in the org.ocap.hn package
    # or one of its subpackages.

    #find dotm -name \*.m -exec grep -l -w org.ocap.hn {} \; |
    echo "dotm/tset.ocap/ch00_sigtests/OcapHn.m" > files.txt
    find dotm/tset.ocap.hn -name \*.m >> files.txt
    find dotm/tset.ocap.hnp -name \*.m >> files.txt
    find dotm/tset.upnp.cl -name \*.m >> files.txt
    find dotm/tset.dlna.cl -name \*.m >> files.txt

    cat files.txt |

    # Generate TPIDs for all unattended TPs they contain that are of
    # maturity $minMaturity or better.

        while read fileName
        do
            awk -v minMaturity=$minMaturity -v fileName=$fileName -f $scriptDir/dotMToTPID.awk $fileName
        done |
        sort >tpids.txt

    majorHeading "Filtering tests."

    # Filter the TPIDs by testFilter.

    if [ -f "$scriptDir/$testFilter" ]
    then
        cat tpids.txt | grep -E -f "$scriptDir/$testFilter" >tpidsFiltered.txt
        mv tpidsFiltered.txt tpids.txt
    else
        cat tpids.txt | grep -E -e "$testFilter" >tpidsFiltered.txt
        mv tpidsFiltered.txt tpids.txt
    fi

    numberOfStreams=$(wc -l tpids.txt | awk '{print $1;}')

    if [ $numberOfStreams -eq 0 ]
    then
        majorHeading "No tests left after filtering; abandoning stream updating."
        return
    fi

    # Generate 'build' commands for all TPs that are left after filtering.

    cat tpids.txt | sed 's|-| |g' |
        while read tset_builder_tCat tArea tSet tpNum
        do
            tCat=$(echo $tset_builder_tCat | sed 's|^tset\.builder\.||')
            (( ++ lineNo ))
            echo "echo Building $lineNo of $numberOfStreams \(dotm/tset.$tCat/$tArea/$tSet.m $tpNum\)"
            echo "tsbuild dotm/tset.$tCat/$tArea/$tSet.m $tpNum >junk 2>&1"
            echo "tsbuildretcode=\$?"
            echo "grep \"Make Error\" junk >/dev/null 2>&1"
            echo "if [ \$? -eq 0 ]"
            echo "then"
            echo "    echo tset.$tCat/$tArea/$tSet.m $tpNum compile problem >> tsbuild.err"
            echo "fi"
            echo "rm junk"
            echo "if [ \$tsbuildretcode -ne 0 ]"
            echo "then"
            echo "  echo tsbuild failed ,, retrying"
            echo "  tsbuild dotm/tset.$tCat/$tArea/$tSet.m $tpNum >/dev/null 2>&1"
            echo "fi"
            echo
        done >hnBuildScript.sh

    rm tpids.txt

    stopRI

    majorHeading "Deleting old test streams."
    rm -rf tset.builder.*

    if [ $numberOfStreams -eq 1 ]
    then
        local streams=stream
    else
        local streams=streams
    fi
    majorHeading "Beginning to build $numberOfStreams test $streams."

    . hnBuildScript.sh

    rm hnBuildScript.sh
}

##############################################################################
#
# function updateEnv
#
##############################################################################

function updateEnv
{
    local riRoot="$1"
    local ocOcapTestRoot=$2

    setEnv "$riRoot"

    # Sync up to the latest version of the environment.
    # This retrieves the latest versions of mgen, util classes, dot-m files
    # and builder files from the repository. This also updates the tsbuild
    # environment files.

    tsbuild-update-svn -b $ocOcapTestRoot -e

    # Update the runtime environment (recompile all of the util classes and
    # regenerate util class dependencies).

    tsbuild-update-env
}

##############################################################################
#
# function updateRI
#
##############################################################################

function updateRI
{
    local riRoot="$1"
    local simulatorMachine=$2
    local simulatorMachineUser=$3
    local OCAPRI_root=$4
    local hnInterface=$5

    setEnv "$riRoot"

    stopRI

    majorHeading "Deleting old RI."

    rm -rf "$riRoot"
    mkdir -p "$riRoot"

    majorHeading "Exporting new RI."

    cd "$riRoot"

    local riRevision=$(svn info $riRepositoryURL | grep "^Revision: " | cut -c11-)

    svn export -r $riRevision $riRepositoryURL/$OCAPRI_root OCAPRI_root
    echo $riRevision >OCAPRI_root/revision.txt

    majorHeading "Copying new RI to simulator machine."

    cd "$riRoot"

    tar czf ri.tar.gz OCAPRI_root
    scp -q -B ri.tar.gz $simulatorMachineUser@$simulatorMachine:ri.tar.gz
    rm ri.tar.gz

    majorHeading "Starting RI build on simulator machine."

    cd "$riRoot"

    ssh -n $simulatorMachineUser@$simulatorMachine updateUPnPSimulatorMachine.sh >updateUPnPSimulatorMachine.out 2>&1 &

    majorHeading "Building platform."

    cd "$PLATFORMROOT"
    make clean purge build

    if [ "$PLATFORMHOST" = "Linux" ]
    then
       echo "Editing platform configuration." 
       rm -f /tmp/platform.cfg
       cp $PLATFORMROOT/platform.cfg /tmp/platform.cfg
       cat /tmp/platform.cfg | sed -e 's/RI.Headend.tunerType = VLC/RI.Headend.tunerType = GST/' > $PLATFORMROOT/platform.cfg
       rm -f /tmp/platform.cfg
    else
       echo "No need to edit platform configuration."
    fi

    majorHeading "Building stack."

    cd "$OCAPROOT"
    ant

    # Set RI network interface.
    echo OCAP.hn.multicast.iface=$hnInterface >>"$OCAPROOT/bin/$OCAPTC/env/final.properties"
    # Disable DTCP.
    if [[ "$OCAPTC" == *Win32* ]]
    then
        tempStr=/$(echo ${OCAPROOT//[:]/})
        finalDTCPDllVal=$tempStr/tools/mock_dll/dtcpip_mock.dll
        finalDTCPStorageVal=$tempStr/tools/mock_dll
    else
        tempStr=$(echo $OCAPROOT | sed 's/[\:]//g')
        finalDTCPDllVal=$tempStr/tools/mock_dll/dtcpip_mock.so
        finalDTCPStorageVal=$tempStr/tools/mock_dll
    fi
    
    echo "DTCP DLL location Value is $finalDTCPDllVal"
    echo "DTCP DLL Storage Value is $finalDTCPStorageVal"

    echo MPEOS.HN.DTCPIP.DLL=$finalDTCPDllVal >>"$OCAPROOT/bin/$OCAPTC/env/mpeenv.ini"
    echo MPEOS.HN.DTCPIP.STORAGE=$finalDTCPStorageVal >>"$OCAPROOT/bin/$OCAPTC/env/mpeenv.ini"

    # Wait up to five minutes for simulator build to finish.

    majorHeading "Waiting for simulator RI build to finish."

    nowInSeconds=$(date +%s)

    (( thenInSeconds = nowInSeconds + 600 ))

    cd "$riRoot"

    while true
    do
        sleep 10

        if grep -q "Building zip: .*/ocap-javadoc.zip" updateUPnPSimulatorMachine.out
        then
            echo "UPnP simulator machine update apparently finished."
            break
        fi

        if [ $(date +%s) -ge $thenInSeconds ]
        then
            echo "UPnP simulator machine update apparently did not finish."
            break
        fi
    done
}

##############################################################################
#
# function waitForTests
#
##############################################################################

function waitForTests
{
    while true
    do
        remaining=$(atec stat | grep '^a' | wc -l)

        if [ $remaining -eq 0 ]
        then
            break
        fi

        echo "  $(date '+%Y/%m/%d %H:%M:%S') Tests remaining: $remaining"

        sleep 30s
    done
}

##############################################################################
#
# function failureToLaunch - Detect tests that did not launch and requeue
#
##############################################################################

function failureToLaunch
{
    cd $tsDir
    make_summary JOBID-RESULT | grep AteError | cut -f 1 > tmp.txt
    make_summary TPID-RESULT | grep AteError | cut -f 1 | sed 's|-| |g' | 
        while read tset_builder_tCat tArea tSet tpNum
        do
            tset_builder="`echo $tset_builder_tCat | sed s/.builder//`"
            tsbuild dotm/$tset_builder/$tArea/$tSet.m $tpNum
            atec adp $tset_builder_tCat/$tArea/$tSet/$tSet$tpNum.d
        done

    for i in `cat tmp.txt`
    do
        rm $trDir/$i
    done
    rm tmp.txt
    cd -
}

main "$@"
