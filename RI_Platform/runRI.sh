#!/bin/bash

source "$PLATFORMROOT/runRIUtilities.sh"

errorDisplayTime=4

envDir=$OCAPROOT/bin/$OCAPTC/env
tunedataDir=$RICOMMONROOT/resources/tunedata
dvrXdataDir=$OCAPROOT/apps/qa/org/cablelabs/xlet/RiExerciser
dvrXCmdDir=$dvrXdataDir/command_files

XletScriptDstDir=$OCAPROOT/bin/$OCAPTC/env/qa/xlet/scripts
XletScriptSrcDir=$OCAPROOT/apps/qa/org/cablelabs/lib/rxscripts

finalPropertiesFile=$envDir/final.properties
mpeenvIniFile=$envDir/mpeenv.ini
hostAppPropsSrcFile=$tunedataDir/hostapp.properties
configPropsSrcFile=$tunedataDir/config.properties
hostAppPropsFile=$envDir/hostapp.properties
configPropsFile=$envDir/qa/xlet/config.properties
dvrXHostAppPropsSrcFile=$dvrXdataDir/hostapp.properties

testSuitesBasicallyWorking="DeviceExtSuite \
DiagnosticsSuite \
EASTestSuite \
FPSuite \
HaviSuite \
ImplSuite \
JavaTVSuite \
JavaxSuite \
ManagerSuite \
MediaSuite \
MiscSuite \
SNMPSuite \
DVRSuite \
OrgSuite"

if [ -z $RILOGPATH ]; then
  export RILOGPATH=.
fi

testSuitesThatHangOrCrash="DiagnosticsSuite ImplSuite MediaSuite OrgSuite"
testSuites="DeviceExtSuite EASTestSuite FPSuite HaviSuite JavaTVSuite JavaxSuite ManagerSuite MiscSuite SNMPSuite DVRSuite"

if [ -e $finalPropertiesFile ]
then
    removeFinalPropertiesFileOnRestore=false
    if grep -q "^OCAP.xait.ignore" $finalPropertiesFile
    then
        removeOcapXaitIgnoreOnRestore=false
    else
        removeOcapXaitIgnoreOnRestore=true
        echo "OCAP.xait.ignore=false" >>$finalPropertiesFile
    fi
else
    removeFinalPropertiesFileOnRestore=true
    echo "OCAP.xait.ignore=false" >$finalPropertiesFile
fi

with_valgrind=0
with_full=0
with_gdb=0
with_ate=0
with_coverage=0
with_junit=0
with_tunetest=0
with_xlet=0
with_dvrtestrunner=0
with_capall=0
with_restoreXAIT=0
with_delete_log=0
with_delete_storage=0
with_setup=0
with_stamp_log=0
with_autodvr=0
restore_ini=0
running_linux=0
with_restart=0
with_tgshell=0
with_mockDLL=0

Xlet=""

iter=0

checkXait()
{
    if [ -z "$1" ]
    then
        echo "ERROR - Parameter #1 is zero length!?"  # Or no parameter passed.
    else
        grep OCAP.xait.ignore $finalPropertiesFile | grep $1 > $0.tmp
        if [ -s $0.tmp ]
        then
            echo "XAIT ignore state is $1"
            rm -f $0.tmp
        else
            echo "======================================================="
            echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
            echo
            echo "ERROR: XAIT ignore state is not set to $1!"
            echo "set OCAP.xait.ignore=$1 in $finalPropertiesFile"
            echo
            echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
            echo "======================================================="
            echo
            echo
            sleep $errorDisplayTime
        fi
    fi
}

checkFileExists()
{
    if [ -z "$1" ]
    then
        echo "ERROR - Parameter #1 is zero length!?"  # Or no parameter passed.
    elif [ -z "$2" ]
    then
        echo "ERROR - Parameter #2 is zero length!?"
    elif [ -s $1 ]
    then
        echo "using $1"
    else
        echo "======================================================="
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo
        echo "ERROR couldn't find:"
        echo "$1"
        echo
        echo "you should:"
        echo "cp $2 $1"
        echo
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo "======================================================="
        echo
        echo
        sleep $errorDisplayTime
    fi
}

checkFileDoesntExist()
{
    if [ -z "$1" ]
    then
        echo "ERROR - Parameter #1 is zero length!?"  # Or no parameter passed.
    elif [ -s $1 ]
    then
        echo "======================================================="
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo
        echo "ERROR:"
        echo "you probably want to mv or rm $1"
        echo
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo "======================================================="
        echo
        echo
        sleep $errorDisplayTime
     else
        echo "$1 is not in the way..."
    fi
}

