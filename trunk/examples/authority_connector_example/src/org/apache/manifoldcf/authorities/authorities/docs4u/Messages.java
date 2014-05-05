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
package org.apache.manifoldcf.authorities.authorities.docs4u;

import java.util.Locale;
import java.util.Map;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;

/** The Messages class provides a connector-specific way of looking up message
* localizations and outputting UI html using Velocity.
*/
public class Messages extends org.apache.manifoldcf.ui.i18n.Messages
{
  public static final String DEFAULT_BUNDLE_NAME="org.apache.manifoldcf.authorities.authorities.docs4u.common";
  public static final String DEFAULT_PATH_NAME="org.apache.manifoldcf.authorities.authorities.docs4u";
  
  /** Constructor - instantiation not allowed.
  */
  protected Messages()
  {
  }
  
  /** Get a localized string from the default bundle.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getString(Locale locale, String messageKey)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  /** Get a localized string from the default bundle, and escape it for use in an HTML
  * attribute.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getAttributeString(Locale locale, String messageKey)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  /** Get a localized string from the default bundle, and escape it for use in an HTML
  * body section.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getBodyString(Locale locale, String messageKey)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  /** Get a localized string from the default bundle, and escape it for use in Javascript within
  * an HTML attribute.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getAttributeJavascriptString(Locale locale, String messageKey)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  /** Get a localized string from the default bundle, and escape it for use in Javascript within
  * an HTML body section.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getBodyJavascriptString(Locale locale, String messageKey)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  /** Get a localized string from the default bundle, applying stringformat message arguments.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getString(Locale locale, String messageKey, Object[] args)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  /** Get a localized string from the default bundle, applying stringformat message arguments,
  * and escape it for use in an HTML attribute.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getAttributeString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }
  
  /** Get a localized string from the default bundle, applying stringformat message arguments,
  * and escape it for use in an HTML body section.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getBodyString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  /** Get a localized string from the default bundle, applying stringformat message arguments,
  * and escape it for use in Javascript within an HTML attribute.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getAttributeJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  /** Get a localized string from the default bundle, applying stringformat message arguments,
  * and escape it for use in Javascript within an HTML body section.
  *@param locale is the desired locale.
  *@param messageKey is the key for the message.
  */
  public static String getBodyJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  // More general methods which allow bundlenames to be specified.
  
  public static String getString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getAttributeString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(Messages.class, bundleName, locale, messageKey, args);
  }
  
  public static String getAttributeJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  // Velocity resource output
  
  /** Output html using a Velocity template.  The template is pulled from resources, as described by
  * the resourceKey argument.
  *@param output is the output stream to be written to.
  *@param locale is the desired locale to render the template in.
  *@param resourceKey is the name of the resource.
  *@param contextObjects are the set of objects that Velocity will have access to when the template is rendered.
  */
  public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,Object> contextObjects)
    throws ManifoldCFException
  {
    outputResourceWithVelocity(output,Messages.class,DEFAULT_BUNDLE_NAME,DEFAULT_PATH_NAME,locale,resourceKey,
      contextObjects);
  }
  
}

