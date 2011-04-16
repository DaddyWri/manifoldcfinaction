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
package org.apache.manifoldcf.crawler.connectors.docs4u;

// These are the basic interfaces we'll need from ManifoldCF
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.crawler.interfaces.*;

// Utility includes
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// This is where we get pull-agent system loggers
import org.apache.manifoldcf.crawler.system.Logging;

// This class implements system-wide static methods
import org.apache.manifoldcf.crawler.system.ManifoldCF;

// This is the base repository class.
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;

// Here's the UI helper classes.
import org.apache.manifoldcf.ui.util.Encoder;

// Here are the imports that are specific for this connector
import org.apache.manifoldcf.examples.docs4u.Docs4UAPI;
import org.apache.manifoldcf.examples.docs4u.D4UFactory;
import org.apache.manifoldcf.examples.docs4u.D4UDocInfo;
import org.apache.manifoldcf.examples.docs4u.D4UDocumentIterator;
import org.apache.manifoldcf.examples.docs4u.D4UException;

/** This is the Docs4U repository connector class.  This extends the base connectors class,
* which implements IRepositoryConnector, and provides some degree of insulation from future
* changes to the IRepositoryConnector interface.  It also provides us with basic support for
* the connector lifecycle methods, so we don't have to implement those each time.
*/
public class Docs4UConnector extends BaseRepositoryConnector
{
  // These are the configuration parameter names
  
  /** Repository root parameter */
  protected final static String PARAMETER_REPOSITORY_ROOT = "rootdirectory";
  
  // These are the document specification node names
  
  /** Parameter to include in document search */
  protected final static String NODE_FIND_PARAMETER = "findparameter";
  /** Metadata to include with indexed documents */
  protected final static String NODE_INCLUDED_METADATA = "includedmetadata";
  
  // These are attribute names, which are shared among the nodes
  
  /** A name */
  protected final static String ATTRIBUTE_NAME = "name";
  /** A value */
  protected final static String ATTRIBUTE_VALUE = "value";
  
  // These are the activity names
  
  /** Fetch activity */
  protected final static String ACTIVITY_FETCH = "fetch";
  
  // Local constants
  
  /** Session expiration time interval */
  protected final static long SESSION_EXPIRATION_MILLISECONDS = 300000L;
  
  // The global deny token
  
  /** Global deny token for Docs4U */
  public final static String GLOBAL_DENY_TOKEN = "DEAD_AUTHORITY";
  
  // Local variables.
  
  /** The root directory */
  protected String rootDirectory = null;
  
  /** The Docs4U API session */
  protected Docs4UAPI session = null;
  /** The expiration time of the Docs4U API session */
  protected long sessionExpiration = -1L;
  
  /** Constructor */
  public Docs4UConnector()
  {
    super();
  }

  /** Tell the world what model this connector uses for addSeedDocuments().
  * This must return a model value as specified above.  The connector does not have to be connected
  * for this method to be called.
  *@return the model type value.
  */
  public int getConnectorModel()
  {
    return MODEL_ADD_CHANGE;
  }

  /** Return the list of activities that this connector supports (i.e. writes into the log).
  * The connector does not have to be connected for this method to be called.
  *@return the list.
  */
  public String[] getActivitiesList()
  {
    return new String[]{ACTIVITY_FETCH};
  }

