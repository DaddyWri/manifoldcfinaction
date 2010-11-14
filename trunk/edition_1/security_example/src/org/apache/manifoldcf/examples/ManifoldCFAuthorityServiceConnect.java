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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import java.io.*;
import java.util.*;
import java.net.*;

/** This connection class provides everything needed to communicate with the
* ManifoldCF Authority Service.
*/
public class ManifoldCFAuthorityServiceConnect
{
  /** The base URL, e.g. http://localhost:8345/mcf-authority-service.
  */
  protected String baseURL;

  /** Constructor.
  *@param baseURL is the base URL of the connection.
  */
  public ManifoldCFAuthorityServiceConnect(String baseURL)
  {
    this.baseURL = baseURL;
  }

  /** Perform an authority service transaction.
  *@param userName is the user name, of the form name@domain.
  *@return the authority response, as a list of response components.
  */
  public List<ResponseComponent> performAuthorityRequest(String userName)
    throws IOException
  {
    HttpClient client = new HttpClient();
    GetMethod method = new GetMethod(formURL(userName));
    try
    {
      int response = client.executeMethod(method);
      // We presume that the data is utf-8, since that's what the authority service
      // uses throughout.
      if (response != HttpStatus.SC_OK)
        throw new IOException("Authority Service http GET error; expected "+
          HttpStatus.SC_OK+", "+
          " saw "+Integer.toString(response)+": "+method.getResponseBodyAsString());
      // Now, create an array of response components
      List<ResponseComponent> rval = new ArrayList<ResponseComponent>();
      InputStream is = method.getResponseBodyAsStream();
      try
      {
        BufferedReader br = new BufferedReader(new InputStreamReader(is,"utf-8"));
        while (true)
        {
          String line = br.readLine();
          if (line == null)
            break;
          rval.add(new ResponseComponent(line));
        }
        return rval;
      }
      finally
      {
        is.close();
      }
    }
    finally
    {
      method.releaseConnection();
    }
  }

  /** Form a full URL given the current baseURL and the user name.
  *@param userName is the user name, of the form name@domain.
  *@return the full URL.
  */
  protected String formURL(String userName)
    throws IOException
  {
    // The replace is necessary because Jetty does not recognize the '+' as being equivalent to a ' '.
    return baseURL + "/userACLs?username="+URLEncoder.encode(userName,"utf-8").replace("+","%20");
  }
  
}
