#!/bin/sh

os=""

if [ `uname` = "Linux" ]
then
    LD_LIBRARY_PATH=$PLATFORMROOT/install/$PLATFORMTC/lib:$LD_LIBRARY_PATH
    GST_PLUGIN_PATH=$PLATFORMROOT/install/$PLATFORMTC/lib/gstreamer-0.10
    SHLIB_SUFFIX=.so
    os="Linux"
else
    PATH=$PLATFORMROOT/install/$PLATFORMTC/bin:$PLATFORMROOT/install/$PLATFORMTC/lib:$PATH
    GST_PLUGIN_PATH=$PLATFORMROOT/install/$PLATFORMTC/lib/gstreamer-0.10
    SHLIB_SUFFIX=.dll
    os="Win32"
fi

errorDisplayTime=1

tunedataDir=$RICOMMONROOT/resources/tunedata
targetDefsFile=$PLATFORMROOT/target/$PLATFORMTC/defs.mk
gstelementsFile=$PLATFORMROOT/src/gstreamer/gstelements.c

with_vlc=0
with_netsrc=0
with_netsink=0
with_inspect=0
with_spts=0
with_gdb=0
with_capall=0

standalone=0
channel=1
host=127.0.0.1
port=4140
blksize=188
buffers=200
element=""
gst_inspect="$PLATFORMROOT/install/$PLATFORMTC/bin/gst-inspect-0.10"
gst_launch="$PLATFORMROOT/install/$PLATFORMTC/bin/gst-launch-0.10"
gst_default_args="--gst-debug-no-color --gst-debug-level=1"
gst_debug="--gst-debug=sptsfilesrc:3,pacedfilesrc:3,queue:4,udpsink:4,netsink:4,udpsrc:4,netsrc:4,multiudpsrc:5,pidfilter:1,esassembler:1,mpegdecoder:1,display:3,displaybuffer:1,basesink:3"
gst_plugins="--gst-plugin-load=$GST_PLUGIN_PATH/libgstcoreelements$SHLIB_SUFFIX --gst-plugin-load=$GST_PLUGIN_PATH/libgstdebug$SHLIB_SUFFIX --gst-plugin-load=$GST_PLUGIN_PATH/libgstcablelabs$SHLIB_SUFFIX --gst-plugin-load=$GST_PLUGIN_PATH/libgstudp$SHLIB_SUFFIX --gst-plugin-load=$GST_PLUGIN_PATH/libgsttcp$SHLIB_SUFFIX"
gst_args="$gst_default_args $gst_plugins $gst_debug"
gst_input_file1="720x480_MPEG-2_CBR_TS_from_ATE_4_programs.mpg"
gst_input_file1_pid1="0x7d0"
gst_input_file1_pid2="0xbb8"
gst_input_file1_pid3="0x1388"
gst_input_file2="hd_airplane.mpg"
gst_input_file2_pid1="0x800"
gst_input_file2_pid2="0x7c0"
gst_input_file3="galaxy_pingpong.mpg"
gst_input_file3_pid1="0x110"
gst_input_file3_pid2="0x210"
gst_input_file4="background.mpg"
gst_input_file4_pid1="0x044"
gst_input_file5="big_buck_bunny_reduced.ts"
gst_input_file5_pid1="0x042"
gst_input_file6="clock.mpg"
gst_input_file6_pid1="0x042"
gst_pipe=""
gst_output_pipe=" queue max-size-buffers=$buffers ! esassembler ! queue max-size-buffers=$buffers ! mpegdecoder ! queue max-size-buffers=$buffers ! display supplied-window=false"
gst_pipe_type=1
gst_pipe_type_str="input"

checkDisplay()
{
    if [ -z "$1" ]
    then
        echo "$0 ERROR - Parameter #1 is zero length!?"  # Or no parameter passed.
        exit -1
    else
        grep UI_TARGET $targetDefsFile | grep -v ^# | grep $1 > $0.tmp
        if [ -s $0.tmp ]
        then
            echo "UI_TARGET is $1"
        else
            echo "======================================================="
            echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
            echo
            echo "ERROR: UI_TARGET is not set to $1!"
            echo
            echo "set UI_TARGET = $1 in $targetDefsFile"
            echo "and then make clean purge build in PLATFORMROOT/src"
            echo
            echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
            echo "======================================================="
            echo
            echo
            sleep $errorDisplayTime
            exit -2
        fi
        rm -f $0.tmp
    fi
}

checkLogging()
{
    grep log_default $gstelementsFile | grep "//" > $0.tmp
    if [ -s $0.tmp ]
    then
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        echo
        echo "WARNING: the GST default log function is not installed"
        echo "there will be no log output from the elements in your"
        echo "pipeline"
        echo
        echo "uncomment gst_debug_log_default in src/gstreamer/gstelements.c"
        echo "and then make in PLATFORMROOT/src to correct..."
        echo
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
        sleep $errorDisplayTime
    else
        echo "log output is possible from the elements in your pipeline..."
    fi
    rm -f $0.tmp
}

