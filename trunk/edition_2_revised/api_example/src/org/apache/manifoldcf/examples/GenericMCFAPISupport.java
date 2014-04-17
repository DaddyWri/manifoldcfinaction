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

import java.io.*;
import java.net.*;
import org.apache.manifoldcf.core.interfaces.*;

/** Generic support for necessary ManifoldCF API functions.
* You will find nothing specific to individual connectors in this
* class, but you will find many useful API invocation methods.
* This class is obviously not complete; for the example I have
* provided only what we needed, and it is left as an exercise for
* the student to provide a complete set.
*/
public class GenericMCFAPISupport extends ManifoldCFAPIConnect
{
  // Node types
  /** Error node */
  protected final static String NODETYPE_ERROR = "error";
  /** Connection status node */
  protected final static String NODETYPE_CHECKRESULT = "check_result";
  /** Repository connection node */
  protected final static String NODETYPE_REPOSITORYCONNECTION =
    "repositoryconnection";
  /** Job node */
  protected final static String NODETYPE_JOB = "job";
  /** Job id node */
  protected final static String NODETYPE_ID = "id";
  /** Description node */
  protected final static String NODETYPE_DESCRIPTION = "description";
  /** Jobstatus node */
  protected final static String NODETYPE_JOBSTATUS = "jobstatus";
  /** Status node */
  protected final static String NODETYPE_STATUS = "status";
  /** Isnew node */
  protected final static String NODETYPE_ISNEW = "isnew";
  /** Class_name node */
  protected final static String NODETYPE_CLASS_NAME = "class_name";
  /** Configuration node */
  protected final static String NODETYPE_CONFIGURATION = "configuration";
  /** Match node */
  protected final static String NODETYPE_MATCH = "match";
  /** Match description */
  protected final static String NODETYPE_MATCH_DESCRIPTION =
    "match_description";
  /** Throttle */
  protected final static String NODETYPE_THROTTLE = "throttle";
  /** Max connections */
  protected final static String NODETYPE_MAX_CONNECTIONS =
    "max_connections";
  /** Rate */
  protected final static String NODETYPE_RATE = "rate";
  /** Start mode */
  protected final static String NODETYPE_START_MODE = "start_mode";
  /** Reseed interval */
  protected final static String NODETYPE_RESEED_INTERVAL =
    "reseed_interval";
  /** Run mode */
  protected final static String NODETYPE_RUN_MODE = "run_mode";
  /** Hopcount mode */
  protected final static String NODETYPE_HOPCOUNT_MODE = "hopcount_mode";
  /** Output specification */
  protected final static String NODETYPE_OUTPUT_SPECIFICATION =
    "output_specification";
  /** Document specification */
  protected final static String NODETYPE_DOCUMENT_SPECIFICATION =
    "document_specification";
  /** Rescan interval */
  protected static final String NODETYPE_RESCAN_INTERVAL =
    "recrawl_interval";
  /** Expiration interval */
  protected static final String NODETYPE_EXPIRATION_INTERVAL =
    "expiration_interval";
  /** Repository connection */
  protected static final String NODETYPE_REPOSITORY_CONNECTION =
    "repository_connection";
  /** Output connection */
  protected static final String NODETYPE_OUTPUT_CONNECTION =
    "output_connection";
  /** Job priority */
  protected static final String NODETYPE_PRIORITY = "priority";
  /** Job identifier */
  protected static final String NODETYPE_JOB_ID = "job_id";

  /** Constructor.
  *@param baseURL is the base URL of the ManifoldCF api service.
  */
  public GenericMCFAPISupport(String baseURL)
  {
    super(baseURL);
  }
  
  /** Check whether or not a given output connection has a
  * satisfactory status.
  *@param connectionName is the name of the connection.
  *@return null if the status is OK, or a status message if it is not.
  */
  public String checkOutputConnection(String connectionName)
    throws IOException, ManifoldCFException
  {
    String connectionURI = 
      "/outputconnections/"+connectionNameEncode(connectionName);
    return getConnectionStatus(performAPIGetOperation(connectionURI));
  }
  
