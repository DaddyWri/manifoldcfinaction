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
import java.util.ArrayList;

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
  /** User name mapping parameter */
  protected final static String PARAMETER_USERMAPPING = "usermapping";

  // Local constants
  
  /** Session expiration time interval */
  protected final static long SESSION_EXPIRATION_MILLISECONDS = 300000L;
  
  // Local variables.
  
  /** The root directory */
  protected String rootDirectory = null;
  /** Match map for username mapping */
  protected MatchMap matchMap = null;

  /** The Docs4U API session */
  protected Docs4UAPI session = null;
  /** The expiration time of the Docs4U API session */
  protected long sessionExpiration = -1L;
  
  /** Constructor */
  public Docs4UAuthorityConnector()
  {
    super();
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
    tabsArray.add("User Mapping");
    out.print(
"<script type=\"text/javascript\">\n"+
"<!--\n"+
"function checkConfig()\n"+
"{\n"+
"  if (editconnection.usernameregexp.value != \"\" && !isRegularExpression(editconnection.usernameregexp.value))\n"+
"  {\n"+
"    alert(\"User name regular expression must be a valid regular expression\");\n"+
"    editconnection.usernameregexp.focus();\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"\n"+
"function checkConfigForSave()\n"+
"{\n"+
"  if (editconnection.repositoryroot.value == \"\")\n"+
"  {\n"+
"    alert(\"Enter a repository root\");\n"+
"    SelectTab(\"Repository\");\n"+
"    editconnection.repositoryroot.focus();\n"+
"    return false;\n"+
"  }\n"+
"  if (editconnection.usernameregexp.value == \"\")\n"+
"  {\n"+
"    alert(\"User name regular expression cannot be null\");\n"+
"    SelectTab(\"User Mapping\");\n"+
"    editconnection.usernameregexp.focus();\n"+
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
    String userMappingString = parameters.getParameter(PARAMETER_USERMAPPING);
    MatchMap localMap;
    if (userMappingString != null)
      localMap = new MatchMap(userMappingString);
    else
    {
      localMap = new MatchMap();
      localMap.appendMatchPair("(.*)","$(1)");
    }
    String usernameRegexp = localMap.getMatchString(0);
    String userTranslation = localMap.getReplaceString(0);

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
    
    if (tabName.equals("User Mapping"))
    {
      out.print(
"<table class=\"displaytable\">\n"+
"  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>User mapping:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      <input type=\"text\" size=\"32\" name=\"usernameregexp\" value=\""+
  Encoder.attributeEscape(usernameRegexp)+"\"/> ==&gt; \n"+
"      <input type=\"text\" size=\"32\" name=\"usertranslation\" value=\""+
  Encoder.attributeEscape(userTranslation)+"\"/>\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
      );
    }
    else
    {
      out.print(
"<input type=\"hidden\" name=\"usernameregexp\" value=\""+
  Encoder.attributeEscape(usernameRegexp)+"\"/>\n"+
"<input type=\"hidden\" name=\"usertranslation\" value=\""+
  Encoder.attributeEscape(userTranslation)+"\"/>\n"
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
    String usernameRegexp = variableContext.getParameter("usernameregexp");
    String userTranslation = variableContext.getParameter("usertranslation");
    if (usernameRegexp != null && userTranslation != null)
    {
      MatchMap localMap = new MatchMap();
      localMap.appendMatchPair(usernameRegexp,userTranslation);
      parameters.setParameter(PARAMETER_USERMAPPING,localMap.toString());
    }

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
    String userMappingString = parameters.getParameter(PARAMETER_USERMAPPING);
    MatchMap localMap = new MatchMap(userMappingString);
    String usernameRegexp = localMap.getMatchString(0);
    String userTranslation = localMap.getReplaceString(0);

    out.print(
"<table class=\"displaytable\">\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>Repository root:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      "+Encoder.bodyEscape(
  parameters.getParameter(PARAMETER_REPOSITORY_ROOT))+"\n"+
"    </td>\n"+
"  </tr>\n"+
"  <tr>\n"+
"    <td class=\"description\"><nobr>User mapping:</nobr></td>\n"+
"    <td class=\"value\">\n"+
"      "+Encoder.bodyEscape(usernameRegexp)+" ==&gt; \n"+
"      "+Encoder.bodyEscape(userTranslation)+"\n"+
"    </td>\n"+
"  </tr>\n"+
"</table>\n"
    );
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
  public void connect(ConfigParams configParameters)
  {
    super.connect(configParameters);
    rootDirectory = configParameters.getParameter(PARAMETER_REPOSITORY_ROOT);
    String userNameMapping = configParameters.getParameter(PARAMETER_USERMAPPING);
    matchMap = new MatchMap(userNameMapping);
  }

  /** Close the connection.  Call this before discarding this instance of the
  * repository connector.
  */
  public void disconnect()
    throws ManifoldCFException
  {
    expireSession();
    matchMap = null;
    rootDirectory = null;
    super.disconnect();
  }

  /** Test the connection.  Returns a string describing the connection integrity.
  *@return the connection's status as a displayable string.
  */
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
  public AuthorizationResponse getAuthorizationResponse(String userName)
    throws ManifoldCFException
  {
    // MHL
    return null;
  }

  /** Obtain the default access tokens for a given user name.
  *@param userName is the user name or identifier.
  *@return the default response tokens, presuming that the connect method fails.
  */
  public AuthorizationResponse getDefaultAuthorizationResponse(String userName)
  {
    // MHL
    return null;
  }

}
