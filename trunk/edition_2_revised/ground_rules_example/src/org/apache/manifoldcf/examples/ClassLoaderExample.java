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
import org.apache.manifoldcf.core.system.ManifoldCF;
import org.apache.manifoldcf.core.system.ManifoldCFResourceLoader;

import java.util.*;
import java.io.*;

/** This class represents your connector, and uses a custom class loader.
*/
public class ClassLoaderExample
{
  private ClassLoaderExample()
  {
  }
  
  /** Main method.  Instantiate the factory implementation using a class loader.
  */
  public static void main(String[] argv)
  {
    try
    {
      // Initialize the system
      ManifoldCF.initializeEnvironment(ThreadContextFactory.make());
      
      // Get the relative path for the "connector libraries" from a configuration parameter
      File connectorLibraryFolder = ManifoldCF.getFileProperty("org.apache.manifoldcf.examples.connectorjarpath");
      if (connectorLibraryFolder == null)
        throw new ManifoldCFException("Must supply the org.apache.manifoldcf.examples.connectorjarpath property!");
      
      // Build the class loader, based on ManifoldCF's root class loader.  This class loader
      // will preferentially look for classes out of the specified directory, and only use the
      // parent class loader if it can't find a class there.
      ManifoldCFResourceLoader connectorLoader = ManifoldCF.createResourceLoader();
      // Set the class path up in the new resource loader.
      ArrayList libList = new ArrayList();
      libList.add(connectorLibraryFolder);
      connectorLoader.setClassPath(libList);
      
      // Get the factory
      System.out.println("Obtaining a factory handle...");
      Class factoryClass = connectorLoader.findClass("org.apache.manifoldcf.examples.CLSessionFactoryImpl");
      ICLSessionFactory factory = (ICLSessionFactory)factoryClass.newInstance();
      // Get the session handle
      System.out.println("Getting an isolated session handle...");
      ICLSession newSession = factory.make();
      // Now, let's call the one method we can!
      System.out.println("Calling isolated method...");
      MyReturnObject result = newSession.doStuff(new MyArgumentObject("hello",123));
      System.out.println("Result: "+result.getValue());
      System.out.println("Done!");
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }
}