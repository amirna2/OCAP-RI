#!/bin/bash



#
# Platform specific kill of ri
#
kill_ri()
{
   echo "killing RI..."

   if [ `uname` = "Linux" ]
   then
      killall -9 ri
   else
      $PLATFORMROOT/install/$PLATFORMTC/bin/pk.exe ri.exe
   fi

}


#
# Platform specific kill of vlc 
#
kill_vlc()
{
   echo "killing VLC..."

   if [ `uname` = "Linux" ]
   then
      killall -9 vlc.exe
   else
      $PLATFORMROOT/install/$PLATFORMTC/bin/pk.exe vlc.exe
   fi

}

#
# Platform specific cleanup of linklocal interface 
#
cleanup_linklocal()
{
   if [ `uname` = "Linux" ]
   then
      file=`ls /tmp/ri_*.linklocal`
      if [ $? -eq 0 ]
      then
         echo $file
         IFS='_' read -a array <<< "$file"
         addr=`echo "${array[1]}"`
         IFS='.' read -a interface <<< "${array[2]}"
         iface=`echo "${interface[0]}"`
         ip addr del $addr dev $iface
         rm -f $file
      fi

   fi

}
