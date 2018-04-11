/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Contributors:  Georg Lundesgaard

package org.apache.log4j.config;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.OptionHandler;

/**
   General purpose Object property setter. Clients repeatedly invokes
   {@link #setProperty setProperty(name,value)} in order to invoke setters
   on the Object specified in the constructor. This class relies on Java reflection.

   <p>Usage:
   <pre>
     PropertySetter ps = new PropertySetter(anObject);
     ps.set("name", "Joe");
     ps.set("age", "32");
     ps.set("isMale", "true");
   </pre>
   will cause the invocations anObject.setName("Joe"), anObject.setAge(32),
   and setMale(true) if such methods exist with those signatures.

   @author Anders Kristensen
   @since 1.1
 */
public class PropertySetter {
  private Object obj;

  /**
    Create a new PropertySetter for the specified Object. This is done
    in prepartion for invoking {@link #setProperty} one or more times.

    @param obj  the object for which to set properties
   */
  public
  PropertySetter(Object obj) {
    this.obj = obj;
  }

  /**
     Set the properties of an object passed as a parameter in one
     go. The <code>properties</code> are parsed relative to a
     <code>prefix</code>.

     @param obj The object to configure.
     @param properties A java.util.Properties containing keys and values.
     @param prefix Only keys having the specified prefix will be set.
  */
  public
  static
  void setProperties(Object obj, Properties properties, String prefix) {
    new PropertySetter(obj).setProperties(properties, prefix);
  }

