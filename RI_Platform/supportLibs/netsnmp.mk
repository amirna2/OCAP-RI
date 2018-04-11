
#
# netsnmp configure arguments
#
NETSNMP_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	--enable-option-checking \
	--disable-as-needed \
	--enable-agent \
	--disable-applications \
	--disable-manuals \
	--disable-scripts \
	--disable-mibs \
	--disable-mib-config-checking \
	--disable-mib-config-debug \
	--disable-new-features \
	--disable-old-features \
	--disable-ucd-snmp-compatibility \
	--disable-mib-loading \
	--enable-des \
	--enable-privacy \
	--enable-internal-md5 \
	--enable-ipv6 \
	--disable-snmpv1 \
	--enable-snmpv2c \
	--enable-debugging \
	--enable-developer \
	--disable-testing-code \
	--disable-reentrant \
	--disable-deprecated \
	--enable-set-support \
	--disable-local-smux \
	--enable-snmptrapd-subagent \
	--enable-mini-agent \
	--disable-mfd-rewrites \
	--disable-embedded-perl \
	--disable-perl-cc-checks \
	--enable-shared \
	--disable-static \
	--enable-fast-install \
	--disable-libtool-lock \
	--without-rpm \
	--without-defaults \
	--without-opaque-special-types \
	--with-logfile=$(RICOMMONROOT)/snmp/netsnmpd.log \
	--with-persistent-directory=$(RICOMMONROOT)/snmp \
	--with-copy-persistent-files="no" \
	--with-default-snmp-version="2" \
	--with-transports="UDP TCP UDPIPv6 TCPIPv6" \
	--with-out-transports="DTLSUDP TLSTCP SSH" \
	--without-root-access \
	--without-kmem-usage \
	--without-dummy-values \
	--with-sys-contact="ocapri@cablelabs.com" \
	--with-sys-location="louisville, co" \
	--with-mib-modules="agentx agent_mibs notification examples/example" \
	--with-out-mib-modules="snmpv3mibs host/hr_network" \
	--without-perl-modules \
	--without-python-modules \
	--without-elf \
	--without-nl \
	--without-libwrap \
	--without-zlib \
	--without-bzip2 \
	--without-mysql \
	--with-openssl=internal

### 
#  --with-enterprise-oid              The enterprise number assigned to the
#                                     vendor by IANA.  See
#                                     http://www.iana.org/cgi-bin/enterprise.pl
#                                     to get one, though using the default is
#                                     probably the right choice is most cases.
#                                     (default 8072 = "enterprise.net-snmp")
#
#  --with-enterprise-sysoid           The base OID for the sysObjectID
#                                     of the system group
#                                     (default .1.3.6.1.4.1.8072.3.2... =
#                                      "netSnmpAgentOIDs...")
#
#  --with-enterprise-notification-oid The OID used for the root of
#				     enterprise specific notifications.
#                                     (default .1.3.6.1.4.1.8072.4 =
#                                      "netSnmpNotificationPrefix")
###
