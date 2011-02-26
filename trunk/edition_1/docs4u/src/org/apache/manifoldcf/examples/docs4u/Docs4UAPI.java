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
package org.apache.manifoldcf.examples.docs4u;

import java.util.*;

/** API of the docs4u content management system.
*/
public interface Docs4UAPI
{
  
  // Basic system
  
  /** Create the instance.
  */
  public void install()
    throws D4UException;
  
  /** Remove the instance.
  */
  public void uninstall()
    throws D4UException;
  
  // User/group methods
  
  /** Create a user or group.
  *@param name is the user or group's name.
  *@param loginID is the user's login ID, null if this is a group.
  *@param groups are the group IDs.
  *@return the user/group ID.
  */
  public String createUserOrGroup(String name, String loginID, String[] groups)
    throws D4UException;
  
  /** Update a user or group.
  *@param userGroupID is the user or group ID.
  *@param name is the user or group's name.
  *@param loginID is the user's login ID, null if this is a group.
  *@param groups are the group IDs.
  */
  public void updateUserOrGroup(String userGroupID, String name, String loginID, String[] groups)
    throws D4UException;
  
  /** Get a user or group's name.
  *@param userGroupID is the user or group ID.
  *@return the name, or null if the ID did not exist.
  */
  public String getUserOrGroupName(String userGroupID)
    throws D4UException;
  
  /** Get a user or group's groups.
  *@param userGroupID is the user or group ID.
  *@return the group id's, or null if the user or group does not exist.
  */
  public String[] getUserOrGroupGroups(String userGroupID)
    throws D4UException;
    
  /** Delete a user or group.
  *@param userGroupID is the user or group ID.
  */
  public void deleteUserOrGroup(String userGroupID)
    throws D4UException;
    
  // Document methods
  
  /** Find documents.
  *@param startTime is the starting timestamp in ms since epoch, or null if none.
  *@param endTime is the ending timestamp in ms since epoch, or null if none.
  *@param metadataMap is a map of metadata name to desired value.
  *@return the iterator of document identifiers matching all the criteria.
  */
  public D4UDocumentIterator findDocuments(Long startTime, Long endTime, Map metadataMap)
    throws D4UException;
  
  /** Create a document.
  *@param docInfo is the document info structure.  Note that it is the responsibility
  * of the caller to close the docInfo object when they are done with it.
  *@return the new document identifier.
  */
  public String createDocument(D4UDocInfo docInfo)
    throws D4UException;
    
  /** Update a document.
  *@param docID is the document identifier.
  *@param docInfo is the updated document information.
  */
  public void updateDocument(String docID, D4UDocInfo docInfo)
    throws D4UException;
    
  /** Find a document.
  *@param docID is the document identifier.
  *@param docInfo is the document information object to be filled in.  Note that
  * it is the responsibility of the caller to close the docInfo object when they are done
  * with it.
  *@return true if document exists, false otherwise.
  */
  public boolean getDocument(String docID, D4UDocInfo docInfo)
    throws D4UException;
  
  /** Get a document's last updated timestamp.
  *@param docID is the document identifier.
  *@return the timestamp, in ms since epoch, or null if the document doesn't exist.
  */
  public Long getDocumentUpdatedTime(String docID)
    throws D4UException;
    
  /** Delete a document.
  *@param docID is the document identifier.
  */
  public void deleteDocument(String docID)
    throws D4UException;
    
}
