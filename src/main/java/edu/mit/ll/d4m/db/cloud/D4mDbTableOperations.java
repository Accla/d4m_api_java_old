package edu.mit.ll.d4m.db.cloud;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Logger;



//import org.apache.accumulo.core.client.IteratorSetting;
//import org.apache.accumulo.core.data.Key;
//import org.apache.accumulo.core.data.Value;
//import org.apache.accumulo.core.iterators.Combiner;
//import org.apache.accumulo.core.iterators.IteratorEnvironment;
//import org.apache.accumulo.core.iterators.IteratorUtil;
//import org.apache.accumulo.core.iterators.LongCombiner;
//import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
//import org.apache.accumulo.core.iterators.TypedValueCombiner;
//import org.apache.accumulo.core.iterators.ValueFormatException;
//import org.apache.accumulo.core.iterators.conf.ColumnSet;
//import org.apache.accumulo.core.iterators.user.MaxCombiner;
//import org.apache.accumulo.core.iterators.user.MinCombiner;
//import org.apache.accumulo.core.iterators.user.SummingCombiner;
//import org.apache.accumulo.core.tabletserver.thrift.TabletStats;
//import org.apache.accumulo.core.util.ArgumentChecker;
//import org.apache.accumulo.core.util.Pair;
import org.apache.hadoop.io.Text;



//import edu.mit.ll.cloud.connection.AccumuloConnection;
import edu.mit.ll.cloud.connection.ConnectionProperties;
import edu.mit.ll.d4m.db.cloud.util.D4mQueryUtil;
import edu.mit.ll.d4m.db.cloud.util.ArgumentChecker;



/**
 * @author wi20909
 */
public class D4mDbTableOperations extends D4mParent {
	private static  Logger log = Logger.getLogger(D4mDbTableOperations.class.getName());
	public String rowReturnString = "";
	public String columnReturnString = "";
	public String valueReturnString = "";
	private long timeout=120000l; //120s
	private ConnectionProperties connProps = new ConnectionProperties();

	D4mTableOpsIF d4mTableOp = null;
	public D4mDbTableOperations() {
		super();
	}

	public D4mDbTableOperations(ConnectionProperties connProps) {
		super();
		this.connProps = connProps;
	}
	public D4mDbTableOperations(String instanceName, String host, String username, String password) {
		super();
		this.connProps.setHost(host);
		this.connProps.setInstanceName(instanceName);
		this.connProps.setUser(username);
		this.connProps.setPass(password);

	}

	public D4mDbTableOperations(String instanceName, String host, String username, String password, String cloudType) {
		super();
		init(instanceName, host,username,password,cloudType);

	}

	public void init(String instanceName, String host, String username, String password,String cloudType) {
		this.connProps.setHost(host);
		this.connProps.setInstanceName(instanceName);
		this.connProps.setUser(username);
		this.connProps.setPass(password);
		setCloudType(cloudType);
		//		doInit();
	}
	private void doInit() throws Exception{
		String instanceName = this.connProps.getInstanceName();
		String host = this.connProps.getHost();
		String username = this.connProps.getUser();
		String password = this.connProps.getPass();
		if(d4mTableOp == null)
			d4mTableOp = D4mFactory.createTableOperations(instanceName, host, username, password);

	}

	public void createTable(String tableName) throws Exception{
		doInit();
		this.d4mTableOp.createTable(tableName);
	}

	public void deleteTable(String tableName) throws Exception{
		doInit();
		this.d4mTableOp.deleteTable(tableName);
	}

	/*
	 *  Get the total number of entries for the specified table names
	 *  tableNames   list of table names of interest	
	 */
	public long getNumberOfEntries(ArrayList<String>  tableNames)  throws Exception{
		doInit();
		long retVal= this.d4mTableOp.getNumberOfEntries(tableNames);

		return retVal;
	}

