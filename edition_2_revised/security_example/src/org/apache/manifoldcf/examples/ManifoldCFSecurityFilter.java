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
  /** The parameter that is supposed to contain the authenticated user name, possibly including the domain */
  static final public String AUTHENTICATED_USER_NAME = "AuthenticatedUserName";
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
    List<String> userAccessTokens = getAccessTokens(authenticatedUserName);

    // Build a new boolean filter, which we'll add to the query at the end
    BooleanFilter bf = new BooleanFilter();
    
    if (userAccessTokens.size() == 0)
    {
      // Only open documents can be included.
      // That query is:
      // (fieldAllowShare is empty AND fieldDenyShare is empty AND fieldAllowDocument is empty AND fieldDenyDocument is empty)
      // We're trying to map to:  -(fieldAllowShare:*) , which should be pretty efficient in Solr because it is negated.  If this turns out not to be so, then we should
      // have the SolrConnector inject a special token into these fields when they otherwise would be empty, and we can trivially match on that token.
      bf.add(new FilterClause(new WildcardFilter(new Term(fieldAllowShare,"*")),BooleanClause.Occur.MUST_NOT));
      bf.add(new FilterClause(new WildcardFilter(new Term(fieldDenyShare,"*")),BooleanClause.Occur.MUST_NOT));
      bf.add(new FilterClause(new WildcardFilter(new Term(fieldAllowDocument,"*")),BooleanClause.Occur.MUST_NOT));
      bf.add(new FilterClause(new WildcardFilter(new Term(fieldDenyDocument,"*")),BooleanClause.Occur.MUST_NOT));
    }
    else
    {
      // Extend the query appropriately for each user access token.
      bf.add(new FilterClause(calculateCompleteSubfilter(fieldAllowShare,fieldDenyShare,userAccessTokens),BooleanClause.Occur.MUST));
      bf.add(new FilterClause(calculateCompleteSubfilter(fieldAllowDocument,fieldDenyDocument,userAccessTokens),BooleanClause.Occur.MUST));
    }

    // Concatenate with the user's original query.
    List<Query> list = rb.getFilters();
    if (list == null)
    {
      list = new ArrayList<Query>();
      rb.setFilters(list);
    }
    list.add(new ConstantScoreQuery(bf));
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
  *@return the calculated filter.
  */
  protected Filter calculateCompleteSubfilter(String allowField, String denyField, List<String> userAccessTokens)
  {
    BooleanFilter bf = new BooleanFilter();
    
    // Add a clause for each token.  This will be added directly to the main filter (as a deny test), as well as to an OR's subclause (as an allow test).
    BooleanFilter orFilter = new BooleanFilter();
    // Add the empty-acl case
    BooleanFilter subUnprotectedClause = new BooleanFilter();
    subUnprotectedClause.add(new FilterClause(new WildcardFilter(new Term(allowField,"*")),BooleanClause.Occur.MUST_NOT));
    subUnprotectedClause.add(new FilterClause(new WildcardFilter(new Term(denyField,"*")),BooleanClause.Occur.MUST_NOT));
    orFilter.add(new FilterClause(subUnprotectedClause,BooleanClause.Occur.SHOULD));
    int i = 0;
    while (i < userAccessTokens.size())
    {
      String accessToken = userAccessTokens.get(i++);
      TermsFilter tf = new TermsFilter();
      tf.addTerm(new Term(allowField,accessToken));
      orFilter.add(new FilterClause(tf,BooleanClause.Occur.SHOULD));
      tf = new TermsFilter();
      tf.addTerm(new Term(denyField,accessToken));
      bf.add(new FilterClause(tf,BooleanClause.Occur.MUST_NOT));
    }
    bf.add(new FilterClause(orFilter,BooleanClause.Occur.MUST));
    return bf;
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
  *@param authenticatedUserName is the user name, of the form name@domain
  *@return the access tokens for that user, appropriate for all non-exception error conditions.
  */
  protected List<String> getAccessTokens(String authenticatedUserName)
    throws IOException
  {
    List<ResponseComponent> responses = connection.performAuthorityRequest(authenticatedUserName);
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