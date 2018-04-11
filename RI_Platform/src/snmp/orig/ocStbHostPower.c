/*
 * Note: this file originally auto-generated by mib2c using
 *        : mib2c.scalar.conf 11805 2005-01-07 09:37:18Z dts12 $
 */

#include <net-snmp/net-snmp-config.h>
#include <net-snmp/net-snmp-includes.h>
#include <net-snmp/agent/net-snmp-agent-includes.h>
#include "ocStbHostPower.h"

/** Initializes the ocStbHostPower module */
void
init_ocStbHostPower(void)
{
    static oid ocStbHostPowerStatus_oid[] = { 1,3,6,1,4,1,4491,2,3,1,1,4,1,1 };
    static oid ocStbHostAcOutletStatus_oid[] = { 1,3,6,1,4,1,4491,2,3,1,1,4,1,2 };

  DEBUGMSGTL(("ocStbHostPower", "Initializing\n"));

    netsnmp_register_scalar(
        netsnmp_create_handler_registration("ocStbHostPowerStatus", handle_ocStbHostPowerStatus,
                               ocStbHostPowerStatus_oid, OID_LENGTH(ocStbHostPowerStatus_oid),
                               HANDLER_CAN_RONLY
        ));
    netsnmp_register_scalar(
        netsnmp_create_handler_registration("ocStbHostAcOutletStatus", handle_ocStbHostAcOutletStatus,
                               ocStbHostAcOutletStatus_oid, OID_LENGTH(ocStbHostAcOutletStatus_oid),
                               HANDLER_CAN_RONLY
        ));
}

int
handle_ocStbHostPowerStatus(netsnmp_mib_handler *handler,
                          netsnmp_handler_registration *reginfo,
                          netsnmp_agent_request_info   *reqinfo,
                          netsnmp_request_info         *requests)
{
    /* We are never called for a GETNEXT if it's registered as a
       "instance", as it's "magically" handled for us.  */

    /* a instance handler also only hands us one request at a time, so
       we don't need to loop over a list of requests; we'll only get one. */
    
    switch(reqinfo->mode) {

        case MODE_GET:
            snmp_set_var_typed_value(requests->requestvb, ASN_INTEGER,
                                     (u_char *) /* XXX: a pointer to the scalar's data */,
                                     /* XXX: the length of the data in bytes */);
            break;


        default:
            /* we should never get here, so this is a really bad error */
            snmp_log(LOG_ERR, "unknown mode (%d) in handle_ocStbHostPowerStatus\n", reqinfo->mode );
            return SNMP_ERR_GENERR;
    }

    return SNMP_ERR_NOERROR;
}
int
handle_ocStbHostAcOutletStatus(netsnmp_mib_handler *handler,
                          netsnmp_handler_registration *reginfo,
                          netsnmp_agent_request_info   *reqinfo,
                          netsnmp_request_info         *requests)
{
    /* We are never called for a GETNEXT if it's registered as a
       "instance", as it's "magically" handled for us.  */

    /* a instance handler also only hands us one request at a time, so
       we don't need to loop over a list of requests; we'll only get one. */
    
    switch(reqinfo->mode) {

        case MODE_GET:
            snmp_set_var_typed_value(requests->requestvb, ASN_INTEGER,
                                     (u_char *) /* XXX: a pointer to the scalar's data */,
                                     /* XXX: the length of the data in bytes */);
            break;


        default:
            /* we should never get here, so this is a really bad error */
            snmp_log(LOG_ERR, "unknown mode (%d) in handle_ocStbHostAcOutletStatus\n", reqinfo->mode );
            return SNMP_ERR_GENERR;
    }

    return SNMP_ERR_NOERROR;
}