	/**
	 * Designates columns (which do not have to exist yet) with a Combiner. 
	 * Note: Do not add more than one combiner on a column.
	 * @param tableName
	 * @param columnStrAll In the format: "col1,col2,col3," where ',' can be any separator
	 * @param combineType "SUM", "MIN", or "MAX" or "SUM_DECIMAL", "MIN_DECIMAL", "MAX_DECIMAL"
	 * @param columnFamily An optional column family (default = "")
	 * @throws D4mException if table doesn't exist, user doesn't have permissions, or something else goes wrong
	 */
	public void designateCombiningColumns(String tableName, String columnStrAll, String combineType, String columnFamily) throws Exception
    {
		doInit();
		this.d4mTableOp.designateCombiningColumns(tableName, columnStrAll, combineType, columnFamily);
	}
	/**
	 * 
	 * @param tableName
	 * @return A nice tabular view of the Combiners present with each column they are active on in the given table
	 * @throws Exception if table doesn't exist, user doesn't have permissions, or something else goes wrong
	 */
	public String listCombiningColumns(String tableName) throws Exception
	{
		doInit();
		return this.d4mTableOp.listCombiningColumns(tableName);
	}

	/**
	 * Removes whatever Combiner is present on the given columns in the given table.
	 * Note: will silently ignore specified columns that do not have combiners present.
	 * @param tableName
	 * @param columnStr In the format: "col1,col2,col3," where ',' can be any separator
	 * @throws D4mException if table doesn't exist, user doesn't have permissions, or something else goes wrong
	 */
	public void revokeCombiningColumns(String tableName, String columnStr, String columnFamily) throws Exception
	{
		doInit();
		this.d4mTableOp.revokeCombiningColumns(tableName, columnStr, columnFamily);
	}

	/**
	 * Adds the splits specified to the table (does nothing if they are already there)
	 * @param tableName
	 * @param splitsStr In the format: "row1,row2,row3," where ',' can be any separator
	 * @throws D4mException 
	 */
	public void addSplits(String tableName, String splitsStr) throws Exception
	{
		ArgumentChecker.notNull(tableName, splitsStr);
		doInit();
		String[] splitStrArr = D4mQueryUtil.processParam(splitsStr);

		this.d4mTableOp.splitTable(tableName, splitStrArr);

	}


	/**
	 * @param tableName  name of table to find splits information
	 * @return  String [0]  name of splits
	 *          String [1]  number of splits per split name
	 *          String [2]  name of tablet servers that contain the splits
	 *          
	 */
	public String[] getAllSplitsInfo(String tableName) throws Exception {
	    doInit();
		String []  results = new String[]{"","",""};

		List<String>  splitNames = this.d4mTableOp.getSplits(tableName);
		if(splitNames.isEmpty() ) return results;

		try {
			List<String>  listNumSplits = this.d4mTableOp.getSplitsNumInEachTablet(tableName);
			List<String>  listTabletOfSplits = this.d4mTableOp.getTabletLocationsForSplits(tableName, splitNames);

			results[0] = concatStringListToCommaSeparatedString(splitNames);
			results[1] = concatStringListToCommaSeparatedString(listNumSplits);
			results[2] = concatStringListToCommaSeparatedString(listTabletOfSplits);


		} catch (D4mException e) {
			e.printStackTrace();
		}


		return results;
	}
	/*
	 *   Returns only the names of the splits as comma-delimited string
	 */
	public String []  getSplits(String tableName) throws Exception {
		return getSplits(tableName, true);
	}

	/*
	 *  Return the list of split names a comma-delimited string
	 *  eg   124,234,2334,5664,
	 */	
	public String getSplitsString(String tableName) throws Exception {
		String result = "";
		List<String> splitList = this.d4mTableOp.getSplits(tableName);
		result = concatStringListToCommaSeparatedString(splitList);
		return result;
		
	}
	/**
	 * Gets the current splits or the table.  If the optional second boolean is true, returns an additional comma-delimited string that holds N+1 numbers
	 * where N is the number of splits and the (i)th number is the number of entries in tablet holding the (i-1)st split and the (i)th split.
	 * @param tableName
	 * @param getNumInEachTablet Optional 2nd boolean - default false
	 * @return An array of strings where 
	 * 		index 0 holds a string of the split names (comma-delimited)
	 *      index 1 holds the number of splits per split name
	 *      index 2 holds the name of tablet servers of each split
	 *      eg [0]  1122,1223,233,4444,
	 *         [1]   1,3,4,5,
	 *         [2]   host1,host2,host3,host4,
	 * @throws Exception
	 */
	public String[] getSplits(String tableName, boolean getNumInEachTablet) throws Exception {
		ArgumentChecker.notNull(tableName);
		doInit();

		String [] results = getAllSplitsInfo(tableName);
		return results;

	}
	public String[] SAVE_getSplits(String tableName, boolean getNumInEachTablet) throws Exception
	{
		ArgumentChecker.notNull(tableName);
		doInit();
		List<String> splitList = this.d4mTableOp.getSplits(tableName,getNumInEachTablet );

		String [] result = null;
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		boolean isFlag=false;
		for(String split: splitList) {
			if(split.equals(":") ) { 
				isFlag= true;
				continue;
			}

			if(!split.equals(":") && !isFlag)
				sb1.append(split).append(",");

			if(isFlag) {
				//Get number of entries per tablet
				sb2.append(split).append(",");
			}
		}
		if(sb2.length() ==0) {
			result = new String[] {sb1.toString()};
		} else {
			result = new String[2];
			result[0] = sb1.toString();
			result[1] = sb2.toString();
		}
		return result;
	}

