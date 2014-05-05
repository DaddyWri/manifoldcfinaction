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

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.solr.search.*;
import org.apache.solr.core.*;
import org.apache.solr.handler.component.*;
import org.apache.solr.request.*;
import org.apache.solr.util.*;
import org.apache.solr.util.plugin.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.*;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.net.*;

/**
* SearchComponent plugin for LCF-specific document-level access control.
* Configuration is under the SolrACLSecurity name.
*/
public class ManifoldCFSecurityFilter extends SearchComponent
{
  /** The component name */
  static final public String COMPONENT_NAME = "ManifoldCFSecurityFilter";
  /** The parameter that is supposed to contain the authorization domain; if not specified, "" is used */
  static final public String AUTHORIZATION_DOMAIN_NAME = "AuthorizationDomainName";
  /** The parameter that is supposed to contain the authenticated user name, possibly including the AD domain */
  static final public String AUTHENTICATED_USER_NAME = "AuthenticatedUserName";
  /** The default value for empty authority fields, so we don't need to do wildcard queries */
  static final public String EMPTY_FIELD_VALUE = "__no_security__";
  
  /** This parameter is an array of strings, which contain the tokens to use if there is no authenticated user name.  It's meant to work with mod_authz_annotate,
  * running under Apache */
  static final public String USER_TOKENS = "UserTokens";
  
  /** The queries that we will not attempt to interfere with */
  static final private String[] globalAllowed = { "solrpingquery" };
  
  /** A logger we can use */
  private static final Logger LOG = LoggerFactory.getLogger(ManifoldCFSecurityFilter.class);

  // Member variables
  
  private String fieldAllowDocument = null;
  private String fieldDenyDocument = null;
  private String fieldAllowParent = null;
  private String fieldDenyParent = null;
  private String fieldAllowShare = null;
  private String fieldDenyShare = null;
  
  /** The connection to the ManifoldCF authority service. */
  private ManifoldCFAuthorityServiceConnect connection = null;
  
  public ManifoldCFSecurityFilter()
  {
    super();
  }

  @Override
  public void init(NamedList args)
  {
    super.init(args);
    String baseURL = (String)args.get("AuthorityServiceBaseURL");
    if (baseURL == null)
      connection = null;
    else
      connection = new ManifoldCFAuthorityServiceConnect(baseURL);
    String allowAttributePrefix = (String)args.get("AllowAttributePrefix");
    String denyAttributePrefix = (String)args.get("DenyAttributePrefix");
    if (allowAttributePrefix == null)
      allowAttributePrefix = "allow_token_";
    if (denyAttributePrefix == null)
      denyAttributePrefix = "deny_token_";
    fieldAllowDocument = allowAttributePrefix+"document";
    fieldDenyDocument = denyAttributePrefix+"document";
    fieldAllowParent = allowAttributePrefix+"parent";
    fieldDenyParent = denyAttributePrefix+"parent";
    fieldAllowShare = allowAttributePrefix+"share";
    fieldDenyShare = denyAttributePrefix+"share";
  }

  /** SearchComponent prepare() method.
  * All SearchComponents have this method.  This one modifies the query based on the input parameters.
  *@param rb is the response builder object, which contains both the input and the response.
  */
  @Override
  public void prepare(ResponseBuilder rb) throws IOException
  {
    // Get the request parameters
    SolrParams params = rb.req.getParams();

    // Log that we got here
    LOG.info("prepare() entry params:\n" + params + "\ncontext: " + rb.req.getContext());
    
    // Certain queries make it through unmodified.
    String qry = (String)params.get(CommonParams.Q);
    if (qry != null)
    {
      //Check global allowed searches
      for (String ga : globalAllowed)
      {
	if (qry.equalsIgnoreCase(ga.trim()))
	  // Allow this query through unchanged
	  return;
      }
    }

    // Get the authorization domain from the parameters (if any)
    String authorizationDomain = params.get(AUTHORIZATION_DOMAIN_NAME);
    if (authorizationDomain == null)
      authorizationDomain = "";
    // Get the authenticated user name from the parameters
    String authenticatedUserName = params.get(AUTHENTICATED_USER_NAME);

    if (authenticatedUserName == null)
      // We could just throw an error, but then many of the innocent queries the Solr does would fail.  So, just return instead.
      return;
    
    LOG.info("ManifoldCFSecurityFilter: Trying to match docs for user '"+authenticatedUserName+"'");
    
    // Check the configuration arguments for validity
    if (connection == null)
    {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error initializing ManifoldCFSecurityFilter component: 'AuthorityServiceBaseURL' init parameter required");
    }
    
    // Talk to the authority service and get the access tokens
    List<String> userAccessTokens = getAccessTokens(authorizationDomain,authenticatedUserName);

    // Build a new boolean query, which we'll add to the query at the end
    BooleanQuery bq = new BooleanQuery();
    
    
    Query allowShareOpen = new TermQuery(new Term(fieldAllowShare,EMPTY_FIELD_VALUE));
    Query denyShareOpen = new TermQuery(new Term(fieldDenyShare,EMPTY_FIELD_VALUE));
      
    Query allowParentOpen = new TermQuery(new Term(fieldAllowParent,EMPTY_FIELD_VALUE));
    Query denyParentOpen = new TermQuery(new Term(fieldDenyParent,EMPTY_FIELD_VALUE));

    Query allowDocumentOpen = new TermQuery(new Term(fieldAllowDocument,EMPTY_FIELD_VALUE));
    Query denyDocumentOpen = new TermQuery(new Term(fieldDenyDocument,EMPTY_FIELD_VALUE));

    if (userAccessTokens.size() == 0)
    {
      // Only open documents can be included.
      // That query is:
      // (fieldAllowShare is empty AND fieldDenyShare is empty AND fieldAllowDocument is empty AND fieldDenyDocument is empty)
      // We're trying to map to:  -(fieldAllowShare:*), which is not the best way to do this kind of thing in Lucene.
      // Filter caching makes it tolerable, but a much better approach is to use a default value as a dedicated term to match.
      // That is what we do below.
      bq.add(allowShareOpen,BooleanClause.Occur.MUST);
      bq.add(denyShareOpen,BooleanClause.Occur.MUST);
      bq.add(allowParentOpen,BooleanClause.Occur.MUST);
      bq.add(denyParentOpen,BooleanClause.Occur.MUST);
      bq.add(allowDocumentOpen,BooleanClause.Occur.MUST);
      bq.add(denyDocumentOpen,BooleanClause.Occur.MUST);
    }
    else
    {
      // Extend the query appropriately for each user access token.
      bq.add(calculateCompleteSubquery(fieldAllowShare,fieldDenyShare,allowShareOpen,denyShareOpen,userAccessTokens),
        BooleanClause.Occur.MUST);
      bq.add(calculateCompleteSubquery(fieldAllowParent,fieldDenyParent,allowParentOpen,denyParentOpen,userAccessTokens),
        BooleanClause.Occur.MUST);
      bq.add(calculateCompleteSubquery(fieldAllowDocument,fieldDenyDocument,allowDocumentOpen,denyDocumentOpen,userAccessTokens),
        BooleanClause.Occur.MUST);
    }

    // Concatenate with the user's original query.
    List<Query> list = rb.getFilters();
    if (list == null)
    {
      list = new ArrayList<Query>();
      rb.setFilters(list);
    }
    list.add(new ConstantScoreQuery(bq));
  }