kill_ate_if()
{
   echo "killing ATE IF..."
   if [ $running_linux -eq 1 ]
   then
      killall -9 ate_if
   else
      $PLATFORMROOT/install/$PLATFORMTC/bin/pk.exe ate_if.exe
   fi

}

kill_ri_ext()
{
   echo "killing RI..."

   if [ $with_coverage -eq 1 ]
   then
      dump_coverage
   fi
 
   kill_ri

   if [ $with_coverage -eq 1 -a $with_ate -eq 1 ]
   then
      sleep 60
   fi

}

restore_mpeenv()
{
   if [ $restore_ini -eq 1 ]
   then
      if [ $removeFinalPropertiesFileOnRestore = "true" ]
      then
          rm $removeFinalPropertiesFileOnRestore
      else
          if [ $removeOcapXaitIgnoreOnRestore = "true" ]
          then
              sed '$d' <$finalPropertiesFile >$finalPropertiesFile.tmp
              mv $finalPropertiesFile.tmp $finalPropertiesFile
          else
              $sed -i '/^OCAP.xait.ignore *=/s/true/false/' $finalPropertiesFile
          fi
      fi
      $sed -i "/^MainClassArgs.0 *=/s/MainClassArgs.0=.*/MainClassArgs.0=org.cablelabs.impl.ocap.OcapMain/" $mpeenvIniFile
      $sed -i "/^MainClassArgs.1 *=/s/MainClassArgs.1=.*/#MainClassArgs.1=%MainClassArgs.1%/" $mpeenvIniFile
   fi
}

