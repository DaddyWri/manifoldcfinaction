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
package org.apache.manifoldcf.agents.output.docs4u;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.system.Logging;
import org.apache.manifoldcf.core.database.BaseTable;

import java.util.*;
import java.io.*;

/** This class manages the usergrouplookup database table, which is part of the Docs4U output
* connector.
*/
public class UserGroupLookupManager extends BaseTable
{
  protected final static String DATABASE_TABLE_NAME = "usergrouplookup";
  
  protected final static String repositoryRootField = "repositoryroot";
  protected final static String userGroupNameField = "usergroupname";
  protected final static String userGroupIDField = "usergroupid";
  protected final static String expirationTimeField = "expirationtime";
  
  /** The overall table cache key */
  protected final static String TABLE_CACHEKEY = "table-"+DATABASE_TABLE_NAME;
  /** The prefix of the per-key cache key */
  protected final static String KEY_CACHEKEY_PREFIX = TABLE_CACHEKEY + "-";
  /** The overall table cache key set */
  protected final static StringSet tableCacheKeySet = new StringSet(TABLE_CACHEKEY);
  
  /** The thread context */
  protected IThreadContext threadContext;
  
  /** Constructor */
  public UserGroupLookupManager(IThreadContext threadContext)
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
    columnMap.put(repositoryRootField,new ColumnDescription("VARCHAR(255)",false,false,null,null,false));
    columnMap.put(userGroupNameField,new ColumnDescription("VARCHAR(255)",false,false,null,null,false));
    columnMap.put(userGroupIDField,new ColumnDescription("VARCHAR(32)",false,false,null,null,false));
    columnMap.put(expirationTimeField,new ColumnDescription("BIGINT",false,false,null,null,false));
    performCreate(columnMap,null);
    // Create a unique index for repository root and user/group name
    performAddIndex(null,new IndexDescription(true,new String[]{repositoryRootField,userGroupNameField}));
    // Create an index for the expiration time, so we can efficiently flush expired rows
    performAddIndex(null,new IndexDescription(false,new String[]{expirationTimeField}));
  }

  /** Delete the database table */
  public void destroy()
    throws ManifoldCFException
  {
    performDrop(tableCacheKeySet);
  }

  /**
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
  */  
  
  /** Construct a cache key for the given lookup key */
  protected static String makeKeyCacheKey(String key)
  {
    return KEY_CACHEKEY_PREFIX + key;
  }
  
}
