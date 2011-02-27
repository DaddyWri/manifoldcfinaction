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
import java.io.*;

/** Implementation of the API of the docs4u content management system.
*/
public class Docs4UAPIImpl implements Docs4UAPI
{
  
  protected final static String docsArea = "docs";
  protected final static String docAllowedPermissionsArea = "docallowed";
  protected final static String docDisallowedPermissionsArea = "docdisallowed";
  protected final static String docMetadataArea = "docmetadata";
  protected final static String usersArea = "users";
  protected final static String userGroupsArea = "usergroups";
  
  protected final static String idFileName = "idfile.txt";
  protected final static String idLockFileName = "idfile.lock";
  protected final static String docsLockFileName = "docs.lock";
  protected final static String usersLockFileName = "users.lock";
  
  // Member variables.
  
  /** Root folder. */
  protected File root;
  
  /** Docs folder. */
  protected File docsFolder;
  /** Doc allowed permissions folder. */
  protected File docAllowedPermissionsFolder;
  /** Doc disallowed permissions folder. */
  protected File docDisallowedPermissionsFolder;
  /** Doc metadata folder */
  protected File docMetadataFolder;
  /** Users folder */
  protected File usersFolder;
  /** User groups folder */
  protected File userGroupsFolder;
  
  /** ID generation file */
  protected File idFile;
  
  /** ID lock file */
  protected File idLockFile;
  /** Docs lock file */
  protected File docsLockFile;
  /** Users lock file */
  protected File usersLockFile;
  
  /** Random number generator. */
  protected static Random randomGenerator = new Random();
  
  /** Constructor.  All this needs to know is where the root of the system is.
  *@param root is the root directory.
  */
  public Docs4UAPIImpl(String root)
    throws D4UException
  {
    // Save the root location
    this.root = new File(root);
    this.docsFolder = new File(this.root,docsArea);
    this.docAllowedPermissionsFolder = new File(this.root,docAllowedPermissionsArea);
    this.docDisallowedPermissionsFolder = new File(this.root,docDisallowedPermissionsArea);
    this.docMetadataFolder = new File(this.root,docMetadataArea);
    this.usersFolder = new File(this.root,usersArea);
    this.userGroupsFolder = new File(this.root,userGroupsArea);
    
    this.idFile = new File(this.root,idFileName);
    
    this.idLockFile = new File(this.root,idLockFileName);
    this.docsLockFile = new File(this.root,docsLockFileName);
    this.usersLockFile = new File(this.root,usersLockFileName);
    
  }
  
  // Basic system
  
  /** Create the instance.
  */
  public void install()
    throws D4UException
  {
    // Presume the root exists; create the subdirectories.
    if (docsFolder.mkdir() == false)
      throw new D4UException("Could not create docs area");
    if (docMetadataFolder.mkdir() == false)
      throw new D4UException("Could not create doc metadata area");
    if (docAllowedPermissionsFolder.mkdir() == false)
      throw new D4UException("Could not create doc allowed permissions area");
    if (docDisallowedPermissionsFolder.mkdir() == false)
      throw new D4UException("Could not create doc disallowed permissions area");
    if (usersFolder.mkdir() == false)
      throw new D4UException("Could not create users area");
    if (userGroupsFolder.mkdir() == false)
      throw new D4UException("Could not create user groups area");
    
    if (writeValue(idFile,"0") == false)
      throw new D4UException("Could not create id file");
    
  }
  
  
  /** Remove the instance.
  */
  public void uninstall()
    throws D4UException
  {
    if (idFile.delete() == false)
      throw new D4UException("Could not delete id file");
    deleteAll(userGroupsFolder);
    deleteAll(usersFolder);
    deleteAll(docAllowedPermissionsFolder);
    deleteAll(docDisallowedPermissionsFolder);
    deleteAll(docMetadataFolder);
    deleteAll(docsFolder);
  }

  
  // User/group methods
  