  /** Return the list of relationship types that this connector recognizes.
  * The connector does not need to be connected for this method to be called.
  *@return the list.
  */
  public String[] getRelationshipTypes()
  {
    return new String[0];
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
        Logging.connectors.warn("Docs4U: Session setup error: "+e.getMessage(),e);
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
  * repository connector.
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
        Logging.connectors.warn("Docs4U: Error checking repository: "+e.getMessage(),e);
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

  /** Queue "seed" documents.  Seed documents are the starting places for crawling activity.  Documents
  * are seeded when this method calls appropriate methods in the passed in ISeedingActivity object.
  *
  * This method can choose to find repository changes that happen only during the specified time interval.
  * The seeds recorded by this method will be viewed by the framework based on what the
  * getConnectorModel() method returns.
  *
  * It is not a big problem if the connector chooses to create more seeds than are
  * strictly necessary; it is merely a question of overall work required.
  *
  * The times passed to this method may be interpreted for greatest efficiency.  The time ranges
  * any given job uses with this connector will not overlap, but will proceed starting at 0 and going
  * to the "current time", each time the job is run.  For continuous crawling jobs, this method will
  * be called once, when the job starts, and at various periodic intervals as the job executes.
  *
  * When a job's specification is changed, the framework automatically resets the seeding start time to 0.  The
  * seeding start time may also be set to 0 on each job run, depending on the connector model returned by
  * getConnectorModel().
  *
  * Note that it is always ok to send MORE documents rather than less to this method.
  * The connector will be connected before this method can be called.
  *@param activities is the interface this method should use to perform whatever framework actions are desired.
  *@param spec is a document specification (that comes from the job).
  *@param startTime is the beginning of the time range to consider, inclusive.
  *@param endTime is the end of the time range to consider, exclusive.
  *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
  */
  public void addSeedDocuments(ISeedingActivity activities, DocumentSpecification spec,
    long startTime, long endTime, int jobMode)
    throws ManifoldCFException, ServiceInterruption
  {
    // Get a session handle
    Docs4UAPI currentSession = getSession();
    // Scan document specification for findparameter nodes
    int i = 0;
    while (i < spec.getChildCount())
    {
      SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_FIND_PARAMETER))
      {
        // Found a findparameter node.  Execute a Docs4U query based on it.
        String findParameterName = sn.getAttributeValue(ATTRIBUTE_NAME);
        String findParameterValue = sn.getAttributeValue(ATTRIBUTE_VALUE);
        Map findMap = new HashMap();
        findMap.put(findParameterName,findParameterValue);
        try
        {
          if (Logging.connectors.isDebugEnabled())
            Logging.connectors.debug("Docs4U: Finding documents where "+findParameterName+
              "= '"+findParameterValue+"'");
          D4UDocumentIterator iter = currentSession.findDocuments(new Long(startTime),
            new Long(endTime),findMap);
          while (iter.hasNext())
          {
            String docID = iter.getNext();
            // Add this to the job queue
            activities.addSeedDocument(docID);
          }
        }
        catch (InterruptedException e)
        {
          throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
        }
        catch (D4UException e)
        {
          Logging.connectors.warn("Docs4U: Error finding documents: "+e.getMessage(),e);
          throw new ManifoldCFException(e.getMessage(),e);
        }
      }
    }
  }

  /** Get document versions given an array of document identifiers.
  * This method is called for EVERY document that is considered. It is therefore important to perform
  * as little work as possible here.
  * The connector will be connected before this method can be called.
  *@param documentIdentifiers is the array of local document identifiers, as understood by this connector.
  *@param oldVersions is the corresponding array of version strings that have been saved for the document identifiers.
  *   A null value indicates that this is a first-time fetch, while an empty string indicates that the previous document
  *   had an empty version string.
  *@param activities is the interface this method should use to perform whatever framework actions are desired.
  *@param spec is the current document specification for the current job.  If there is a dependency on this
  * specification, then the version string should include the pertinent data, so that reingestion will occur
  * when the specification changes.  This is primarily useful for metadata.
  *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
  *@param usesDefaultAuthority will be true only if the authority in use for these documents is the default one.
  *@return the corresponding version strings, with null in the places where the document no longer exists.
  * Empty version strings indicate that there is no versioning ability for the corresponding document, and the document
  * will always be processed.
  */
  public String[] getDocumentVersions(String[] documentIdentifiers, String[] oldVersions, IVersionActivity activities,
    DocumentSpecification spec, int jobMode, boolean usesDefaultAuthority)
    throws ManifoldCFException, ServiceInterruption
  {
    // First, the metadata specified will affect the indexing of the document.
    // So, we need to put the metadata specification as it currently exists into the version string.
    List metadataNames = new ArrayList();
    int i = 0;
    while (i < spec.getChildCount())
    {
      SpecificationNode sn = spec.getChild(i++);
      if (sn.getType().equals(NODE_INCLUDED_METADATA))
        metadataNames.add(sn.getAttributeValue(ATTRIBUTE_NAME));
    }
    // Sort the list of metadata names, since it will be used for a version string
    String[] namesToVersion = (String[])metadataNames.toArray(new String[0]);
    java.util.Arrays.sort(namesToVersion);
    
    // Get the current docs4u session
    Docs4UAPI currentSession = getSession();
    // Prepare a place for the return values
    String[] rval = new String[documentIdentifiers.length];
    // Capture Docs4U exceptions
    try
    {
      i = 0;
      while (i < documentIdentifiers.length)
      {
        if (Logging.connectors.isDebugEnabled())
          Logging.connectors.debug("Docs4U: Getting update time for '"+documentIdentifiers[i]+"'");
        Long time = currentSession.getDocumentUpdatedTime(documentIdentifiers[i]);
        // A null return means the document doesn't exist
        if (time == null)
          rval[i] = null;
        else
        {
          StringBuffer versionBuffer = new StringBuffer();
          // Pack the metadata names.
          packList(versionBuffer,namesToVersion,'+');
          // Add the updated time.
          versionBuffer.append(time.toString());
          rval[i] = versionBuffer.toString();
        }
        i++;
      }
      return rval;
    }
    catch (InterruptedException e)
    {
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (D4UException e)
    {
      Logging.connectors.warn("Docs4U: Error versioning documents: "+e.getMessage(),e);
      throw new ManifoldCFException(e.getMessage(),e);
    }
  }

  /** Process a set of documents.
  * This is the method that should cause each document to be fetched, processed, and the results either added
  * to the queue of documents for the current job, and/or entered into the incremental ingestion manager.
  * The document specification allows this class to filter what is done based on the job.
  * The connector will be connected before this method can be called.
  *@param documentIdentifiers is the set of document identifiers to process.
  *@param versions is the corresponding document versions to process, as returned by getDocumentVersions() above.
  *       The implementation may choose to ignore this parameter and always process the current version.
  *@param activities is the interface this method should use to queue up new document references
  * and ingest documents.
  *@param spec is the document specification.
  *@param scanOnly is an array corresponding to the document identifiers.  It is set to true to indicate when the processing
  * should only find other references, and should not actually call the ingestion methods.
  *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
  */
  public void processDocuments(String[] documentIdentifiers, String[] versions, IProcessActivity activities,
    DocumentSpecification spec, boolean[] scanOnly, int jobMode)
    throws ManifoldCFException, ServiceInterruption
  {
    // Get the current session
    Docs4UAPI currentSession = getSession();
    // Capture exceptions from Docs4U
    try
    {
      int i = 0;
      while (i < documentIdentifiers.length)
      {
        // ScanOnly indicates that we should only extract, never index.
        if (!scanOnly[i])
        {
          String docID = documentIdentifiers[i];
          String version = versions[i];
          
          // Fetch and index the document.  First, we fetch, but we keep track of time.
          
          // Set up variables for recording the fetch activity status
          long startTime = System.currentTimeMillis();
          long dataSize = 0L;
          String status = "OK";
          String description = null;
          boolean fetchOccurred = false;
          
          D4UDocInfo docData = D4UFactory.makeDocInfo();
          try
          {
            // Get the document's URL first, so we don't have a potential race condition.
            String url = currentSession.getDocumentURL(docID);
            if (url == null || currentSession.getDocument(docID,docData) == false)
            {
              // Not found: delete it
              activities.deleteDocument(docID);
            }
            else
            {
              // Found: index it
              fetchOccurred = true;
              RepositoryDocument rd = new RepositoryDocument();
              InputStream is = docData.readData();
              if (is != null)
              {
                try
                {
                  // Set the contents
                  dataSize = docData.readDataLength().longValue();
                  rd.setBinary(is,dataSize);
                  
                  // Unpack metadata info
                  ArrayList metadataNames = new ArrayList();
                  unpackList(metadataNames,version,0,'+');
                  int j = 0;
                  while (j < metadataNames.size())
                  {
                    String metadataName = (String)metadataNames.get(j++);
                    // Get the value from the doc info object
                    String[] metadataValues = docData.getMetadata(metadataName);
                    if (metadataValues != null)
                    {
                      // Add to the repository document
                      rd.addField(metadataName,metadataValues);
                    }
                  }
                  
                  // Handle the security information
                  rd.setACL(docData.getAllowed());
                  // For disallowed, we must add in a global deny token
                  String[] disallowed = docData.getDisallowed();
                  List<String> list = new ArrayList<String>();
                  list.add(GLOBAL_DENY_TOKEN);
                  j = 0;
                  while (j < disallowed.length)
                  {
                    list.add(disallowed[j++]);
                  }
                  rd.setDenyACL(list.toArray(disallowed));
                  
                  // Index the document!
                  activities.ingestDocument(docID,version,url,rd);
                }
                finally
                {
                  is.close();
                }
              }
            }
          }
          catch (D4UException e)
          {
            status = "ERROR";
            description = e.getMessage();
            throw e;
          }
          finally
          {
            docData.close();
            // Record the status
            if (fetchOccurred)
              activities.recordActivity(new Long(startTime),ACTIVITY_FETCH,dataSize,docID,status,description,null);
          }
        }
        i++;
      }
    }
    catch (InterruptedIOException e)
    {
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (IOException e)
    {
      Logging.connectors.warn("Docs4U: Error transferring files: "+e.getMessage(),e);
      throw new ManifoldCFException(e.getMessage(),e);
    }
    catch (InterruptedException e)
    {
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (D4UException e)
    {
      Logging.connectors.warn("Docs4U: Error getting documents: "+e.getMessage(),e);
      throw new ManifoldCFException(e.getMessage(),e);
    }
  }

  /** Free a set of documents.  This method is called for all documents whose versions have been fetched using
  * the getDocumentVersions() method, including those that returned null versions.  It may be used to free resources
  * committed during the getDocumentVersions() method.  It is guaranteed to be called AFTER any calls to
  * processDocuments() for the documents in question.
  * The connector will be connected before this method can be called.
  *@param documentIdentifiers is the set of document identifiers.
  *@param versions is the corresponding set of version identifiers (individual identifiers may be null).
  */
  public void releaseDocumentVersions(String[] documentIdentifiers, String[] versions)
    throws ManifoldCFException
  {
    // Nothing needed
  }

  /** Get the maximum number of documents to amalgamate together into one batch, for this connector.
  * The connector does not need to be connected for this method to be called.
  *@return the maximum number. 0 indicates "unlimited".
  */
  public int getMaxDocumentRequest()
  {
    return 1;
  }

  // UI support methods.
  //
  // The UI support methods come in two varieties.  The first group (inherited from IConnector) is involved
  //  in setting up connection configuration information.
  //
  // The second group is listed here.  These methods are is involved in presenting and editing document specification
  //  information for a job.
  //
  // The two kinds of methods are accordingly treated differently, in that the first group cannot assume that
  // the current connector object is connected, while the second group can.  That is why the first group
  // receives a thread context argument for all UI methods, while the second group does not need one
  // (since it has already been applied via the connect() method).
    
  /** Output the specification header section.
  * This method is called in the head section of a job page which has selected a repository connection of the
  * current type.  Its purpose is to add the required tabs to the list, and to output any javascript methods
  * that might be needed by the job editing HTML.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param ds is the current document specification for this job.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  public void outputSpecificationHeader(IHTTPOutput out, DocumentSpecification ds, ArrayList tabsArray)
    throws ManifoldCFException, IOException
  {
    // Add the tabs
    tabsArray.add("Documents");
    tabsArray.add("Metadata");

    // Start the javascript
    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"
    );
    
    // Output the overall check function
    out.print(
"function checkSpecification()\n"+
"{\n"+
"  if (checkDocumentsTab() == false)\n"+
"    return false;\n"+
"  if (checkMetadataTab() == false)\n"+
"    return false;\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

    // Output a useful method which sets a specified command
    // value, and then re-posts the form using the supplied anchor
    out.print(
"function SpecOp(n, opValue, anchorvalue)\n"+
"{\n"+
"  eval(\"editjob.\"+n+\".value = \\\"\"+opValue+\"\\\"\");\n"+
"  postFormSetAnchor(anchorvalue);\n"+
"}\n"+
"\n"
    );

    // Output the actual javascript for the tabs
    outputDocumentsTabJavascript(out,ds);
    outputMetadataTabJavascript(out,ds);

    // Terminate the javascript tag
    out.print(
"//-->\n"+
"</script>\n"
    );
    
  }
    
  /** Output the javascript for the Documents tab.
  */
  protected void outputDocumentsTabJavascript(IHTTPOutput out, DocumentSpecification ds)
    throws ManifoldCFException, IOException
  {
    // The check function for this tab, which does nothing
    // (Called whenever the tab is navigated away from)
    out.print(
"function checkDocumentsTab()\n"+
"{\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

    // Delete a row from the displayed table
    out.print(
"function FindDelete(n)\n"+
"{\n"+
"  SpecOp(\"findop_\"+n, \"Delete\", \"find_\"+n);\n"+
"}\n"+
"\n"
    );
    
    // Add a row to the displayed table
    out.print(
"function FindAdd(n)\n"+
"{\n"+
"  if (editjob.findname.value == \"\")\n"+
"  {\n"+
"    alert(\"Please select a metadata name first.\");\n"+
"    editjob.findname.focus();\n"+
"    return;\n"+
"  }\n"+
"  if (editjob.findvalue.value == \"\")\n"+
"  {\n"+
"    alert(\"Metadata value cannot be blank.\");\n"+
"    editjob.findvalue.focus();\n"+
"    return;\n"+
"  }\n"+
"  SpecOp(\"findop\", \"Add\", \"find_\"+n);\n"+
"}\n"+
"\n"
    );
  }

  /** Output the javascript for the Metadata tab.
  */
  protected void outputMetadataTabJavascript(IHTTPOutput out, DocumentSpecification ds)
    throws ManifoldCFException, IOException
  {
    // The check function for this tab, which does nothing
    // (Called whenever the tab is navigated away from)
    out.print(
"function checkMetadataTab()\n"+
"{\n"+
"  return true;\n"+
"}\n"+
"\n"
    );

  }
  
  /** Output the specification body section.
  * This method is called in the body section of a job page which has selected a repository connection of the
  * current type.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate
  *  <html>, <body>, and <form> tags.  The name of the form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param ds is the current document specification for this job.
  *@param tabName is the current tab name.
  */
  public void outputSpecificationBody(IHTTPOutput out, DocumentSpecification ds, String tabName)
    throws ManifoldCFException, IOException
  {
    // Do the "Documents" tab
    outputDocumentsTab(out,ds,tabName);
    // Do the "Metadata" tab
    outputMetadataTab(out,ds,tabName);
  }
  
  /** Take care of "Documents" tab.
  */
  protected void outputDocumentsTab(IHTTPOutput out, DocumentSpecification ds, String tabName)
    throws ManifoldCFException, IOException
  {
    int i;
    int k;
    
    if (tabName.equals("Documents"))
    {
      // Present a table of all the metadata name/values we've done so far.
      // The table will have three columns: a column for the delete button, a column for the
      // metadata name, and a column for the metadata value.
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Matches:</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Metadata name</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Value</nobr></td>\n"+
"        </tr>\n"
      );
      
      i = 0;
      k = 0;
      while (i < ds.getChildCount())
      {
        SpecificationNode sn = ds.getChild(i++);
        // Look for FIND_PARAMETER nodes in the document specification
        if (sn.getType().equals(NODE_FIND_PARAMETER))
        {
          // Pull the metadata name and value from the FIND_PARAMETER node
          String findParameterName = sn.getAttributeValue(ATTRIBUTE_NAME);
          String findParameterValue = sn.getAttributeValue(ATTRIBUTE_VALUE);
          // We'll need a suffix for each row, used for form element names and
          // for anchor names.
          String findParameterSuffix = "_"+Integer.toString(k);
          // Output the row.
          out.print(
"        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <input type=\"hidden\" name=\"findop"+findParameterSuffix+"\" value=\"\"/>\n"+
"            <input type=\"hidden\" name=\"findname"+findParameterSuffix+"\" value=\""+
  Encoder.attributeEscape(findParameterName)+"\"/>\n"+
"            <input type=\"hidden\" name=\"findvalue"+findParameterSuffix+"\" value=\""+
  Encoder.attributeEscape(findParameterValue)+"\"/>\n"+
"            <a name=\""+"find_"+Integer.toString(k)+"\">\n"+
"              <input type=\"button\" value=\"Delete\" onClick='Javascript:FindDelete(\""+
  Integer.toString(k)+"\")' alt=\"Delete match #"+Integer.toString(k)+"\"/>\n"+
"            </a>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(findParameterName)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(findParameterValue)+"\n"+
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
"        <tr class=\"formrow\"><td class=\"formcolumnmessage\" colspan=\"3\">No documents specified</td></tr>\n"
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
"                <input type=\"button\" value=\"Add\" onClick='Javascript:FindAdd(\""+
  Integer.toString(k+1)+"\")' alt=\"Add new match\"/>\n"+
"                <input type=\"hidden\" name=\"findcount\" value=\""+Integer.toString(k)+"\"/>\n"+
"                <input type=\"hidden\" name=\"findop\" value=\"\"/>\n"+
"              </a>\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <select name=\"findname\">\n"+
"              <option value=\"\" selected=\"true\">--Select metadata name --</option>\n"
        );
      
        int q= 0;
        while (q < matchNames.length)
        {
          out.print(
"              <option value=\""+Encoder.attributeEscape(matchNames[q])+"\">"+
  Encoder.bodyEscape(matchNames[q])+"</option>\n"
          );
          q++;
        }
        
        out.print(
"            </select>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              <input type=\"text\" size=\"32\" name=\"findvalue\" value=\"\"/>\n"+
"            </nobr>\n"+
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
      
      // Finish off cell and add the final match value box.
      out.print(
"      </table>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      // Output the hiddens for the "Documents" tab.
      // This is a repeat of the logic for the displayed form, except only the pertinent hiddens
      // are output.
      k = 0;
      i = 0;
      while (i < ds.getChildCount())
      {
        SpecificationNode sn = ds.getChild(i++);
        // Look for FIND_PARAMETER nodes in the document specification
        if (sn.getType().equals(NODE_FIND_PARAMETER))
        {
          // Pull the metadata name and value from the FIND_PARAMETER node
          String findParameterName = sn.getAttributeValue(ATTRIBUTE_NAME);
          String findParameterValue = sn.getAttributeValue(ATTRIBUTE_VALUE);
          String findParameterSuffix = "_"+Integer.toString(k);
          // Output the row.
          out.print(
"<input type=\"hidden\" name=\"findname"+findParameterSuffix+"\" value=\""+
  Encoder.attributeEscape(findParameterName)+"\"/>\n"+
"<input type=\"hidden\" name=\"findvalue"+findParameterSuffix+"\" value=\""+
  Encoder.attributeEscape(findParameterValue)+"\"/>\n"
          );
          k++;
        }
      }
      out.print(
"<input type=\"hidden\" name=\"findcount\" value=\""+Integer.toString(k)+"\"/>\n"
      );
    }
  }
  
  /** Take care of "Metadata" tab.
  */
  protected void outputMetadataTab(IHTTPOutput out, DocumentSpecification ds, String tabName)
    throws ManifoldCFException, IOException
  {
    int i;
    
    // Do the "Metadata" tab
    if (tabName.equals("Metadata"))
    {
      // The tab is selected.  Output a description and a value, the value consisting
      // of a checkbox for every kind of metadata.
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Include:</nobr></td>\n"+
"    <td class=\"value\">\n"
      );
      
      // Get the allowed set of metadata
      // We need a try/catch block because an exception can be thrown when we query the
      // repository.
      try
      {
        String[] matchNames = getMetadataNames();
        // Loop through the current metadata selection and build a hash map
        i = 0;
        Map currentSelections = new HashMap();
        while (i < ds.getChildCount())
        {
          SpecificationNode sn = ds.getChild(i++);
          if (sn.getType().equals(NODE_INCLUDED_METADATA))
          {
            String metadataName = sn.getAttributeValue(ATTRIBUTE_NAME);
            currentSelections.put(metadataName,metadataName);
          }
        }
        // Now, loop through the available selections, and build a checkbox for each.
        // Checkboxes will be separated by <br/> tags.
        i = 0;
        while (i < matchNames.length)
        {
          String matchName = matchNames[i];
          boolean isChecked = (currentSelections.get(matchName) != null);
          out.print(
"      <input type=\"checkbox\" name=\"metadata\" value=\""+Encoder.attributeEscape(matchName)+"\""+
  (isChecked?" checked=\"true\"":"")+"/> "+Encoder.bodyEscape(matchName)+"<br/>\n"
          );
          i++;
        }
      }
      catch (ManifoldCFException e)
      {
        // If there was an error, display just the text
        out.print(
"        Error: "+Encoder.bodyEscape(e.getMessage())+"\n"
        );
      }
      catch (ServiceInterruption e)
      {
        out.print(
"        Transient error: "+Encoder.bodyEscape(e.getMessage())+"\n"
        );
      }

      out.print(
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      // The tab is not selected.  Output hidden form elements.
      i = 0;
      while (i < ds.getChildCount())
      {
        SpecificationNode sn = ds.getChild(i++);
        if (sn.getType().equals(NODE_INCLUDED_METADATA))
        {
          String metadataName = sn.getAttributeValue(ATTRIBUTE_NAME);
          out.print(
"<input type=\"hidden\" name=\"metadata\" value=\""+Encoder.attributeEscape(metadataName)+"\"/>\n"
          );
        }
      }
    }
  }
  
  /** Process a specification post.
  * This method is called at the start of job's edit or view page, whenever there is a possibility that form
  * data for a connection has been posted.  Its purpose is to gather form information and modify the
  * document specification accordingly.  The name of the posted form is always "editjob".
  * The connector will be connected before this method can be called.
  *@param variableContext contains the post data, including binary file-upload information.
  *@param ds is the current document specification for this job.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of
  * the job (and cause a redirection to an error page).
  */
  public String processSpecificationPost(IPostParameters variableContext, DocumentSpecification ds)
    throws ManifoldCFException
  {
    // Pick up the Documents tab data
    String rval = processDocumentsTab(variableContext,ds);
    if (rval != null)
      return rval;
    // Pick up the Metadata tab data
    rval = processMetadataTab(variableContext,ds);
    return rval;
  }
  
  /** Process form post for Documents tab.
  */
  protected String processDocumentsTab(IPostParameters variableContext, DocumentSpecification ds)
    throws ManifoldCFException
  {
    // Remove old find parameter document specification information
    removeNodes(ds,NODE_FIND_PARAMETER);
    
    // Parse the number of records that were posted
    String findCountString = variableContext.getParameter("findcount");
    if (findCountString != null)
    {
      int findCount = Integer.parseInt(findCountString);
              
      // Loop throught them and add to the new document specification information
      int i = 0;
      while (i < findCount)
      {
        String suffix = "_"+Integer.toString(i++);
        // Only add the name/value if the item was not deleted.
        String findParameterOp = variableContext.getParameter("findop"+suffix);
        if (findParameterOp == null || !findParameterOp.equals("Delete"))
        {
          String findParameterName = variableContext.getParameter("findname"+suffix);
          String findParameterValue = variableContext.getParameter("findvalue"+suffix);
          addFindParameterNode(ds,findParameterName,findParameterValue);
        }
      }
    }
      
    // Now, look for a global "Add" operation
    String operation = variableContext.getParameter("findop");
    if (operation != null && operation.equals("Add"))
    {
      // Pick up the global parameter name and value
      String findParameterName = variableContext.getParameter("findname");
      String findParameterValue = variableContext.getParameter("findvalue");
      addFindParameterNode(ds,findParameterName,findParameterValue);
    }

    return null;
  }


  /** Process form post for Metadata tab.
  */
  protected String processMetadataTab(IPostParameters variableContext, DocumentSpecification ds)
    throws ManifoldCFException
  {
    // Remove old included metadata nodes
    removeNodes(ds,NODE_INCLUDED_METADATA);

    // Get the posted metadata values
    String[] metadataNames = variableContext.getParameterValues("metadata");
    if (metadataNames != null)
    {
      // Add each metadata name as a node to the document specification
      int i = 0;
      while (i < metadataNames.length)
      {
        String metadataName = metadataNames[i++];
        addIncludedMetadataNode(ds,metadataName);
      }
    }
    
    return null;
  }

  /** Add a FIND_PARAMETER node to a document specification.
  */
  protected static void addFindParameterNode(DocumentSpecification ds,
    String findParameterName, String findParameterValue)
  {
    // Create a new specification node with the right characteristics
    SpecificationNode sn = new SpecificationNode(NODE_FIND_PARAMETER);
    sn.setAttribute(ATTRIBUTE_NAME,findParameterName);
    sn.setAttribute(ATTRIBUTE_VALUE,findParameterValue);
    // Add to the end
    ds.addChild(ds.getChildCount(),sn);
  }

  /** Add an INCLUDED_METADATA node to a document specification.
  */
  protected static void addIncludedMetadataNode(DocumentSpecification ds,
    String metadataName)
  {
    // Build the proper node
    SpecificationNode sn = new SpecificationNode(NODE_INCLUDED_METADATA);
    sn.setAttribute(ATTRIBUTE_NAME,metadataName);
    // Add to the end
    ds.addChild(ds.getChildCount(),sn);
  }
  
  /** Remove all of a specified node type from a document specification.
  */
  protected static void removeNodes(DocumentSpecification ds,
    String nodeTypeName)
  {
    int i = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i);
      if (sn.getType().equals(nodeTypeName))
        ds.removeChild(i);
      else
        i++;
    }
  }
  
  /** View specification.
  * This method is called in the body section of a job's view page.  Its purpose is to present the document
  * specification information to the user.  The coder can presume that the HTML that is output from
  * this configuration will be within appropriate <html> and <body> tags.
  * The connector will be connected before this method can be called.
  *@param out is the output to which any HTML should be sent.
  *@param ds is the current document specification for this job.
  */
  public void viewSpecification(IHTTPOutput out, DocumentSpecification ds)
    throws ManifoldCFException, IOException
  {
    out.print(
"<table class=\"displaytable\">\n"
    );
    viewDocumentsTab(out,ds);
    viewMetadataTab(out,ds);
    out.print(
"</table>\n"
    );
  }

  /** View the "Documents" tab contents
  */
  protected void viewDocumentsTab(IHTTPOutput out, DocumentSpecification ds)
    throws ManifoldCFException, IOException
  {
    int i;
    int k;
    
    out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Matches:</nobr></td>\n"+
"    <td class=\"boxcell\">\n"+
"      <table class=\"formtable\">\n"+
"        <tr class=\"formheaderrow\">\n"+
"          <td class=\"formcolumnheader\"><nobr>Metadata name</nobr></td>\n"+
"          <td class=\"formcolumnheader\"><nobr>Value</nobr></td>\n"+
"        </tr>\n"
    );
    
    i = 0;
    k = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(NODE_FIND_PARAMETER))
      {
        String findParameterName = sn.getAttributeValue(ATTRIBUTE_NAME);
        String findParameterValue = sn.getAttributeValue(ATTRIBUTE_VALUE);
        out.print(
"        <tr class=\""+(((k % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(findParameterName)+"\n"+
"            </nobr>\n"+
"          </td>\n"+
"          <td class=\"formcolumncell\">\n"+
"            <nobr>\n"+
"              "+Encoder.bodyEscape(findParameterValue)+"\n"+
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
  
  /** View the "Metadata" tab contents
  */
  protected void viewMetadataTab(IHTTPOutput out, DocumentSpecification ds)
    throws ManifoldCFException, IOException
  {
    // Output included metadata
    out.print(
"  <tr>\n"+
"    <td class=\"description\"><nobr>Included:</nobr></td>\n"+
"    <td class=\"value\">\n"
    );
    
    boolean seenData = false;
    int i = 0;
    while (i < ds.getChildCount())
    {
      SpecificationNode sn = ds.getChild(i++);
      if (sn.getType().equals(NODE_INCLUDED_METADATA))
      {
        String metadataName = sn.getAttributeValue(ATTRIBUTE_NAME);
        if (seenData)
          out.print(", ");
        out.print(Encoder.bodyEscape(metadataName));
        seenData = true;
      }
    }
    
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

