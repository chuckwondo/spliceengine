package com.splicemachine.derby.impl.db;

import javax.security.auth.login.Configuration;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.splicemachine.db.impl.ast.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.splicemachine.access.api.SConfiguration;
import com.splicemachine.access.configuration.AuthenticationConfiguration;
import com.splicemachine.db.iapi.ast.ISpliceVisitor;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.reference.Property;
import com.splicemachine.db.iapi.services.context.ContextManager;
import com.splicemachine.db.iapi.services.context.ContextService;
import com.splicemachine.db.iapi.services.monitor.Monitor;
import com.splicemachine.db.iapi.services.property.PropertyFactory;
import com.splicemachine.db.iapi.sql.compile.CompilerContext;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.depend.DependencyManager;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.sql.dictionary.SchemaDescriptor;
import com.splicemachine.db.iapi.sql.execute.ExecutionFactory;
import com.splicemachine.db.iapi.store.access.AccessFactory;
import com.splicemachine.db.iapi.store.access.TransactionController;
import com.splicemachine.db.impl.db.BasicDatabase;
import com.splicemachine.db.shared.common.sanity.SanityManager;
import com.splicemachine.ddl.DDLMessage.DDLChange;
import com.splicemachine.derby.ddl.AddForeignKeyToPipeline;
import com.splicemachine.derby.ddl.AddIndexToPipeline;
import com.splicemachine.derby.ddl.AddUniqueConstraintToPipeline;
import com.splicemachine.derby.ddl.DDLAction;
import com.splicemachine.derby.ddl.DDLDriver;
import com.splicemachine.derby.ddl.DDLUtils;
import com.splicemachine.derby.ddl.DDLWatcher;
import com.splicemachine.derby.ddl.DropForeignKeyFromPipeline;
import com.splicemachine.derby.ddl.DropIndexFromPipeline;
import com.splicemachine.derby.impl.sql.execute.operations.batchonce.BatchOnceVisitor;
import com.splicemachine.derby.impl.store.access.SpliceAccessManager;
import com.splicemachine.derby.impl.store.access.SpliceTransaction;
import com.splicemachine.derby.impl.store.access.SpliceTransactionManager;
import com.splicemachine.derby.lifecycle.EngineLifecycleService;
import com.splicemachine.primitives.Bytes;
import com.splicemachine.si.api.txn.TxnView;
import com.splicemachine.si.impl.driver.SIDriver;
import com.splicemachine.utils.SpliceLogUtils;

public class SpliceDatabase extends BasicDatabase{

    private static Logger LOG=Logger.getLogger(SpliceDatabase.class);
    private AtomicBoolean registered = new AtomicBoolean(false);

    @Override
    public void boot(boolean create,Properties startParams) throws StandardException{
        Configuration.setConfiguration(null);
        SConfiguration config = SIDriver.driver().getConfiguration();
      //  System.setProperty("derby.language.logQueryPlan", Boolean.toString(true));
        if(config.debugLogStatementContext()) {
            System.setProperty("com.splicemachine.enableLegacyAsserts",Boolean.TRUE.toString());
            System.setProperty("derby.language.logStatementText",Boolean.toString(true));
        }
        if(config.debugDumpClassFile()) {
            System.setProperty("com.splicemachine.enableLegacyAsserts",Boolean.TRUE.toString());
            SanityManager.DEBUG_SET("DumpClassFile");
        }if(config.debugDumpBindTree()) {
            System.setProperty("com.splicemachine.enableLegacyAsserts",Boolean.TRUE.toString());
            SanityManager.DEBUG_SET("DumpBindTree");
        }if(config.debugDumpOptimizedTree()) {
            System.setProperty("com.splicemachine.enableLegacyAsserts",Boolean.TRUE.toString());
            SanityManager.DEBUG_SET("DumpOptimizedTree");
        }

        configureAuthentication();

        create=Boolean.TRUE.equals(EngineLifecycleService.isCreate.get()); //written like this to avoid autoboxing

        if(create){
            SpliceLogUtils.info(LOG,"Creating the Splice Machine database");
        }else{
            SpliceLogUtils.info(LOG,"Booting the Splice Machine database");
        }
        super.boot(create,startParams);
    }