  /** Create a user or group.
  *@param name is the user or group's name.
  *@param loginID is the user's login ID, null if this is a group.
  *@param groups are the group IDs.
  *@return the user/group ID.
  */
  public String createUserOrGroup(String name, String loginID, String[] groups)
    throws D4UException
  {
    // Get an ID for the new user/group
    String id = getNewID();

    updateUserOrGroup(id,name,loginID,groups);
    
    return id;
  }
  
  
  /** Update a user or group.
  *@param userGroupID is the user or group ID.
  *@param name is the user or group's name.
  *@param loginID is the user's login ID, null if this is a group.
  *@param groups are the group IDs.
  */
  public void updateUserOrGroup(String userGroupID, String name, String loginID, String[] groups)
    throws D4UException
  {
    // Each user or group is represented by a pair of files.  The first contains name and optional login ID.
    // The second contains the groups array.

    String[] userGroupFileContent = makeUserGroupFileContent(name,loginID);
    
    File[] usersLocks = new File[]{usersLockFile};
    makeLocks(usersLocks);
    try
    {
      writeValues(new File(usersFolder,userGroupID),userGroupFileContent);
      writeValues(new File(userGroupsFolder,userGroupID),groups);
    }
    finally
    {
      clearLocks(usersLocks);
    }
  }
  
  /** Get a user or group's name.
  *@param userGroupID is the user or group ID.
  *@return the name, or null if the ID did not exist.
  */
  public String getUserOrGroupName(String userGroupID)
    throws D4UException
  {
    File[] usersLocks = new File[]{usersLockFile};
    makeLocks(usersLocks);
    try
    {
      String[] userGroupFileContent = readValues(new File(usersFolder,userGroupID));
      if (userGroupFileContent == null)
        return null;
      return getUserGroupName(userGroupFileContent);
    }
    finally
    {
      clearLocks(usersLocks);
    }
  }
  
  /** Get a user or group's groups.
  *@param userGroupID is the user or group ID.
  *@return the group id's, or null if the user or group does not exist.
  */
  public String[] getUserOrGroupGroups(String userGroupID)
    throws D4UException
  {
    File[] usersLocks = new File[]{usersLockFile};
    makeLocks(usersLocks);
    try
    {
      return readValues(new File(userGroupsFolder,userGroupID));
    }
    finally
    {
      clearLocks(usersLocks);
    }
  }
    
  /** Delete a user or group.
  *@param userGroupID is the user or group ID.
  */
  public void deleteUserOrGroup(String userGroupID)
    throws D4UException
  {
    File[] usersLocks = new File[]{usersLockFile};
    makeLocks(usersLocks);
    try
    {
      new File(usersFolder,userGroupID).delete();
      new File(userGroupsFolder,userGroupID).delete();
    }
    finally
    {
      clearLocks(usersLocks);
    }
  }
    
  // Document methods
  
  /** Find documents.
  *@param startTime is the starting timestamp in ms since epoch, or null if none.
  *@param endTime is the ending timestamp in ms since epoch, or null if none.
  *@param metadataMap is a map of metadata name to desired value.
  *@return the iterator of document identifiers matching all the criteria.
  */
  public D4UDocumentIterator findDocuments(Long startTime, Long endTime, Map metadataMap)
    throws D4UException
  {
    File[] files = docMetadataFolder.listFiles();
    List includedFiles = new ArrayList();
    int i = 0;
    while (i < files.length)
    {
      File theFile = files[i++];
      long fileStamp = theFile.lastModified();
      if (startTime != null && startTime.longValue() <= fileStamp)
        continue;
      if (endTime != null && endTime.longValue() >= fileStamp)
        continue;
      if (metadataMap != null)
      {
        // Read this metadata file
        String[] content = readValues(theFile);
        D4UDocInfo stuff = new D4UDocInfoImpl();
        getMetadataContent(stuff,content);
        Iterator iter = metadataMap.keySet().iterator();
        boolean include = true;
        while (iter.hasNext())
        {
          String attributeName = (String)iter.next();
          String value = (String)metadataMap.get(attributeName);
          String[] values = stuff.getMetadata(attributeName);
          if (values == null)
          {
            include = false;
            break;
          }
          int j = 0;
          while (j < values.length)
          {
            if (values[j].equals(value))
              break;
            j++;
          }
          if (j == values.length)
          {
            include = false;
            break;
          }
        }
        if (!include)
          continue;
      }
      includedFiles.add(theFile.getName());
    }
    return new DocIterator(includedFiles);
  }
  
