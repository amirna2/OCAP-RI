package org.cablelabs.impl.snmp.agentx.util;

import java.util.HashMap;

public class AgentXTransactionManager
{
    private HashMap myTransactions;
    
    /**
     * Get Map for testing purposes
     * @return
     */
    protected HashMap getMap()
    {
        return myTransactions;
    }
    
    /**
     * Set Map for testing purposes
     * @return
     */
    protected void setMap(HashMap transactions)
    {
        myTransactions = transactions;
    }
    
    public AgentXTransactionManager()
    {
        myTransactions = new HashMap();
    }
    
    public AgentXTransaction startTransaction(long transactionId)
    {
        AgentXTransaction transaction = new AgentXTransaction(transactionId);
        myTransactions.put(new Integer((int) transactionId), transaction);
        return transaction;
    }
    
    public AgentXTransaction getTransaction(long transactionId)
    {
        return (AgentXTransaction)myTransactions.get(new Integer((int)transactionId));
    }
    
    public void endTransaction(long transactionId)
    {
        myTransactions.remove(new Integer((int) transactionId));
    }
}
