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
package org.apache.manifoldcf.authorities.authorities.docs4u;

// These are the basic interfaces we'll need from ManifoldCF
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.authorities.interfaces.*;

// Utility includes
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;

// This is where we get pull-agent system loggers
import org.apache.manifoldcf.authorities.system.Logging;

// This class implements system-wide static methods
import org.apache.manifoldcf.authorities.system.ManifoldCF;

// This is the base authority class.
import org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector;

// Here's the UI helper classes.
import org.apache.manifoldcf.ui.util.Encoder;

// Here are the imports that are specific for this connector
import org.apache.manifoldcf.examples.docs4u.Docs4UAPI;
import org.apache.manifoldcf.examples.docs4u.D4UFactory;
import org.apache.manifoldcf.examples.docs4u.D4UException;

/** This is the Docs4U authority connector class.  This extends the base authorities class,
* which implements IAuthorityConnector, and provides some degree of insulation from future
* changes to the IAuthorityConnector interface.  It also provides us with basic support for
* the connector lifecycle methods, so we don't have to implement those each time.
*/
public class Docs4UAuthorityConnector extends BaseAuthorityConnector
{
  // These are the configuration parameter names
  
  /** Repository root parameter */
  protected final static String PARAMETER_REPOSITORY_ROOT = "rootdirectory";

  /** This is the active directory global deny token.  This should be ingested with all documents. */
  public static final String globalDenyToken = GLOBAL_DENY_TOKEN;
  
  // The prebuilt authorization responses for error conditions
  
  /** Unreachable Docs4U */
  private static final AuthorizationResponse unreachableResponse = new AuthorizationResponse(
    new String[]{globalDenyToken},AuthorizationResponse.RESPONSE_UNREACHABLE);
  /** User not found */
  private static final AuthorizationResponse userNotFoundResponse = new AuthorizationResponse(
    new String[]{globalDenyToken},AuthorizationResponse.RESPONSE_USERNOTFOUND);

  // Local constants
  
  /** Session expiration time interval */
  protected final static long SESSION_EXPIRATION_MILLISECONDS = 300000L;
  
  // Cache manager.
  
  /** The cache manager. */
  protected ICacheManager cacheManager = null;

  // Local variables.
  
  /** The root directory */
  protected String rootDirectory = null;

  /** The Docs4U API session */
  protected Docs4UAPI session = null;
  /** The expiration time of the Docs4U API session */
  protected long sessionExpiration = -1L;
  
  /** Constructor */
  public Docs4UAuthorityConnector()
  {
    super();
  }

  /** Set thread context.
  */
  @Override
  public void setThreadContext(IThreadContext tc)
    throws ManifoldCFException
  {
    super.setThreadContext(tc);
    cacheManager = CacheManagerFactory.make(tc);
  }
  
  /** Clear thread context.
  */
  @Override
  public void clearThreadContext()
  {
    super.clearThreadContext();
    cacheManager = null;
  }