enable_mockDLL()
{
    if [[ "$OCAPTC" == *Win32* ]]
    then
        path=/$(echo ${OCAPROOT//[:]/})
        finalDTCPDllVal=$path/tools/mock_dll/dtcpip_mock.dll
        finalDTCPStorageVal=$path/tools/mock_dll
    else
        path=$(echo $OCAPROOT | sed 's/[\:]//g')
        finalDTCPDllVal=$path/tools/mock_dll/dtcpip_mock.so
        finalDTCPStorageVal=$path/tools/mock_dll
    fi

    if [ -e $mpeenvIniFile ]
    then
       if grep -q "^MPEOS.HN.DTCPIP.DLL" $mpeenvIniFile
          then
             sed --in-place '/MPEOS.HN.DTCPIP.DLL/d' $mpeenvIniFile
       fi
       if grep -q "^MPEOS.HN.DTCPIP.STORAGE" $mpeenvIniFile
          then
             sed --in-place '/MPEOS.HN.DTCPIP.STORAGE/d' $mpeenvIniFile
       fi
       echo "MPEOS.HN.DTCPIP.DLL=$finalDTCPDllVal" >> $mpeenvIniFile
       echo "MPEOS.HN.DTCPIP.STORAGE=$finalDTCPStorageVal" >> $mpeenvIniFile
    else
       echo "MPEOS.HN.DTCPIP.DLL=$finalDTCPDllVal" > $mpeenvIniFile
       echo "MPEOS.HN.DTCPIP.STORAGE=$finalDTCPStorageVal" >> $mpeenvIniFile
    fi
}

signal_handler()
{
   echo "signal_handler: pid was $$";
   kill_ri_ext
   kill_vlc
   kill_ate_if
   save_log_if_requested
   restore_mpeenv
   exit 1
}

make_gdb_command_file()
{
   echo make_gdb_command_file
   if [ $running_linux -eq 1 ]
   then
      echo "file ./install/$PLATFORMTC/bin/ri" > ri_gdb.cmd
      echo "set args ./platform_linux.cfg" >> ri_gdb.cmd
   else
      echo "file ./install/$PLATFORMTC/bin/ri.exe" > ri_gdb.cmd
      echo "set args ./platform_win32.cfg" >> ri_gdb.cmd
   fi
   echo "handle SIGQUIT pass nostop noprint" >> ri_gdb.cmd
   echo "handle SIGUSR1 pass nostop noprint" >> ri_gdb.cmd
   echo "handle SIGUSR2 pass nostop noprint" >> ri_gdb.cmd
   echo "handle SIGTRAP nostop" >> ri_gdb.cmd
   echo "handle SIGINT stop" >> ri_gdb.cmd
   echo "run" >> ri_gdb.cmd
   echo "init-if-undefined \$_exitcode = 9999" >> ri_gdb.cmd
   echo "if \$_exitcode != 9999" >> ri_gdb.cmd
   echo "   quit" >> ri_gdb.cmd
   echo "else" >> ri_gdb.cmd
   echo "   where" >> ri_gdb.cmd
   if [ $with_ate -eq 1 ]
   then
      echo "   thread apply all where" >> ri_gdb.cmd
      echo "   quit" >> ri_gdb.cmd
   fi
   echo "end" >> ri_gdb.cmd
}

run_for_linux()
{
   echo "----> Starting ri iteration $iter <----"

   if [ $with_ate -eq 1 ]
   then
      if [ $with_valgrind -eq 1 ]
      then
         echo with_valgrind
         mkdir -p valgrind_logs
         if [ $with_full -eq 1 ]
         then
            echo with_full
            valgrind --log-file=valgrind_logs/valgrind_log_$iter.log -v --leak-check=full -v --track-origins=yes ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg &
         else
            valgrind --log-file=valgrind_logs/valgrind_log_$iter.log -v --track-origins=yes ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg &
         fi
      elif [ $with_gdb -eq 1 ]
      then
         rm -f ri_gdb.cmd
         make_gdb_command_file
         if [ $with_capall -eq 1 ]
         then
            echo run_for_linux with_capall with_gdb
            gdb -command ri_gdb.cmd  2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log &
         else
            echo run_for_linux with_gdb
            gdb -command ri_gdb.cmd  &
         fi
      else
         if [ $with_capall -eq 1 ]
         then
            echo run_for_linux with_capall
            ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log &
         else
            echo run_for_linux
            ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg &
         fi
      fi
   else
      if [ $with_valgrind -eq 1 ]
      then
         echo with_valgrind
         mkdir -p valgrind_logs
         if [ $with_full -eq 1 ]
         then
            echo with_full
            valgrind --log-file=valgrind_logs/valgrind_log_$iter.log -v --leak-check=full --track-origins=yes ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg
         else
            valgrind --log-file=valgrind_logs/valgrind_log_$iter.log -v --track-origins=yes ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg
         fi
      elif [ $with_gdb -eq 1 ]
      then
         rm -f ri_gdb.cmd
         make_gdb_command_file
         if [ $with_capall -eq 1 ]
         then
            echo run_for_linux with_capall with_gdb
            gdb -command ri_gdb.cmd  2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
         else
            echo run_for_linux with_gdb
            gdb -command ri_gdb.cmd
         fi
      else
         if [ $with_capall -eq 1 ]
         then
            echo run_for_linux with_capall
            ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
         else
            echo run_for_linux
            ./install/$PLATFORMTC/bin/ri ./platform_linux.cfg
         fi
      fi
   fi
}

run_for_win32()
{
   echo "----> Starting ri iteration $iter <----"

   if [ $with_ate -eq 1 ]
   then
      if [ $with_valgrind -eq 1 ]
      then
         echo "ERROR! there is no valgrind available for win32"
      elif [ $with_gdb -eq 1 ]
      then
         rm -f ri_gdb.cmd
         make_gdb_command_file
         if [ $with_capall -eq 1 ]
         then
            echo run_for_win32 with_capall with_gdb
            gdb -command ri_gdb.cmd  2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log &
         else
            echo run_for_win32 with_gdb
            gdb -command ri_gdb.cmd &
         fi
      else
         if [ $with_capall -eq 1 ]
         then
            echo run_for_win32 with_capall
            ./install/$PLATFORMTC/bin/ri.exe ./platform_win32.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log &
         else
            echo run_for_win32
            ./install/$PLATFORMTC/bin/ri.exe ./platform_win32.cfg &
         fi
      fi
   else
      if [ $with_valgrind -eq 1 ]
      then
         echo "ERROR! there is no valgrind available for win32"
      elif [ $with_gdb -eq 1 ]
      then
         rm -f ri_gdb.cmd
         make_gdb_command_file
         if [ $with_capall -eq 1 ]
         then
            echo run_for_win32 with_capall with_gdb
            gdb -command ri_gdb.cmd  2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
         else
            echo run_for_win32 with_gdb
            gdb -command ri_gdb.cmd
         fi
      else
         if [ $with_capall -eq 1 ]
         then
            echo run_for_win32 with_capall
            ./install/$PLATFORMTC/bin/ri.exe ./platform_win32.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
         else
            echo run_for_win32
            ./install/$PLATFORMTC/bin/ri.exe ./platform_win32.cfg
         fi
      fi
   fi
}

run_ri()
{
   if [ $with_restoreXAIT -eq 1 ]
   then
      echo "restoring XAIT..."
      cp $RICOMMONROOT/resources/fdcdata/Ate-XAIT.default $RICOMMONROOT/resources/fdcdata/Ate-XAIT.bin
   fi

   if [ $with_coverage -eq 1 ]
   then
      instr_ri
   fi

   if [ $running_linux -eq 1 ]
   then
      echo run_for_linux
      if [ -s $RICOMMONROOT/resources/Linux/tsplayer-file.txt ]
      then
         echo "tsplayer-file.txt:"
         cat $RICOMMONROOT/resources/Linux/tsplayer-file.txt
      else
         echo "$tunedataDir/720x480_MPEG-2_CBR_TS_from_ATE_4_programs.mpg" > $RICOMMONROOT/resources/Linux/tsplayer-file.txt
      fi
      run_for_linux
   else
      echo run_for_win32
      if [ -s $RICOMMONROOT/resources/Win32/tsplayer-file.txt ]
      then
         echo "tsplayer-file.txt:"
         cat $RICOMMONROOT/resources/Win32/tsplayer-file.txt
      else
         echo "$tunedataDir/720x480_MPEG-2_CBR_TS_from_ATE_4_programs.mpg" > $RICOMMONROOT/resources/Win32/tsplayer-file.txt
      fi
      run_for_win32
   fi
   echo ">>>>- RI PID $! -<<<<"
}

run_in_ate()
{
   if [ $with_setup -eq 1 ]
   then
      rm -f $hostAppPropsFile
      rm -f $configPropsFile

      $sed -i '/^OCAP.xait.ignore */s/true/false/' $finalPropertiesFile
   fi

   iter=0
   checkFileDoesntExist $hostAppPropsFile
   checkFileDoesntExist $configPropsFile
   checkXait "false"

   if [ $running_linux -eq 1 ]
   then
      export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$PLATFORMROOT/install/$PLATFORMTC/lib
   fi

   while [ true ]
   do
      cleanup_linklocal
      run_ri
      if [ $running_linux -eq 1 ]
      then
         ./install/$PLATFORMTC/bin/ate_if ./platform.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
      else
         ./install/$PLATFORMTC/bin/ate_if.exe ./platform.cfg 2>&1 | tee -a $PLATFORMROOT/$RILOGPATH/ri-iter-$iter.log
      fi
      kill_ri_ext
      kill_vlc
      save_log_if_requested
      let iter=iter+1
   done
}

run_junit_tests()
{
   iter=0
   $sed -i '/^MainClassArgs.0 *=/s/org.cablelabs.impl.ocap.OcapMain/org.cablelabs.test.textui.TestRunner/' $mpeenvIniFile
   $sed -i '/^#MainClassArgs.1 *=/s/^#//' $mpeenvIniFile
   $sed -i '/^OCAP.xait.ignore *=/s/true/false/' $finalPropertiesFile

   rm -f $hostAppPropsFile
   rm -f $configPropsFile
   rm -f junit_test_results.log

   for junit_test in $testSuites
   do
      $sed -i "/^MainClassArgs.1 *=/s/MainClassArgs.1=.*/MainClassArgs.1=$junit_test/" $mpeenvIniFile

      rm -f $envDir/junit_test_results.log

      run_ri &
      sleep 10
      while true # TODO: add timeout
      do
         # TODO: fix configurable log file name
         if tail -2 $envDir/junit_test_results.log | head -1 | grep -q "^OK "
         then
            break
         fi
         if tail -2 $envDir/junit_test_results.log | head -1 | grep -q "^Tests run: "
         then
            break
         fi
         sleep 5
      done

      sleep 5
      (
         echo "== $junit_test"
         echo
         cat $envDir/junit_test_results.log
         echo
      ) >> junit_test_results.log

      kill_ri_ext
      kill_vlc
      save_log_if_requested
      let iter=iter+1
   done
   $sed -i '/^MainClassArgs.0 *=/s/org.cablelabs.test.textui.TestRunner/org.cablelabs.impl.ocap.OcapMain/' $mpeenvIniFile
   $sed -i '/^MainClassArgs.1 *=/s/MainClassArgs.1.*/#MainClassArgs.1=%MainClassArgs.1%/' $mpeenvIniFile
}

delete_log_if_required()
{
   if [ $with_delete_log -eq 1 ] &&  [ -a $PLATFORMROOT/$RILOGPATH/RILog.txt ]
   then
      echo "deleting RILog.txt"
      rm $PLATFORMROOT/$RILOGPATH/RILog.txt
      echo "deleting ri-iter-*.log"
      rm $PLATFORMROOT/$RILOGPATH/ri-iter-*.log
   fi
}

save_log_if_requested()
{
   if [ $with_stamp_log -eq 1 ] &&  [ -a $PLATFORMROOT/$RILOGPATH/RILog.txt ]
   then
      newlogname=RILog.$(date +%Y-%m-%d-%H%M).txt
      echo "Saving RILog.txt as $newlogname..."
      cp --backup=numbered "$PLATFORMROOT/$RILOGPATH/RILog.txt" "$PLATFORMROOT/$RILOGPATH/$newlogname"
   fi
}

delete_storage_if_required()
{
   if [ $with_delete_storage -eq 1 ]
   then
      echo "deleting all storage"
      rm -rf $OCAPROOT/bin/$OCAPTC/env/persistent/
      rm -rf $OCAPROOT/bin/$OCAPTC/env/storage/
   fi
}

run_tune_test()
{
   if [ $with_setup -eq 1 ]
   then
      cp -f $hostAppPropsSrcFile $hostAppPropsFile
      cp -f $configPropsSrcFile $configPropsFile

      $sed -i '/^OCAP.xait.ignore */s/false/true/' $finalPropertiesFile
   fi

   iter=0
   checkFileExists $hostAppPropsFile $hostAppPropsSrcFile
   checkFileExists $configPropsFile $configPropsSrcFile
   checkXait "true"
   run_ri
   kill_vlc
}

run_tgshell()
{
   if [ $with_setup -eq 1 ]
   then
      if [ -z "$TGSHELL_HOME" ]
      then
         echo "TGSHELL_HOME is not set! Unable to perform setup..."
         exit 1
      fi
	  if [ -z "$TGSHELL_RESOURCE_HOME" ]
      then
         echo "TGSHELL_RESOURCE_HOME is not set! Unable to perform setup..."
         exit 1
      fi
      if [ ! -e "$TGSHELL_HOME/com/tvworks/testing/TGShell/TGShell.class" ]
      then
         echo "TGSHELL_HOME does not appear to point to tgshell! Unable to perform setup..."
         exit 1
      fi
	  if [ ! -e "$TGSHELL_RESOURCE_HOME/hostapp.properties.tgshell" ]
      then
         echo " No hostapp.properties.tgshell is found in TGSHELL_RESOURCE_HOME! Unable to perform setup..."
         exit 1
      fi
      if [ ! -e $OCAPROOT/bin/$OCAPTC/env/qa/TGShell ]
      then
         mkdir -p $OCAPROOT/bin/$OCAPTC/env/qa/TGShell
      fi
      cp -rf $TGSHELL_HOME/* $envDir/qa/TGShell/
      cp -f $TGSHELL_RESOURCE_HOME/hostapp.properties.tgshell $hostAppPropsFile

      $sed -i '/^OCAP.xait.ignore */s/false/true/' $finalPropertiesFile
   fi

   iter=0
   checkFileExists $hostAppPropsFile $envDir/qa/TGShell/hostapp.properties.tgshell
   checkXait "true"
   if [ $with_restart -eq 1 ]
      then
      echo run_ri_loop
      run_ri_loop
   else
      run_ri
   fi
   kill_vlc
}

copy_scripts()
{
   mkdir -p $XletScriptDstDir
   mkdir -p $XletScriptDstDir/level1
   mkdir -p $XletScriptDstDir/level1/utils
   mkdir -p $XletScriptDstDir/level2
   mkdir -p $XletScriptDstDir/level2/utils
   mkdir -p $XletScriptDstDir/level3
   mkdir -p $XletScriptDstDir/level3/utils
   echo "copy_scripts from $XletScriptSrcDir/level1"
   cp $XletScriptSrcDir/level1/*.bsh $XletScriptDstDir/level1
   cp $XletScriptSrcDir/level1/utils/*.bsh $XletScriptDstDir/level1/utils
   echo "copy_scripts from $XletScriptSrcDir/level2"

   if [ ! -d $XletScriptDstDir/level2 ]; then
       mkdir  $XletScriptDstDir/level2
   fi

    for i in $( ls $XletScriptSrcDir/level2); do
        # Check whether its a directory
        if [ ${i##*.} != "bsh" ]; then

            if [ ! -d $XletScriptDstDir/level2/$i ]; then
                mkdir  $XletScriptDstDir/level2/$i
	    fi # checking for dirs at first level
	    for j in $( ls $XletScriptSrcDir/level2/$i); do
                # Check whether its a directory
                if [ ${j##*.} != "bsh" ]; then
                    if [ ! -d $XletScriptDstDir/level2/$i/$j ]; then
                        mkdir  $XletScriptDstDir/level2/$i/$j
		    fi #checking for dirs at 2nd level (j)
                    # checking whether the folder is empty and if not, then only copy the files.
                    if [ `ls -1 $XletScriptSrcDir/level2/$i/$j | wc -l` -ne 0 ]; then
                        cp -r $XletScriptSrcDir/level2/$i/$j/* $XletScriptDstDir/level2/$i/$j
                    fi
                else
                    cp -r $XletScriptSrcDir/level2/$i/*.bsh $XletScriptDstDir/level2/$i/
                fi
            done #j for loop
        else
            cp $XletScriptSrcDir/level2/*.bsh $XletScriptDstDir/level2/
        fi # checking for .bsh at first level
    done #end of for $i in level2

   echo "copy_scripts from $XletScriptSrcDir/level3"
   cp $XletScriptSrcDir/level3/*.bsh $XletScriptDstDir/level3
   cp $XletScriptSrcDir/level3/utils/*.bsh $XletScriptDstDir/level3/utils
   echo "available scripts:"
   ls $XletScriptDstDir
}

run_xlet()
{
   xletDataDir=$OCAPROOT/apps/qa/org/cablelabs/xlet/$Xlet
   xletHostAppPropsSrcFile=$xletDataDir/hostapp.properties
   xletConfigPropsSrcFile=$xletDataDir/config.properties

   if [ -s $xletHostAppPropsSrcFile ]
   then
       echo "using $xletHostAppPropsSrcFile"
       if [ $with_setup -eq 1 ]
       then
          cp -f $xletHostAppPropsSrcFile $hostAppPropsFile
          if [ -s $xletConfigPropsSrcFile ]
          then
             cp -f $xletConfigPropsSrcFile $configPropsFile
          else
             cp -f $configPropsSrcFile $configPropsFile
          fi
          $sed -i '/^OCAP.xait.ignore */s/false/true/' $finalPropertiesFile
       fi

       iter=0
       checkFileExists $hostAppPropsFile $xletHostAppPropsSrcFile 
       checkFileExists $configPropsFile $configPropsSrcFile
       checkXait "true"
       copy_scripts
       run_ri
       kill_vlc
   else
       echo "couldn't find $xletHostAppPropsSrcFile"
       echo "does $Xlet and $xletDataDir exist?"
   fi
}

run_dvrtestrunner()
{
   if [ $with_setup -eq 1 ]
   then
      echo "======================================================="
      echo "RUNNING dvrtestrunner"
      echo "======================================================="

      echo "app.0.application_identifier=0x000000017203" > $hostAppPropsFile
      echo "app.0.application_control_code=AUTOSTART" >> $hostAppPropsFile
      echo "app.0.visibility=VISIBLE" >> $hostAppPropsFile
      echo "app.0.priority=0xff" >> $hostAppPropsFile
      echo "app.0.launchOrder=0x0" >> $hostAppPropsFile
      echo "app.0.app_profiles.0.profile=0x102" >> $hostAppPropsFile
      echo "app.0.app_profiles.0.version_major=0x1" >> $hostAppPropsFile
      echo "app.0.app_profiles.0.version_minor=0x0" >> $hostAppPropsFile
      echo "app.0.app_profiles.0.version_micro=0x0" >> $hostAppPropsFile
      echo "app.0.application_version=0x0" >> $hostAppPropsFile
      echo "app.0.application_name=DVRTestRunner" >> $hostAppPropsFile
      echo "app.0.base_directory=/syscwd/qa/xlet" >> $hostAppPropsFile
      echo "app.0.classpath_extension=" >> $hostAppPropsFile
      echo "app.0.initial_class_name=org.cablelabs.xlet.DvrTest.DVRTestRunnerXlet" >> $hostAppPropsFile
      echo "app.0.args.0=config_file=config.properties" >> $hostAppPropsFile

      echo "###############################" > $configPropsFile
      echo "#### DvrTestRunner Config #####" >> $configPropsFile
      echo "###############################" >> $configPropsFile
      echo "DVR_by_FPQ=TRUE" >> $configPropsFile
      echo "DVR_sourceId_0=0x45a" >> $configPropsFile
      echo "DVR_sourceId_1=0x44c" >> $configPropsFile
      echo "DVR_sourceId_2=0x5e7" >> $configPropsFile   
      echo "DVR_sourceId_3=0x5e9" >> $configPropsFile   
      echo "DVR_sourceId_4=0x6e4" >> $configPropsFile   
      echo "DVR_FPQ_0=447000000,1,8" >> $configPropsFile
      echo "DVR_FPQ_1=489000000,2,16" >> $configPropsFile
      echo "DVR_FPQ_2=599000000,2,16" >> $configPropsFile
      echo "DVR_FPQ_3=651000000,1,16" >> $configPropsFile
      echo "DVR_FPQ_4=699000000,25992,16" >> $configPropsFile

      $sed -i '/^OCAP.xait.ignore */s/false/true/' $finalPropertiesFile
   fi

   iter=0
   checkFileExists $hostAppPropsFile "by running -dvrtestrunner -setup"
   checkFileExists $configPropsFile "by running -dvrtestrunner -setup"
   checkXait "true"
   run_ri
   kill_vlc
}

run_auto_dvr()
{
   if [ $with_setup -eq 1 ]
   then
      cp -f $dvrXHostAppPropsSrcFile $hostAppPropsFile
      cp -f $configPropsSrcFile $configPropsFile
      $sed -i '/^OCAP.xait.ignore */s/false/true/' $finalPropertiesFile
   fi

   iter=0

   checkFileExists $hostAppPropsFile $dvrXHostAppPropsSrcFile
   checkFileExists $configPropsFile $configPropsSrcFile
   checkXait "true"
   run_ri
   kill_vlc
}

run_ri_loop()
{
   while [ true ]
   do
      run_ri
      kill_ri
      kill_vlc
      save_log_if_requested
      echo "Restarting RI"
   done
}

instr_ri()
{
   if [ -f $OCAPROOT/tools/generic/emma/emma.jar ]
   then
      echo "Instrumenting RI"
      java -cp $OCAPROOT/tools/generic/emma/emma.jar emma instr -m overwrite -cp $envDir/sys/ocap-classes.jar
   else
      emma_error
   fi
}

dump_coverage()
{
   if [ -f $OCAPROOT/tools/generic/emma/emma.jar ]
   then
      echo "Dumping Coverage Information"
      java -cp $OCAPROOT/tools/generic/emma/emma.jar emma ctl -c coverage.get,,true,false
   else
      emma_error
   fi
}

emma_error()
{
   echo "======================================================="
   echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   echo
   echo "ERROR: EMMA is not installed in $OCAPROOT/tools/generic/emma";
   echo "Please install emma.jar into this location before using the -coverage option";
   echo "emma.jar also needs to be in your runtime classpath or the RI will not run";
   echo
   echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
   echo "======================================================="
   exit 1
}

main()
{
   if [ `uname` = "Linux" ]
   then
      sed='sed'
      running_linux=1
   else
      sed='sed -b'
   fi

   cleanup_linklocal
   delete_log_if_required
   delete_storage_if_required

   if [ $with_mockDLL -eq 1 ]
   then
      enable_mockDLL
   fi

   if [ $with_ate -eq 1 ]
   then
      echo run_in_ate
      run_in_ate
   elif [ $with_junit -eq 1 ]
      then
      echo run_junit_tests
      run_junit_tests
   elif [ $with_tunetest -eq 1 ]
      then
      echo run_tune_test
      run_tune_test
   elif [ $with_autodvr -eq 1 ]
      then
      echo run_auto_dvr
      run_auto_dvr
   elif [ $with_xlet -eq 1 ]
      then
      echo "run_xlet for $Xlet"
      run_xlet
   elif [ $with_dvrtestrunner -eq 1 ]
      then
      echo run_dvrtestrunner
      run_dvrtestrunner
   elif [ $with_tgshell -eq 1 ]
      then
      echo run_tgshell
      run_tgshell
   elif [ $with_restart -eq 1 ]
      then
      echo run_ri_loop
      run_ri_loop
   else
      run_ri
   fi
   restore_mpeenv
}

help()
{
   echo "  ";
   echo "usage: runRI.sh [-ate, -tunetest, -junit, -suite xxx, -gdb, -valgrind, -full, -capall, -restoreXAIT, -deletelog, -deletestorage, -setup, -restoreMPEENV, -coverage, -autodvr <xxx>, -tgshell -mockDLL]";
   echo " -ate runs the ri in a loop for ATE testing";
   echo " -tunetest runs the ri tune test";
   echo " -xlet <Xlet> runs the xlet as provided in the command";
   echo " -dvrtestrunner runs the ri dvrtestrunner xlet";
   echo " -junit executes unit tests";
   echo " -suite xxx, used with -junit, where xxx is the particular test suite. If ommitted all basically working test suites will be run"
   echo " -gdb runs the ri in gdb, may be combined with other flags (e.g. -tunetest or -ate)";
   echo " -valgrind runs the ri in valgrind, may be combined with other flags (e.g. -tunetest or -ate)";
   echo " -full used with -valgrind (adds -v --leak-check=full)";
   echo " -capall combined with any, captures stdout & stderr to ri-iter-N.log";
   echo " -restoreXAIT combined with any, reverts to default XAIT on each run";
   echo " -deletelog deletes RILog.txt at the start of this script";
   echo " -deletestorage deletes $OCAPROOT/bin/$OCAPTC/env/persistent and $OCAPROOT/bin/$OCAPTC/env/storage";
   echo " -setup sets up config files before running the ri, may be combined with other flags (e.g. -tunetest or -ate)";
   echo " -restoreMPEENV restores the mpeenv.ini file on exit.";
   echo " -coverage instruments the stack for test coverage, dumps the data on restart, EMMA is required to be installed";
   echo " -autodvr xxx, uses DvrExercisor, where xxx is the (optional) command file (e.g. random_tune.cmd)";
   echo " -restart auto-restarts the executable in case of death";
   echo " -tgshell runs the ri with tgshell (requires TGSHELL_HOME be set and point to the unzipped tgshell xlet)";
   echo " -mockDLL combined with any, enables the use of the mock.dll for DTCP/IP";
   echo "  ";
   echo " RILog.txt and ri-iter-X.log are both written to the path \$PLATFORMROOT/\$RILOGPATH - to change where log files are written, assign the \$RILOGPATH environment variable to a relative path"    
   echo "  ";
   echo "Note: Basically working test suites are $testSuitesBasicallyWorking";
   echo "test suites that hang or crash the RI are $testSuitesThatHangOrCrash";
}

trap signal_handler INT KILL

until [ -z $1 ]
do
   case $1 in
      "-ate") with_ate=1;with_tunetest=0;with_dvrtestrunner=0;;
      "-tunetest") with_tunetest=1;with_ate=0;with_dvrtestrunner=0;;
      "-xlet")
           with_xlet=1;with_ate=0;with_tune_test=0;with_dvrtestrunner=0;
           leadCharacter=`echo $2 | cut -c1`
           if [ "$leadCharacter" != "" ] && [ "$leadCharacter" != "-" ]; then
              Xlet=$2
              shift
           fi
           ;;
	  "-restart") with_restart=1;;
      "-dvrtestrunner") with_dvrtestrunner=1;with_ate=0;with_tune_test=0;;
      "-junit") with_junit=1;;
      "-gdb") with_gdb=1;with_valgrind=0;;
      "-valgrind") with_valgrind=1;with_gdb=0;;
      "-full") with_full=1;;
      "-capall") with_capall=1;;
      "-restoreXAIT") with_restoreXAIT=1;;
      "-deletelog") with_delete_log=1;;
      "-stamplog") with_stamp_log=1;;
      "-deletestorage") with_delete_storage=1;;
      "-setup") with_setup=1;;
      "-suite") testSuites=$2;shift;;
      "-restoreMPEENV") restore_ini=1;;
      "-coverage") with_coverage=1;;
      "-autodvr") with_autodvr=1;;
      "-tgshell") with_tgshell=1;;
      "-mockDLL") with_mockDLL=1;;
      "-?" | "?" | "-help" | "help" | "-h" | "h") help; exit 0;;
      *) echo "unrecognized arg: $1"; help; exit 1;;
   esac
   shift
done

main