  /** Create a document.
  *@param docInfo is the document info structure.  Note that it is the responsibility
  * of the caller to close the docInfo object when they are done with it.
  *@return the new document identifier.
  */
  public String createDocument(D4UDocInfo docInfo)
    throws D4UException
  {
    // Get an ID for the new document
    String id = getNewID();

    updateDocument(id,docInfo);

    return id;
  }
    
  /** Update a document.
  *@param docID is the document identifier.
  *@param docInfo is the updated document information.
  */
  public void updateDocument(String docID, D4UDocInfo docInfo)
    throws D4UException
  {
    // Each document consists of four files.  The first contains metadata.  The second contains the
    // document content.  The third contains the allowed users/groups array.  The fourth contains
    // the disallowed user/groups array.

    String[] metadataContent = makeMetadataContent(docInfo);
    
    File[] docsLocks = new File[]{docsLockFile};
    makeLocks(docsLocks);
    try
    {
      try
      {
        OutputStream os = new FileOutputStream(new File(docsFolder,docID));
        try
        {
          docInfo.getData(os);
        }
        finally
        {
          os.close();
        }
      }
      catch (IOException e)
      {
        throw new D4UException(e.getMessage(),e);
      }
        
      writeValues(new File(docMetadataFolder,docID),metadataContent);
      writeValues(new File(docAllowedPermissionsFolder,docID),docInfo.getAllowed());
      writeValues(new File(docAllowedPermissionsFolder,docID),docInfo.getDisallowed());
    }
    finally
    {
      clearLocks(docsLocks);
    }
  }
  
  
  /** Find a document.
  *@param docID is the document identifier.
  *@param docInfo is the document information object to be filled in.  Note that
  * it is the responsibility of the caller to close the docInfo object when they are done
  * with it.
  *@return true if document exists, false otherwise.
  */
  public boolean getDocument(String docID, D4UDocInfo docInfo)
    throws D4UException
  {
    File[] docsLocks = new File[]{docsLockFile};
    makeLocks(docsLocks);
    try
    {
      try
      {
        InputStream is = new FileInputStream(new File(docsFolder,docID));
        try
        {
          docInfo.setData(is);
        }
        finally
        {
          is.close();
        }
      }
      catch (IOException e)
      {
        return false;
      }

      String[] metadataContent = readValues(new File(docsFolder,docID));
      if (metadataContent == null)
        throw new D4UException("Could not find document metadata");
      getMetadataContent(docInfo,metadataContent);
      String[] allowed = readValues(new File(docAllowedPermissionsFolder,docID));
      if (allowed == null)
        throw new D4UException("Could not find document allowed permissions");
      docInfo.setAllowed(allowed);
      String[] disallowed = readValues(new File(docDisallowedPermissionsFolder,docID));
      if (disallowed == null)
        throw new D4UException("Could not find document disallowed permissions");
      docInfo.setDisallowed(disallowed);

      return true;
    }
    finally
    {
      clearLocks(docsLocks);
    }

  }
  
  /** Get a document's last updated timestamp.
  *@param docID is the document identifier.
  *@return the timestamp, in ms since epoch, or null if the document doesn't exist.
  */
  public Long getDocumentUpdatedTime(String docID)
    throws D4UException
  {
    long time = new File(docsFolder,docID).lastModified();
    if (time == 0L)
      return null;
    return new Long(time);
  }
    
