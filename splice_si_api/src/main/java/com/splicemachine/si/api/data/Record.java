package com.splicemachine.si.api.data;

import org.apache.spark.sql.catalyst.expressions.UnsafeRow;

/**
 *
 *
 *
 */
public interface Record<T> {
    /**
     *
     * Txn ID 1
     *
     * @return
     */
    long getTxnId1();

    /**
     *
     * Txn ID 1
     *
     * @return
     */
    void setTxnId1(long transactionId1);

    /**
     *
     * Version Number of Updates, required due to
     * read committed nature of select for update functionality.
     *
     * @return
     */
    long getVersion();

    /**
     *
     * Version Number of Updates, required due to
     * read committed nature of select for update functionality.
     *
     * @return
     */
    void setVersion(long version);

    /**
     *
     * Tombstone Marker For Deletes
     *
     */
    boolean hasTombstone();

    /**
     *
     * Tombstone Marker For Deletes
     *
     */
    void setHasTombstone(boolean hasTombstone);

    /**
     *
     * Txn ID representing the parent timestamp of a hierarchical
     * transaction or the increment plus node id of a collapsible transaction.
     *
     * @return
     */
    long getTxnId2();


    /**
     *
     * Txn ID representing the parent timestamp of a hierarchical
     * transaction or the increment plus node id of a collapsible transaction.
     *
     * @return
     */
    void setTxnId2(long transactionId2);

    /**
     *
     * The effective commit timestamp of the data, -1 if rolledback
     *
     * @return
     */
    long getEffectiveTimestamp();

    /**
     *
     * The effective commit timestamp of the data, -1 if rolledback
     *
     * @return
     */
    void setEffectiveTimestamp(long effectiveTimestamp);


    /**
     *
     * Number of Columns
     *
     * @return
     */
    int numberOfColumns();

    /**
     *
     * Number of Columns
     *
     * @return
     */
    void setNumberOfColumns(int numberOfColumns);


    /**
     *
     * Set Actual Data
     *
     * @return
     */
    T getData();

    /**
     *
     * Set Actual Data
     *
     * @return
     */
    void setData(T data);



}