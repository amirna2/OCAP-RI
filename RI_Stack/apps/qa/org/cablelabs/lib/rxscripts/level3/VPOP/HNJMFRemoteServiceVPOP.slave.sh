if [ -z "$1" ]
then
    echo "No VPOP server name provided"
else
    VPOP_SERVER_NAME=$1
    echo "VPOP server name provided is $VPOP_SERVER_NAME"
fi

# add the required properties to enable VPOP
cd $OCAPROOT/bin/$OCAPTC/env
if [ -e final.properties ]
then
    if grep -q "^OCAP.hn.server.name" final.properties
    then
        sed -i '/^OCAP.hn.server.name */s/*/$VPOP_SERVER_NAME/' final.properties
    else
        echo "OCAP.hn.server.name=$VPOP_SERVER_NAME" >> final.properties
    fi

    if grep -q "^OCAP.hn.server.vpop.enabled" final.properties
    then
        sed -i '/^OCAP.hn.server.vpop.enabled */s/false/true/' final.properties
    else
        echo "OCAP.hn.server.vpop.enabled=true" >> final.properties
    fi
else
    echo "OCAP.hn.server.name=$VPOP_SERVER_NAME" > final.properties
    echo "OCAP.hn.server.vpop.enabled=true" >> final.properties 
fi

cd $PLATFORMROOT
sh runRI.sh -capall -setup -deletelog -deletestorage -xlet RiExerciser
