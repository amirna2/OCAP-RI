#!/bin/bash

#     args are all optional
#
#     timeout: time in seconds to wait for the RiScriptlet xlet to complete 
#              the script (this includes RI startup time). Default is 1200 seconds
#     scriptName: the script to be executed. Default: all scripts are executed
#     vpopServerName: Name of the server for VPOP content. No default
#     vpopIpAddress: IP address of the server for VPOP content. No default.
#     numReps: If a scriptFile is specified, then this is the number of times
#              to repeat running that file.  If numReps is not specified, then the
#              default is 1 ( run each test just once )
#
#     reRun: if reRun=1, rerun any tests that fail. Default value=0 i.e. do NOT rerun failed tests.

#     Parameters can be entered in ANY order. The correct way of running this script is: 
#        ./runRx.sh -timeout=<timeout_value> -script=<scriptName>
#	 -vpopServerName=<vpopServerName_value>
#	 -vpopIpAddress=<vpopIpAddress_value>
#	 -numReps=<numReps> -reRun=<rerunValue>

#
#     Are the environment variables set up?
#

# now run the scripts
function runScripts
{
declare -i index=0
# copy incoming array into a new local array
declare -a masterScriptArray=("${!1}")
declare -a slaveScriptArray=("${!2}")

for ii in ${masterScriptArray[@]}; do
   echo
   echo running script $index: ${masterScriptArray[index]}
   if [[ ${masterScriptArray[index]} == *LiveStreamingSlowSpeedTrick.bsh* ]]
   then
       echo "Increasing timeout to 40 mins"
       TIMEOUTSECS=2400 # 40 mins
   else
       echo "timeout is $TIMEOUTSECS"
   fi 
   startSeconds=$SECONDS
   #
   # Change hostapp.properties so that the desired script is used.
   #
   cd $OCAPROOT/bin/$OCAPTC/env
   cp $OCAPROOT/apps/qa/org/cablelabs/xlet/RiScriptlet/ocap.RiScriptlet.perm .
   cp $OCAPROOT/apps/qa/org/cablelabs/xlet/RiScriptlet/hostapp.properties .
   echo " Writing to hostapp.properties: ${masterScriptArray[index]} "
   # Keep on OR'ing here for new properties, do not create a new CAT line/rule 
   (cat hostapp.properties | sed -e "s@app.0.args.0=script_0=scripts/level3/.*@app.0.args.0=script_0=scripts/level3/${masterScriptArray[$index]}@" | sed -e "s/temporary/${VPOPSERVERNAME}/") >xxx.properties
   cp xxx.properties hostapp.properties

   #
   # delete RxScriptlet completion flag file, if present
   #
   rm -f $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE
   if [ -e $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE ] 
   then
      echo "Error deleting SCRIPT_COMPLETE file"
      exit 1
   fi

   # Start the RI
   # 
   cd $PLATFORMROOT
   if [ "$WITHGDB" -eq "1" ]
   then
       ./runRI.sh -capall -deletelog -deletestorage -gdb &
   else
       ./runRI.sh -capall -deletelog -deletestorage &> /dev/null &
   fi
 
   if [ "${slaveScriptArray[index]}" != "" ]
   then
       # need to source bash_profile on the remote machine as the command is sent over SSH. 
       echo "Calling slave script on the remote machine..."
       ssh admin@$VPOPIPADDR "source ~/setEnvForVPOP; "' sh $OCAPROOT'/apps/qa/org/cablelabs/lib/rxscripts/level3/"${slaveScriptArray[index]} $VPOPSERVERNAME < /dev/null > /tmp/myLogFile 2>&1 &"
   fi

   echo waiting for RI to complete script ${masterScriptArray[index]}

   TIMED_OUT_MASTER_SCRIPT=1
   for ((a=1; a <= TIMEOUTSECS ; a++))
   do
      if [ -e $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE ]
      then
         TIMED_OUT_MASTER_SCRIPT=0
         break
      else
         sleep 1
      fi

   done 

   if [ "$TIMED_OUT_MASTER_SCRIPT" -eq "1" ]
   then
      echo TIMED OUT waiting for RI to complete script ${masterScriptArray[index]}
      echo -e "TimedOut\t${masterScriptArray[index]}" > $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE
   else
      echo script ${masterScriptArray[index]} complete
   fi

   cat $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE

   # add result to  results file
   if [ $RERUN -eq 1 ] 
   then
         echo -n "Re-run results:" >> $PLATFORMROOT/runRxResults.txt
   fi
   cat $OCAPROOT/bin/$OCAPTC/env/persistent/usr/1/6035/SCRIPT_COMPLETE >> $PLATFORMROOT/runRxResults.txt

   # Save the ri log file
   if [ $RERUN -eq 1 ]
   then
      cp $PLATFORMROOT/ri-iter-0.log $PLATFORMROOT/rx-${masterScriptArray[index]}-$RERUN.log
   else
      # If variable name has a / in it, we cannot create a log file for it.
      # Unless we change / to a -
      if [[ "${masterScriptArray[index]}" =~ .*/.* ]]
      then
         tempScriptName=${masterScriptArray[index]/\//-}
	 echo "Master script array index after changing is $tempScriptName"
	 cp $PLATFORMROOT/ri-iter-0.log $PLATFORMROOT/rx-$tempScriptName.log
      else
	 cp $PLATFORMROOT/ri-iter-0.log $PLATFORMROOT/rx-${masterScriptArray[index]}.log
      fi
   fi
   kill_ri
   kill_vlc
   if [ "${slaveScriptArray[index]}" != "" ]
   then
       echo "Killing slave process"
       ssh admin@$VPOPIPADDR "/usr/bin/killall ri"
   fi
   finishSeconds=$SECONDS
   elapsed=`expr $finishSeconds - $startSeconds`
   echo ...   $elapsed seconds to execute scriptlet $index

   index=$index+1
done
}

if [ -f ~/setEnv ]; then
   echo "Resetting environment variables"
   source ~/setEnv
   echo reset OCAPROOT $OCAPROOT
   echo reset PLATFORMROOT  $PLATFORMROOT
   echo reset RICOMMONROOT $RICOMMONROOT
   echo    ... OCAP environment variables set
   echo
fi

source "$PLATFORMROOT/runRIUtilities.sh"

if [ -z "$OCAPROOT" ] 
then
   echo "Need to set OCAP environment variables"
   exit 1
fi
echo "OCAPROOT " $OCAPROOT

function usage
{
   echo "The correct way of running this script is: 
         ./runRx.sh -timeout=<timeout_value> -script=<scriptName>
	 -vpopServerName=<vpopServerName_value>
	 -vpopIpAddress=<vpopIpAddress_value>
	 -numReps=<numReps> -reRun=<1=rerun 0=don't rerun>"
}

# default values
RERUN=0 # do not re-run failed scripts
SCRIPTNAME=ALL
NUMREPS=1 # just run the scripts once
TIMEOUTSECS=1200 # 20 mins
VPOPSERVERNAME=" "
VPOPIPADDR=" "
WITHGDB=0 #default is don't run with GDB.

#Check to see if at least one argument was specified
if [ $# -lt 1 ] ; then
   echo "No argument was specified, using defaults:"
   echo "Running script/scripts ONCE, Script: $SCRIPTNAME, Script Timeout duration: $YTIMEOUTSECS, Not re-running failed scripts"
fi

for input in $*
do
   case $input in
   
   -h | -help | -\?)
      usage
      exit 0 
      ;;
  
   -timeout=*)
      TIMEOUTSECS=${input#*=}
      echo "TIMEOUT SECS IS $TIMEOUTSECS"
      ;;

   -script=*)
      SCRIPTNAME=${input#*=}        # Delete everything up till "="
      echo "SCRIPTNAME IS $SCRIPTNAME"
      ;;

   -vpopServerName=*)
      VPOPSERVERNAME=${input#*=}        # Delete everything up till "="
      echo "VPOPSERVERNAME IS $VPOPSERVERNAME"
      ;;

   -vpopIpAddress=*)
      VPOPIPADDR=${input#*=}        # Delete everything up till "="
      echo "VPOPIPADDR IS $VPOPIPADDR"
      ;;

   -numReps=*)
      NUMREPS=${input#*=}        # Delete everything up till "="
      echo "NUMREPS IS $NUMREPS"
      ;;

   -reRun=*)
      RERUN=${input#*=}        # Delete everything up till "="
      echo "RERUN IS $RERUN"
      ;;

   -gdb=*)
      WITHGDB=${input#*=}        # Delete everything up till "="
      echo "WITHGDB IS $WITHGDB"
      ;;

   -*)
      echo "WARN: Unknown option (ignored): $input" >&2
      ;;
 
   *)  # no more options. Stop while loop
     ;;
 esac