selectChannel()
{
    case $1 in
      1) location="$tunedataDir/$gst_input_file1";
         prgm=1; pid="$gst_input_file1_pid1";
         pidList="0x0000 0x0064 0x07d0 0x07d1";;
      2) location="$tunedataDir/$gst_input_file1";
         prgm=2; pid="$gst_input_file1_pid2";
         pidList="0x0000 0x0065 0x0bb8 0x0bb9";;
      3) location="$tunedataDir/$gst_input_file1";
         prgm=4; pid="$gst_input_file1_pid3";
         pidList="0x0000 0x0067 0x1388 0x1389";;
      4) location="$tunedataDir/$gst_input_file2";
         prgm=2; pid="$gst_input_file2_pid1";
         pidList="0x0000 0x0031 0x0800 0x0801";;
      5) location="$tunedataDir/$gst_input_file2";
         prgm=3; pid="$gst_input_file2_pid2";
         pidList="0x0000 0x0030 0x07c0 0x07c1";;
      6) location="$tunedataDir/$gst_input_file3";
         prgm=25991; pid="$gst_input_file3_pid1";
         pidList="0x0000 0x0100 0x0110 0x0120";;
      7) location="$tunedataDir/$gst_input_file3";
         prgm=25992; pid="$gst_input_file3_pid2";
         pidList="0x0000 0x0200 0x0210 0x0220";;
      8) location="$tunedataDir/$gst_input_file4";
         prgm=1; pid="$gst_input_file4_pid1";
         pidList="0x0000 0x0042 0x0044 ";;
      9) location="$tunedataDir/$gst_input_file5";
         prgm=1; pid="$gst_input_file5_pid1";
         pidList="0x0000 0x0042 0x0044 0x0045";;
      10) location="$tunedataDir/$gst_input_file6";
         prgm=1; pid="$gst_input_file6_pid1";
         pidList="0x0000 0x0042 0x0044 ";;
      *) echo "unrecognized channel $1";
         location="$tunedataDir/$gst_input_file1";
         prgm=1; pid="$gst_input_file1_pid1";
         pidList="0x0000 0x0064 0x07d0 0x07d1";;
    esac

    if [ $gst_pipe_type -eq 1 ]
    then
        if [ $with_vlc -eq 1 ]
        then
            gst_launch="$RICOMMONROOT/resources/$os/VLC/vlc.exe"
            gst_args="--loop --ts-out $host:$port $location"
            gst_pipe=""
        else
            if [ $with_netsink -eq 1 ]
            then
                gst_pipe="pacedfilesrc blksize=$blksize location=$location pidlist='$pidList' ! queue ! netsink host=$host port=$port blksize=$blksize";
            else
                gst_pipe="pacedfilesrc blksize=$blksize location=$location pidlist='$pidList' ! queue ! udpsink host=$host port=$port";
            fi
        fi
    else
        if [ $with_vlc -eq 1 ]
        then
            gst_launch="$RICOMMONROOT/resources/$os/VLC/vlc.exe"
            gst_args="udp://@:$port"
            gst_pipe=""
        elif [ $standalone -eq 1 ]
        then
            if [ $tofile -eq 1 ]
            then
                gst_pipe="pacedfilesrc blksize=$blksize location=$location loop=1 pidlist='$pidList' ! queue ! filesink location=./spts.mpg";
            else
                gst_pipe="pacedfilesrc blksize=$blksize location=$location pidlist='$pidList' ! queue ! pidfilter pidlist=$pid ! $gst_output_pipe";
            fi
        else
            if [ $with_netsrc -eq 1 ]
            then
                gst_pipe="netsrc port=$port  blksize=$blksize ! queue max-size-buffers=$buffers ! pidfilter pidlist=$pid ! $gst_output_pipe";
            else
                gst_pipe="udpsrc port=$port ! queue ! pidfilter pidlist=$pid ! $gst_output_pipe";
            fi
        fi
    fi
}

make_gdb_command_file()
{
    echo "file $gst_launch " > $1.cmd
    echo -n "set args $gst_args " >> $1.cmd
    echo -n $gst_pipe >> $1.cmd
}

