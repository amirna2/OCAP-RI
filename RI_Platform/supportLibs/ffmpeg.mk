
#
# FFMPEG configure arguments
#
ifeq ($(FFMPEG_DEBUG),yes)
	FFMPEG_DEBUG_FLAG = --enable-debug --disable-optimizations
else
	FFMPEG_DEBUG_FLAG = --disable-debug --enable-optimizations
endif
FFMPEG_CONFIGURE = ./configure \
	--prefix=$(PLATFORM_INST_DIR) \
	--disable-static \
	--enable-shared \
	--disable-gpl \
	--disable-nonfree \
	--enable-ffmpeg \
	--disable-ffplay \
	--disable-ffserver \
	--disable-postproc \
	--disable-swscale \
	--disable-avfilter \
	--disable-avfilter-lavf \
	--disable-vhook \
	$(FFMPEG_THREADS) \
	--disable-x11grab \
	--disable-vdpau \
	--disable-network \
	--disable-ipv6 \
	--disable-mpegaudio-hp \
	--enable-gray \
	--enable-fastdiv \
	--disable-small \
	--disable-aandct \
	--disable-fft \
	--disable-golomb \
	--disable-mdct \
	--disable-rdft \
	--disable-hardcoded-tables \
	--enable-memalign-hack	 \
	--disable-beos-netserver \
	--disable-encoders --enable-encoder=bmp \
	--disable-decoders --enable-decoder=mpeg1video --enable-decoder=mpeg2video \
	--disable-muxers --enable-muxer=image2 \
	--disable-demuxers \
	--enable-demuxer=mpegts \
	--disable-parsers --enable-parser=mpegaudio --enable-parser=mpegvideo \
	--disable-bsfs \
	--disable-protocols --enable-protocol=file \
	--disable-indevs \
	--disable-outdevs \
	--disable-devices \
	--disable-filters \
	--disable-avisynth \
	--disable-bzlib \
	--disable-libamr-nb \
	--disable-libamr-wb \
	--disable-libdc1394 \
	--disable-libdirac \
	--disable-libfaac \
	--disable-libfaad \
	--disable-libfaadbin \
	--disable-libgsm \
	--disable-libmp3lame \
	--disable-libnut \
	--disable-libopenjpeg \
	--disable-libschroedinger \
	--disable-libspeex \
	--disable-libtheora \
	--disable-libvorbis \
	--disable-libx264 \
	--disable-libxvid \
	--disable-mlib \
	--disable-zlib \
	--source-path=$(FFMPEG_ROOT) \
	--target-os=$(FFMPEG_TARGET_OS) \
	--target-path=$(PLATFORM_INST_DIR) \
	--cc=$(RI_CC) \
	--arch=$(FFMPEG_ARCH) \
	--cpu=$(FFMPEG_CPU) \
	--disable-powerpc-perf \
	--disable-altivec \
	--disable-amd3dnow \
	--disable-amd3dnowext \
	--disable-mmx \
	--disable-mmx2 \
	--disable-sse \
	--disable-ssse3 \
	--disable-armv5te \
	--disable-armv6 \
	--disable-armv6t2 \
	--disable-armvfp \
	--disable-iwmmxt \
	--disable-mmi \
	--disable-neon \
	--disable-vis \
	--disable-yasm \
	$(FFMPEG_DEBUG_FLAG) \
	--disable-gprof \
	--enable-extra-warnings \
	--disable-stripping