  /** Output the configuration header section.
  * This method is called in the head section of the connector's configuration page.  Its purpose is to
  * add the required tabs to the list, and to output any javascript methods that might be needed by
  * the configuration editing HTML.
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the desired locale of the output.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, List<String> tabsArray)
    throws ManifoldCFException, IOException
  {
    tabsArray.add("Repository");
    tabsArray.add("User Mapping");
    Messages.outputResourceWithVelocity(out,locale,"ConfigurationHeader.html",null);
  }

  /** Output the configuration body section.
  * This method is called in the body section of the connector's configuration page.  Its purpose is to
  * present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within
  * appropriate <html>, <body>, and <form> tags.  The name of the form is always "editconnection".
  * The connector does not need to be connected for this method to be called.
  *@param threadContext is the local thread context.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the desired output locale.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  *@param tabName is the current tab name.
  */
  @Override
  public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters, String tabName)
    throws ManifoldCFException, IOException
  {
    // Output the Repository tab
    Map<String,Object> velocityContext = new HashMap<String,Object>();
    velocityContext.put("TabName",tabName);
    fillInRepositoryTab(velocityContext,parameters);
    Messages.outputResourceWithVelocity(out,locale,"Configuration_Repository.html",velocityContext);
    
  }
  
  /** Fill in velocity parameters for Repository tab.
  */
  protected static void fillInRepositoryTab(Map<String,Object> velocityContext,
    ConfigParams parameters)
  {
    String repositoryRoot = parameters.getParameter(PARAMETER_REPOSITORY_ROOT);
    if (repositoryRoot == null)
      repositoryRoot = "";
    velocityContext.put("repositoryroot",repositoryRoot);
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
  @Override
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
  *@param locale is the locale that the html should be output with.
  *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
  */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
    Locale locale, ConfigParams parameters)
    throws ManifoldCFException, IOException
  {
    Map<String,Object> velocityContext = new HashMap<String,Object>();
    fillInRepositoryTab(velocityContext,parameters);
    Messages.outputResourceWithVelocity(out,locale,"ConfigurationView.html",velocityContext);
  }
  
  /** Get the current session, or create one if not valid.
  */
  protected Docs4UAPI getSession()
    throws ManifoldCFException
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
        Logging.authorityConnectors.warn("Docs4U: Session setup error: "+e.getMessage(),e);
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
  @Override
  public void connect(ConfigParams configParameters)
  {
    super.connect(configParameters);
    rootDirectory = configParameters.getParameter(PARAMETER_REPOSITORY_ROOT);
  }

  /** Close the connection.  Call this before discarding this instance of the
  * repository connector.
  */
  @Override
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
  @Override
  public String check()
    throws ManifoldCFException
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
      Logging.authorityConnectors.warn("Docs4U: Error checking repository: "+e.getMessage(),e);
      return "Error: "+e.getMessage();
    }
    // If it passed, return "everything ok" message
    return super.check();
  }

  /** This method is periodically called for all connectors that are connected but not
  * in active use.
  */
  @Override
  public void poll()
    throws ManifoldCFException
  {
    if (session != null)
    {
      if (System.currentTimeMillis() >= sessionExpiration)
        expireSession();
    }
  }

  /** Obtain the access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the response tokens (according to the current authority).
  * (Should throws an exception only when a condition cannot be properly described within the authorization response object.)
  */
  @Override
  public AuthorizationResponse getAuthorizationResponse(String userName)
    throws ManifoldCFException
  {
    if (Logging.authorityConnectors.isDebugEnabled())
      Logging.authorityConnectors.debug("Docs4U: Received request for user '"+userName+"'");
    
    ICacheDescription objectDescription = new AuthorizationResponseDescription(userName,rootDirectory);
    
    // Enter the cache
    ICacheHandle ch = cacheManager.enterCache(new ICacheDescription[]{objectDescription},null,null);
    try
    {
      ICacheCreateHandle createHandle = cacheManager.enterCreateSection(ch);
      try
      {
        // Lookup the object
        AuthorizationResponse response = (AuthorizationResponse)cacheManager.lookupObject(createHandle,objectDescription);
        if (response != null)
          return response;
        // Create the object.
        response = getAuthorizationResponseUncached(userName);
        // Save it in the cache
        cacheManager.saveObject(createHandle,objectDescription,response);
        // And return it...
        return response;
      }
      finally
      {
        cacheManager.leaveCreateSection(createHandle);
      }
    }
    finally
    {
      cacheManager.leaveCache(ch);
    }
  }
  
  /** Uncached version of the getAuthorizationResponse method.
  *@param userName is the user name or identifier.
  *@return the response tokens (according to the current authority).
  * (Should throws an exception only when a condition cannot be properly described within the authorization response object.)
  */
  protected AuthorizationResponse getAuthorizationResponseUncached(String userName)
    throws ManifoldCFException
  {
    if (Logging.authorityConnectors.isDebugEnabled())
      Logging.authorityConnectors.debug("Docs4U: Calculating response access tokens for user '"+userName+"'");

    // Map the user to the final value; no mapping here!  Expected to be done by a regexp mapper upstream...
    String d4uUser = userName;

    if (Logging.authorityConnectors.isDebugEnabled())
      Logging.authorityConnectors.debug("Docs4U: Mapped user name is '"+d4uUser+"'");
    
    // Set up the session
    Docs4UAPI currentSession = getSession();
    
    try
    {
      // Find the user
      String userID = currentSession.findUser(d4uUser);
      if (userID == null)
        return userNotFoundResponse;
      // Find the user's groups
      String[] groupIDs = currentSession.getUserOrGroupGroups(userID);
      if (groupIDs == null)
        return userNotFoundResponse;
      // Construct an AuthorizationResponse from the set
      String[] tokens = new String[groupIDs.length+1];
      int i = 0;
      while (i < groupIDs.length)
      {
        tokens[i] = groupIDs[i];
        i++;
      }
      tokens[i] = userID;
      return new AuthorizationResponse(tokens,AuthorizationResponse.RESPONSE_OK);
    }
    catch (InterruptedException e)
    {
      throw new ManifoldCFException(e.getMessage(),e,ManifoldCFException.INTERRUPTED);
    }
    catch (D4UException e)
    {
      Logging.authorityConnectors.error("Docs4U: Authority error: "+e.getMessage(),e);
      throw new ManifoldCFException("Docs4U: Authority error: "+e.getMessage(),e);
    }
  }

  /** Obtain the default access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the default response tokens, presuming that the connect method fails.
  */
  @Override
  public AuthorizationResponse getDefaultAuthorizationResponse(String userName)
  {
    return unreachableResponse;
  }

  protected static long responseLifetime = 60000L;
  protected static int LRUsize = 1000;
  protected static StringSet emptyStringSet = new StringSet();
  
  /** This is the cache object descriptor for cached access tokens from
  * this connector.
  */
  protected static class AuthorizationResponseDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription
  {
    /** The user name associated with the access tokens */
    protected String userName;
    /** The repository path */
    protected String repositoryRoot;
    /** The expiration time */
    protected long expirationTime = -1;
    
    /** Constructor. */
    public AuthorizationResponseDescription(String userName, String repositoryRoot)
    {
      super("Docs4UAuthority",LRUsize);
      this.userName = userName;
      this.repositoryRoot = repositoryRoot;
    }

    /** Return the invalidation keys for this object. */
    public StringSet getObjectKeys()
    {
      return emptyStringSet;
    }

    /** Get the critical section name, used for synchronizing the creation of the object */
    public String getCriticalSectionName()
    {
      return getClass().getName() + "-" + userName + "-" + repositoryRoot;
    }

    /** Return the object expiration interval */
    public long getObjectExpirationTime(long currentTime)
    {
      if (expirationTime == -1)
        expirationTime = currentTime + responseLifetime;
      return expirationTime;
    }

    public int hashCode()
    {
      return userName.hashCode() + repositoryRoot.hashCode();
    }
    
    public boolean equals(Object o)
    {
      if (!(o instanceof AuthorizationResponseDescription))
        return false;
      AuthorizationResponseDescription ard = (AuthorizationResponseDescription)o;
      return ard.userName.equals(userName) && ard.repositoryRoot.equals(repositoryRoot);
    }
    
  }

}