  /** Delete a document.
  *@param docID is the document identifier.
  */
  public void deleteDocument(String docID)
    throws D4UException
  {
    File[] docsLocks = new File[]{docsLockFile};
    makeLocks(docsLocks);
    try
    {
      new File(docsFolder,docID).delete();
      new File(docsFolder,docID).delete();
      new File(docAllowedPermissionsFolder,docID).delete();
      new File(docDisallowedPermissionsFolder,docID).delete();
    }
    finally
    {
      clearLocks(docsLocks);
    }
  }
  
  // Utility methods

  /** Get a new identifier.
  */
  protected String getNewID()
    throws D4UException
  {
    File[] idLocks = new File[]{idLockFile};
    makeLocks(idLocks);
    try
    {
      // Read the next ID from the ID file
      String value = readValue(idFile);
      if (value == null)
        throw new D4UException("Could not read ID file");
      // Increment the value
      try
      {
        int x = Integer.parseInt(value);
        x++;
        writeValue(idFile,Integer.toString(x));
      }
      catch (NumberFormatException e)
      {
        throw new D4UException("Bad number in ID file");
      }
      return value;
    }
    finally
    {
      clearLocks(idLocks);
    }
  }

  protected static String[] makeMetadataContent(D4UDocInfo docInfo)
  {
    // Format is as follows:
    // Each metadata entry consists of (1) the metadata item name, (2) a count of the number of values, and (3) the values,
    // in order.
    
    String[] metadataNames = docInfo.getMetadataNames();
    
    // First, calculate the array size we need.
    int i = 0;
    int count = 0;
    while (i < metadataNames.length)
    {
      count += 2;
      String[] values = docInfo.getMetadata(metadataNames[i++]);
      count += values.length;
    }
    
    String[] rval = new String[count];
    i = 0;
    count = 0;
    while (i < metadataNames.length)
    {
      String metadataName = metadataNames[i++];
      rval[count++] = metadataName;
      String[] values = docInfo.getMetadata(metadataName);
      rval[count++] = Integer.toString(values.length);
      int j = 0;
      while (j < values.length)
      {
        rval[count++] = values[j++];
      }
    }

    return rval;
  }
  
  protected static void getMetadataContent(D4UDocInfo docInfo, String[] content)
  {
    docInfo.clearMetadata();
    int i = 0;
    while (i < content.length)
    {
      String metadataName = content[i++];
      int size;
      if (i < content.length)
      {
        try
        {
          size = Integer.parseInt(content[i++]);
        }
        catch (NumberFormatException e)
        {
          size = 0;
        }
      }
      else
        size = 0;
      String[] values = new String[size];
      int j = 0;
      while (j < size)
      {
        String value;
        if (i < content.length)
          value = content[i++];
        else
          value = "";
        values[j++] = value;
      }
      docInfo.setMetadata(metadataName,values);
    }
  }

  /** Create user or group content. */
  protected static String[] makeUserGroupFileContent(String name, String loginID)
  {
    String[] rval;
    if (loginID != null)
    {
      rval = new String[2];
      rval[1] = loginID;
    }
    else
    {
      rval = new String[1];
    }
    rval[0] = name;
    return rval;
  }
  
  /** Get user name from user/group content */
  protected static String getUserGroupName(String[] content)
  {
    return content[0];
  }
  
  /** Get login ID from user/group content */
  protected static String getUserGroupLoginID(String[] content)
  {
    if (content.length > 1)
      return content[1];
    return null;
  }

  /** Delete an entire directory.
  */
  protected static void deleteAll(File directory)
    throws D4UException
  {
    File[] files = directory.listFiles();
    if (files == null)
      return;
    int i = 0;
    while (i < files.length)
    {
      File f = files[i++];
      if (f.isDirectory())
        deleteAll(f);
      else
      {
        if (f.delete() == false)
          throw new D4UException("Could not delete '"+f.toString()+"'");
      }
    }
    if (directory.delete() == false)
      throw new D4UException("Could not remove directory '"+directory.toString()+"'");
  }