    @Override
    public LanguageConnectionContext setupConnection(ContextManager cm,String user,String drdaID,String dbname,CompilerContext.DataSetProcessorType dspt)
            throws StandardException{

        final LanguageConnectionContext lctx=super.setupConnection(cm, user, drdaID, dbname, dspt);

        // If you add a visitor, be careful of ordering.

        List<Class<? extends ISpliceVisitor>> afterOptVisitors=new ArrayList<>();
        afterOptVisitors.add(UnsupportedFormsDetector.class);
        afterOptVisitors.add(AssignRSNVisitor.class);
        afterOptVisitors.add(RowLocationColumnVisitor.class);
        afterOptVisitors.add(JoinConditionVisitor.class);
        afterOptVisitors.add(FindHashJoinColumns.class);
        afterOptVisitors.add(FixSubqueryColRefs.class);
        afterOptVisitors.add(BatchOnceVisitor.class);
        afterOptVisitors.add(LimitOffsetVisitor.class);
        afterOptVisitors.add(PlanPrinter.class);

        List<Class<? extends ISpliceVisitor>> afterBindVisitors=new ArrayList<>(1);
        afterBindVisitors.add(RepeatedPredicateVisitor.class);

        List<Class<? extends ISpliceVisitor>> afterParseClasses=Collections.emptyList();
        lctx.setASTVisitor(new SpliceASTWalker(afterParseClasses, afterBindVisitors, afterOptVisitors));

        return lctx;
    }

    /**
     * This is the light creation of languageConnectionContext that removes 4 rpc calls per context creation.
     */
    public LanguageConnectionContext generateLanguageConnectionContext(TxnView txn,
                                                                       ContextManager cm,
                                                                       String user,
                                                                       String drdaID,
                                                                       String dbname,
                                                                       String sessionUserName,
                                                                       SchemaDescriptor defaultSchemaDescriptor) throws StandardException{
        TransactionController tc=((SpliceAccessManager)af).marshallTransaction(cm,txn);
        cm.setLocaleFinder(this);
        pushDbContext(cm);
        LanguageConnectionContext lctx=lcf.newLanguageConnectionContext(cm,tc,lf,this,user,drdaID,dbname,CompilerContext.DataSetProcessorType.DEFAULT_CONTROL);
        pushClassFactoryContext(cm,lcf.getClassFactory());
        ExecutionFactory ef=lcf.getExecutionFactory();
        ef.newExecutionContext(cm);
        lctx.initializeSplice(sessionUserName, defaultSchemaDescriptor);
        return lctx;
    }

    /**
     * This will perform a lookup of the user (index and main table) and the default schema (index and main table)
     * <p/>
     * This method should only be used by start() methods in coprocessors.  Do not use for sinks or observers.
     */
    public LanguageConnectionContext generateLanguageConnectionContext(TxnView txn,ContextManager cm,String user,String drdaID,String dbname, CompilerContext.DataSetProcessorType type) throws StandardException{
        TransactionController tc=((SpliceAccessManager)af).marshallTransaction(cm,txn);
        cm.setLocaleFinder(this);
        pushDbContext(cm);
        LanguageConnectionContext lctx=lcf.newLanguageConnectionContext(cm,tc,lf,this,user,drdaID,dbname,type);
        cm.setActiveThread();
        ContextService.getFactory().setCurrentContextManager(cm);

        pushClassFactoryContext(cm,lcf.getClassFactory());
        ExecutionFactory ef=lcf.getExecutionFactory();
        ef.newExecutionContext(cm);
        lctx.initialize();
        return lctx;
    }

    @Override
    public void startReplicationMaster(String dbmaster,String host,int port,String replicationMode) throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void stopReplicationMaster() throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void stopReplicationSlave() throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void failover(String dbname) throws StandardException{
        throw StandardException.plainWrapException(new SQLException("Unsupported Exception"));
    }

    @Override
    public void freeze() throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void unfreeze() throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void backupAndEnableLogArchiveMode(String backupDir,boolean deleteOnlineArchivedLogFiles,boolean wait) throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void disableLogArchiveMode(boolean deleteOnlineArchivedLogFiles) throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    @Override
    public void checkpoint() throws SQLException{
        throw new SQLException("Unsupported Exception");
    }

    protected void configureAuthentication(){
        SConfiguration configuration =SIDriver.driver().getConfiguration();
        if(configuration.authenticationNativeCreateCredentialsDatabase()) {
            System.setProperty(Property.AUTHENTICATION_NATIVE_CREATE_CREDENTIALS_DATABASE,Boolean.toString(true));
        }

        String authTypeString=configuration.getAuthentication();
        AuthenticationType authType= AuthenticationType.valueOf(authTypeString);
        switch(authType){
            case NONE:
                SpliceLogUtils.warn(LOG,"using no auth for Splice Machine");
                System.setProperty("derby.connection.requireAuthentication","false");
                System.setProperty("derby.database.sqlAuthorization","false");
                break;
            case LDAP:
                configureLDAPAuth(configuration);
                break;
            case NATIVE:
                configureNative(configuration,false);
                break;
            case CUSTOM:
                configureCustomAuth(configuration);
                break;
            default:// Default is Native with warning:
                configureNative(configuration,true);
        }
    }

