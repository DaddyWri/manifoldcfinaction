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

/** Session implementation.
*/
public class CLSessionImpl implements ICLSession
{
  /** Constructor */
  public CLSessionImpl()
  {
  }

  /** Our method which demonstrates something actually happening. */
  public MyReturnObject doStuff(MyArgumentObject inputObject)
    throws MyException
  {
    if (inputObject.getField2() == 0)
      throw new MyException("Hey, you sent in a zero!");
    
    System.out.println("The isolated class got a string of '"+inputObject.getField1()+"'");
    
    // Respond with a string of our own.
    return new MyReturnObject("This is my response");
  }

}
