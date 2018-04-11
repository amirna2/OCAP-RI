#!/bin/sh

if [ $# -eq 1 ]; then
  echo "Processing $1"
  PATH=`cygpath -u $PLATFORMROOT/install/$PLATFORMTC/bin`:$PATH ffmpeg -i $1 -f image2 image%05d.bmp
else
  echo -e "Decode video from specified MPEG transport stream file and save every"
  echo -e "frame into current directory as bitmap (imageXXXXX.bmp) file.\n"
  echo -e "Usage: $0 <path/to/your/mpeg/ts/stream>"
fi
