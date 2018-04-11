#!/bin/sh

svn export svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg 2>&1 | tee ffmpeg-svn.log
mv ffmpeg ffmpeg-svn
mv ffmpeg-svn.log ffmpeg-svn/
tar cjvf ffmpeg-svn.tar.bz2 ffmpeg-svn
rm -Rf ffmpeg-svn

