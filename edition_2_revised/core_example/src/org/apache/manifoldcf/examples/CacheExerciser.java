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
import org.apache.manifoldcf.core.system.Logging;
import java.util.*;
import java.io.*;

/** This class demonstrates simple cache usage, written
* as a ManifoldCF core command.  It uses the explicit caching model.
*/
public class CacheExerciser
{
  /** The number of reader threads. */
  protected static final int NUM_ACCESS_THREADS = 3;
  
  /** The object name is used for the caching key */
  protected static final String OBJECT_NAME = "my cached object";
  
  private CacheExerciser()
  {
  }
  
  public static void main(String[] argv)
  {
    try
    {
      // All processes must have initialization
      ManifoldCF.initializeEnvironment(ThreadContextFactory.make());
      Logging.misc.debug("System successfully initialized");
      
      // Create the underlying resource whose value we are going to cache
      UnderlyingResource resource = new UnderlyingResource();
      
      int i;
      
      // Create the access threads
      AccessThread[] accessThreads = new AccessThread[NUM_ACCESS_THREADS];
      i = 0;
      while (i < accessThreads.length)
      {
        accessThreads[i] = new AccessThread("Access#"+Integer.toString(i),resource);
        i++;
      }

      // Create the changer threads
      String[] values = new String[]{"my favorite color is blue","e=mc^2","I've got a lovely bunch of coconuts"};
      
      ChangerThread[] changerThreads = new ChangerThread[values.length];
      i = 0;
      while (i < changerThreads.length)
      {
        changerThreads[i] = new ChangerThread("Changer#"+Integer.toString(i),
          resource,values[i]);
        i++;
      }
      
      // Prepare to start the threads.  We'll need a second try/catch, because we want to be
      // sure we shut the threads down on exit.
      try
      {
        i = 0;
        while (i < accessThreads.length)
        {
          accessThreads[i++].start();
        }
        i = 0;
        while (i < changerThreads.length)
        {
          changerThreads[i++].start();
        }
        
        Logging.misc.debug("Threads have been started");

        // Let the threads run for a while, and then we'll exit and shut them down
        ManifoldCF.sleep(5000L);
      }
      finally
      {
        Logging.misc.debug("Shutting threads down");

        while (true)
        {
          // We continue until the threads have given up on their own.
          boolean aliveThreadsExist = false;
          i = 0;
          while (i < accessThreads.length)
          {
            if (accessThreads[i].isAlive())
            {
              aliveThreadsExist = true;
              accessThreads[i].interrupt();
            }
            i++;
          }
          i = 0;
          while (i < changerThreads.length)
          {
            if (changerThreads[i].isAlive())
            {
              aliveThreadsExist = true;
              changerThreads[i].interrupt();
            }
            i++;
          }
          if (!aliveThreadsExist)
            break;
          
          // Yield, so the other threads can do stuff.
          Thread.yield();
        }
        
        Logging.misc.debug("Threads are down");
      }
      
      Logging.misc.debug("Example run complete");
    }
    catch (ManifoldCFException e)
    {
      // Exception during initialization: Print it to standard error, and quit
      e.printStackTrace(System.err);
      System.exit(-1);
    }
    catch (InterruptedException e)
    {
      // If we were interrupted, just exit
    }
  }
  
  /** This thread accesses the cached object */
  protected static class AccessThread extends Thread
  {
    protected String threadName;
    protected UnderlyingResource resource;
    
    /** Constructor. */
    public AccessThread(String threadName, UnderlyingResource resource)
    {
      super(threadName);
      setDaemon(true);
      this.threadName = threadName;
      this.resource = resource;
    }
    