  /** Check whether or not a given repository connection exists.
  *@param connectionName is the name of the connection.
  *@return true if the connection exists, false otherwise.
  */
  public boolean doesRepositoryConnectionExist(String connectionName)
    throws IOException, ManifoldCFException
  {
    String connectionURI =
      "/repositoryconnections/"+connectionNameEncode(connectionName);
    return getRepositoryConnection(
      performAPIGetOperation(connectionURI)) != null;
  }
  
  
  /** Check if a given job exists.
  *@param jobName is the name of the job.
  *@return the jobid if it exists, null otherwise.
  */
  public String findJobID(String jobName)
    throws IOException, ManifoldCFException
  {
    String jobsURI = "/jobs";
    Configuration response = performAPIGetOperation(jobsURI);
    // Scan for a job that matches this name.
    // Note that the name does not need to be unique, so we will
    // return only the first match.
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
      else if (node.getType().equals(NODETYPE_JOB))
      {
        String currentJobName = "";
        String currentJobID = null;
        int j = 0;
        while (j < node.getChildCount())
        {
          ConfigurationNode jobNode = node.findChild(j++);
          if (jobNode.getType().equals(NODETYPE_DESCRIPTION))
            currentJobName = jobNode.getValue();
          else if (jobNode.getType().equals(NODETYPE_ID))
            currentJobID = jobNode.getValue();
        }
        if (currentJobName.equals(jobName))
          return currentJobID;
      }
    }
    // No match found.
    return null;
  }
  
  /** Get a job's document specification.
  *@param jobID is the job identifier.
  *@return the document specification node hierarchy.
  */
  public Configuration getJobDocumentSpecification(String jobID)
    throws IOException, ManifoldCFException
  {
    String jobURI = "/jobs/"+jobID;
    Configuration job = getJob(performAPIGetOperation(jobURI));
    if (job == null)
      return null;
    // Job node found.  Locate the document specification.
    int j = 0;
    while (j < job.getChildCount())
    {
      ConfigurationNode jobNode = job.findChild(j++);
      if (jobNode.getType().equals(NODETYPE_DOCUMENT_SPECIFICATION))
        return makeRootHierarchy(jobNode);
    }
    // No document specification found!
    return null;
  }
  
  /** Set a job's document specification.
  */
  public void setJobDocumentSpecification(String jobID,
    Configuration documentSpecification)
    throws IOException, ManifoldCFException
  {
    String jobURI = "/jobs/"+jobID;
    Configuration job = getJob(performAPIGetOperation(jobURI));
    if (job == null)
      return;
    removeFromChildren(job,NODETYPE_DOCUMENT_SPECIFICATION);
    addChildHierarchy(job,NODETYPE_DOCUMENT_SPECIFICATION,
      documentSpecification);
    // Update the job.
    Configuration root = new Configuration();
    addChildHierarchy(root,NODETYPE_JOB,job);
    errorCheck(performAPIPutOperation(jobURI,root));
  }
  
  /** Get a job's status.
  *@param jobID is the job identifier.
  *@return the job status string.
  */
  public String getJobStatus(String jobID)
    throws IOException, ManifoldCFException
  {
    String jobstatusURI = "/jobstatuses/"+jobID;
    return getJobStatus(performAPIGetOperation(jobstatusURI));
  }
  

  /** Create a repository connection.
  *@param connectionName is the name of the connection.
  *@param connectionDescription is the description for the connection.
  *@param connectionClass is the connector class to use.
  *@param maxConnections is the maximum number of connections to allow.
  *@param maxAverageFetchRate is the maximum average documents
  * per millisecond.
  *@param connectionConfiguration is a Configuration structure describing
  * the connection configuration.
  */
  public void createRepositoryConnection(String connectionName,
    String connectionDescription,
    String connectionClass,
    int maxConnections,
    Double maxAverageFetchRate,
    Configuration connectionConfiguration)
    throws IOException, ManifoldCFException
  {
    String connectionURI =
      "/repositoryconnections/"+connectionNameEncode(connectionName);
    Configuration root = new Configuration();
    ConfigurationNode repositoryConnection =
      new ConfigurationNode(NODETYPE_REPOSITORYCONNECTION);
    addSimpleValueNode(repositoryConnection,NODETYPE_ISNEW,"true");
    if (connectionDescription != null)
      addSimpleValueNode(repositoryConnection,NODETYPE_DESCRIPTION,
        connectionDescription);
    addSimpleValueNode(repositoryConnection,NODETYPE_CLASS_NAME,
      connectionClass);
    addSimpleValueNode(repositoryConnection,NODETYPE_MAX_CONNECTIONS,
      Integer.toString(maxConnections));
    ConfigurationNode node = new ConfigurationNode(NODETYPE_THROTTLE);
    if (maxAverageFetchRate != null)
    {
      addSimpleValueNode(node,NODETYPE_MATCH,"");
      addSimpleValueNode(node,NODETYPE_MATCH_DESCRIPTION,"All");
      addSimpleValueNode(node,NODETYPE_RATE,
        maxAverageFetchRate.toString());
    }
    repositoryConnection.addChild(repositoryConnection.getChildCount(),
      node);
    // Copy connection configuration
    addChildHierarchy(repositoryConnection,NODETYPE_CONFIGURATION,
      connectionConfiguration);
    root.addChild(root.getChildCount(),repositoryConnection);
    errorCheck(performAPIPutOperation(connectionURI,root));
  }
  
  /** Create a continuous job.
  *@param description is the job's textual description.
  *@param repositoryConnectionName is the job's repository connection.
  *@param outputConnectionName is the job's output connection.
  *@param priority is the document priority.
  *@param expirationInterval is the number of milliseconds
  * before expiration of a document.
  *@param rescanInterval is the number of milliseconds before a
  * document should be rescanned.
  *@param reseedInterval is the number of milliseconds before
  * reseeding should take place.
  *@param documentSpecification is a Configuration describing the job's
  * document specification.
  *@param outputSpecification is a Configuration structure describing
  * the job's
  * output specification.
  *@return the job identifier.
  */
  public String createContinuousJob(String description,
    String repositoryConnectionName, String outputConnectionName,
    int priority,
    Long expirationInterval,
    Long rescanInterval,
    Long reseedInterval,
    Configuration documentSpecification,
    Configuration outputSpecification)
    throws IOException, ManifoldCFException
  {
    String createJobURI = "/jobs";
    Configuration root = new Configuration();
    ConfigurationNode jobNode = new ConfigurationNode(NODETYPE_JOB);
    addSimpleValueNode(jobNode,NODETYPE_DESCRIPTION,description);
    addSimpleValueNode(jobNode,NODETYPE_REPOSITORY_CONNECTION,
      repositoryConnectionName);
    addSimpleValueNode(jobNode,NODETYPE_OUTPUT_CONNECTION,
      outputConnectionName);
    addSimpleValueNode(jobNode,NODETYPE_PRIORITY,
      Integer.toString(priority));
    addSimpleValueNode(jobNode,NODETYPE_START_MODE,"manual");
    addSimpleValueNode(jobNode,NODETYPE_RUN_MODE,"continuous");
    addSimpleValueNode(jobNode,NODETYPE_HOPCOUNT_MODE,"accurate");
    if (reseedInterval != null)
      addSimpleValueNode(jobNode,NODETYPE_RESEED_INTERVAL,
        reseedInterval.toString());
    else
      addSimpleValueNode(jobNode,NODETYPE_RESEED_INTERVAL,"infinite");
    if (rescanInterval != null)
      addSimpleValueNode(jobNode,NODETYPE_RESCAN_INTERVAL,
        rescanInterval.toString());
    else
      addSimpleValueNode(jobNode,NODETYPE_RESCAN_INTERVAL,"infinite");
    if (expirationInterval != null)
      addSimpleValueNode(jobNode,NODETYPE_EXPIRATION_INTERVAL,
        expirationInterval.toString());
    else
      addSimpleValueNode(jobNode,NODETYPE_EXPIRATION_INTERVAL,"infinite");
    addChildHierarchy(jobNode,NODETYPE_OUTPUT_SPECIFICATION,
      outputSpecification);
    addChildHierarchy(jobNode,NODETYPE_DOCUMENT_SPECIFICATION,
      documentSpecification);
    root.addChild(root.getChildCount(),jobNode);
    
    return getJobID(performAPIPostOperation(createJobURI,root));
  }
  
  /** Start a job.
  *@param jobID is the job identifier.
  */
  public void startJob(String jobID)
    throws IOException, ManifoldCFException
  {
    String startURI = "/start/"+jobID;
    errorCheck(performAPIPutOperation(startURI,new Configuration()));
  }

  // Protected methods
  
  /** Encode a ManifoldCF connection name, according to the
  * API specification.
  *@param connectionName is the connection name.
  *@return the encoded connection name.
  */
  protected static String connectionNameEncode(String connectionName)
    throws UnsupportedEncodingException
  {
    StringBuffer sb = new StringBuffer();
    int i = 0;
    while (i < connectionName.length())
    {
      char x = connectionName.charAt(i++);
      if (x == '/')
        sb.append('.').append('+');
      else if (x == '.')
        sb.append('.').append('.');
      else
        sb.append(x);
    }
    return URLEncoder.encode(sb.toString(),"utf-8").replace("+","%20");
  }

  
  /** Check for error response.
  *@param response is the response hierarchy.
  */
  protected static void errorCheck(Configuration response)
    throws ManifoldCFException
  {
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
    }
  }
  
  /** Look for connection status from a response.
  *@param response is the response hierarchy.
  *@return the status, or null if not found.
  */
  protected static String getConnectionStatus(Configuration response)
    throws ManifoldCFException
  {
    // This should be empty (if no connection was found), or it
    // should contain connection status, or error.
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
      else if (node.getType().equals(NODETYPE_CHECKRESULT))
        // The actual status is contained as the value.
        return node.getValue();
    }
    // Nothing found: null result.
    return null;
  }
  
  /** Look for a job id given a response hierarchy.
  *@param response is the response hierarchy.
  *@return the job identifier, or null if it wasn't in the response.
  */
  protected static String getJobID(Configuration response)
    throws ManifoldCFException
  {
    int j = 0;
    String rval = null;
    while (j < response.getChildCount())
    {
      ConfigurationNode cn = response.findChild(j++);
      if (cn.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+cn.getValue());
      else if (cn.getType().equals(NODETYPE_JOB_ID))
        rval = cn.getValue();
    }
    return rval;
  }
  
  /** Get a job status given a response hierarchy.
  *@param response is the response hierarchy.
  *@return the job status, or null if it could not be found.
  */
  protected static String getJobStatus(Configuration response)
    throws ManifoldCFException
  {
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
      else if (node.getType().equals(NODETYPE_JOBSTATUS))
      {
        int j = 0;
        while (j < node.getChildCount())
        {
          ConfigurationNode child = node.findChild(j++);
          if (child.getType().equals(NODETYPE_STATUS))
            return child.getValue();
        }
      }
    }
    return null;
  }

  protected static Configuration getJob(Configuration response)
    throws ManifoldCFException
  {
    // Look for the job record in the response.
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
      else if (node.getType().equals(NODETYPE_JOB))
        return makeRootHierarchy(node);
    }
    return null;
  }
  
  /** Check response for whether repository connection exists or not.
  *@param response is the response hierarchy.
  *@return the repository connection hierarchy, or null if it does
  * not exist.
  */
  protected static Configuration getRepositoryConnection(
    Configuration response)
    throws ManifoldCFException
  {
    // This should be empty (if no connection was found), or it
    // should contain connection data, or error.
    int i = 0;
    while (i < response.getChildCount())
    {
      ConfigurationNode node = response.findChild(i++);
      if (node.getType().equals(NODETYPE_ERROR))
        // Standard practice for error nodes is to
        // throw ManifoldCFException
        throw new ManifoldCFException("Server error: "+node.getValue());
      else if (node.getType().equals(NODETYPE_REPOSITORYCONNECTION))
        return makeRootHierarchy(node);
    }
    // Nothing found: null result.
    return null;
  }

  // Node manipulation shortcut methods
  
  /** Add a simple value node to a parent.
  *@param node is the parent.
  *@param nodeType is the type of node to add.
  *@param value is its value.
  */
  protected static void addSimpleValueNode(IHierarchyParent node,
    String nodeType, String value)
  {
    ConfigurationNode addNode = new ConfigurationNode(nodeType);
    addNode.setValue(value);
    node.addChild(node.getChildCount(),addNode);
  }

  /** Remove all children matching a given type from a parent.
  *@param node is the parent.
  *@param nodeType is the type to remove.
  */
  protected static void removeFromChildren(IHierarchyParent node,
    String nodeType)
  {
    // Look for document specification child.
    int j = 0;
    while (j < node.getChildCount())
    {
      ConfigurationNode jobNode = node.findChild(j);
      if (jobNode.getType().equals(nodeType))
        // Found the correct node.  Remove it.
        node.removeChild(j);
      else
        j++;
    }
  }

  protected static void addChildHierarchy(IHierarchyParent node,
    String nodeType, Configuration source)
  {
    ConfigurationNode newNode = new ConfigurationNode(nodeType);
    if (source != null)
    {
      int i = 0;
      while (i < source.getChildCount())
      {
        newNode.addChild(newNode.getChildCount(),source.findChild(i++));
      }
    }
    node.addChild(node.getChildCount(),newNode);
  }
  
  protected static Configuration makeRootHierarchy(IHierarchyParent node)
  {
    Configuration rval = new Configuration();
    int i = 0;
    while (i < node.getChildCount())
    {
      rval.addChild(rval.getChildCount(),node.findChild(i++));
    }
    return rval;
  }

  protected static void addParameterNode(IHierarchyParent parent,
    String parameter, String value)
  {
    ConfigurationNode node = new ConfigurationNode("_PARAMETER_");
    node.setAttribute("name",parameter);
    node.setValue(value);
    parent.addChild(parent.getChildCount(),node);
  }

  protected static void addAttributeNode(IHierarchyParent parent,
    String nodeType, String attributeName, String attributeValue)
  {
    ConfigurationNode node = new ConfigurationNode(nodeType);
    node.setAttribute(attributeName,attributeValue);
    parent.addChild(parent.getChildCount(),node);
  }
}
