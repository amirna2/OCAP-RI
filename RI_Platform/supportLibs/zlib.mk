
#
# zlib configure arguments
#
ZLIB_CONFIGURE = \
	CC=$(RI_CC) \
	./configure \
	--prefix=$(PLATFORM_INST_DIR) \
	--shared
