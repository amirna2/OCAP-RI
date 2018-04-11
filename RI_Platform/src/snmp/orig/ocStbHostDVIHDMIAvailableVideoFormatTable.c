/*
 * Note: this file originally auto-generated by mib2c using
 *        : mib2c.create-dataset.conf 9375 2004-02-02 19:06:54Z rstory $
 */

#include <net-snmp/net-snmp-config.h>
#include <net-snmp/net-snmp-includes.h>
#include <net-snmp/agent/net-snmp-agent-includes.h>
#include "ocStbHostDVIHDMIAvailableVideoFormatTable.h"

/** Initialize the ocStbHostDVIHDMIAvailableVideoFormatTable table by defining its contents and how it's structured */
void
initialize_table_ocStbHostDVIHDMIAvailableVideoFormatTable(void)
{
    static oid ocStbHostDVIHDMIAvailableVideoFormatTable_oid[] = {1,3,6,1,4,1,4491,2,3,1,1,1,2,4,2};
    size_t ocStbHostDVIHDMIAvailableVideoFormatTable_oid_len = OID_LENGTH(ocStbHostDVIHDMIAvailableVideoFormatTable_oid);
    netsnmp_table_data_set *table_set;

    /* create the table structure itself */
    table_set = netsnmp_create_table_data_set("ocStbHostDVIHDMIAvailableVideoFormatTable");

    /* comment this out or delete if you don't support creation of new rows */
    table_set->allow_creation = 1;

    /***************************************************
     * Adding indexes
     */
    DEBUGMSGTL(("initialize_table_ocStbHostDVIHDMIAvailableVideoFormatTable",
                "adding indexes to table ocStbHostDVIHDMIAvailableVideoFormatTable\n"));
    netsnmp_table_set_add_indexes(table_set,
                           ASN_INTEGER,  /* index: ocStbHostDVIHDMIAvailableVideoFormatIndex */
                           0);

    DEBUGMSGTL(("initialize_table_ocStbHostDVIHDMIAvailableVideoFormatTable",
                "adding column types to table ocStbHostDVIHDMIAvailableVideoFormatTable\n"));		 
    netsnmp_table_set_multi_add_default_row(table_set,
                                            COLUMN_OCSTBHOSTDVIHDMIAVAILABLEVIDEOFORMATINDEX, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTDVIHDMIAVAILABLEVIDEOFORMAT, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTDVIHDMISUPPORTED3DSTRUCTURES, ASN_OCTET_STR, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTDVIHDMIACTIVE3DSTRUCTURE, ASN_INTEGER, 0,
                                            NULL, 0,
                              0);
    
    /* registering the table with the master agent */
    /* note: if you don't need a subhandler to deal with any aspects
       of the request, change ocStbHostDVIHDMIAvailableVideoFormatTable_handler to "NULL" */
    netsnmp_register_table_data_set(netsnmp_create_handler_registration("ocStbHostDVIHDMIAvailableVideoFormatTable", ocStbHostDVIHDMIAvailableVideoFormatTable_handler,
                                                        ocStbHostDVIHDMIAvailableVideoFormatTable_oid,
                                                        ocStbHostDVIHDMIAvailableVideoFormatTable_oid_len,
                                                        HANDLER_CAN_RWRITE),
                            table_set, NULL);
}

/** Initializes the ocStbHostDVIHDMIAvailableVideoFormatTable module */
void
init_ocStbHostDVIHDMIAvailableVideoFormatTable(void)
{

  /* here we initialize all the tables we're planning on supporting */
    initialize_table_ocStbHostDVIHDMIAvailableVideoFormatTable();
}

/** handles requests for the ocStbHostDVIHDMIAvailableVideoFormatTable table, if anything else needs to be done */
int
ocStbHostDVIHDMIAvailableVideoFormatTable_handler(
    netsnmp_mib_handler               *handler,
    netsnmp_handler_registration      *reginfo,
    netsnmp_agent_request_info        *reqinfo,
    netsnmp_request_info              *requests) {
    /* perform anything here that you need to do.  The requests have
       already been processed by the master table_dataset handler, but
       this gives you chance to act on the request in some other way
       if need be. */
    return SNMP_ERR_NOERROR;
}