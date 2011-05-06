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
    performDrop(null);
  }

  /** Given a user/group name, look up an id.
  *@param userGroupName is the user or group name.
  *@return user/group ID, or null of the user/group name was not found.
  */
  public String lookupUserGroup(String userGroupName)
    throws ManifoldCFException
  {
    ArrayList params = new ArrayList();
    params.add(userGroupName);
    IResultSet results = performQuery("SELECT "+userGroupIDField+" FROM "+getTableName()+
      " WHERE "+userGroupNameField+"=?",params,null,null);
    if (results.getRowCount() == 0)
      return null;
    IResultRow row = results.getRow(0);
    return (String)row.getValue(userGroupIDField);
  }
  
  /** Add a user/group name and id to the table.
  *@param userGroupName is the user/group name.
  *@param userGroupID is the user/group ID.
  *@param expirationTime is the time the record expires.
  */
  public void addUserGroup(String userGroupName, String userGroupID, long expirationTime)
    throws ManifoldCFException
  {
    Map map = new HashMap();
    map.put(userGroupNameField,userGroupName);
    map.put(userGroupIDField,userGroupID);
    map.put(expirationTimeField,new Long(expirationTime));
    performInsert(map,null);
  }
  
  /** Clean out expired records.
  *@param currentTime is the current time.
  */
  public void cleanupExpiredRecords(long currentTime)
    throws ManifoldCFException
  {
    ArrayList params = new ArrayList();
    params.add(new Long(currentTime));
    performDelete("WHERE "+expirationTimeField+"<=?",params,null);
  }
  
}