main()
{
    if [ $with_inspect -eq 1 ]
    then
        if [ $with_capall -eq 1 ]
        then
            echo "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" >>  gst-inspect.log
            echo "        Inspecting $element" >>  gst-inspect.log
            echo "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" >>  gst-inspect.log
            $gst_inspect $gst_plugins $element 2>&1 | tee -a gst-inspect.log
        else
            $gst_inspect $gst_default_args $gst_plugins $element
        fi
    elif [ $with_spts -eq 1 ]
    then
        selectChannel $channel
        gst_pipe="sptsfilesrc program=$prgm location=$location loop=1 ! queue ! filesink location=./spts-ch$channel-MPEG2.ts";
        echo "converting: $gst_launch $gst_args $gst_pipe" > gst-convert.log
        $gst_launch $gst_args $gst_pipe 2>&1 | tee -a gst-convert.log
    else
        checkDisplay $os
        checkLogging
        selectChannel $channel

        if [ $with_gdb -eq 1 ]
        then
            rm -f gst-$gst_pipe_type_str.cmd
            make_gdb_command_file gst-$gst_pipe_type_str
            if [ $with_capall -eq 1 ]
            then
                gdb -command gst-$gst_pipe_type_str.cmd  2>&1 | tee -a gst-$gst_pipe_type_str.log
            else
                gdb -command gst-$gst_pipe_type_str.cmd
            fi
        else
            if [ $with_capall -eq 1 ]
            then
                echo "running: $gst_launch $gst_args $gst_pipe" > gst-$gst_pipe_type_str.log

                $gst_launch $gst_args $gst_pipe 2>&1 | tee -a gst-$gst_pipe_type_str.log
            else
                echo "running: $gst_launch $gst_args $gst_pipe"
                $gst_launch $gst_args $gst_pipe
            fi
        fi
    fi
}

help()
{
   echo "  ";
   echo "usage: runGST.sh [ -inspect, -gdb, -capall, -in, -out, -standalone, -ch ]";
   echo " -spts -in ch<N> converts the input MPTS to an SPTS with the providd program selection";
   echo " -inspect <element> runs gst-inspect on the provided element";
   echo " -gdb runs the pipeline in gdb, may be combined with -capall, -in, or -out)";
   echo " -vlc runs VLC instead of the pipeline, may be combined with -in, or -out)";
   echo " -netsrc runs netsrc instead of udpsrc in the pipeline";
   echo " -netsink runs netsink instead of udpsink in the pipeline";
   echo " -capall combined with any other flags, captures stdout & stderr to the log";
   echo " -in runs the input pipeline";
   echo " -out runs the output pipeline";
   echo " -standalone runs the output pipeline with a filesrc rather than udpsrc"
   echo " -capout runs the standalone pipeline with a filesink rather than udpsink"
   echo " -ch <channel number> selectes channel 1 through 8, where:"
   echo "  ch1: $gst_input_file1 prgm=1 pid=$gst_input_file1_pid1";
   echo "  ch2: $gst_input_file1 prgm=2 pid=$gst_input_file1_pid2";
   echo "  ch3: $gst_input_file1 prgm=3 pid=$gst_input_file1_pid3";
   echo "  ch4: $gst_input_file2 prgm=1 pid=$gst_input_file2_pid1";
   echo "  ch5: $gst_input_file2 prgm=2 pid=$gst_input_file2_pid2";
   echo "  ch6: $gst_input_file3 prgm=1 pid=$gst_input_file3_pid1";
   echo "  ch7: $gst_input_file3 prgm=2 pid=$gst_input_file3_pid2";
   echo "  ch8: $gst_input_file4 prgm=1 pid=$gst_input_file4_pid1";
   echo "  ch9: $gst_input_file5 prgm=1 pid=$gst_input_file5_pid1";
   echo "  ch10: $gst_input_file6 prgm=1 pid=$gst_input_file6_pid1";
   echo " -host <xxx.xxx.xxx.xxx> sets the host IP address to use"
   echo " -port <xxxxx> sets the host IP port to use"
   echo " -blksize <xxxxx> sets the blksize for the sptsfilesrc to use"
   echo " -buffers <xxxxx> sets the max-size-buffers for the queues to use"
}

until [ -z $1 ]
do
   case $1 in
      "-inspect") with_inspect=1;element=$2;shift;;
      "-spts") with_spts=1;shift;;
      "-gdb") with_gdb=1;;
      "-vlc") with_vlc=1;;
      "-netsrc") with_netsrc=1;;
      "-netsink") with_netsink=1;;
      "-capall") with_capall=1;;
      "-in") gst_pipe_type=1;gst_pipe_type_str="input";;
      "-out") gst_pipe_type=0;gst_pipe_type_str="output";;
      "-ch") channel=$2;shift;;
      "-host") host=$2;shift;;
      "-port") port=$2;shift;;
      "-blksize") blksize=$2;shift;;
      "-buffers") buffers=$2;shift;;
      "-standalone") standalone=1;gst_pipe_type=0;gst_pipe_type_str="output";;
      "-capout") tofile=1;standalone=1;gst_pipe_type=0;gst_pipe_type_str="output";;
      "-?" | "?" | "-help" | "help" | "-h" | "h") help; exit 0;;
      *) echo "unrecognized arg: $1"; help; exit 1;;
   esac
   shift
done

main

