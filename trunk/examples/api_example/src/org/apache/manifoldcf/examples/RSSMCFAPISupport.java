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
import java.util.*;
import org.apache.manifoldcf.core.interfaces.*;

/** ManifoldCF support that is specific to RSS connections.
* This is a minimal set necessary to flesh out our example.
*/
public class RSSMCFAPISupport extends GenericMCFAPISupport
{
  /** Constructor.
  *@param baseURL is the base URL of the ManifoldCF api service.
  */
  public RSSMCFAPISupport(String baseURL)
  {
    super(baseURL);
  }

  /** Create an RSS connection with the specified parameters.
  *@param connectionName is the name of the connection.
  *@param connectionDescription is the connection description.
  *@param maxAverageFetchRate is the maximum average documents
  * per millisecond.
  *@param emailAddress is the email address.
  *@param maxKBperSecondPerConnection is the bandwidth limit.
  *@param maxConnectionsPerServer is the connection limit.
  *@param maxFetchesPerMinutePerServer is the fetch rate limit.
  *@param connectionConfiguration is a Configuration structure describing
  * the connection configuration.
  */
  public void createRSSRepositoryConnection(String connectionName,
    String connectionDescription,
    Double maxAverageFetchRate,
    String emailAddress,
    Integer maxKBperSecondPerConnection,
    Integer maxConnectionsPerServer,
    Integer maxFetchesPerMinutePerServer)
    throws IOException, ManifoldCFException
  {
    Configuration connectionConfiguration = new Configuration();
    addParameterNode(connectionConfiguration,"Email address",emailAddress);
    addParameterNode(connectionConfiguration,"Robots usage","all");
    if (maxKBperSecondPerConnection != null)
      addParameterNode(connectionConfiguration,
        "KB per second",maxKBperSecondPerConnection.toString());
    if (maxConnectionsPerServer != null)
      addParameterNode(connectionConfiguration,
        "Max server connections",maxConnectionsPerServer.toString());
    if (maxFetchesPerMinutePerServer != null)
      addParameterNode(connectionConfiguration,
        "Max fetches per minute",maxFetchesPerMinutePerServer.toString());
    createRepositoryConnection(connectionName,connectionDescription,
      "org.apache.manifoldcf.crawler.connectors.rss.RSSConnector",
      100,maxAverageFetchRate,connectionConfiguration);
  }

  /** Create a continuous RSS job with the specified parameters.
  *@param description is the job's textual description.
  *@param repositoryConnectionName is the job's repository connection.
  *@param outputConnectionName is the job's output connection.
  *@param priority is the document priority.
  *@param expirationInterval is the number of milliseconds before
  * expiration of a document.
  *@param rescanInterval is the number of milliseconds before a
  * document should be rescanned.
  *@param reseedInterval is the number of milliseconds before
  * reseeding should take place.
  *@param feeds are the list of feed URLs for the job.
  *@param defaultFeedRescan is the default feed refetch time in minutes.
  *@param minFeedRescan is the minimum feed refetch time in minutes.
  *@param badFeedRescan is the time to wait before trying to fetch a
  * bad feed again, in minutes.
  *@param outputSpecification is a Configuration structure describing
  * the job's output specification.
  *@return the job identifier.
  */
  public String createRSSJob(String description,
    String repositoryConnectionName,
    String outputConnectionName,
    int priority,
    Long expirationInterval,
    Long rescanInterval,
    Long reseedInterval,
    List<String> feeds,
    Integer defaultFeedRescan,
    Integer minFeedRescan,
    Integer badFeedRescan,
    Configuration outputSpecification)
    throws IOException, ManifoldCFException
  {
    Configuration documentSpecification = new Configuration();
    addAttributeNode(documentSpecification,"chromedmode","mode","none");
    addAttributeNode(documentSpecification,"feedtimeout","value","60");
    if (badFeedRescan != null)
      addAttributeNode(documentSpecification,"badfeedrescan","value",
        badFeedRescan.toString());
    if (minFeedRescan != null)
      addAttributeNode(documentSpecification,"minfeedrescan","value",
        minFeedRescan.toString());
    if (defaultFeedRescan != null)
      addAttributeNode(documentSpecification,"feedrescan","value",
        defaultFeedRescan.toString());
    int i = 0;
    while (i < feeds.size())
    {
      String feed = feeds.get(i++);
      addAttributeNode(documentSpecification,"feed","url",feed);
    }
    return createContinuousJob(description,
      repositoryConnectionName, outputConnectionName,
      priority, expirationInterval, rescanInterval, reseedInterval,
      documentSpecification, outputSpecification);
  }
  
  /** Set an RSS job's feeds.
  *@param jobID is the job identifier
  *@param seeds is the list of seeds.
  */
  public void setRSSJobFeeds(String jobID, List<String> feeds)
    throws IOException, ManifoldCFException
  {
    Configuration documentSpecification =
      getJobDocumentSpecification(jobID);
    removeFromChildren(documentSpecification,"feed");
    int i = 0;
    while (i < feeds.size())
    {
      String feedURL = feeds.get(i++);
      addAttributeNode(documentSpecification,"feed","url",feedURL);
    }
    setJobDocumentSpecification(jobID,documentSpecification);
  }
}