done

# Making sure the while running VPOP tests
# the right command line parameters have been provided.
if [[ "$SCRIPTNAME" == *VPOP* ]]
then
   if [ "$VPOPSERVERNAME" == " " ]
   then
      echo "VPOP Server Name needs to be passed in as a command line parameter. Try ./runRx.sh -h for details"
      exit 0
   else
      if [ "$VPOPIPADDR" == " " ]
      then
         echo "VPOP IP Address needs to be passed in as a command line parameter. Try ./runRx.sh -h for details"
         exit 0
      fi
   fi
fi

#
# All scripts should be copied, including util subdirs, but without .svn dirs
#
if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts
fi

#
# copy level 1
#
if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1 ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1
fi
cp -r $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level1/*.bsh \
   $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1

if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1/utils ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1/utils
fi
for i in $( ls $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level1/utils); do
   cp $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level1/utils/$i $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level1/utils
done

#
# copy level 2
#
#
if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2 ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2
fi

for i in $( ls $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2); do
    # Check whether its a directory
    if [ ${i##*.} != "bsh" ]; then

        if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i ]; then
            mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i
        fi # checking for dirs at first level
	for j in $( ls $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2/$i); do
            # Check whether its a directory
            if [ ${j##*.} != "bsh" ]; then
                if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i/$j ]; then
                    mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i/$j
                fi # checking for dirs at second level
                # checking whether the folder is empty and if not, then only copy the files.
                if [ `ls -1 $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2/$i/$j | wc -l` -ne 0 ]; then
                    cp -r $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2/$i/$j/* $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i/$j
                fi
            else
                cp -r $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2/$i/*.bsh $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/$i/
            fi
        done #j for loop
    else
        cp $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level2/*.bsh $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level2/
    fi # checking for .bsh at first level
done #end of for $i in level2

#
# copy level 3
#
if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3 ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3
fi
cp -r $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/*.bsh \
   $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3

if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/utils ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/utils
fi
for i in $( ls $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/utils); do
    cp $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/utils/$i $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/utils
done

if [ ! -d $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/VPOP ]; then
   mkdir  $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/VPOP
fi
for i in $( ls $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/VPOP); do
    cp $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/VPOP/$i $OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts/level3/VPOP
done

# copy config.properties file
cp $RICOMMONROOT/resources/tunedata/config.properties $OCAPROOT/bin/$OCAPTC/env/qa/xlet

# remove old results file
rm -f $PLATFORMROOT/runRxResults.txt
if [ -e $PLATFORMROOT/runRxResults.txt ] 
then
   echo "Error deleting runRxResults.txt file"
   exit 1
fi

# create new empty results file
touch $PLATFORMROOT/runRxResults.txt

#
cd $OCAPROOT/bin/$OCAPTC/env
#
# change the final.properties file so xait.ignore is set to true
#
if [ -e final.properties ]
then
    if grep -q "^OCAP.xait.ignore" final.properties 
    then
        sed -i '/^OCAP.xait.ignore */s/false/true/' final.properties
    else
        echo "OCAP.xait.ignore=true" >> final.properties
    fi

    if grep -q "^OCAP.hn.server.chunkEncodingMode" final.properties
    then
        sed -i '/^OCAP.hn.server.chunkEncodingMode */s/as-appropriate/always/' final.properties
    else
       echo "OCAP.hn.server.chunkEncodingMode=always" >> final.properties
    fi

    if grep -q "^OCAP.hn.server.connectionStallingTimeoutMS" final.properties
    then
        sed -i '/OCAP.hn.server.connectionStallingTimeoutMS/d' final.properties
        echo "OCAP.hn.server.connectionStallingTimeoutMS=30000" >> final.properties
    else
       echo "OCAP.hn.server.connectionStallingTimeoutMS=30000" >> final.properties
    fi
    
    if grep -q "^OCAP.hn.server.vpop.enabled" final.properties
    then
        sed -i '/^OCAP.hn.server.vpop.enabled */s/false/true/' final.properties
    else
        echo "OCAP.hn.server.vpop.enabled=true" >> final.properties
    fi
         
