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

import org.apache.manifoldcf.core.interfaces.*;

import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.HttpStatus;
import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.ParseException;

import java.io.*;
import java.util.*;
import java.nio.charset.Charset;

/** This connection class provides everything needed to communicate with the
* ManifoldCF API.
*/
public class ManifoldCFAPIConnect
{
  /** The base URL, e.g. http://localhost:8345/mcf-api-service.
  */
  protected final String baseURL;

  protected final static Charset UTF_8 = Charset.forName("UTF-8");

  /** Constructor.
  *@param baseURL is the base URL of the connection.
  */
  public ManifoldCFAPIConnect(String baseURL)
  {
    this.baseURL = baseURL;
  }

  /** Read the response data and convert to the appropriate string.
  *@param httpResponse is the response object.
  *@return the response string.
  */
  public static String convertToString(HttpResponse httpResponse)
    throws IOException
  {
    HttpEntity entity = httpResponse.getEntity();
    if (entity != null)
    {
      InputStream is = entity.getContent();
      try
      {
        Charset charSet;
        try
        {
          ContentType ct = ContentType.get(entity);
          if (ct == null)
            charSet = UTF_8;
          else
            charSet = ct.getCharset();
        }
        catch (ParseException e)
        {
          charSet = UTF_8;
        }
        char[] buffer = new char[65536];
        Reader r = new InputStreamReader(is,charSet);
        Writer w = new StringWriter();
        try
        {
          while (true)
          {
            int amt = r.read(buffer);
            if (amt == -1)
              break;
            w.write(buffer,0,amt);
          }
        }
        finally
        {
          w.flush();
        }
        return w.toString();
      }
      finally
      {
        is.close();
      }
    }
    return "";
  }

  /** Perform a JSON API GET operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@return the response, as a Configuration object.
  */
  public Configuration performAPIGetOperation(String restPath)
    throws ManifoldCFException, IOException
  {
    Configuration rval = new Configuration();
    rval.fromJSON(performAPIRawGetOperation(restPath));
    return rval;
  }
  
  /** Perform a JSON API PUT operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@param input is the input Configuration object.
  *@return the response, as a Configuration object.
  */
  public Configuration performAPIPutOperation(String restPath,
    Configuration input)
    throws ManifoldCFException, IOException
  {
    Configuration rval = new Configuration();
    rval.fromJSON(performAPIRawPutOperation(restPath,
      input.toJSON()));
    return rval;
  }

  /** Perform a JSON API POST operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@param input is the input Configuration object.
  *@return the response, as a Configuration object.
  */
  public Configuration performAPIPostOperation(String restPath,
    Configuration input)
    throws ManifoldCFException, IOException
  {
    Configuration rval = new Configuration();
    rval.fromJSON(performAPIRawPostOperation(restPath,
      input.toJSON()));
    return rval;
  }

  /** Perform a JSON API DELETE operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@return the response, as a Configuration object.
  */
  public Configuration performAPIDeleteOperation(String restPath)
    throws ManifoldCFException, IOException
  {
    Configuration rval = new Configuration();
    rval.fromJSON(performAPIRawDeleteOperation(restPath));
    return rval;
  }

  /** Perform an API GET operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@return the json response.
  */
  public String performAPIRawGetOperation(String restPath)
    throws IOException
  {
    HttpClient client = HttpClients.createDefault();
    HttpGet method = new HttpGet(formURL(restPath));
    HttpResponse httpResponse = client.execute(method);
    int response = httpResponse.getStatusLine().getStatusCode();
    String responseString = convertToString(httpResponse);
    if (response != HttpStatus.SC_OK &&
      response != HttpStatus.SC_NOT_FOUND)
      throw new IOException("API http GET error; expected "+
        HttpStatus.SC_OK+" or "+HttpStatus.SC_NOT_FOUND+", "+
        " saw "+Integer.toString(response)+": "+responseString);
    return responseString;
  }

