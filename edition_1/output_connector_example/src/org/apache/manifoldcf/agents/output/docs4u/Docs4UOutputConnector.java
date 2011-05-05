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

// These are the basic interfaces we'll need from ManifoldCF
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;

// Utility includes
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

// This is where we get agent system loggers
import org.apache.manifoldcf.agents.system.Logging;

// This class implements system-wide static methods
import org.apache.manifoldcf.agents.system.ManifoldCF;

// This is the base output connector class.
import org.apache.manifoldcf.agents.output.BaseOutputConnector;

// Here's the UI helper classes.
import org.apache.manifoldcf.ui.util.Encoder;

// Here are the imports that are specific for this connector
import org.apache.manifoldcf.examples.docs4u.Docs4UAPI;
import org.apache.manifoldcf.examples.docs4u.D4UFactory;
import org.apache.manifoldcf.examples.docs4u.D4UDocInfo;
import org.apache.manifoldcf.examples.docs4u.D4UDocumentIterator;
import org.apache.manifoldcf.examples.docs4u.D4UException;

/** This is the Docs4U output connector class.  This extends the base output connectors class,
* which implements IOutputConnector, and provides some degree of insulation from future
* changes to the IOutputConnector interface.  It also provides us with basic support for
* the connector lifecycle methods, so we don't have to implement those each time.
* Note well: Output connectors should have no dependencies on classes from the 
* org.apache.manifoldcf.crawler or org.apache.manifoldcf.authorities packages.  They should only
* have dependencies on the agents and core packages.
*/
public class Docs4UOutputConnector extends BaseOutputConnector
{
  // These are the configuration parameter names
  
  /** Repository root parameter */
  protected final static String PARAMETER_REPOSITORY_ROOT = "rootdirectory";
  
  // These are the output specification node names
  
  /** Mapping regular expressions for access tokens to Docs4U user/group ID's */
  protected final static String NODE_SECURITY_MAP = "securitymap";
  /** Mapping from source metadata name to target metadata name */
  protected final static String NODE_METADATA_MAP = "metadatamap";
  /** The URL metadata name */
  protected final static String NODE_URL_METADATA_NAME = "urlmetadataname";
  
  // These are attribute names, which are shared among the nodes
  
  /** A value */
  protected final static String ATTRIBUTE_VALUE = "value";
  /** Source metadata name */
  protected final static String ATTRIBUTE_SOURCE = "source";
  /** Target metadata name */
  protected final static String ATTRIBUTE_TARGET = "target";
  
  // These are the activity names
  
  /** Save activity */
  protected final static String ACTIVITY_SAVE = "save";
  /** Delete activity */
  protected final static String ACTIVITY_DELETE = "delete";
  
  // Local constants
  
  /** Session expiration time interval */
  protected final static long SESSION_EXPIRATION_MILLISECONDS = 300000L;
  
  // Local variables.
  
  /** The root directory */
  protected String rootDirectory = null;
  
  /** The Docs4U API session */
  protected Docs4UAPI session = null;
  /** The expiration time of the Docs4U API session */
  protected long sessionExpiration = -1L;
  
  /** The UserGroupLookupManager class */
  protected UserGroupLookupManager userGroupLookupManager = null;
  
  /** Constructor */
  public Docs4UOutputConnector()
  {
    super();
  }

  /** Return the list of activities that this output connector supports (i.e. writes into the log).
  * The connector does not have to be connected for this method to be called.
  *@return the list.
  */
  public String[] getActivitiesList()
  {
    return new String[]{ACTIVITY_SAVE,ACTIVITY_DELETE};
  }

  /** Install the connector.
  * This method is called to initialize persistent storage for the connector, such as database tables etc.
  * It is called when the connector is registered.
  *@param threadContext is the current thread context.
  */
  public void install(IThreadContext threadContext)
    throws ManifoldCFException
  {
    super.install(threadContext);
    new UserGroupLookupManager(threadContext).initialize();
  }

  /** Uninstall the connector.
  * This method is called to remove persistent storage for the connector, such as database tables etc.
  * It is called when the connector is deregistered.
  *@param threadContext is the current thread context.
  */
  public void deinstall(IThreadContext threadContext)
    throws ManifoldCFException
  {
    new UserGroupLookupManager(threadContext).destroy();
    super.deinstall(threadContext);
  }

  /** Clear out any state information specific to a given thread.
  * This method is called when this object is returned to the connection pool.
  */
  public void clearThreadContext()
  {
    userGroupLookupManager = null;
    super.clearThreadContext();
  }

  /** Attach to a new thread.
  *@param threadContext is the new thread context.
  */
  public void setThreadContext(IThreadContext threadContext)
    throws ManifoldCFException
  {
    super.setThreadContext(threadContext);
    userGroupLookupManager = new UserGroupLookupManager(threadContext);
  }