    public void run()
    {
      // First, create a thread context
      IThreadContext tc = ThreadContextFactory.make();
      
      // Catch any exceptions
      try
      {
        // Make a cache manager
        ICacheManager cacheManager = CacheManagerFactory.make(tc);
        
        // Construct a cache description of the object
        ResourceDescription rd = new ResourceDescription(OBJECT_NAME);

        // Now, repeatedly grab locks and do things
        while (true)
        {
          // Make sure we catch the exit signal
          if (Thread.currentThread().isInterrupted())
            break;
          
          // This cycle's output string
          String eventName = new Long(System.currentTimeMillis()).toString() +
            ": " + threadName;
          
          // Access the cached resource, and create it if it isn't yet calculated
          ICacheHandle transactionHandle = cacheManager.enterCache(
            new ICacheDescription[]{rd},null,null);
          try
          {
            // Enter the lookup/create block
            ICacheCreateHandle createHandle = cacheManager.enterCreateSection(transactionHandle);
            try
            {
              // The cached object is just a string; try to look it up
              String cachedString = (String)cacheManager.lookupObject(createHandle,rd);
              if (cachedString == null)
              {
                System.out.println(eventName + ": Created cached value and saved it");
                // We could not find it.  "Create" it, and output the fact that we are creating it.  The
                // cached object creation is based on the underlying resource, upon which it depends
                cachedString = resource.resourceValue;
                // Save it in the cache
                cacheManager.saveObject(createHandle,rd,cachedString);
              }
              // Now, print the cached value, either retrieved from cache, or created and saved
              System.out.println(eventName + ": '"+cachedString+"'");
            }
            finally
            {
              cacheManager.leaveCreateSection(createHandle);
            }
          }
          finally
          {
            cacheManager.leaveCache(transactionHandle);
          }
          
          // Just yield; we want to spin as quick as we can to demonstrate caching
          Thread.yield();
        }
      }
      catch (ManifoldCFException e)
      {
        // If we were interrupted, just exit the thread
        if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
        {
          // Otherwise, log the error and exit
          Logging.misc.error("Thread got unexpected exception: "+e.getMessage(),e);
        }
      }
    }
  }

  /** This thread changes the underlying data upon which the cached object depends. */
  protected static class ChangerThread extends Thread
  {
    protected String threadName;
    protected UnderlyingResource resource;
    protected String myResourceValue;
    
    /** Constructor. */
    public ChangerThread(String threadName, UnderlyingResource resource, String myResourceValue)
    {
      super(threadName);
      setDaemon(true);
      this.threadName = threadName;
      this.resource = resource;
      this.myResourceValue = myResourceValue;
    }
    
    public void run()
    {
      // First, create a thread context
      IThreadContext tc = ThreadContextFactory.make();
      
      // Catch any exceptions
      try
      {
        // Make a cache manager
        ICacheManager cacheManager = CacheManagerFactory.make(tc);
        
        // Construct an invalidation string set for invalidating the resource
        StringSet invalidationKeys = new StringSet(OBJECT_NAME);
        
        // Now, repeatedly grab locks and do things
        while (true)
        {
          // Make sure we catch the exit signal
          if (Thread.currentThread().isInterrupted())
            break;
          
          // Change the underlying resource value, and invalidate the cache accordingly
          ICacheHandle transactionHandle = cacheManager.enterCache(new ICacheDescription[0],
            invalidationKeys,null);
          try
          {
            // Change the underlying resource
            resource.resourceValue = myResourceValue;
            System.out.println(System.currentTimeMillis()+": Underlying resource changed to '"+
              myResourceValue+"'");
            // Invalidate the cache keys
            cacheManager.invalidateKeys(transactionHandle);
          }
          finally
          {
            // Leave the cache transaction
            cacheManager.leaveCache(transactionHandle);
          }
          
          // Just yield; we want to spin as quick as we can to demonstrate caching
          Thread.yield();
        }
      }
      catch (ManifoldCFException e)
      {
        // If we were interrupted, just exit the thread
        if (e.getErrorCode() != ManifoldCFException.INTERRUPTED)
        {
          // Otherwise, log the error and exit
          Logging.misc.error("Thread got unexpected exception: "+e.getMessage(),e);
        }
      }
    }
  }

  /** This is the cache description for the object to be cached. */
  protected static class ResourceDescription extends org.apache.manifoldcf.core.cachemanager.BaseDescription
  {
    protected String objectName;
    protected StringSet objectKeys;
    
    /** Constructor. */
    public ResourceDescription(String objectName)
    {
      super(null);
      this.objectName = objectName;
      this.objectKeys = new StringSet(objectName);
    }
    
    /** Get the cache keys for an object (which may or may not exist yet in
    * the cache).  This method is called in order for cache manager to throw the correct locks.
    * @return the object's cache keys, or null if the object should not
    * be cached.
    */
    public StringSet getObjectKeys()
    {
      return objectKeys;
    }

    /** Get the critical section name for this description object.
    * This is used to synchronize creation of the described object,
    * and thus is used only for objects that will be cached.  This
    * method does not need to return decent results for objects that
    * are never cached.
    *@return the critical section name.
    */
    public String getCriticalSectionName()
    {
      return "exampleobject-"+objectName;
    }

    /** Hash code */
    public int hashCode()
    {
      return objectName.hashCode();
    }
    
    /** Equals */
    public boolean equals(Object o)
    {
      if (!(o instanceof ResourceDescription))
        return false;
      return ((ResourceDescription)o).objectName.equals(objectName);
    }
    
  }
  
  /** This class represents the underlying resource, which can be changed, causing
  * cache invalidation. */
  protected static class UnderlyingResource
  {
    public String resourceValue = "";
  }
  
}
