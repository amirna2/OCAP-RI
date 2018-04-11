/*
 * Note: this file originally auto-generated by mib2c using
 *        : mib2c.create-dataset.conf 9375 2004-02-02 19:06:54Z rstory $
 */

#include <net-snmp/net-snmp-config.h>
#include <net-snmp/net-snmp-includes.h>
#include <net-snmp/agent/net-snmp-agent-includes.h>
#include "ocStbHostInBandTunerTable.h"

/** Initialize the ocStbHostInBandTunerTable table by defining its contents and how it's structured */
void
initialize_table_ocStbHostInBandTunerTable(void)
{
    static oid ocStbHostInBandTunerTable_oid[] = {1,3,6,1,4,1,4491,2,3,1,1,1,2,7,1};
    size_t ocStbHostInBandTunerTable_oid_len = OID_LENGTH(ocStbHostInBandTunerTable_oid);
    netsnmp_table_data_set *table_set;

    /* create the table structure itself */
    table_set = netsnmp_create_table_data_set("ocStbHostInBandTunerTable");

    /* comment this out or delete if you don't support creation of new rows */
    table_set->allow_creation = 1;

    /***************************************************
     * Adding indexes
     */
    DEBUGMSGTL(("initialize_table_ocStbHostInBandTunerTable",
                "adding indexes to table ocStbHostInBandTunerTable\n"));
    netsnmp_table_set_add_indexes(table_set,
                           ASN_UNSIGNED,  /* index: ocStbHostAVInterfaceIndex */
                           0);

    DEBUGMSGTL(("initialize_table_ocStbHostInBandTunerTable",
                "adding column types to table ocStbHostInBandTunerTable\n"));		 
    netsnmp_table_set_multi_add_default_row(table_set,
                                            COLUMN_OCSTBHOSTINBANDTUNERMODULATIONMODE, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERFREQUENCY, ASN_UNSIGNED, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERINTERLEAVER, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERPOWER, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERAGCVALUE, ASN_UNSIGNED, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERSNRVALUE, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERUNERROREDS, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERCORRECTEDS, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERUNCORRECTABLES, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERCARRIERLOCKLOST, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERPCRERRORS, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERPTSERRORS, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERSTATE, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERBER, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERSECSSINCELOCK, ASN_UNSIGNED, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNEREQGAIN, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERMAINTAPCOEFF, ASN_INTEGER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERTOTALTUNECOUNT, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERTUNEFAILURECOUNT, ASN_COUNTER, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERTUNEFAILFREQ, ASN_UNSIGNED, 0,
                                            NULL, 0,
                                            COLUMN_OCSTBHOSTINBANDTUNERBANDWIDTH, ASN_INTEGER, 0,
                                            NULL, 0,
                              0);
    
    /* registering the table with the master agent */
    /* note: if you don't need a subhandler to deal with any aspects
       of the request, change ocStbHostInBandTunerTable_handler to "NULL" */
    netsnmp_register_table_data_set(netsnmp_create_handler_registration("ocStbHostInBandTunerTable", ocStbHostInBandTunerTable_handler,
                                                        ocStbHostInBandTunerTable_oid,
                                                        ocStbHostInBandTunerTable_oid_len,
                                                        HANDLER_CAN_RWRITE),
                            table_set, NULL);
}

/** Initializes the ocStbHostInBandTunerTable module */
void
init_ocStbHostInBandTunerTable(void)
{

  /* here we initialize all the tables we're planning on supporting */
    initialize_table_ocStbHostInBandTunerTable();
}

/** handles requests for the ocStbHostInBandTunerTable table, if anything else needs to be done */
int
ocStbHostInBandTunerTable_handler(
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
