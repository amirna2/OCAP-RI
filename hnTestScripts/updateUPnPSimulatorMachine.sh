#! /bin/bash

# This is the script that builds the RI on the Linux UPnP simulator machine.
# It runs on the Linux UPnP simulator machine.

function stopRI
{
    killall -9 ri      2>/dev/null
    killall -9 vlc.exe 2>/dev/null
    killall -9 ate_if  2>/dev/null
}

if [ ! -e $HOME/ri.tar.gz ]
then
    echo "Can't find $HOME/ri.tar.gz."
    exit 1
fi

stopRI

cd "$RIROOT"/..

rm -rf OCAPRI_root

mv $HOME/ri.tar.gz .
tar xzf ri.tar.gz
rm ri.tar.gz

cd "$PLATFORMROOT"
make

cd "$OCAPROOT"
ant

# Set RI network interface.
echo 'OCAP.hn.multicast.iface=eth0' >>"$OCAPROOT/bin/$OCAPTC/env/final.properties"