    private void configureLDAPAuth(SConfiguration config){
        System.setProperty("derby.connection.requireAuthentication","true");
        System.setProperty("derby.database.sqlAuthorization","true");
        String authenticationLDAPSearchAuthDN = config.getAuthenticationLdapSearchauthdn();
        String authenticationLDAPSearchAuthPW = config.getAuthenticationLdapSearchauthpw();
        String authenticationLDAPSearchBase = config.getAuthenticationLdapSearchbase();
        String authenticationLDAPSearchFilter = config.getAuthenticationLdapSearchfilter();
        String authenticationLDAPServer = config.getAuthenticationLdapServer();
        SpliceLogUtils.info(LOG,"using LDAP to authorize Splice Machine with "+
                        "{ldap={searchAuthDN=%s,searchAuthPW=%s,searchBase=%s, searchFilter=%s}}",
                authenticationLDAPSearchAuthDN,
                authenticationLDAPSearchAuthPW,
                authenticationLDAPSearchBase,
                authenticationLDAPSearchFilter);
        System.setProperty("derby.authentication.provider","LDAP");
        System.setProperty("derby.authentication.ldap.searchAuthDN",authenticationLDAPSearchAuthDN);
        System.setProperty("derby.authentication.ldap.searchAuthPW",authenticationLDAPSearchAuthPW);
        System.setProperty("derby.authentication.ldap.searchBase",authenticationLDAPSearchBase);
        System.setProperty("derby.authentication.ldap.searchFilter",authenticationLDAPSearchFilter);
        System.setProperty("derby.authentication.server",authenticationLDAPServer);
    }

    private void configureCustomAuth(SConfiguration configuration){
        System.setProperty("derby.connection.requireAuthentication","true");
        System.setProperty("derby.database.sqlAuthorization","true");
        String authenticationCustomProvider = configuration.getAuthenticationCustomProvider();
        Level logLevel = Level.INFO;
        if(authenticationCustomProvider.equals(AuthenticationConfiguration.DEFAULT_AUTHENTICATION_CUSTOM_PROVIDER)){
            logLevel=Level.WARN;
        }
        LOG.log(logLevel,String.format("using custom authentication for SpliceMachine using class %s",authenticationCustomProvider));
        System.setProperty("derby.authentication.provider",authenticationCustomProvider);
    }

    private void configureNative(SConfiguration config,boolean warn){
        System.setProperty("derby.connection.requireAuthentication","true");
        System.setProperty("derby.database.sqlAuthorization","true");
        System.setProperty("derby.authentication.provider","NATIVE:spliceDB:LOCAL");
        String authenticationNativeAlgorithm = config.getAuthenticationNativeAlgorithm();
        System.setProperty("derby.authentication.builtin.algorithm",authenticationNativeAlgorithm);
        if(warn)
            SpliceLogUtils.warn(LOG,"authentication provider could not be determined from entry {%s},  using native",AuthenticationConfiguration.AUTHENTICATION);
    }

    @Override
    protected void bootValidation(boolean create,Properties startParams) throws StandardException{
        SpliceLogUtils.trace(LOG, "bootValidation create %s, startParams %s", create, startParams);
        pf=(PropertyFactory)Monitor.bootServiceModule(create,this,com.splicemachine.db.iapi.reference.Module.PropertyFactory,startParams);
    }

