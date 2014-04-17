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

/** The engine class actually makes the automaton "go".
* It maintains the actual state, and executes the transitions.
*/
public class Engine
{
  protected State currentState;
  
  /** Constructor.
  *@param initialState is the starting state.
  */
  public Engine(State initialState)
  {
    this.currentState = initialState;
  }
  
  /** Execute the automaton.  When this method returns, the
  * system should shut down.
  */
  public void execute()
    throws InterruptedException
  {
    while (true)
    {
      if (currentState == null)
        break;
      // Get all the transitions out of the current state.
      Transition[] transitions = currentState.getTransitions();
      // Evaluate each one of these transitions in turn
      boolean newStateFound = false;
      int i = 0;
      while (i < transitions.length)
      {
        Transition t = transitions[i++];
        if (t.isTransitionReady())
        {
          currentState = t.executeTransition();
          newStateFound = true;
          break;
        }
      }
      if (newStateFound)
        continue;
      // No transitions are ready.  Sleep for 10 seconds, and try again.
      Thread.sleep(10000L);
    }
  }
}
