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

import java.io.*;

/** This interface represents a document from the Docs4U content management system.
*/
public interface D4UDocInfo
{
  /** Write a stream of the content.  It is the caller's responsibility to close
  * the stream when finished.
  *@param dataStream the document content stream.
  */
  public void getData(OutputStream dataStream)
    throws D4UException;
  
  /** Set the content from a stream.  It is the caller's responsibility to close
  * the stream when done.
  *@param dataStream is the data stream.
  */
  public void setData(InputStream dataStream)
    throws D4UException;
  
  /** Get allowed users and groups.
  *@return the user and group IDs.
  */
  public String[] getAllowed();
  
  /** Set allowed users and groups.
  *@param userGroupIDs are the allowed user and group IDs.
  */
  public void setAllowed(String[] userGroupIDs);
  
  /** Get disallowed users and groups.
  *@return the disallowed user and group IDs.
  */
  public String[] getDisallowed();
  
  /** Set disallowed users and groups.
  *@param userGroupIDs are the disallowed user and group IDs.
  */
  public void setDisallowed(String[] userGroupIDs);
  
  /** Get a list of applicable metadata names.
  *@return the metadata names.
  */
  public String[] getMetadataNames();
  
  /** Get specified metadata.
  *@param metadataName is the name of the metadata.
  *@return the values, or null for no metadata.
  */
  public String[] getMetadata(String metadataName);
  
  /** Set specified metadata.
  *@param metadataName is the name of the metadata.
  *@param values are the metadata values, or null if none.
  */
  public void setMetadata(String metadataName, String[] values);
  
  /** Close the document info object. 
  */
  public void close()
    throws D4UException;
}