    public void registerDDL(){
        if(!registered.compareAndSet(false,true)) return; //only allow one registration
        DDLDriver.driver().ddlWatcher().registerDDLListener(new DDLWatcher.DDLListener(){
            @Override
            public void startGlobalChange(){
                System.out.println("Boot Store startGlobalChange -> ");
            }

            @Override
            public void finishGlobalChange(){
                System.out.println("Boot Store finishGlobalChange -> ");
            }

            @Override
            public void startChange(DDLChange change) throws StandardException{
                DataDictionary dataDictionary=getDataDictionary();
                DependencyManager dependencyManager=dataDictionary.getDependencyManager();
                switch(change.getDdlChangeType()){
                    case CREATE_INDEX:
                        DDLUtils.preCreateIndex(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_INDEX:
                        DDLUtils.preDropIndex(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_SEQUENCE:
                        DDLUtils.preDropSequence(change,dataDictionary,dependencyManager);
                        break;
                    case CHANGE_PK:
                    case ADD_CHECK:
                    case ADD_FOREIGN_KEY:
                    case ADD_NOT_NULL:
                    case ADD_COLUMN:
                    case ADD_PRIMARY_KEY:
                    case ADD_UNIQUE_CONSTRAINT:
                    case DROP_COLUMN:
                    case DROP_CONSTRAINT:
                    case DROP_PRIMARY_KEY:
                    case DROP_FOREIGN_KEY:
                    case DICTIONARY_UPDATE:
                    case CREATE_TABLE:
                    case CREATE_SCHEMA:
                        break;
                    case DROP_TABLE:
                        DDLUtils.preDropTable(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_VIEW:
                        DDLUtils.preDropView(change,dataDictionary,dependencyManager);
                        break;
                    case ALTER_TABLE:
                        DDLUtils.preAlterTable(change,dataDictionary,dependencyManager);
                        break;
                    case RENAME_TABLE:
                        DDLUtils.preRenameTable(change,dataDictionary,dependencyManager);
                        break;
                    case CREATE_TRIGGER:
                        DDLUtils.preCreateTrigger(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_TRIGGER:
                        DDLUtils.preDropTrigger(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_ALIAS:
                        DDLUtils.preDropAlias(change,dataDictionary,dependencyManager);
                        break;
                    case RENAME_INDEX:
                        DDLUtils.preRenameIndex(change,dataDictionary,dependencyManager);
                        break;
                    case RENAME_COLUMN:
                        DDLUtils.preRenameColumn(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_SCHEMA:
                        DDLUtils.preDropSchema(change,dataDictionary,dependencyManager);
                        break;
                    case DROP_ROLE:
                        DDLUtils.preDropRole(change,dataDictionary,dependencyManager);
                        break;
                    case TRUNCATE_TABLE:
                        DDLUtils.preTruncateTable(change,dataDictionary,dependencyManager);
                        break;
                    case REVOKE_PRIVILEGE:
                        DDLUtils.preRevokePrivilege(change,dataDictionary,dependencyManager);
                        break;
                    case ALTER_STATS:
                        DDLUtils.preAlterStats(change,dataDictionary,dependencyManager);
                        break;
                    case ENTER_RESTORE_MODE:
                        SIDriver.driver().lifecycleManager().enterRestoreMode();
                        Collection<LanguageConnectionContext> allContexts=ContextService.getFactory().getAllContexts(LanguageConnectionContext.CONTEXT_ID);
                        for(LanguageConnectionContext context : allContexts){
                            context.enterRestoreMode();
                        }
                        break;
                }
                final List<DDLAction> ddlActions = new ArrayList<>();
                ddlActions.add(new AddIndexToPipeline());
                ddlActions.add(new DropIndexFromPipeline());
                ddlActions.add(new AddForeignKeyToPipeline());
                ddlActions.add(new DropForeignKeyFromPipeline());
                ddlActions.add(new AddUniqueConstraintToPipeline());
                for (DDLAction action : ddlActions) {
                    action.accept(change);
                }
            }

            @Override
            @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",justification = "Intentional")
            public void changeSuccessful(String changeId,DDLChange change) throws StandardException{
                switch(change.getDdlChangeType()){
                    // nuttin.  Cache invalidation is done in DependencyManager thru DDLUtils calls
                }
            }

            @Override
            public void changeFailed(String changeId){
                LOG.warn("Change failed "+ changeId);
            }
        });
    }

    @Override
    protected void bootStore(boolean create,Properties startParams) throws StandardException{
        //boot the ddl environment if necessary
//        DDLEnvironment env = DDLEnvironmentLoader.loadEnvironment(SIDriver.driver().getConfiguration());

        SpliceLogUtils.trace(LOG,"bootStore create %s, startParams %s",create,startParams);
        af=(AccessFactory)Monitor.bootServiceModule(create,this,AccessFactory.MODULE,startParams);
        ((SpliceAccessManager) af).setDatabase(this);
        if(create){
            TransactionController tc=af.getTransaction(ContextService.getFactory().getCurrentContextManager());
            ((SpliceTransaction)((SpliceTransactionManager)tc).getRawTransaction()).elevate(Bytes.toBytes("boot"));
        }

    }

}