  /** Create locks.
  */
  protected static void makeLocks(File[] lockDirs)
    throws D4UException
  {
    while (true)
    {
      if (makeLocksNoWait(lockDirs) == false)
      {
        waitRandom();
        continue;
      }
      break;
    }
  }

  /** Create locks without waiting.
  */
  protected static boolean makeLocksNoWait(File[] lockDirs)
    throws D4UException
  {
    int i = 0;
    while (i < lockDirs.length)
    {
      if (lockDirs[i].mkdir() == false)
      {
        // Assume we didn't get lock i.
        while (i > 0)
        {
          --i;
          if (lockDirs[i].delete() == false)
            throw new D4UException("Couldn't clean up locks; failing");
        }
        return false;
      }
    }
    return true;
  }
  
  /** Clear locks.
  */
  protected static void clearLocks(File[] lockDirs)
    throws D4UException
  {
    int i = 0;
    while (i < lockDirs.length)
    {
      if (lockDirs[i].delete() == false)
        throw new D4UException("Couldn't clean up locks; failing");
      i++;
    }
  }
  
  /** Wait for a random amount of time.
  */
  protected static void waitRandom()
  {
    long amt = (long)randomGenerator.nextInt(60000);
    try
    {
      Thread.sleep(amt);
    }
    catch (InterruptedException e)
    {
    }
  }
  

  /** Write a set of strings to a file.
  */
  protected static boolean writeValues(File file, String[] values)
  {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    while (i < values.length)
    {
      sb.append(values[i++]).append("\n");
    }
    return writeValue(file,sb.toString());
  }
  
  /** Read a set of strings from a file.
  */
  protected static String[] readValues(File file)
  {
    String x = readValue(file);
    if (x == null)
      return null;
    int index = 0;
    int count = 0;
    while (true)
    {
      int newIndex = x.indexOf("\n",index);
      if (newIndex == -1)
        break;
      count++;
      index = newIndex+1;
    }
    
    String[] rval = new String[count];
    index = 0;
    count = 0;
    while (true)
    {
      int newIndex = x.indexOf("\n",index);
      if (newIndex == -1)
        break;
      rval[count++] = x.substring(index,newIndex);
      index = newIndex+1;
    }
    
    return rval;
  }
  
  /** Write a string as the entire contents of a file.
  */
  protected static boolean writeValue(File file, String value)
  {
    try
    {
      OutputStream os = new FileOutputStream(file);
      try
      {
        byte[] bytes = value.getBytes("utf-8");
        os.write(bytes,0,bytes.length);
        os.flush();
        return true;
      }
      finally
      {
        os.close();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return false;
    }
  }
  
  /** Read the entire contents of a file as a string.
  */
  protected static String readValue(File file)
  {
    try
    {
      InputStream is = new FileInputStream(file);
      try
      {
        Reader r = new InputStreamReader(is,"utf-8");
        StringBuffer sb = new StringBuffer();
        while (true)
        {
          int value = r.read();
          if (value == -1)
            break;
          sb.append((char)value);
        }
        return sb.toString();
      }
      finally
      {
        is.close();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return null;
    }
  }

  protected static class DocIterator implements D4UDocumentIterator
  {
    protected List theFiles;
    protected int index = 0;
    
    public DocIterator(List files)
    {
      theFiles = files;
    }
    
    /** Check if there's another document.
    *@return true if there's more.
    */
    public boolean hasNext()
      throws D4UException
    {
      return index < theFiles.size();
    }
    
    /** Get the next document.
    *@return the next document ID.
    */
    public String getNext()
      throws D4UException
    {
      if (index == theFiles.size())
        return null;
      return (String)theFiles.get(index++);
    }

  }
  
}