  /** Output the configuration header section.
  * This method is called in the head section of the connector's configuration page.  Its purpose is to
  * add the required tabs to the list, and to output any javascript methods that might be needed by
  * the configuration editing HTML.
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
    ConfigParams parameters, ArrayList tabsArray)
    throws ManifoldCFException, IOException
  {
    tabsArray.add("Repository");
    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"+
"function checkConfigForSave()\n"+
"{\n"+
"  if (editconnection.repositoryroot.value == \"\")\n"+
"  {\n"+
"    alert(\"Enter a repository root\");\n"+
"    SelectTab(\"Repository\");\n"+
"    editconnection.repositoryroot.focus();\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"//-->\n"+
"</script>\n"
    );
  }

  /** Output the configuration body section.
  * This method is called in the body section of the connector's configuration page.  Its purpose is to
  * present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within
  * appropriate <html>, <body>, and <form> tags.  The name of the form is always "editconnection".
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabName is the current tab name.
  */
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
    ConfigParams parameters, String tabName)
    throws ManifoldCFException, IOException
  {
    String repositoryRoot = parameters.getParameter(PARAMETER_REPOSITORY_ROOT);
    if (repositoryRoot == null)
      repositoryRoot = "";

    if (tabName.equals("Repository"))
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Repository root:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      <input type=\"text\" size=\"64\" name=\"repositoryroot\" value=\""+
  Encoder.attributeEscape(repositoryRoot)+"\"/>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\"repositoryroot\" value=\""+
  Encoder.attributeEscape(repositoryRoot)+"\"/>\n"
      );
    }
  }

  /** Process a configuration post.
  * This method is called at the start of the connector's configuration page, whenever there is a possibility
  * that form data for a connection has been posted.  Its purpose is to gather form information and modify
  * the configuration parameters accordingly.
  * The name of the posted form is always "editconnection".
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param variableContext is the set of variables available from the post, including binary file post information.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of the
  *   connection (and cause a redirection to an error page).
  */
  public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
    ConfigParams parameters)
    throws ManifoldCFException
  {
    String repositoryRoot = variableContext.getParameter("repositoryroot");
    if (repositoryRoot != null)
      parameters.setParameter(PARAMETER_REPOSITORY_ROOT,repositoryRoot);
    return null;
  }

  /** View configuration.
  * This method is called in the body section of the connector's view configuration page.  Its purpose is to present
  * the connection information to the user.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  */
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out, ConfigParams parameters)
    throws ManifoldCFException, IOException
  {
    out.print(
"<table class=\"displaytable\">\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Repository root:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      "+Encoder.bodyEscape(
  parameters.getParameter(PARAMETER_REPOSITORY_ROOT))+"\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
    );
  }
  
  /** Get the current session, or create one if not valid.
  */
  protected Docs4UAPI getSession()
    throws ManifoldCFException, ServiceInterruption
  {
    if (session == null)
    {
      // We need to establish a new session
      try
      {
        session = D4UFactory.makeAPI(rootDirectory);
      }
      catch (D4UException e)
      {
        // Here we need to decide if the exception is transient or permanent.
        // Permanent exceptions should throw ManifoldCFException.  Transient
        // ones should throw an appropriate ServiceInterruption, based on the
        // actual error.
        Logging.ingest.warn("Docs4U: Session setup error: "+e.getMessage(),e);
        throw new ManifoldCFException("Session setup error: "+e.getMessage(),e);
      }
    }
    // Reset the expiration time
    sessionExpiration = System.currentTimeMillis() + SESSION_EXPIRATION_MILLISECONDS;
    return session;
  }
  
  /** Expire any current session.
  */
  protected void expireSession()
  {
    session = null;
    sessionExpiration = -1L;
  }
  
  /** Connect.
  *@param configParameters is the set of configuration parameters, which
  * in this case describe the root directory.
  */
  public void connect(ConfigParams configParameters)
  {
    super.connect(configParameters);
    // This is needed by getDocumentBins()
    rootDirectory = configParameters.getParameter(PARAMETER_REPOSITORY_ROOT);
  }

  /** Close the connection.  Call this before discarding this instance of the
  * connector.
  */
  public void disconnect()
    throws ManifoldCFException
  {
    expireSession();
    rootDirectory = null;
    super.disconnect();
  }

  /** Test the connection.  Returns a string describing the connection integrity.
  *@return the connection's status as a displayable string.
  */
  public String check()
    throws ManifoldCFException
  {
    try
    {
      // Get or establish the session
      Docs4UAPI currentSession = getSession();
      // Check session integrity
      try
      {
        currentSession.sanityCheck();
      }
      catch (D4UException e)
      {
        Logging.ingest.warn("Docs4U: Error checking repository: "+e.getMessage(),e);
        return "Error: "+e.getMessage();
      }
      // If it passed, return "everything ok" message
      return super.check();
    }
    catch (ServiceInterruption e)
    {
      // Convert service interruption into a transient error for display
      return "Transient error: "+e.getMessage();
    }
  }

  /** This method is periodically called for all connectors that are connected but not
  * in active use.
  */
  public void poll()
    throws ManifoldCFException
  {
    if (session != null)
    {
      if (System.currentTimeMillis() >= sessionExpiration)
        expireSession();
    }
  }

  /** Get the bin name strings for a document identifier.  The bin name describes the queue to which the
  * document will be assigned for throttling purposes.  Throttling controls the rate at which items in a
  * given queue are fetched; it does not say anything about the overall fetch rate, which may operate on
  * multiple queues or bins.
  * For example, if you implement a web crawler, a good choice of bin name would be the server name, since
  * that is likely to correspond to a real resource that will need real throttle protection.
  * The connector must be connected for this method to be called.
  *@param documentIdentifier is the document identifier.
  *@return the set of bin names.  If an empty array is returned, it is equivalent to there being no request
  * rate throttling available for this identifier.
  */
  public String[] getBinNames(String documentIdentifier)
  {
    return new String[]{rootDirectory};
  }
  
  /** Request arbitrary connector information.
  * This method is called directly from the API in order to allow API users to perform any one of several
  * connector-specific queries.  These are usually used to create external UI's.  The connector will be
  * connected before this method is called.
  *@param output is the response object, to be filled in by this method.
  *@param command is the command, which is taken directly from the API request.
  *@return true if the resource is found, false if not.  In either case, output may be filled in.
  */
  public boolean requestInfo(Configuration output, String command)
    throws ManifoldCFException
  {
    // Look for the commands we know about
    if (command.equals("metadata"))
    {
      // Use a try/catch to capture errors from repository communication
      try
      {
        // Get the metadata names
        String[] metadataNames = getMetadataNames();
        // Code these up in the output, in a form that yields decent JSON
        int i = 0;
        while (i < metadataNames.length)
        {
          String metadataName = metadataNames[i++];
          // Construct an appropriate node
          ConfigurationNode node = new ConfigurationNode("metadata");
          ConfigurationNode child = new ConfigurationNode("name");
          child.setValue(metadataName);
          node.addChild(node.getChildCount(),child);
          output.addChild(output.getChildCount(),node);
        }
      }
      catch (ServiceInterruption e)
      {
        ManifoldCF.createServiceInterruptionNode(output,e);
      }
      catch (ManifoldCFException e)
      {
        ManifoldCF.createErrorNode(output,e);
      }
    }
    else
      return super.requestInfo(output,command);
    return true;
  }

  /** Detect if a mime type is indexable or not.  This method is used by participating repository connectors to pre-filter the number of
  * unusable documents that will be passed to this output connector.
  *@param mimeType is the mime type of the document.
  *@return true if the mime type is indexable by this connector.
  */
  public boolean checkMimeTypeIndexable(String mimeType)
    throws ManifoldCFException, ServiceInterruption
  {
    return true;
  }

  /** Pre-determine whether a document (passed here as a File object) is indexable by this connector.  This method is used by participating
  * repository connectors to help reduce the number of unmanageable documents that are passed to this output connector in advance of an
  * actual transfer.  This hook is provided mainly to support search engines that only handle a small set of accepted file types.
  *@param localFile is the local file to check.
  *@return true if the file is indexable.
  */
  public boolean checkDocumentIndexable(File localFile)
    throws ManifoldCFException, ServiceInterruption
  {
    return true;
  }

  /** Get an output version string, given an output specification.  The output version string is used to uniquely describe the pertinent details of
  * the output specification and the configuration, to allow the Connector Framework to determine whether a document will need to be output again.
  * Note that the contents of the document cannot be considered by this method, and that a different version string (defined in IRepositoryConnector)
  * is used to describe the version of the actual document.
  *
  * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
  * necessary.
  *@param spec is the current output specification for the job that is doing the crawling.
  *@return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal,
  * the document will not need to be sent again to the output data store.
  */
  public String getOutputDescription(OutputSpecification spec)
    throws ManifoldCFException, ServiceInterruption
  {
    String urlMetadataName = "";
    String securityMap = "";
    ArrayList metadataMappings = new ArrayList();
    
    int i = 0;
    while (i < spec.getChildCount())
    {
      SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_URL_METADATA_NAME))
        urlMetadataName = sn.getAttributeValue(ATTRIBUTE_VALUE);
      else if (sn.getType().equals(NODE_SECURITY_MAP))
        securityMap = sn.getAttributeValue(ATTRIBUTE_VALUE);
      else if (sn.getType().equals(NODE_METADATA_MAP))
      {
        String recordSource = sn.getAttributeValue(ATTRIBUTE_SOURCE);
        String recordTarget = sn.getAttributeValue(ATTRIBUTE_TARGET);
        String[] fixedList = new String[]{recordSource,recordTarget};
        StringBuffer packBuffer = new StringBuffer();
        packFixedList(packBuffer,fixedList,':');
        metadataMappings.add(packBuffer.toString());
      }
    }
    
    // Now, form the final string.
    StringBuffer sb = new StringBuffer();
    
    pack(sb,urlMetadataName,'+');
    pack(sb,securityMap,'+');
    packList(sb,metadataMappings,',');

    return sb.toString();
  }

  /** Add (or replace) a document in the output data store using the connector.
  * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
  * necessary.
  * The OutputSpecification is *not* provided to this method, because the goal is consistency, and if output is done it must be consistent with the
  * output description, since that was what was partly used to determine if output should be taking place.  So it may be necessary for this method to decode
  * an output description string in order to determine what should be done.
  *@param documentURI is the URI of the document.  The URI is presumed to be the unique identifier which the output data store will use to process
  * and serve the document.  This URI is constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
  *@param outputDescription is the description string that was constructed for this document by the getOutputDescription() method.
  *@param document is the document data to be processed (handed to the output data store).
  *@param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in with the repository document.  May be null.
  *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
  *@return the document status (accepted or permanently rejected).
  */
  public int addOrReplaceDocument(String documentURI, String outputDescription,
    RepositoryDocument document, String authorityNameString, IOutputAddActivity activities)
    throws ManifoldCFException, ServiceInterruption
  {
    // First, unpack the output description.
    int index = 0;
    StringBuffer urlMetadataNameBuffer = new StringBuffer();
    StringBuffer securityMapBuffer = new StringBuffer();
    ArrayList metadataMappings = new ArrayList();

    index = unpack(urlMetadataNameBuffer,outputDescription,index,'+');
    index = unpack(securityMapBuffer,outputDescription,index,'+');
    index = unpackList(metadataMappings,outputDescription,index,',');
    
    String urlMetadataName = urlMetadataNameBuffer.toString();
    Map fieldMap = new HashMap();
    int j = 0;
    while (j < metadataMappings.size())
    {
      String metadataMapping = (String)metadataMappings.get(j++);
      // Unpack
      String[] mappingData = new String[2];
      unpackFixedList(mappingData,metadataMapping,0,':');
      fieldMap.put(mappingData[0],mappingData[1]);
    }

    // Handle activity logging.
    long startTime = System.currentTimeMillis();
    String resultCode = "OK";
    String resultReason = null;
    long byteCount = 0L;
    
    try
    {
      // Get a Docs4U session to work with.
      Docs4UAPI session = getSession();
      try
      {
        // Let's form the D4UDocInfo object for the document.  Do this first, since there's no
        // guarantee we'll succeed here.
        
        D4UDocInfo docObject = D4UFactory.makeDocInfo();
        try
        {
          // First, fill in the security info, since that might well cause us to reject the document outright.
          // We can only accept the document if the security information is compatible with the Docs4U
          // model, and if the mapped user or group exists in the target repository.
          
          if (document.countDirectoryACLs() > 0)
          {
            resultCode = "REJECTED";
            resultReason = "Directory ACLs present";
            return DOCUMENTSTATUS_REJECTED;
          }
          
          String[] shareAcl = document.getShareACL();
          String[] shareDenyAcl = document.getShareDenyACL();
          if ((shareAcl != null && shareAcl.length > 0) ||
            (shareDenyAcl != null && shareDenyAcl.length > 0))
          {
            resultCode = "REJECTED";
            resultReason = "Share ACLs present";
            return DOCUMENTSTATUS_REJECTED;
          }
          
          String[] acl = performUserGroupMapping(document.getACL());
          String[] denyAcl = performUserGroupMapping(document.getDenyACL());
          if (acl == null || denyAcl == null)
          {
            resultCode = "REJECTED";
            resultReason = "Access tokens did not map";
            return DOCUMENTSTATUS_REJECTED;
          }
          docObject.setAllowed(acl);
          docObject.setDisallowed(denyAcl);
          
          // Next, map the metadata.  If this doesn't succeed, nothing is lost and we can still continue.
          Iterator fields = document.getFields();
          while (fields.hasNext())
          {
            String field = (String)fields.next();
            String mappedField = (String)fieldMap.get(field);
            if (mappedField != null)
            {
              if (Logging.ingest.isDebugEnabled())
                Logging.ingest.debug("For document '"+documentURI+"', field '"+field+"' maps to target field '"+mappedField+"'");
              // We have a source field and a target field; copy the attribute
              Object[] values = document.getField(field);
              // We only handle string metadata at this time.
              String[] stringValues = new String[values.length];
              int k = 0;
              while (k < stringValues.length)
              {
                stringValues[k] = (String)values[k];
                k++;
              }
              docObject.setMetadata(mappedField,stringValues);
            }
            else
            {
              if (Logging.ingest.isDebugEnabled())
                Logging.ingest.debug("For document '"+documentURI+"', field '"+field+"' discarded");
            }
          }
          
          // Finally, copy the content.  The input stream returned by getBinaryStream() should NOT
          // be closed, just read.
          byteCount = document.getBinaryLength();
          docObject.setData(document.getBinaryStream());
          
          // Next, look up the Docs4U identifier for the document.
          Map lookupMap = new HashMap();
          lookupMap.put(urlMetadataName,documentURI);
          D4UDocumentIterator iter = session.findDocuments(null,null,lookupMap);
          String documentID;
          if (iter.hasNext())
          {
            documentID = iter.getNext();
            session.updateDocument(documentID,docObject);
          }
          else
            documentID = session.createDocument(docObject);
          return DOCUMENTSTATUS_ACCEPTED;
        }
        finally
        {
          docObject.close();
        }
      }
      catch (InterruptedException e)
      {
        // We don't log interruptions, just exit immediately.
        resultCode = null;
        // Throw an interruption signal.
        throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (D4UException e)
      {
        resultCode = "ERROR";
        resultReason = e.getMessage();
        Logging.ingest.warn("Docs4U: Error ingesting '"+documentURI+"': "+e.getMessage(),e);
        // Decide whether this is a service interruption or a real error, and throw accordingly.
        // Docs4U never throws service interruptions.
        throw new ManifoldCFException("Error ingesting '"+documentURI+"': "+e.getMessage(),e);
      }
    }
    finally
    {
      // Log the activity - but only if it wasn't interrupted
      if (resultCode != null)
      {
        activities.recordActivity(new Long(startTime),ACTIVITY_SAVE,new Long(byteCount),documentURI,
          resultCode,resultReason);
      }
    }
  }

  /** Perform the mapping from access token to user/group name to Docs4U user/group ID.
  *@return null if the mapping cannot be performed, in which case the document will be
  * rejected by the caller.
  */
  protected String[] performUserGroupMapping(String[] inputACL)
    throws ManifoldCFException, ServiceInterruption, D4UException
  {
    // MHL
    return null;
  }
  
  /** Remove a document using the connector.
  * Note that the last outputDescription is included, since it may be necessary for the connector to use such information to know how to properly remove the document.
  *@param documentURI is the URI of the document.  The URI is presumed to be the unique identifier which the output data store will use to process
  * and serve the document.  This URI is constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
  *@param outputDescription is the last description string that was constructed for this document by the getOutputDescription() method above.
  *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
  */
  public void removeDocument(String documentURI, String outputDescription, IOutputRemoveActivity activities)
    throws ManifoldCFException, ServiceInterruption
  {
    // Unpack what we need from the output description
    StringBuffer urlMetadataNameBuffer = new StringBuffer();
    unpack(urlMetadataNameBuffer,outputDescription,0,'+');
    String urlMetadataName = urlMetadataNameBuffer.toString();

    // Handle activity logging.
    long startTime = System.currentTimeMillis();
    String resultCode = "OK";
    String resultReason = null;
    
    try
    {
      // Get a Docs4U session to work with.
      Docs4UAPI session = getSession();
      try
      {
        Map lookupMap = new HashMap();
        lookupMap.put(urlMetadataName,documentURI);
        D4UDocumentIterator iter = session.findDocuments(null,null,lookupMap);
        if (iter.hasNext())
        {
          String documentID = iter.getNext();
          session.deleteDocument(documentID);
        }
      }
      catch (InterruptedException e)
      {
        // We don't log interruptions, just exit immediately.
        resultCode = null;
        // Throw an interruption signal.
        throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
      }
      catch (D4UException e)
      {
        resultCode = "ERROR";
        resultReason = e.getMessage();
        Logging.ingest.warn("Docs4U: Error removing '"+documentURI+"': "+e.getMessage(),e);
        // Decide whether this is a service interruption or a real error, and throw accordingly.
        throw new ManifoldCFException("Error removing '"+documentURI+"': "+e.getMessage(),e);
      }
    }
    finally
    {
      // Log the activity - but only if it wasn't interrupted
      if (resultCode != null)
      {
        activities.recordActivity(new Long(startTime),ACTIVITY_DELETE,null,documentURI,
          resultCode,resultReason);
      }
    }
  }

  /** Notify the connector of a completed job.
  * This is meant to allow the connector to flush any internal data structures it has been keeping around, or to tell the output repository that this
  * is a good time to synchronize things.  It is called whenever a job is either completed or aborted.
  *@param activities is the handle to an object that the implementer of an output connector may use to perform operations, such as logging processing activity.
  */
  public void noteJobComplete(IOutputNotifyActivity activities)
    throws ManifoldCFException, ServiceInterruption
  {
    // Does nothing for Docs4U
  }

  // UI support methods.
  //
  // The UI support methods come in two varieties.  The first group (inherited from IConnector) is involved
  //  in setting up connection configuration information.
  //
  // The second group is listed here.  These methods are is involved in presenting and editing output specification
  //  information for a job.
  //
  // The two kinds of methods are accordingly treated differently, in that the first group cannot assume that
  // the current connector object is connected, while the second group can.  That is why the first group
  // receives a thread context argument for all UI methods, while the second group does not need one
  // (since it has already been applied via the connect() method).
    
  /** Output the specification header section.
  * This method is called in the head section of a job page which has selected an output connection of the
  * current type.  Its purpose is to add the required tabs to the list, and to output any javascript methods
  * that might be needed by the job editing HTML.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param os is the current output specification for this job.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  public void outputSpecificationHeader(IHTTPOutput out, OutputSpecification os, ArrayList tabsArray)
    throws ManifoldCFException, IOException
  {
    // Add the tabs
    tabsArray.add("Docs4U Metadata");
    tabsArray.add("Docs4U Security");
    
    // Start the javascript
    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"
    );
    
    // Output the overall check function
    out.print(
"function checkOutputSpecification()\n"+
"{\n"+
"  if (ocCheckMetadataMappingTab() == false)\n"+
"    return false;\n"+
"  if (ocCheckAccessMappingTab() == false)\n"+
"    return false;\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

    // Output the overall check function for save
    out.print(
"function checkOutputSpecificationForSave()\n"+
"{\n"+
"  if (ocCheckMetadataMappingTabForSave() == false)\n"+
"    return false;\n"+
"  if (ocCheckAccessMappingTabForSave() == false)\n"+
"    return false;\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

    // Output a useful method which sets a specified command
    // value, and then re-posts the form using the supplied anchor
    out.print(
"function ocSpecOp(n, opValue, anchorvalue)\n"+
"{\n"+
"  eval(\"editjob.\"+n+\".value = \\\"\"+opValue+\"\\\"\");\n"+
"  postFormSetAnchor(anchorvalue);\n"+
"}\n"+
"\n"
    );

    // Output the actual javascript for the tabs
    outputMetadataMappingTabJavascript(out,os);
    outputAccessMappingTabJavascript(out,os);

    // Terminate the javascript tag
    out.print(
"//-->\n"+
"</script>\n"
    );
    
  }

  /** Output the javascript for the Metadata Mapping tab.
  */
  protected void outputMetadataMappingTabJavascript(IHTTPOutput out, OutputSpecification os)
    throws ManifoldCFException, IOException
  {
    // The check function for this tab, which does nothing
    // (Called whenever the tab is navigated away from)
    out.print(
"function ocCheckMetadataMappingTab()\n"+
"{\n"+
"  return true;\n"+
"}\n"+
"\n"
    );
    
    out.print(
"function ocCheckMetadataMappingTabForSave()\n"+
"{\n"+
"  if (editjob.ocurlmetadataname.value == \"\")\n"+
"  {\n"+
"    alert(\"URL metadata name cannot be blank.\");\n"+
"    SelectTab(\"Docs4U Metadata\");\n"+
"    editjob.ocmetadataname.focus();\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"\n"
    );
    
    // Delete a row from the displayed table
    out.print(
"function ocMetadataDelete(n)\n"+
"{\n"+
"  ocSpecOp(\"ocmetadataop_\"+n, \"Delete\", \"ocmetadata_\"+n);\n"+
"}\n"+
"\n"
    );
    
    // Add a row to the displayed table
    out.print(
"function ocMetadataAdd(n)\n"+
"{\n"+
"  if (editjob.ocmetadatasource.value == \"\")\n"+
"  {\n"+
"    alert(\"Metadata source name cannot be blank.\");\n"+
"    editjob.ocmetadatasource.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob.ocmetadatatarget.value == \"\")\n"+
"  {\n"+
"    alert(\"Please select a metadata target value first.\");\n"+
"    editjob.ocmetadatatarget.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob.ocmetadatatarget.value == editjob.ocurlmetadataname.value)\n"+
"  {\n"+
"    alert(\"URL metadata name must differ from all mapping metadata names.\");\n"+
"    editjob.ocmetadatatarget.focus();\n"+
"    return;\n"+
"  }\n"+
"  ocSpecOp(\"ocmetadataop\", \"Add\", \"ocmetadata_\"+n);\n"+
"}\n"+
"\n"
    );

  }

  /** Output the javascript for the Documents tab.
  */
  protected void outputAccessMappingTabJavascript(IHTTPOutput out, OutputSpecification os)
    throws ManifoldCFException, IOException
  {
    // The check function for this tab, which does nothing
    // (Called whenever the tab is navigated away from)
    out.print(
"function ocCheckAccessMappingTab()\n"+
"{\n"+
"  if (editjob.ocsecurityregexp.value != \"\" && !isRegularExpression(editjob.ocsecurityregexp.value))\n"+
"  {\n"+
"    alert(\"Security mapping regular expression must be a valid regular expression\");\n"+
"    editjob.ocsecurityregexp.focus();\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"\n"
    );
    
    out.print(
"function ocCheckAccessMappingTabForSave()\n"+
"{\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

  }
  
  /** Output the specification body section.
  * This method is called in the body section of a job page which has selected an output connection of the
  * current type.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate
  *  <html>, <body>, and <form> tags.  The name of the form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param os is the current output specification for this job.
  *@param tabName is the current tab name.
  */
  public void outputSpecificationBody(IHTTPOutput out, OutputSpecification os, String tabName)
    throws ManifoldCFException, IOException
  {
    // Do the "Metadata Mapping" tab
    outputMetadataMappingTab(out,os,tabName);
    // Do the "Access Mapping" tab
    outputAccessMappingTab(out,os,tabName);
  }
  
  /** Take care of "Metadata Mapping" tab.
  */
  protected void outputMetadataMappingTab(IHTTPOutput out, OutputSpecification os, String tabName)
    throws ManifoldCFException, IOException
  {
    int i;
    int k;
    
    
    if (tabName.equals("Docs4U Metadata"))
    {
      // Output the outer table, which is just a standard 2-column table.
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
      );

      // Before we start, locate the current URL map attribute, if any.
      // Also, build a map of the currently used Docs4U attributes.
      String urlMetadataName = null;
      Map usedAttributes = new HashMap();
      i = 0;
      while (i < os.getChildCount())
      {
        SpecificationNode sn = os.getChild(i++);
        if (sn.getType().equals(NODE_URL_METADATA_NAME))
          urlMetadataName = sn.getAttributeValue(ATTRIBUTE_VALUE);
        else if (sn.getType().equals(NODE_METADATA_MAP))
        {
          String target =sn.getAttributeValue(ATTRIBUTE_TARGET);
          usedAttributes.put(target,target);
        }
      }
      
      // Output the URL field mapping, by itself.
      // First, we must obtain the attribute names from the repository.
      try
      {
        String[] matchNames = getMetadataNames();
        
        // Success!  Now display the URL attribute, with a pruned selection pulldown.
        out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Docs4U URL attribute:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      <select name=\"ocurlmetadataname\">\n"+
"        <option value=\"\""+((urlMetadataName==null)?" selected=\"true\"":"")+
  ">-- Select URL metadata attribute --</option>\n"
        );
        
        // Write the pruned options.
        k = 0;
        while (k < matchNames.length)
        {
          String option = matchNames[k++];
          if (usedAttributes.get(option) == null)
          {
            boolean isMe = urlMetadataName != null && option.equals(urlMetadataName);
            out.print(
"        <option value=\""+Encoder.attributeEscape(option)+"\""+(isMe?" selected=\"true\"":"")+">"+
  Encoder.bodyEscape(option)+"</option>\n"
            );
          }
        }

        // Close off the row
        out.print(
"      </select>\n"+
"    </td>\n"+
"  </tr>"
        );
      }
      catch (ManifoldCFException e)
      {
        // If there was an error, display it as the entire row contents
        out.print(
"        <tr class=\"formrow\"><td class=\"message\" colspan=\"2\">Error: "+
  Encoder.bodyEscape(e.getMessage())+"</td></tr>\n"
        );
      }
      catch (ServiceInterruption e)
      {
        out.print(
"        <tr class=\"formrow\"><td class=\"message\" colspan=\"2\">Transient error: "+
  Encoder.bodyEscape(e.getMessage())+"</td></tr>\n"
        );
      }

      // Output the general mapping table.
      out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Mapping:</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Source metadata name</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Docs4U metadata name</nobr></td>\n"+
"        </tr>\n"
      );

      // Look for mappings in the output specification, and output them when found.
      i = 0;
      k = 0;
      while (i < os.getChildCount())
      {
        SpecificationNode sn = os.getChild(i++);
        // Look for METADATA_MAP nodes in the output specification
        if (sn.getType().equals(NODE_METADATA_MAP))
        {
          // Pull the source and target metadata names from the node
          String metadataRecordSource = sn.getAttributeValue(ATTRIBUTE_SOURCE);
          String metadataRecordTarget = sn.getAttributeValue(ATTRIBUTE_TARGET);
          // We'll need a suffix for each row, used for form element names and
          // for anchor names.
          String metadataRecordSuffix = "_"+Integer.toString(k);
          // Output the row.
          out.print(
"        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <input type=\"hidden\" name=\"ocmetadataop"+metadataRecordSuffix+"\" value=\"\"/>\n"+
"            <input type=\"hidden\" name=\"ocmetadatasource"+metadataRecordSuffix+"\" value=\""+
  Encoder.attributeEscape(metadataRecordSource)+"\"/>\n"+
"            <input type=\"hidden\" name=\"ocmetadatatarget"+metadataRecordSuffix+"\" value=\""+
  Encoder.attributeEscape(metadataRecordTarget)+"\"/>\n"+
"            <a name=\""+"metadata_"+Integer.toString(k)+"\">\n"+
"              <input type=\"button\" value=\"Delete\" onClick='Javascript:ocMetadataDelete(\""+
  Integer.toString(k)+"\")' alt=\"Delete mapping #"+Integer.toString(k)+"\"/>\n"+
"            </a>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(metadataRecordSource)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(metadataRecordTarget)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"        </tr>\n"
          );

          // Increment the row counter
          k++;
        }
      }
      
      if (k == 0)
      {
        // There are no records yet.  Print an empty record summary
        out.print(
"        <tr class=\"formrow\"><td class=\"formcolumnmessage\" colspan=\"3\">No mappings specified</td></tr>\n"
        );
      }
      
      // Output a separator
      out.print(
"        <tr class=\"formrow\"><td class=\"formseparator\" colspan=\"3\"><hr/></td></tr>\n"
      );

      // Output the Add button, in its own table row
      
      // We need a try/catch block because an exception can be thrown when we query the
      // repository.
      try
      {
        String[] matchNames = getMetadataNames();
        // Success!  Now output the row content
        out.print(
"        <tr class=\"formrow\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              <a name=\"find_"+Integer.toString(k)+"\">\n"+
"                <input type=\"button\" value=\"Add\" onClick='Javascript:ocMetadataAdd(\""+
  Integer.toString(k+1)+"\")' alt=\"Add new mapping\"/>\n"+
"                <input type=\"hidden\" name=\"ocmetadatacount\" value=\""+Integer.toString(k)+"\"/>\n"+
"                <input type=\"hidden\" name=\"ocmetadataop\" value=\"\"/>\n"+
"              </a>\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              <input type=\"text\" size=\"32\" name=\"ocmetadatasource\" value=\"\"/>\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <select name=\"ocmetadatatarget\">\n"+
"              <option value=\"\" selected=\"true\">--Select target attribute name --</option>\n"
        );
      
        k = 0;
        while (k < matchNames.length)
        {
          String metadataTargetName = matchNames[k++];
          if (usedAttributes.get(metadataTargetName) == null)
          {
            out.print(
"              <option value=\""+Encoder.attributeEscape(metadataTargetName)+"\">"+
  Encoder.bodyEscape(metadataTargetName)+"</option>\n"
            );
          }
        }
        
        out.print(
"            </select>\n"+
"          </td>\n"+
"        </tr>\n"
        );
      }
      catch (ManifoldCFException e)
      {
        // If there was an error, display it as the entire row contents
        out.print(
"        <tr class=\"formrow\"><td class=\"formcolumnmessage\" colspan=\"3\">Error: "+
  Encoder.bodyEscape(e.getMessage())+"</td></tr>\n"
        );
      }
      catch (ServiceInterruption e)
      {
        out.print(
"        <tr class=\"formrow\"><td class=\"formcolumnmessage\" colspan=\"3\">Transient error: "+
  Encoder.bodyEscape(e.getMessage())+"</td></tr>\n"
        );
      }

      // Close off general mapping table.
      out.print(
"      </table>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      // Emit hiddens.
      // Loop through all nodes and emit the ones we recognize
      k = 0;
      i = 0;
      String urlMetadataName = "";
      while (i < os.getChildCount())
      {
        SpecificationNode sn = os.getChild(i++);
        if (sn.getType().equals(NODE_URL_METADATA_NAME))
          // Get the value
          urlMetadataName = sn.getAttributeValue(ATTRIBUTE_VALUE);
        else if (sn.getType().equals(NODE_METADATA_MAP))
        {
          String metadataRecordSource = sn.getAttributeValue(ATTRIBUTE_SOURCE);
          String metadataRecordTarget = sn.getAttributeValue(ATTRIBUTE_TARGET);
          String metadataRecordSuffix = "_"+Integer.toString(k);
          // Output the row hiddens
          out.print(
"<input type=\"hidden\" name=\"ocmetadatasource"+metadataRecordSuffix+
  "\" value=\""+Encoder.attributeEscape(metadataRecordSource)+"\"/>\n"+
"<input type=\"hidden\" name=\"ocmetadatatarget"+metadataRecordSuffix+
  "\" value=\""+Encoder.attributeEscape(metadataRecordTarget)+"\"/>\n"
          );
          k++;
        }
      }
      // Output the count of rows and the url metadata name
      out.print(
"<input type=\"hidden\" name=\"ocmetadatacount\" value=\""+Integer.toString(k)+"\"/>\n"+
"<input type=\"hidden\" name=\"ocurlmetadataname\" value=\""+Encoder.attributeEscape(urlMetadataName)+"\"/>\n"
      );
    }
  }
  
  /** Take care of "Access Mapping" tab.
  */
  protected void outputAccessMappingTab(IHTTPOutput out, OutputSpecification os, String tabName)
    throws ManifoldCFException, IOException
  {
    int i;
    
    // Look for mapping data
    MatchMap mm = new MatchMap();
    mm.appendMatchPair("(.*)","$(1)");
    
    i = 0;
    while (i < os.getChildCount())
    {
      SpecificationNode sn = os.getChild(i++);
      if (sn.getType().equals(NODE_SECURITY_MAP))
      {
        String mappingString = sn.getAttributeValue(ATTRIBUTE_VALUE);
        mm = new MatchMap(mappingString);
      }
    }
    String regexp = mm.getMatchString(0);
    String translation = mm.getReplaceString(0);
    
    if (tabName.equals("Docs4U Security"))
    {
      // Output the outer table, which is just a standard 2-column table.
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Access token mapping:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      <nobr>\n"+
"         <input type=\"text\" name=\"ocsecurityregexp\" value=\""+Encoder.attributeEscape(regexp)+"\"/>\n"+
"         ==&gt;\n"+
"         <input type=\"text\" name=\"ocsecuritytranslation\" value=\""+Encoder.attributeEscape(translation)+"\"/>\n"+
"      </nobr>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      // Output hiddens for the production
      out.print(
"<input type=\"hidden\" name=\"ocsecurityregexp\" value=\""+Encoder.attributeEscape(regexp)+"\"/>\n"+
"<input type=\"hidden\" name=\"ocsecuritytranslation\" value=\""+Encoder.attributeEscape(translation)+"\"/>\n"
      );
    }
  }

  /** Process a specification post.
  * This method is called at the start of job's edit or view page, whenever there is a possibility that form
  * data for a connection has been posted.  Its purpose is to gather form information and modify the
  * output specification accordingly.  The name of the posted form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param variableContext contains the post data, including binary file-upload information.
  *@param os is the current output specification for this job.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of
  * the job (and cause a redirection to an error page).
  */
  public String processSpecificationPost(IPostParameters variableContext, OutputSpecification os)
    throws ManifoldCFException
  {
    // Pick up the Metadata Mapping tab data
    String rval = processMetadataMappingTab(variableContext,os);
    if (rval != null)
      return rval;
    // Pick up the Access Mapping tab data
    rval = processAccessMappingTab(variableContext,os);
    return rval;
  }
  
  /** Process form post for metadata tab.
  */
  protected String processMetadataMappingTab(IPostParameters variableContext, OutputSpecification os)
    throws ManifoldCFException
  {
    // Remove old url metadata name
    removeNodes(os,NODE_URL_METADATA_NAME);
    // Get the url metadata name
    String urlMetadataName = variableContext.getParameter("ocurlmetadataname");
    if (urlMetadataName != null)
      addUrlMetadataNameNode(os,urlMetadataName);
    
    // Remove old metadata mapping output specification information
    removeNodes(os,NODE_METADATA_MAP);
    
    // Parse the number of records that were posted
    String recordCountString = variableContext.getParameter("ocmetadatacount");
    if (recordCountString != null)
    {
      int recordCount = Integer.parseInt(recordCountString);
              
      // Loop throught them and add to the new document specification information
      int i = 0;
      while (i < recordCount)
      {
        String suffix = "_"+Integer.toString(i++);
        // Only add the name/value if the item was not deleted.
        String metadataOp = variableContext.getParameter("ocmetadataop"+suffix);
        if (metadataOp == null || !metadataOp.equals("Delete"))
        {
          String metadataSource = variableContext.getParameter("ocmetadatasource"+suffix);
          String metadataTarget = variableContext.getParameter("ocmetadatatarget"+suffix);
          addMetadataMappingNode(os,metadataSource,metadataTarget);
        }
      }
    }
      
    // Now, look for a global "Add" operation
    String operation = variableContext.getParameter("ocmetadataop");
    if (operation != null && operation.equals("Add"))
    {
      // Pick up the global parameter name and value
      String metadataSource = variableContext.getParameter("ocmetadatasource");
      String metadataTarget = variableContext.getParameter("ocmetadatatarget");
      addMetadataMappingNode(os,metadataSource,metadataTarget);
    }

    return null;
  }

  /** Add a METADATA_MAP node to an output specification.
  */
  protected static void addMetadataMappingNode(OutputSpecification os,
    String metadataSource, String metadataTarget)
  {
    // Create a new specification node with the right characteristics
    SpecificationNode sn = new SpecificationNode(NODE_METADATA_MAP);
    sn.setAttribute(ATTRIBUTE_SOURCE,metadataSource);
    sn.setAttribute(ATTRIBUTE_TARGET,metadataTarget);
    // Add to the end
    os.addChild(os.getChildCount(),sn);
  }

  /** Add a URL_METADATA_NAME node to an output specification.
  */
  protected static void addUrlMetadataNameNode(OutputSpecification os,
    String urlMetadataName)
  {
    // Create a new specification node with the right characteristics
    SpecificationNode sn = new SpecificationNode(NODE_URL_METADATA_NAME);
    sn.setAttribute(ATTRIBUTE_VALUE,urlMetadataName);
    // Add to the end
    os.addChild(os.getChildCount(),sn);
  }
  
  /** Process form post for security tab.
  */
  protected String processAccessMappingTab(IPostParameters variableContext, OutputSpecification os)
    throws ManifoldCFException
  {
    // Remove old security map node
    removeNodes(os,NODE_SECURITY_MAP);

    String regexp = variableContext.getParameter("ocsecurityregexp");
    String translation = variableContext.getParameter("ocsecuritytranslation");
    if (regexp == null)
      regexp = "";
    if (translation == null)
      translation = "";
      
    MatchMap mm = new MatchMap();
    mm.appendMatchPair(regexp,translation);
      
    addSecurityMapNode(os,mm.toString());
    return null;
  }
  
  /** Add a SECURITY_MAP node to an output specification
  */
  protected static void addSecurityMapNode(OutputSpecification os, String value)
  {
    // Create a new specification node with the right characteristics
    SpecificationNode sn = new SpecificationNode(NODE_SECURITY_MAP);
    sn.setAttribute(ATTRIBUTE_VALUE,value);
    // Add to the end
    os.addChild(os.getChildCount(),sn);
  }
  
  /** Remove all of a specified node type from an output specification.
  */
  protected static void removeNodes(OutputSpecification os,
    String nodeTypeName)
  {
    int i = 0;
    while (i < os.getChildCount())
    {
      SpecificationNode sn = os.getChild(i);
      if (sn.getType().equals(nodeTypeName))
        os.removeChild(i);
      else
        i++;
    }
  }

  /** View specification.
  * This method is called in the body section of a job's view page.  Its purpose is to present the output
  * specification information to the user.  The coder can presume that the HTML that is output from
  * this configuration will be within appropriate <html> and <body> tags.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param os is the current output specification for this job.
  */
  public void viewSpecification(IHTTPOutput out, OutputSpecification os)
    throws ManifoldCFException, IOException
  {
    out.print(
"<table class=\"displaytable\">\n"
    );
    viewMetadataMappingTab(out,os);
    viewAccessMappingTab(out,os);
    out.print(
"</table>\n"
    );
  }

  /** View the "Metadata Mapping" tab contents
  */
  protected void viewMetadataMappingTab(IHTTPOutput out, OutputSpecification os)
    throws ManifoldCFException, IOException
  {
    int i;
    int k;

    out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Docs4U URL attribute:</nobr></td>\n"+
"    <td class=\"value\">\n"
    );

    String urlMetadataName = null;
    i = 0;
    while (i < os.getChildCount())
    {
      SpecificationNode sn = os.getChild(i++);
      if (sn.getType().equals(NODE_URL_METADATA_NAME))
        urlMetadataName = sn.getAttributeValue(ATTRIBUTE_VALUE);
    }
    
    out.print(
"      <nobr>"+Encoder.bodyEscape(urlMetadataName)+"</nobr>\n"
    );
    
    out.print(
"    </td>\n"+
"  </tr>\n"
    );

    out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Mappings:</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"><nobr>Source attribute</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Docs4U attribute</nobr></td>\n"+
