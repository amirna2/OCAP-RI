/*
 * Note: this file originally auto-generated by mib2c using
 *        : mib2c.create-dataset.conf 9375 2004-02-02 19:06:54Z rstory $
 */

#include <net-snmp/net-snmp-config.h>
#include <net-snmp/net-snmp-includes.h>
#include <net-snmp/agent/net-snmp-agent-includes.h>
#include "ocStbHostSystemTempTable.h"

/** Initialize the ocStbHostSystemTempTable table by defining its contents and how it's structured */
void
initialize_table_ocStbHostSystemTempTable(void)
{
    static oid ocStbHostSystemTempTable_oid[] = {1,3,6,1,4,1,4491,2,3,1,1,4,3,1};
    size_t ocStbHostSystemTempTable_oid_len = OID_LENGTH(ocStbHostSystemTempTable_oid);
    netsnmp_table_data_set *table_set;

    /* create the table structure itself */
    table_set = netsnmp_create_table_data_set("ocStbHostSystemTempTable");

    /* comment this out or delete if you don't support creation of new rows */
    table_set->allow_creation = 1;

    /***************************************************
     * Adding indexes
     */
    DEBUGMSGTL(("initialize_table_ocStbHostSystemTempTable",
                "adding indexes to table ocStbHostSystemTempTable\n"));
    netsnmp_table_set_add_indexes(table_set,
                           ASN_INTEGER,  /* index: hrDeviceIndex */
                           ASN_UNSIGNED,  /* index: ocStbHostSystemTempIndex */
                           0);

    DEBUGMSGTL(("initialize_table_ocStbHostSystemTempTable",
                "adding column types to table ocStbHostSystemTempTable\n"));		 
    netsnmp_table_set_multi_add_default_row(table_set,
                                            COLUMN_OCSTBHOSTSYSTEMTEMPINDEX, ASN_UNSIGNED, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTSYSTEMTEMPDESCR, ASN_OCTET_STR, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTSYSTEMTEMPVALUE, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTSYSTEMTEMPLASTUPDATE, ASN_TIMETICKS, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTSYSTEMTEMPMAXVALUE, ASN_INTEGER, 0,
                                            NULL, 0,
                              0);
    
    /* registering the table with the master agent */
    /* note: if you don't need a subhandler to deal with any aspects
       of the request, change ocStbHostSystemTempTable_handler to "NULL" */
    netsnmp_register_table_data_set(netsnmp_create_handler_registration("ocStbHostSystemTempTable", ocStbHostSystemTempTable_handler,
                                                        ocStbHostSystemTempTable_oid,
                                                        ocStbHostSystemTempTable_oid_len,
                                                        HANDLER_CAN_RWRITE),
                            table_set, NULL);
}

/** Initializes the ocStbHostSystemTempTable module */
void
init_ocStbHostSystemTempTable(void)
{

  /* here we initialize all the tables we're planning on supporting */
    initialize_table_ocStbHostSystemTempTable();
}

/** handles requests for the ocStbHostSystemTempTable table, if anything else needs to be done */
int
ocStbHostSystemTempTable_handler(
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