  public void setProperties(Properties properties, String prefix)
  {
      int len = prefix.length();

      for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); )
      {
          String key = (String) e.nextElement();

          // handle only properties that start with the desired frefix.
          if (key.startsWith(prefix))
          {


              // ignore key if it contains dots after the prefix
              if (key.indexOf('.', len + 1) > 0)
              {
                  //System.err.println("----------Ignoring---["+key
                  //	     +"], prefix=["+prefix+"].");
                  continue;
              }

              String value = OptionConverter.findAndSubst(key, properties);
              key = key.substring(len);
              if (("layout".equals(key) || "errorhandler".equals(key)) && obj instanceof Appender)
              {
                  continue;
              }
              //
              //   if the property type is an OptionHandler
              //     (for example, triggeringPolicy of org.apache.log4j.rolling.RollingFileAppender)
              try
              {
                  Method method = lookupMethod(key, false);
                  if (OptionHandler.class.isAssignableFrom(method.getReturnType()))
                  {
                      OptionHandler opt = (OptionHandler)
                              OptionConverter.instantiateByKey(properties, prefix + key,
                                      method.getReturnType(),
                                      null);
                      PropertySetter setter = new PropertySetter(opt);
                      setter.setProperties(properties, prefix + key + ".");
                      method.invoke(this.obj, new Object[]{opt});
                      continue;
                  }
              } catch (IllegalAccessException ex)
              {
                  LogLog.warn("Failed to set property [" + key +
                          "] to value \"" + value + "\". ", ex);
              } catch (InvocationTargetException ex)
              {
                  if (ex.getTargetException() instanceof InterruptedException
                          || ex.getTargetException() instanceof InterruptedIOException)
                  {
                      Thread.currentThread().interrupt();
                  }
                  LogLog.warn("Failed to set property [" + key +
                          "] to value \"" + value + "\". ", ex);
              } catch (RuntimeException ex)
              {
                  LogLog.warn("Failed to set property [" + key +
                          "] to value \"" + value + "\". ", ex);
              } catch (NoSuchMethodException nsme)
              {
                  LogLog.warn("Failed to find method for property [" + key + "] to value \"" + value + "\". ", nsme);
              }
              setProperty(key, value);
          }
      }

      activate();

  }

    /*
        Assuming only one setXX or getXX method is available, callers assuming the setter only takes one parameter
     */
    private Method lookupMethod(String key, boolean setter) throws NoSuchMethodException
    {
        Method[] methods = obj.getClass().getMethods();
        String lowerKey;
        if (setter)
        {
            lowerKey = "set" + key.toLowerCase();
        }
        else
        {
            lowerKey = "get" + key.toLowerCase();
        }

        for (int i=0;i<methods.length;i++)
        {
            if (methods[i].getName().toLowerCase().equals(lowerKey))
            {
                return methods[i];
            }
        }
        throw new NoSuchMethodException("Unable to find method for property: " + key);
    }

    /**
     Set a property on this PropertySetter's Object. If successful, this
     method will invoke a setter method on the underlying Object. The
     setter is the one for the specified property name and the value is
     determined partly from the setter argument type and partly from the
     value specified in the call to this method.

     <p>If the setter expects a String no conversion is necessary.
     If it expects an int, then an attempt is made to convert 'value'
     to an int using new Integer(value). If the setter expects a boolean,
     the conversion is by new Boolean(value).

     @param name    name of the property
     @param value   String value of the property
   */
  public
  void setProperty(String name, String value) {
    if (value == null) return;

    //LogLog.debug("---------Key: "+name+", type="+prop.getPropertyType());
      try {
        Method method = lookupMethod(name, true);
        setProperty(method, name, value);
      } catch (PropertySetterException ex) {
        LogLog.warn("Failed to set property [" + name +
                    "] to value \"" + value + "\". ", ex.rootCause);
      } catch (NoSuchMethodException e)
      {
          LogLog.warn("Unable to find method for property [" + name +
                      "] to value \"" + value + "\". ", e);

      }
  }

  /**
      Set the named property given a {@link Method}.

      @param setter A Method to invoke
      @param name The named of the property to set.
      @param value The value of the property.
   */
  private void setProperty(Method setter, String name, String value)
    throws PropertySetterException {
    if (setter == null) {
      throw new PropertySetterException("No setter for property ["+name+"].");
    }
    Class[] paramTypes = setter.getParameterTypes();
    if (paramTypes.length != 1) {
      throw new PropertySetterException("#params for setter != 1");
    }

    Object arg;
    try {
      arg = convertArg(value, paramTypes[0]);
    } catch (Throwable t) {
      throw new PropertySetterException("Conversion to type ["+paramTypes[0]+
					"] failed. Reason: "+t);
    }
    if (arg == null) {
      throw new PropertySetterException(
          "Conversion to type ["+paramTypes[0]+"] failed.");
    }
    LogLog.debug("Setting property [" + name + "] to [" +arg+"].");
    try {
      setter.invoke(obj, new Object[]  { arg });
    } catch (IllegalAccessException ex) {
      throw new PropertySetterException(ex);
    } catch (InvocationTargetException ex) {
        if (ex.getTargetException() instanceof InterruptedException
                || ex.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
        throw new PropertySetterException(ex);
    } catch (RuntimeException ex) {
      throw new PropertySetterException(ex);
    }
  }


  /**
     Convert <code>val</code> a String parameter to an object of a
     given type.
  */
  private
  Object convertArg(String val, Class type) {
    if(val == null)
      return null;

    String v = val.trim();
    if (String.class.isAssignableFrom(type)) {
      return val;
    } else if (Integer.TYPE.isAssignableFrom(type)) {
      return new Integer(v);
    } else if (Long.TYPE.isAssignableFrom(type)) {
      return new Long(v);
    } else if (Boolean.TYPE.isAssignableFrom(type)) {
      if ("true".equalsIgnoreCase(v)) {
        return Boolean.TRUE;
      } else if ("false".equalsIgnoreCase(v)) {
        return Boolean.FALSE;
      }
    } else if (Priority.class.isAssignableFrom(type)) {
      return OptionConverter.toLevel(v, (Level) Level.DEBUG);
    } else if (ErrorHandler.class.isAssignableFrom(type)) {
      return OptionConverter.instantiateByClassName(v,
	  ErrorHandler.class, null);
    }
    return null;
  }

  public
  void activate() {
    if (obj instanceof OptionHandler) {
      ((OptionHandler) obj).activateOptions();
    }
  }
}