else
    echo "OCAP.xait.ignore=true" > final.properties
    echo "OCAP.hn.server.disableDtcpIp=true" >> final.properties
    echo "OCAP.hn.server.chunkEncodingMode=always" >> final.properties
    echo "OCAP.hn.server.connectionStallingTimeoutMS=30000" >> final.properties
    echo "OCAP.hn.server.vpop.enabled=true" >> final.properties
fi

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

if [ -e mpeenv.ini ]
then
   if grep -q "^MPEOS.HN.DTCPIP.DLL" mpeenv.ini
      then
         sed --in-place '/MPEOS.HN.DTCPIP.DLL/d' mpeenv.ini
   fi
   if grep -q "^MPEOS.HN.DTCPIP.STORAGE" mpeenv.ini
      then
         sed --in-place '/MPEOS.HN.DTCPIP.STORAGE/d' mpeenv.ini
   fi
   echo "MPEOS.HN.DTCPIP.DLL=$finalDTCPDllVal" >> mpeenv.ini
   echo "MPEOS.HN.DTCPIP.STORAGE=$finalDTCPStorageVal" >> mpeenv.ini
else
   echo "MPEOS.HN.DTCPIP.DLL=$finalDTCPDllVal" > mpeenv.ini
   echo "MPEOS.HN.DTCPIP.STORAGE=$finalDTCPStorageVal" >> mpeenv.ini