"        </tr>\n"
    );
    
    i = 0;
    k = 0;
    while (i < os.getChildCount())
    {
      SpecificationNode sn = os.getChild(i++);
      if (sn.getType().equals(NODE_METADATA_MAP))
      {
        String metadataRecordSource = sn.getAttributeValue(ATTRIBUTE_SOURCE);
        String metadataRecordTarget = sn.getAttributeValue(ATTRIBUTE_TARGET);
        out.print(
"        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(metadataRecordSource)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(metadataRecordTarget)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"        </tr>\n"
        );
        k++;
      }
    }
    
    out.print(
"      </table>\n"+
"    </td>\n"+
"  </tr>\n"
    );

  }

  /** View the "Access Mapping" tab contents
  */
  protected void viewAccessMappingTab(IHTTPOutput out, OutputSpecification os)
    throws ManifoldCFException, IOException
  {
    int i;
  
    out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Docs4U security mapping:</nobr></td>\n"+
"    <td class=\"value\">\n"
    );

    MatchMap mm = new MatchMap();
    mm.appendMatchPair("(.*)","$(1)");

    i = 0;
    while (i < os.getChildCount())
    {
      SpecificationNode sn = os.getChild(i++);
      if (sn.getType().equals(NODE_SECURITY_MAP))
      {
        String mappingString = sn.getAttributeValue(ATTRIBUTE_VALUE);
        mm = new MatchMap(mappingString);
      }
    }
    String regexp = mm.getMatchString(0);
    String translation = mm.getReplaceString(0);
    
    out.print(
"      <nobr>"+Encoder.bodyEscape(regexp)+" ==&gt; "+Encoder.bodyEscape(translation)+"</nobr>\n"
    );
    
    out.print(
"    </td>\n"+
"  </tr>\n"
    );
  }

  // Protected pack/unpack methods for version strings
  
  /** Stuffer for packing a single string with an end delimiter */
  protected static void pack(StringBuffer output, String value, char delimiter)
  {
    int i = 0;
    while (i < value.length())
    {
      char x = value.charAt(i++);
      if (x == '\\' || x == delimiter)
        output.append('\\');
      output.append(x);
    }
    output.append(delimiter);
  }

  /** Unstuffer for the above. */
  protected static int unpack(StringBuffer sb, String value, int startPosition, char delimiter)
  {
    while (startPosition < value.length())
    {
      char x = value.charAt(startPosition++);
      if (x == '\\')
      {
        if (startPosition < value.length())
          x = value.charAt(startPosition++);
      }
      else if (x == delimiter)
        break;
      sb.append(x);
    }
    return startPosition;
  }

  /** Stuffer for packing lists of fixed length */
  protected static void packFixedList(StringBuffer output, String[] values, char delimiter)
  {
    int i = 0;
    while (i < values.length)
    {
      pack(output,values[i++],delimiter);
    }
  }

  /** Unstuffer for unpacking lists of fixed length */
  protected static int unpackFixedList(String[] output, String value, int startPosition, char delimiter)
  {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    while (i < output.length)
    {
      sb.setLength(0);
      startPosition = unpack(sb,value,startPosition,delimiter);
      output[i++] = sb.toString();
    }
    return startPosition;
  }

  /** Stuffer for packing lists of variable length */
  protected static void packList(StringBuffer output, ArrayList values, char delimiter)
  {
    pack(output,Integer.toString(values.size()),delimiter);
    int i = 0;
    while (i < values.size())
    {
      pack(output,values.get(i++).toString(),delimiter);
    }
  }

  /** Another stuffer for packing lists of variable length */
  protected static void packList(StringBuffer output, String[] values, char delimiter)
  {
    pack(output,Integer.toString(values.length),delimiter);
    int i = 0;
    while (i < values.length)
    {
      pack(output,values[i++],delimiter);
    }
  }

  /** Unstuffer for unpacking lists of variable length.
  *@param output is the array to write the unpacked result into.
  *@param value is the value to unpack.
  *@param startPosition is the place to start the unpack.
  *@param delimiter is the character to use between values.
  *@return the next position beyond the end of the list.
  */
  protected static int unpackList(ArrayList output, String value, int startPosition, char delimiter)
  {
    StringBuffer sb = new StringBuffer();
    startPosition = unpack(sb,value,startPosition,delimiter);
    try
    {
      int count = Integer.parseInt(sb.toString());
      int i = 0;
      while (i < count)
      {
        sb.setLength(0);
        startPosition = unpack(sb,value,startPosition,delimiter);
        output.add(sb.toString());
        i++;
      }
    }
    catch (NumberFormatException e)
    {
    }
    return startPosition;
  }

  // Protected UI support methods
  
  /** Get an ordered list of metadata names.
  */
  protected String[] getMetadataNames()
    throws ManifoldCFException, ServiceInterruption
  {
    Docs4UAPI currentSession = getSession();
    try
    {
      String[] rval = currentSession.getMetadataNames();
      java.util.Arrays.sort(rval);
      return rval;
    }
    catch (InterruptedException e)
    {
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (D4UException e)
    {
      throw new ManifoldCFException(e.getMessage(),e);
    }
  }
  
}

