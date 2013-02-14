package org.apache.derby.impl.sql.execute.operations;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.splicemachine.derby.test.SpliceNetDerbyTest;

//public class CallStatementOperationTest extends SpliceDerbyTest {
public class CallStatementOperationTest extends SpliceNetDerbyTest {
	private static Logger LOG = Logger.getLogger(CallStatementOperationTest.class);
	
	@BeforeClass 
	public static void startup() throws Exception {
		startConnection();	
	} 


	@Test
	public void testCallSysSchemas() throws SQLException {
		LOG.info("start testCallStatement");
		Statement s = null;
		ResultSet result = null;
		try {
			CallableStatement cs = conn.prepareCall("CALL SYSIBM.SQLTABLES('', '', '', '', 'GETSCHEMAS=1')");
			//CallableStatement cs = conn.prepareCall("CALL SYSIBM.METADATA()");
			result = cs.executeQuery();
			
			//s = conn.createStatement();
			//result = s.executeQuery("SELECT SCHEMANAME AS TABLE_SCHEM, CAST(NULL AS VARCHAR(128)) AS TABLE_CATALOG FROM SYS.SYSSCHEMAS WHERE SCHEMANAME LIKE '%' ORDER BY TABLE_SCHEM");
			
			int count = 0;
			while (result.next()) {
				LOG.info("c1="+result.getString(1)+",c2="+result.getString(2));
				Assert.assertTrue(result.getBoolean(1));
				count++;
			}
			Assert.assertEquals(11, count);
		} finally {
			try {
				if (result!=null)
					result.close();
				if(s!=null)
					s.close();
			} catch (SQLException e) {
				//no need to print out
			}
		}		
	}


	@AfterClass 
	public static void shutdown() throws SQLException {
		stopConnection();		
	}
}