fi

# 
# Build an array of scripts to exclude based on exclude list file
#
declare -a excludeScriptNames
cd $PLATFORMROOT
declare -i index=0
var=`cat ../hnTestScripts/exclude_list.txt`
for i in $var; do
    excludeScriptNames[index]=$i
    index=$index+1
done

#
# Build an array of scripts to execute
# 
declare -a masterScriptNames
declare -a slaveScriptNames
# no script was specified
if [ "$SCRIPTNAME" == "ALL" ] 
then
   declare -i found=0
   declare -i index=0
   # lists all the scripts in level 3 which have no slave scripts
   # as they are not in subfolders under level3
   for i in $( ls -F $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3 | grep -v \/ | sed -e "s/\*//"); do
       if [[ $i == *.bsh ]]
       then
	   # look for script in excluded array
           found=0
           for ii in ${excludeScriptNames[@]}; do

               if echo "$ii" | grep -q "$i";
               then
                  found=1
                  break
               fi
               ii=$ii+1
           done
	   
	   # include script if it wasn't excluded
           if [ "$found" -gt "0" ]
           then
               echo Excluding script $i 
           else
               masterScriptNames[index]=$i
               slaveScriptNames[index]=
               echo masterScript $index: ${masterScriptNames[index]}
               echo slaveScript $index: ${slaveScriptNames[index]}
               index=$index+1
           fi
       fi
   done

   cd $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3 

   #lists the sub-folders under level 3 folder
   for i in $( ls -F $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3 | grep \/ | sed -e "s/\///"); do
     echo Descending into $i
     cd $i
     for ii in $( ls -F | grep -v \/ | sed -e "s/\*//"); do
       found=0
       for iii in ${excludeScriptNames[@]}; do
            if echo "$iii" | grep -q "$ii";
            then
                found=1
                break
            fi
            iii=$iii+1
       done
       	# include script if it wasn't excluded
       if [ "$found" -gt "0" ]
       then
            echo Excluding script $ii 
       else
            if [[ $ii == *.master.bsh ]]
            then
                masterScriptNames[index]=$i\/$ii
                slaveScriptNames[index]=$( echo ${masterScriptNames[index]} | sed -e "s/.master.bsh/.slave.sh/" )
                echo masterScript $index: ${masterScriptNames[index]}
                echo slaveScript $index: ${slaveScriptNames[index]}
                index=$index+1
            else
                echo skipping $index: $ii
            fi
        fi

     done
     cd ..
   done
else
   # was a numReps specified?
   if [ "$NUMREPS" -eq "1" ]
   then
      echo Executing script once only
      masterScriptNames[0]=$SCRIPTNAME
      slaveScriptNames[0]=
   else
      echo Executing script $NUMREPS times
      for ((i=0; i < $NUMREPS ; i++))
      do
         masterScriptNames[i]=$SCRIPTNAME
         slaveScriptNames[i]=
      done
   fi

   if [ ! -s $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/$SCRIPTNAME ];then
      echo $OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts/level3/$SCRIPTNAME not found
      exit 1
   fi
fi
# run the first run for scripts
runScripts masterScriptNames[@] slaveScriptNames[@] 0

declare -a failedScriptNamesWithDir
declare -a failedScriptNames
declare -i idx=0
failedScriptNamesWithDir=`(grep -i scripts runRxResults.txt | grep FAIL | cut -f 2) && (grep -i scripts runRxResults.txt | grep TimedOut | cut -f 2)`
for ii in ${failedScriptNamesWithDir[@]}; do
    failedScriptNames[$idx]=`basename $ii`
    echo "Failed scriptname is $failedScriptNames[$idx]"
    idx=$idx+1
done


# if requested run the second run for scripts while failed/timedOut earlier
if [ $RERUN -eq 0 ]
then
      echo Not rerunning failed scripts
else
      kill_ri
      kill_vlc
      if [ $idx -gt 0 ]
      then
          echo Rerunning failed scripts
          runScripts failedScriptNames[@] 1
      fi
fi

# Create the zip file
cd $PLATFORMROOT
rxLogList=(`ls -F | grep 'rx-*'`)
for i in ${rxLogList[@]}; do
   zip -g  rxLogFiles.zip $i
done
