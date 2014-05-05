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

import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.interfaces.*;
import java.io.*;

/** This class demonstrates the use of local trust stores. */
public class TrustStoreExample
{
  private TrustStoreExample()
  {
  }

  protected static void addCertificate(IKeystoreManager trustStore,
    String fileName, String aliasName)
    throws IOException, ManifoldCFException
  {
    InputStream is = new FileInputStream(fileName);
    try
    {
      trustStore.importCertificate(aliasName,is);
    }
    finally
    {
      is.close();
    }
  }
  
  /** Main method of trust store example class */
  public static void main(String[] argv)
  {
    try
    {
      ManifoldCF.initializeEnvironment(ThreadContextFactory.make());
      // Create a local trust store with nothing in it, and no password.
      System.out.println("Creating empty trust store...");
      IKeystoreManager trustStore = KeystoreManagerFactory.make("");
      // Add the first certificate to the trust store.
      System.out.println("Adding first certificate...");
      addCertificate(trustStore,"certificate1.crt","Cert #1");
      // Add the second certificate.
      System.out.println("Adding second certificate...");
      addCertificate(trustStore,"certificate2.crt","Cert #2");
      // Convert the trust store to a string.
      System.out.println("Converting to string...");
      String trustStoreContents = trustStore.getString();
      System.out.println("The trust store contents are:");
      System.out.println(trustStoreContents);
      // Create a new trust store with the string data.
      System.out.println("Building a trust store from string...");
      IKeystoreManager restoredTrustStore = 
        KeystoreManagerFactory.make("",trustStoreContents);
      String[] contents = restoredTrustStore.getContents();
      System.out.println("Restored trust store has "+Integer.toString(contents.length)+" certs");
      int i = 0;
      while (i < contents.length)
      {
        System.out.println(" "+contents[i]+":"+restoredTrustStore.getDescription(contents[i]));
        i++;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
}
