if [ -a ../setEnv ]; then
        . ../setEnv
else
        . ../../setEnv
fi
cd $PLATFORMROOT;./runRI.sh $@
