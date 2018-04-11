#!/usr/bin/bash
# Convenience script for starting Eclipse with RI specific environment variables.

which eclipse > /dev/null 2>&1
if [ $? -ne 0 -a ! -x $ECLIPSE_HOME/eclipse.exe ]; then
    echo "Can't find executable eclipse.exe. Either set ECLIPSE_HOME or add eclipse to your PATH"
    exit 1
fi

if [ -a ../setEnv ]; then
        . ../setEnv
else
        . ../../setEnv
fi

$ECLIPSE_HOME/eclipse.exe &
