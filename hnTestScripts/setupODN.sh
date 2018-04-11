#
# set up the RI to run ODN
#
hostIPAddr=192.168.0.105
hdhrIPAddr=0x101A8A4E
siData=ODN-SI-CiscoDev2-061312.bin
xaitData=ODN-XAIT-5.2-APRIL-2012.bin
ocData=ODN-OC-5.2-061312.bin
echo
echo Host:     $hostIPAddr
echo HDHR:     $hdhrIPAddr
echo siData:   $siData
echo xaitData: $xaitData
echo ocData:   $ocData
echo

echo Setting XAIT files ...
if [ ! -f $RICOMMONROOT/resources/fdcdata/$siData ]; then
  echo $RICOMMONROOT/resources/fdcdata/$siData does not exist
  exit
fi 
if [ ! -f $RICOMMONROOT/resources/fdcdata/$xaitData ]; then
  echo $RICOMMONROOT/resources/fdcdata/$xaitData does not exist
  exit
fi
if [ ! -f $RICOMMONROOT/resources/fdcdata/$ocData ]; then
  echo $RICOMMONROOT/resources/fdcdata/$ocData does not exist
  exit
fi
cd $RICOMMONROOT/resources
rm fdc-files.txt
echo $siData   >> fdc-files.txt
echo $xaitData >> fdc-files.txt
echo $ocData   >> fdc-files.txt
cat $RICOMMONROOT/resources/fdc-files.txt
echo
echo Setting platform configurations ...
#
# platform configuration to use HDHR and 2 tuners
cd $PLATFORMROOT
cat platform.cfg | \
   sed "s/RI.Platform.IpAddr = 127.0.0.1/RI.Platform.IpAddr = $hostIPAddr/" | \
   sed 's/RI.Headend.tunerType = VLC/RI.Headend.tunerType = HDHR/' | \
   sed 's/RI.Platform.numTuners = 1/RI.Platform.numTuners = 2/' | \
   sed "s/RI.Headend.tuner.0.StreamerPort = 4212/RI.Headend.tuner.0.StreamerPort = $hdhrIPAddr/" | \
   sed "s/RI.Headend.tuner.1.StreamerPort = 4213/RI.Headend.tuner.1.StreamerPort = $hdhrIPAddr/" \
   > platform.cfg.tmp
   mv platform.cfg.tmp platform.cfg
#
# final.properties for ethernet connection and to read XAIT
echo
echo Setting final.properties ...
cd $OCAPROOT/bin/$OCAPTC/env
rm final.properties
echo OCAP.xait.ignore=false > final.properties
echo OCAP.guides.accessDeclaredMembersPerm=true >> final.properties
echo OCAP.hn.multicast.iface=eth0 >> final.properties
echo OCAP.mgrmgr.manager.OcapSecurity=org.cablelabs.impl.manager.security.NoAccessControl >> final.properties
echo OCAP.cablecard.manufacturer=512 >> final.properties
#
echo
echo If necessary, delete hostapp.properties
if [ -f hostapp.properties ] 
   then
   echo deleting hostapp.properties
   rm hostapp.properties
fi
#
# enable reading the out of band data
echo
echo setting SITP.ENABLE.OOB.PSI=TRUE
cat mpeenv.ini | \
   sed "s/SITP.ENABLE.OOB.PSI=FALSE/SITP.ENABLE.OOB.PSI=TRUE/" \
   > mpeenv.ini.tmp
   mv mpeenv.ini.tmp mpeenv.ini
#
cd $PLATFORMROOT
echo
echo Usage:
echo ./runRI.sh
echo
echo For ODN these settings are needed
echo
echo 1. SI and XAIT in $RICOMMONROOT/resources
echo 2. ODN files in $RICOMMONROOT/resources/fdc-files.txt
echo 3. Host IP and HDHR settings in $RIPLATFORMROOT/platform.cfg
echo 4. No $OCAPROOT/env/$OCAPTC/bin/hostapp.properties
echo 5. In final.properties use the XAIT and set OCAP.hn.multicast.iface=
echo 6. In  mpeenv.ini SITP.ENABLE.OOP.PSI=TRUE