  /** Perform an API PUT operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@param input is the input JSON.
  *@return the json response.
  */
  public String performAPIRawPutOperation(String restPath,
    String input)
    throws IOException
  {
    HttpClient client = HttpClients.createDefault();
    HttpPut method = new HttpPut(formURL(restPath));
    method.setEntity(new StringEntity(input,ContentType.create("text/plain","UTF-8")));
    HttpResponse httpResponse = client.execute(method);
    int response = httpResponse.getStatusLine().getStatusCode();
    String responseString = convertToString(httpResponse);
    if (response != HttpStatus.SC_OK && response != HttpStatus.SC_CREATED)
      throw new IOException("API http error; expected "+
        HttpStatus.SC_OK+" or "+HttpStatus.SC_CREATED+", saw "+
        Integer.toString(response)+": "+responseString);
    return responseString;
  }

  /** Perform an API POST operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@param input is the input JSON.
  *@return the json response.
  */
  public String performAPIRawPostOperation(String restPath,
    String input)
    throws IOException
  {
    HttpClient client = HttpClients.createDefault();
    HttpPost method = new HttpPost(formURL(restPath));
    method.setEntity(new StringEntity(input,ContentType.create("text/plain","UTF-8")));
    HttpResponse httpResponse = client.execute(method);
    int response = httpResponse.getStatusLine().getStatusCode();
    String responseString = convertToString(httpResponse);
    if (response != HttpStatus.SC_CREATED)
      throw new IOException("API http error; expected "+
        HttpStatus.SC_CREATED+", saw "+
        Integer.toString(response)+": "+responseString);
    return responseString;
  }

  /** Perform an API DELETE operation.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@return the json response.
  */
  public String performAPIRawDeleteOperation(String restPath)
    throws IOException
  {
    HttpClient client = HttpClients.createDefault();
    HttpDelete method = new HttpDelete(formURL(restPath));
    HttpResponse httpResponse = client.execute(method);
    int response = httpResponse.getStatusLine().getStatusCode();
    String responseString = convertToString(httpResponse);
    if (response != HttpStatus.SC_OK)
      throw new IOException("API http error; expected "+
        HttpStatus.SC_OK+", saw "+
        Integer.toString(response)+": "+responseString);
    return responseString;
  }

  /** Form a full URL given the current baseURL and the REST path.
  *@param restPath is the URL path of the REST object, starting with "/".
  *@return the full URL.
  */
  protected String formURL(String restPath)
  {
    return baseURL + "/json" + restPath;
  }
  
  /** Use this utility method to dump a Configuration object to standard
  * output for debugging purposes.
  *@param root is the Configuration object to dump.
  */
  public static void dumpConfiguration(Configuration root)
  {
    int i = 0;
    while (i < root.getChildCount())
    {
      ConfigurationNode child = root.findChild(i++);
      dumpConfigurationNode(child,0);
    }
  }
  
  /** Use this utility method to dump a ConfigurationNode object to
  * standard output for debugging purposes.
  *@param node is the ConfigurationNode object to dump
  *@param level is the depth at which this node should be listed.
  */
  public static void dumpConfigurationNode(ConfigurationNode node, int level)
  {
    printSpaces(level);
    System.out.println("Node '"+node.getType()+"'; value '"+node.getValue()+"'; attributes:");
    Iterator iter = node.getAttributes();
    while (iter.hasNext())
    {
      String attributeName = (String)iter.next();
      String attributeValue = node.getAttributeValue(attributeName);
      printSpaces(level+1);
      System.out.println("Attribute '"+attributeName+"' value '"+attributeValue+"'");
    }
    printSpaces(level);
    System.out.println("Children:");
    int i = 0;
    while (i < node.getChildCount())
    {
      ConfigurationNode child = node.findChild(i++);
      dumpConfigurationNode(child,level+1);
    }
  }
  
  /** Print spaces according to the provided level.
  *@param i is the level number.
  */
  protected static void printSpaces(int i)
  {
    int j = 0;
    while (j < i)
    {
      System.out.print("  ");
      j++;
    }
  }

}