  /** All search components have a process() method.
  * This search component doesn't need to do anything with it.
  *@param rb is the object describing the request and response.
  */
  @Override
  public void process(ResponseBuilder rb) throws IOException
  {
    LOG.info("ManifoldCFSecurityFilter: process() called");
  }

  /** Calculate a complete subclause, representing something like:
  * ((fieldAllowShare is empty AND fieldDenyShare is empty) OR fieldAllowShare HAS token1 OR fieldAllowShare HAS token2 ...)
  *     AND fieldDenyShare DOESN'T_HAVE token1 AND fieldDenyShare DOESN'T_HAVE token2 ...
  *@param allowField is the field name of the allow field.
  *@param denyField is the field name of the deny field.
  *@param userAccessTokens is the list of access tokens associated with the specified user.
  *@return the calculated query.
  */
  /** Calculate a complete subclause, representing something like:
  * ((fieldAllowShare is empty AND fieldDenyShare is empty) OR fieldAllowShare HAS token1 OR fieldAllowShare HAS token2 ...)
  *     AND fieldDenyShare DOESN'T_HAVE token1 AND fieldDenyShare DOESN'T_HAVE token2 ...
  *@param allowField is the field name of the allow field.
  *@param denyField is the field name of the deny field.
  *@param allowOpen is the query to use if there are no allow access tokens.
  *@param denyOpen is the query to use if there are no deny access tokens.
  *@param userAccessTokens is the list of access tokens associated with the specified user.
  *@return the calculated query.
  */
  protected Query calculateCompleteSubquery(String allowField, String denyField, Query allowOpen, Query denyOpen,
    List<String> userAccessTokens)
  {
    BooleanQuery bq = new BooleanQuery();
    // No query limits!!
    bq.setMaxClauseCount(1000000);
      
    // Add the empty-acl case
    BooleanQuery subUnprotectedClause = new BooleanQuery();
    subUnprotectedClause.add(allowOpen,BooleanClause.Occur.MUST);
    subUnprotectedClause.add(denyOpen,BooleanClause.Occur.MUST);
    bq.add(subUnprotectedClause,BooleanClause.Occur.SHOULD);
    for (String accessToken : userAccessTokens)
    {
      bq.add(new TermQuery(new Term(allowField,accessToken)),BooleanClause.Occur.SHOULD);
      bq.add(new TermQuery(new Term(denyField,accessToken)),BooleanClause.Occur.MUST_NOT);
    }
    return bq;
  }
  
  //---------------------------------------------------------------------------------
  // SolrInfoMBean
  //---------------------------------------------------------------------------------
  @Override
  public String getDescription()
  {
    return "ManifoldCF Solr security enforcement plugin";
  }

  @Override
  public String getVersion()
  {
    return "$Revision: 02.05.10096 $";
  }

  @Override
  public String getSourceId()
  {
    return "$Id$";
  }

  @Override
  public String getSource()
  {
    return "ManifoldCFSecurityFilter.java $";
  }
	
  // Protected methods
  
  /** Get access tokens given a username.
  *@param authorizationDomain is the authorization domain.
  *@param authenticatedUserName is the user name, of the form name@domain
  *@return the access tokens for that user, appropriate for all non-exception error conditions.
  */
  protected List<String> getAccessTokens(String authorizationDomain, String authenticatedUserName)
    throws IOException
  {
    List<ResponseComponent> responses = connection.performAuthorityRequest(authorizationDomain,
      authenticatedUserName);
    // Construct a response by filtering out everything but tokens.
    int i = 0;
    List<String> rval = new ArrayList<String>();
    while (i < responses.size())
    {
      ResponseComponent c = responses.get(i++);
      if (c.getType() == ResponseComponent.RESPONSECOMPONENT_TOKEN)
        rval.add(c.getValue());
    }
    return rval;
  }
  
}