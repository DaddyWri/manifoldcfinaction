/* $Id$ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.manifoldcf.examples;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.system.Logging;
import java.util.*;
import java.io.*;

/** This class demonstrates use of the ManifoldCF database abstraction, creating and working with
* a single database table.
*/
public class DatabaseExample extends org.apache.manifoldcf.core.database.BaseTable
{
  protected final static String DATABASE_TABLE_NAME = "example_table";
  
  protected final static String idField = "id";
  protected final static String keyField = "keyname";
  protected final static String valueField = "value";
  
  /** The overall table cache key */
  protected final static String TABLE_CACHEKEY = "table-"+DATABASE_TABLE_NAME;
  /** The prefix of the per-key cache key */
  protected final static String KEY_CACHEKEY_PREFIX = TABLE_CACHEKEY + "-";
  /** The overall table cache key set */
  protected final static StringSet tableCacheKeySet = new StringSet(TABLE_CACHEKEY);
  
  /** The thread context */
  protected IThreadContext threadContext;
  
  /** Constructor */
  public DatabaseExample(IThreadContext threadContext)
    throws ManifoldCFException
  {
    super(DBInterfaceFactory.make(threadContext,
        ManifoldCF.getMasterDatabaseName(),
        ManifoldCF.getMasterDatabaseUsername(),
        ManifoldCF.getMasterDatabasePassword()),
      DATABASE_TABLE_NAME);
    this.threadContext = threadContext;
  }
  
  /** Initialize the database table */
  public void initialize()
    throws ManifoldCFException
  {
    // Create the table
    Map columnMap = new HashMap();
    columnMap.put(idField,new ColumnDescription("BIGINT",true,false,null,null,false));
    columnMap.put(keyField,new ColumnDescription("VARCHAR(255)",false,false,null,null,false));
    columnMap.put(valueField,new ColumnDescription("VARCHAR(255)",false,true,null,null,false));
    performCreate(columnMap,null);
    // Create an index
    performAddIndex(null,new IndexDescription(true,new String[]{keyField,valueField}));
  }
  
  /** Look up all the values matching a key */
  public String[] findValues(String key)
    throws ManifoldCFException
  {
    // We will cache this against the table as a whole, and also against the
    // values for the given key.  Any changes to either will invalidate it.
    StringSet cacheKeys = new StringSet(new String[]{TABLE_CACHEKEY,makeKeyCacheKey(key)});
    // Construct the parameters
    ArrayList params = new ArrayList();
    params.add(key);
    // Perform the query
    IResultSet set = performQuery("SELECT "+valueField+" FROM "+getTableName()+
      " WHERE "+keyField+"=?",params,cacheKeys,null);
    // Assemble the results
    String[] results = new String[set.getRowCount()];
    int i = 0;
    while (i < results.length)
    {
      IResultRow row = set.getRow(i);
      results[i] = (String)row.getValue(valueField);
      i++;
    }
    return results;
  }
  
  /** Clear all values for a key */
  public void deleteKeyValues(String key)
    throws ManifoldCFException
  {
    // Prepare the parameters
    ArrayList params = new ArrayList();
    params.add(key);
    // Prepare the invalidation keys
    StringSet invalidationKeys = new StringSet(new String[]{makeKeyCacheKey(key)});
    // Perform the delete
    performDelete("WHERE "+keyField+"=?",params,invalidationKeys);
  }
  
  /** Add a value to a key */
  public void addKeyValue(String key, String value)
    throws ManifoldCFException
  {
    // Prepare the fields
    Map fields = new HashMap();
    fields.put(idField,IDFactory.make(threadContext));
    fields.put(keyField,key);
    fields.put(valueField,value);
    // Prepare the invalidation keys
    StringSet invalidationKeys = new StringSet(new String[]{makeKeyCacheKey(key)});
    performInsert(fields,invalidationKeys);
  }
  
  /** Delete all rows that have a given value */
  public void deleteValue(String value)
    throws ManifoldCFException
  {
    // Prepare the parameters
    ArrayList params = new ArrayList();
    params.add(value);
    // Prepare the invalidation keys
    StringSet invalidationKeys = new StringSet(new String[]{TABLE_CACHEKEY});
    // Perform the delete
    performDelete("WHERE "+valueField+"=?",params,invalidationKeys);
  }
  
  /** Delete the database table */
  public void destroy()
    throws ManifoldCFException
  {
    performDrop(tableCacheKeySet);
  }
  
  /** Construct a cache key for the given lookup key */
  protected static String makeKeyCacheKey(String key)
  {
    return KEY_CACHEKEY_PREFIX + key;
  }
  
  protected static String printValues(String[] values)
  {
    StringBuffer sb = new StringBuffer("{");
    int i = 0;
    while (i < values.length)
    {
      if (i > 0)
        sb.append(",");
      sb.append(values[i++]);
    }
    sb.append("}");
    return sb.toString();
  }
  
  public static void main(String[] argv)
  {
    try
    {
      // All processes must have initialization
      ManifoldCF.initializeEnvironment();
      Logging.misc.debug("System successfully initialized");
      
      // Create a thread context
      IThreadContext threadContext = ThreadContextFactory.make();
      
      // Create a table manager instance
      DatabaseExample tableManager = new DatabaseExample(threadContext);
      
      // Create the database table
      tableManager.initialize();
      try
      {
        Logging.misc.debug("Database table created");
        
        // Add some key/value pairs to table
        tableManager.addKeyValue("key1","value1");
        tableManager.addKeyValue("key1","value2");
        tableManager.addKeyValue("key2","value1");
        tableManager.addKeyValue("key2","value3");
        // Lookup and print all values for "key1" and "key2".
        System.out.println("key1 values: "+printValues(tableManager.findValues("key1")));
        System.out.println("key2 values: "+printValues(tableManager.findValues("key2")));
        // Second fetch of key1 values, which will be done via the cache
        System.out.println("key1 values again: "+printValues(tableManager.findValues("key1")));
        // Now, delete all values matching "value1"
        tableManager.deleteValue("value1");
        // Lookup and print all values for "key1" and "key2".
        System.out.println("key1 values, after delete: "+printValues(tableManager.findValues("key1")));
        System.out.println("key2 values, after delete: "+printValues(tableManager.findValues("key2")));
        // Clear values for key1
        tableManager.deleteKeyValues("key1");
        // Lookup and print all values for "key1" and "key2".
        System.out.println("key1 values, after 2nd delete: "+printValues(tableManager.findValues("key1")));
        System.out.println("key2 values, after 2nd delete: "+printValues(tableManager.findValues("key2")));
        
        Logging.misc.debug("Database exercise completed");
      }
      finally
      {
        // Clean up the database table
        tableManager.destroy();
        Logging.misc.debug("Database table destroyed");
      }
    }
    catch (ManifoldCFException e)
    {
      // Exception during initialization: Print it to standard error, and quit
      e.printStackTrace(System.err);
      System.exit(-1);
    }
  }
  
}
