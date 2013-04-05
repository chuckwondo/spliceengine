package com.splicemachine.si2.si.impl;

import com.splicemachine.si2.data.api.SDataLib;
import com.splicemachine.si2.data.api.SGet;
import com.splicemachine.si2.data.api.SRowLock;
import com.splicemachine.si2.data.api.SScan;
import com.splicemachine.si2.data.api.STable;
import com.splicemachine.si2.data.api.STableWriter;
import com.splicemachine.si2.si.api.ClientTransactor;
import com.splicemachine.si2.si.api.FilterState;
import com.splicemachine.si2.si.api.TimestampSource;
import com.splicemachine.si2.si.api.TransactionId;
import com.splicemachine.si2.si.api.Transactor;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SiTransactor implements Transactor, ClientTransactor {
    static final Logger LOG = Logger.getLogger(SiTransactor.class);

    private final TimestampSource timestampSource;
    private final SDataLib dataLib;
    private final STableWriter dataWriter;
    private final DataStore dataStore;
    private final TransactionStore transactionStore;

    public SiTransactor(TimestampSource timestampSource, SDataLib dataLib, STableWriter dataWriter,
                        DataStore dataStore, TransactionStore transactionStore) {
        this.timestampSource = timestampSource;
        this.dataLib = dataLib;
        this.dataWriter = dataWriter;
        this.dataStore = dataStore;
        this.transactionStore = transactionStore;
    }

    @Override
    public TransactionId beginTransaction(boolean allowWrites, boolean readUncommitted, boolean readCommitted)
            throws IOException {
        return beginTransactionDirect(null, null, TransactionStatus.ACTIVE, allowWrites, readUncommitted, readCommitted);
    }

    @Override
    public TransactionId beginChildTransaction(TransactionId parent, boolean dependent, boolean allowWrites,
                                               Boolean readUncommitted, Boolean readCommitted) throws IOException {
        final TransactionId childTransactionId = beginTransactionDirect(parent, dependent, null, allowWrites, readUncommitted, readCommitted);
        transactionStore.addChildToTransaction(parent, childTransactionId);
        return childTransactionId;
    }

    private TransactionId beginTransactionDirect(TransactionId parent, Boolean dependent, TransactionStatus status,
                                                 boolean allowWrites, Boolean readUncommitted, Boolean readCommitted) throws IOException {
        final SiTransactionId transactionId = new SiTransactionId(timestampSource.nextTimestamp());
        transactionStore.recordNewTransaction(transactionId, parent, dependent, allowWrites,
                readUncommitted, readCommitted, status);
        return transactionId;
    }

    @Override
    public TransactionId transactionIdFromString(String transactionId) {
        return new SiTransactionId(Long.valueOf(transactionId));
    }

    @Override
    public TransactionId getTransactionIdFromPut(Object put) {
        return dataStore.getTransactionIdFromOperation(put);
    }

    @Override
    public TransactionId getTransactionIdFromDelete(Delete delete) {
        return dataStore.getTransactionIdFromOperation(delete);
    }

    @Override
    public TransactionId getTransactionIdFromGet(Object get) {
        return dataStore.getTransactionIdFromOperation(get);
    }

    @Override
    public TransactionId getTransactionIdFromScan(Object scan) {
        return dataStore.getTransactionIdFromOperation(scan);
    }

    @Override
    public void commit(TransactionId transactionId) throws IOException {
        ensureTransactionActive(transactionId);
        final TransactionStruct transaction = transactionStore.getTransactionStatus(transactionId);
        if (transaction.parent == null || !transaction.dependent) {
            transactionStore.recordTransactionStatusChange(transactionId, TransactionStatus.COMMITTING);
            // TODO: need to sort out how to take child transactions through COMMITTING state
            final long endId = timestampSource.nextTimestamp();
            transactionStore.recordTransactionCommit(transactionId, endId, TransactionStatus.COMMITED);
            for (Long childId : transaction.children) {
                final TransactionStruct childTransaction = transactionStore.getTransactionStatus(childId);
                final TransactionStatus childStatus = childTransaction.getEffectiveStatus();
                if (childStatus == TransactionStatus.ACTIVE
                        || childTransaction.dependent
                        && (childStatus == TransactionStatus.COMMITED
                        || childStatus == TransactionStatus.COMMITTING)) {
                    commitChild(childId, endId);
                }
            }
        } else {
            // perform "local" commit only within the parent transaction
            transactionStore.recordTransactionStatusChange(transactionId, TransactionStatus.COMMITED);
        }
    }

    private void commitChild(long childId, long endId) throws IOException {
        transactionStore.recordTransactionCommit(new SiTransactionId(childId), endId, TransactionStatus.COMMITED);
    }

    @Override
    public void abort(TransactionId transactionId) throws IOException {
        TransactionStruct transaction = transactionStore.getTransactionStatus(transactionId);
        if (transaction.getEffectiveStatus().equals(TransactionStatus.ACTIVE)) {
            transactionStore.recordTransactionStatusChange(transactionId, TransactionStatus.ABORT);
        }
    }

    @Override
    public void fail(TransactionId transactionId) throws IOException {
        ensureTransactionActive(transactionId);
        transactionStore.recordTransactionStatusChange(transactionId, TransactionStatus.ERROR);
    }

    @Override
    public void initializeGet(TransactionId transactionId, SGet get) throws IOException {
        initializeGetDirect(transactionId, get);
    }

    @Override
    public void initializeGets(TransactionId transactionId, List gets) throws IOException {
        for (Object get : gets) {
            initializeGetDirect(transactionId, (SGet) get);
        }
    }

    private void initializeGetDirect(TransactionId transactionId, SGet get) {
        dataStore.setSiNeededAttribute(get);
        dataStore.setTransactionId((SiTransactionId) transactionId, get);
    }

    @Override
    public void preProcessGet(SGet get) throws IOException {
        setGetTimeRange(get);
        dataLib.setGetMaxVersions(get);
    }

    private void setGetTimeRange(SGet get) {
        dataLib.setGetTimeRange(get, 0, Long.MAX_VALUE);
    }

    @Override
    public void initializeScan(TransactionId transactionId, SScan scan) {
        dataStore.setSiNeededAttribute(scan);
        dataStore.setTransactionId((SiTransactionId) transactionId, scan);
    }

    @Override
    public void preProcessScan(SScan scan) {
        dataLib.setScanTimeRange(scan, 0L, Long.MAX_VALUE);
        dataLib.setScanMaxVersions(scan);
    }

    @Override
    public void initializePut(TransactionId transactionId, Object put) {
        dataStore.setSiNeededAttribute(put);
        dataStore.setTransactionId((SiTransactionId) transactionId, put);
    }

    @Override
    public void initializePut(Object put1, Object put2) {
        if (dataStore.getSiNeededAttribute(put1)) {
            final SiTransactionId transactionId = dataStore.getTransactionIdFromOperation(put1);
            initializePut(transactionId, put2);
        }
    }

    @Override
    public void initializeGetFromDelete(Object delete, Object put) {
        if (dataStore.getSiNeededAttribute(delete)) {
            final SiTransactionId transactionId = dataStore.getTransactionIdFromOperation(delete);
            initializePut(transactionId, put);
        }
    }

    @Override
    public Object newDeletePut(TransactionId transactionId, Object rowKey) {
        SiTransactionId siTransactionId = (SiTransactionId) transactionId;
        final Object deletePut = dataLib.newPut(rowKey);
        initializePut(siTransactionId, deletePut);
        dataStore.setTombstoneOnPut(deletePut, siTransactionId);
        dataStore.setDeletePutAttribute(deletePut);
        return deletePut;
    }

    @Override
    public boolean isDeletePut(Object put) {
        final Boolean deleteAttribute = dataStore.getDeletePutAttribute(put);
        return (deleteAttribute != null && deleteAttribute);
    }

    @Override
    public boolean processPut(STable table, Object put) throws IOException {
        Boolean siNeeded = dataStore.getSiNeededAttribute(put);
        if (siNeeded != null && siNeeded) {
            SiTransactionId transactionId = dataStore.getTransactionIdFromOperation(put);
            final TransactionStruct transaction = transactionStore.getTransactionStatus(transactionId);
            ensureTransactionAllowsWrites(transactionId);
            Object rowKey = dataLib.getPutKey(put);
            SRowLock lock = dataWriter.lockRow(table, rowKey);
            try {
                checkForConflict(transaction, table, lock, rowKey);
                Object newPut = dataStore.newLockWithPut(transactionId, put, lock);
                flagPutAsNotNeedingIndexTreatment(newPut);
                dataStore.addTransactionIdToPut(newPut, transactionId);
                dataWriter.write(table, newPut, lock);
            } finally {
                dataWriter.unLockRow(table, lock);
            }
            return true;
        } else {
            return false;
        }
    }

    private void flagPutAsNotNeedingIndexTreatment(Object newPut) {
        dataLib.addAttribute(newPut, "iu", new byte[] {});
    }

    private void checkForConflict(TransactionStruct putTransaction, STable table, SRowLock lock, Object rowKey) throws IOException {
        Long rootId = putTransaction.getRootBeginTimestamp();
        TransactionId transactionId = new SiTransactionId(putTransaction.beginTimestamp);
        List keyValues = dataStore.getCommitTimestamp(table, rowKey);
        if (keyValues != null) {
            int index = 0;
            boolean loop = true;
            while (loop) {
                if (index >= keyValues.size()) {
                    loop = false;
                } else {
                    Object c = keyValues.get(index);
                    long cellTimestamp = dataLib.getKeyValueTimestamp(c);
                    TransactionStruct transaction = transactionStore.getTransactionStatus(cellTimestamp);
                    if (transaction.getEffectiveStatus().equals(TransactionStatus.COMMITED)) {
                        if (transaction.commitTimestamp > rootId) {
                            writeWriteConflict(transactionId);
                        }
                    } else if (transaction.getEffectiveStatus().equals(TransactionStatus.ACTIVE)
                            || transaction.getEffectiveStatus().equals(TransactionStatus.COMMITTING)) {
                        // if the KeyValue was written by the current running transaction then it is not a conflict
                        if (transaction.getRootBeginTimestamp() != rootId) {
                            writeWriteConflict(transactionId);
                        }
                    }
                    index++;
                }
            }
        }
    }

    private void writeWriteConflict(TransactionId transactionId) throws IOException {
        fail(transactionId);
        throw new DoNotRetryIOException("write/write conflict");
    }

    @Override
    public Object filterResult(FilterState filterState, Object result) throws IOException {
        SiFilterState siFilterState = (SiFilterState) filterState;
        ensureTransactionActive(siFilterState.transactionId);

        List<Object> filteredCells = new ArrayList<Object>();
        final List keyValues = dataLib.listResult(result);
        if (keyValues != null) {
            Object qualifierToSkip = null;
            Object familyToSkip = null;

            for (Object keyValue : keyValues) {
                if (familyToSkip != null
                        && dataLib.valuesEqual(familyToSkip, dataLib.getKeyValueFamily(keyValue))
                        && dataLib.valuesEqual(qualifierToSkip, dataLib.getKeyValueQualifier(keyValue))) {
                    // skipping to next column
                } else {
                    familyToSkip = null;
                    qualifierToSkip = null;
                    Filter.ReturnCode returnCode = filterKeyValue(filterState, keyValue);
                    switch (returnCode) {
                        case SKIP:
                            break;
                        case INCLUDE:
                            filteredCells.add(keyValue);
                            qualifierToSkip = dataLib.getKeyValueQualifier(keyValue);
                            familyToSkip = dataLib.getKeyValueFamily(keyValue);
                            break;
                        case NEXT_COL:
                            qualifierToSkip = dataLib.getKeyValueQualifier(keyValue);
                            familyToSkip = dataLib.getKeyValueFamily(keyValue);
                            break;
                    }
                }
            }
        }
        return dataLib.newResult(dataLib.getResultKey(result), filteredCells);
    }

    private void ensureTransactionActive(TransactionId transactionId) throws IOException {
        TransactionStruct transaction = transactionStore.getTransactionStatus(transactionId);
        if (!transaction.getEffectiveStatus().equals(TransactionStatus.ACTIVE)) {
            throw new DoNotRetryIOException("transaction is not ACTIVE: " + transactionId.getTransactionID());
        }
    }

    private void ensureTransactionAllowsWrites(TransactionId transactionId) throws IOException {
        TransactionStruct transaction = transactionStore.getTransactionStatus(transactionId);
        if (!transaction.allowWrites) {
            throw new DoNotRetryIOException("transaction is read only");
        }
    }

    public boolean shouldKeep(Object keyValue, TransactionId transactionId) throws IOException {
        final long snapshotTimestamp = transactionId.getId();
        final long keyValueTimestamp = dataLib.getKeyValueTimestamp(keyValue);
        final TransactionStruct transaction = transactionStore.getTransactionStatus(keyValueTimestamp);
        switch (transaction.getEffectiveStatus()) {
            case ACTIVE:
                return snapshotTimestamp == keyValueTimestamp;
            case ERROR:
            case ABORT:
                return false;
            case COMMITTING:
                //TODO: needs special handling
                return false;
            case COMMITED:
                return snapshotTimestamp >= transaction.commitTimestamp;
        }
        throw new RuntimeException("unknown transaction status");
    }

    @Override
    public boolean isFilterNeeded(Object operation) {
        Boolean result = dataStore.getSiNeededAttribute(operation);
        if (result == null) {
            return false;
        }
        return result;
    }

    @Override
    public FilterState newFilterState(STable table, TransactionId transactionId) throws IOException {
        ensureTransactionActive(transactionId);
        return new SiFilterState(table, transactionStore.getTransactionStatus(transactionId));
    }

    @Override
    public Filter.ReturnCode filterKeyValue(FilterState filterState, Object keyValue) throws IOException {
        SiFilterState siFilterState = (SiFilterState) filterState;
        Object rowKey = dataLib.getKeyValueRow(keyValue);
        if (siFilterState.currentRowKey == null || !dataLib.valuesEqual(siFilterState.currentRowKey, rowKey)) {
            siFilterState.currentRowKey = rowKey;
            siFilterState.committedTransactions = new HashMap<Long, Long>();
            siFilterState.stillRunningTransactions = new HashSet<Long>();
            siFilterState.lastValidQualifier = null;
        }
        if (dataStore.isCommitTimestampKeyValue(keyValue)) {
            filterProcessCommitTimestamp(keyValue, siFilterState);
            return Filter.ReturnCode.SKIP;
        } else if (dataStore.isTombstoneKeyValue(keyValue)) {
            if (filterKeepDataValue(keyValue, siFilterState)) {
                siFilterState.tombstoneTimestamp = dataLib.getKeyValueTimestamp(keyValue);
                return Filter.ReturnCode.NEXT_COL;
            }
        } else if (dataStore.isDataKeyValue(keyValue)) {
            if (dataLib.valuesEqual(dataLib.getKeyValueQualifier(keyValue), siFilterState.lastValidQualifier)) {
                return Filter.ReturnCode.NEXT_COL;
            } else {
                if (siFilterState.tombstoneTimestamp != null &&
                        dataLib.getKeyValueTimestamp(keyValue) < siFilterState.tombstoneTimestamp) {
                    return Filter.ReturnCode.NEXT_COL;
                } else if (filterKeepDataValue(keyValue, siFilterState)) {
                    siFilterState.lastValidQualifier = dataLib.getKeyValueQualifier(keyValue);
                    return Filter.ReturnCode.INCLUDE;
                }
            }
        }
        return Filter.ReturnCode.SKIP;
    }

    private boolean filterKeepDataValue(Object keyValue, SiFilterState siFilterState) throws IOException {
        long dataTimestamp = dataLib.getKeyValueTimestamp(keyValue);
        Long commitTimestamp = siFilterState.committedTransactions.get(dataTimestamp);
        Boolean stillRunning = siFilterState.stillRunningTransactions.contains(dataTimestamp);
        if (commitTimestamp == null) {
            // debugging code
            final TransactionStruct transactionStatus = transactionStore.getTransactionStatus(dataTimestamp);
            commitTimestamp = transactionStatus.commitTimestamp;
            // If the transaction was committed it should have been included in the committedTransactions map
            assert (commitTimestamp == null);
        }
        if (isCommittedBeforeThisTransaction(siFilterState, commitTimestamp)
                || isThisTransactionsData(siFilterState, dataTimestamp, commitTimestamp)
                || readCommittedAndCommitted(siFilterState, commitTimestamp)
                || readDirtyAndActive(siFilterState, stillRunning)) {
            return true;
        }
        return false;
    }

    private boolean readDirtyAndActive(SiFilterState siFilterState, Boolean stillRunning) {
        return siFilterState.transactionStruct.getEffectiveReadUncommitted() && stillRunning;
    }

    private boolean readCommittedAndCommitted(SiFilterState siFilterState, Long commitTimestamp) {
        return siFilterState.transactionStruct.getEffectiveReadCommitted() && (commitTimestamp != null);
    }

    private boolean isCommittedBeforeThisTransaction(SiFilterState siFilterState, Long commitTimestamp) {
        return (commitTimestamp != null && commitTimestamp < siFilterState.transactionId.getId());
    }

    private boolean isThisTransactionsData(SiFilterState siFilterState, long dataTimestamp, Long commitTimestamp)
            throws IOException {
        final TransactionStruct dataTransaction = transactionStore.getTransactionStatus(dataTimestamp);
        final TransactionStatus dataStatus = dataTransaction.getEffectiveStatus();
        return ((dataStatus == TransactionStatus.ACTIVE
                || dataStatus == TransactionStatus.COMMITTING
                || dataStatus == TransactionStatus.COMMITED)
                && dataTransaction.getRootBeginTimestamp() == siFilterState.transactionId.getId());
    }

    private void filterProcessCommitTimestamp(Object keyValue, SiFilterState siFilterState) throws IOException {
        long beginTimestamp = dataLib.getKeyValueTimestamp(keyValue);
        Object commitTimestampValue = dataLib.getKeyValueValue(keyValue);
        Long commitTimestamp = null;
        Boolean stillRunning = false;
        if (dataStore.isSiNull(commitTimestampValue)) {
            final Object[] temp = filterHandleUnknownTransactionStatus(siFilterState, keyValue, beginTimestamp, commitTimestamp);
            commitTimestamp = (Long) temp[0];
            stillRunning = (Boolean) temp[1];
        } else {
            commitTimestamp = (Long) dataLib.decode(commitTimestampValue, Long.class);
        }
        if (commitTimestamp != null) {
            siFilterState.committedTransactions.put(beginTimestamp, commitTimestamp);
        }
        if (stillRunning) {
            siFilterState.stillRunningTransactions.add(beginTimestamp);
        }
    }

    private Object[] filterHandleUnknownTransactionStatus(SiFilterState siFilterState, Object keyValue,
                                                          long beginTimestamp, Long commitTimestamp) throws IOException {
        TransactionStruct transactionStruct = transactionStore.getTransactionStatus(beginTimestamp);
        Boolean stillRunning = false;
        switch (transactionStruct.getEffectiveStatus()) {
            case ACTIVE:
                stillRunning = true;
                break;
            case ERROR:
            case ABORT:
                cleanupErrors();
                break;
            case COMMITTING:
                //TODO: needs special handling
                stillRunning = true;
                break;
            case COMMITED:
                rollForward(siFilterState, keyValue, transactionStruct);
                commitTimestamp = transactionStruct.commitTimestamp;
                break;
        }
        return new Object[]{commitTimestamp, stillRunning};
    }

    private void rollForward(SiFilterState siFilterState, Object keyValue, TransactionStruct transactionStruct) {
        if (transactionStruct.parent == null || !transactionStruct.dependent) {
            dataStore.setCommitTimestamp(siFilterState.table, keyValue, transactionStruct.beginTimestamp, transactionStruct.commitTimestamp);
        }
    }

    private void cleanupErrors() {
        //TODO: implement this
    }
}