	/**
	 * Get the number of splits in each tablet.
	 * N+1 numbers where N is the number of splits and the (i)th number is the number of entries in
	 *  tablet holding the (i-1)st split and the (i)th split.
	 * Return a comma-delimited list
	 *  @param tableName
	 */
	public String [] getSplitsNumInEachTablet(String tableName) throws Exception {
		List<String> list = this.d4mTableOp.getSplitsNumInEachTablet(tableName);
		StringBuffer sb = new StringBuffer();
		for(String s : list) {
			sb.append(s).append(",");
		}
		String [] result = new String [1];
		result[0] = sb.toString();
		return result;
	}

	/**
	 * Merge tablets between (startRow, endRow] on the table. 
	 * @param tableName
	 * @param startRow single row name or the empty string/null to start at first tablet server
	 * @param endRow single row name or the empty string/null to end at last tablet server
	 */
	public void mergeSplits(String tableName, String startRow, String endRow) throws Exception
	{
		ArgumentChecker.notNull(tableName);
		doInit();
		if (startRow != null && startRow.isEmpty())
			startRow = null;
		if (endRow != null && endRow.isEmpty())
			endRow = null;
		this.d4mTableOp.merge(tableName, startRow, endRow);
	}

	/**
	 * Ensures that newSplitsString represents the state of splits of the table by merging away any splits present in the table not in newSplitsString.
	 * Merges away all splits if newSplitsString is null or empty
	 * @param tableName
	 * @param newSplitsString
	 * @throws Exception TableNotFoundException
	 */
	public void putSplits(String tableName, String newSplitsString) throws Exception // TableNotFoundException
	{
		ArgumentChecker.notNull(tableName);
		doInit();
		if (newSplitsString == null || newSplitsString.isEmpty()) {
			mergeSplits(tableName, null, null);
			return;
		}
		String oldSplitsString = getSplitsString(tableName);

		List<String> newSplitsList = Arrays.asList(D4mQueryUtil.processParam(newSplitsString));
		NavigableSet<String> oldSplitsSet = new TreeSet<String>();

		if (!oldSplitsString.isEmpty())
			oldSplitsSet.addAll(Arrays.asList(D4mQueryUtil.processParam(oldSplitsString)));

		// algorithm: first go through old list and merge anything not in new
		// then add the new set
		for (Iterator<String> iter = oldSplitsSet.iterator(); iter.hasNext(); ) {
			String oldSplit = iter.next();
			if (!newSplitsList.contains(oldSplit)) {
				// merge away oldSplit
				String before = oldSplitsSet.lower(oldSplit);
				String after  = oldSplitsSet.higher(oldSplit); // might be null for either or both
				mergeSplits(tableName, before, after);
				iter.remove(); // remove from oldSplitsSet now that we merged the split away
			}
		}
		addSplits(tableName, newSplitsString);

	}

	/*
	 * Concatenate the string to a comma-delimited string
	 */
	private String  concatStringListToCommaSeparatedString(List<String> strList) {
		StringBuffer sb = new StringBuffer();

		for(int i = 0; i < strList.size() ; i++) {
			String s = strList.get(i);
			sb.append(s).append(",");
		}
		return sb.toString();
	}


}
/*
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% D4M: Dynamic Distributed Dimensional Data Model
% MIT Lincoln Laboratory
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% (c) <2010> Massachusetts Institute of Technology
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 */

