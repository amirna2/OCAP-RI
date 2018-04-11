// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

package org.cablelabs.gear.test;

import junit.framework.*;
import org.havi.ui.*;
import org.w3c.dom.*;
import java.io.StringReader;
import java.util.Vector;
import java.lang.reflect.Constructor;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 *
 */
public class XMLTestConfig extends Assert
{

    /******************* Methods for initializing test ***************/

    /**
     * Retrieves a DOM <code>Document</code> from the specified XML file. The
     * file is located as a resource relative to the specified base class. The
     * file is parsed using a DOM parser, then a DOM <code>Document</code> is
     * retrieved and returned to the caller.
     * 
     * @param base
     *            The class to use as a base for finding the file.
     * @param path
     *            A file path specifying the location of the XML file relative
     *            to the base class.
     * @return A DOM <code>Document</code> object containing the parsed contents
     *         of the XML file.
     */
    public static Document loadXMLDocument(Class base, String path)
    {
        Document doc = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();

            Errors errors = new Errors();
            parser.setErrorHandler(errors);

            doc = parser.parse(new InputSource(new StringReader(base.getResource(path).toString())));
        }
        catch (Exception e)
        {
            fail("XML parser failure for xml file <" + path + ">");
        }

        assertNotNull("Failed to create document from xml file <" + path + ">", doc);

        return doc;
    }

    /**
     * Retrieves a specific "test" <code>Element</code> from the specified file.
     * This test <code>Element</code> should contain all of the test cases and
     * test data for use in a single test function.
     * 
     * @param xmlFile
     *            A file path specifying the location of the XML file relative
     *            to the base class.
     * @param base
     *            The class to use as a base for finding the file.
     * @param testName
     *            The "name" attribute associated with the "test"
     *            <code>Element</code> to return.
     * @return An <code>Element</code> containing all of the test case, and test
     *         data for the specified test.
     */
    public static Element getTestElement(String xmlFile, Class testClass, String testName)
    {
        Element element;

        // load the test data doc
        Document doc = loadXMLDocument(testClass, xmlFile);

        // get "root" element
        NodeList list = doc.getElementsByTagName("TestSuite");
        element = (Element) list.item(0);

        // get "testclass" element
        element = getNamedChildElement(element, "TestClass", testClass.getName());

        // get "test" element
        element = getNamedChildElement(element, "Test", testName);

        if (element == null) assertNotNull("unable to load test <" + testName + ">", element);

        return (element);
    }

    /**
     * Retrieves a child element of the given <code>Node</code> with the given
     * <code>Element</code> tag name and the given "name" attribute.
     * 
     * @param parent
     *            A DOM <code>Node</code> object that contains the requested
     *            element.
     * @param elementTag
     *            The tag name of the type of <code>Element</code> that we're
     *            interested in.
     * @param name
     *            The name to look for in the "name" attribute of the child
     *            elements.
     * 
     * @return The child <code>Element</code> found matching the specified type
     *         and name requirements.
     */
    public static Element getNamedChildElement(Node parent, String elementTag, String name)
    {
        NodeList childElements = null;
        Element element;

        if (parent instanceof Document)
        {
            childElements = ((Document) parent).getElementsByTagName(elementTag);
        }
        else if (parent instanceof Element)
        {
            childElements = ((Element) parent).getElementsByTagName(elementTag);
        }

        if (childElements != null)
        {
            // look for the appropriatly named child
            for (int i = 0; i < childElements.getLength(); i++)
            {
                element = (Element) childElements.item(i);
                if (element.getAttribute("name").equals(name))
                {
                    return (element);
                }
            }
        }

        return (null);
    }

    /**
     * Retrieves all of the test case <code>Element</code>s contained in the
     * given parent "Test" <code>Element</code>. The search can optionally be
     * narrowed by specifying the <code>targetType</code> and
     * <code>targetId</code> parameters which correlate to the "targetType" and
     * "targetId" attributes on the "TestCase" <code>Element</code>.
     * 
     * @param parentElement
     *            The parent <code>Element</code> containing the "TestCase"
     *            <code>Element</code>s to retrieve.
     * @param targetType
     *            The type of object this test case is targeted at. (i.e.
     *            screen, device, ...) <br>
     *            This parameter should be <code>null</code> if all contained
     *            test cases should be returned.
     * @param targetId
     *            A <code>String</code> uniquely describing the target to find
     *            test cases for. <br>
     *            This parameter should be <code>null</code> if all contained
     *            test cases should be returned.
     * @return An array containing all of the "TestCase" <code>Element</code>s
     *         found within the parent <code>Element</code>.
     * 
     */
    public static Element[] getTestCases(Element parentElement, String targetType, String targetId)
    {
        NodeList testCaseNodes = parentElement.getElementsByTagName("TestCase");
        Vector testCaseElements = new Vector(testCaseNodes.getLength());

        for (int i = 0; i < testCaseNodes.getLength(); i++)
        {
            Element testCase = (Element) testCaseNodes.item(i);
            if (targetType != null && targetId != null)
            {
                if (testCase.getAttribute("targetType").equals(targetType)
                        && testCase.getAttribute("targetId").equals(targetId))
                {
                    testCaseElements.add(testCase);
                }
            }
            else
            {
                testCaseElements.add(testCase);
            }
        }

        Element[] elements = new Element[testCaseElements.size()];
        for (int j = 0; j < elements.length; j++)
        {
            elements[j] = (Element) testCaseElements.get(j);
        }

        return elements;
    }

    /*************************** Methods for creating test data ***************/

    /**
     * Retrieves all of the test data of a particular type from the specified
     * "TestCase" <code>Element</code>.
     * 
     * @param type
     *            The type of test data to look for. Valid values are "Setup",
     *            "Input", or "Result".
     * @param testCaseElement
     *            The "TestCase" <code>Element</code> to find data in.
     * @return An array of <code>TestData</code> objects. Each object containing
     *         a single piece of data to be used in the test.
     */
    public static TestData[] getTestData(String type, Element testCaseElement)
    {
        NodeList dataNodes = testCaseElement.getElementsByTagName(type);
        TestData[] data = new TestData[dataNodes.getLength()];

        for (int i = 0; i < dataNodes.getLength(); i++)
        {
            Element dataElement = (Element) dataNodes.item(i);
            TestData tempData = new TestData();

            // get command
            tempData.command = dataElement.getAttribute("command");

            // get additional miscellaneous data
            tempData.misc = dataElement.getAttribute("misc");

            // check for template
            // passing in dataElement, so "misc" can be set if a deviceID is
            // set in the template element
            loadTemplate(dataElement, tempData);

            if (tempData.data == null)
            {
                // check for object
                tempData.data = loadObject(dataElement);
            }

            data[i] = tempData;
        }

        return (data);
    }

    /**
     * Retrieves a subset of a full array of <code>TestData</code> objects based
     * on their command value.
     * 
     * @param allTestData
     *            The full array of <code>TestData</code> to retrieve the subset
     *            from.
     * @param command
     *            The command of the <code>TestData</code> objects to be
     *            returned.
     * @return An array of <code>TestData</code> objects with the requested
     *         command value.
     */
    public static TestData[] subsetTestData(TestData[] allTestData, String command)
    {
        Vector v = new Vector(allTestData.length);

        for (int i = 0; i < allTestData.length; i++)
        {
            if (allTestData[i].command.equals(command)) v.add(allTestData[i]);
        }

        TestData[] subset = new TestData[v.size()];

        for (int j = 0; j < subset.length; j++)
        {
            subset[j] = (TestData) v.elementAt(j);
        }

        return subset;
    }

    /**
     * Loads a template if one is specified within the given
     * <code>Element</code> and is set as the <code>data</code> field of the
     * given <code>TestData</code> object.
     * 
     * @param parentElement
     *            The <code>Element</code> to try to find a "Template"
     *            <code>Element</code> in.
     * @param testData
     *            The <code>TestData</code> object to set the <code>data</code>
     *            field on if a template is found.
     */
    public static void loadTemplate(Element parentElement, TestData testData)
    {
        Object template = null;

        // check for template
        Element templateElement = null;

        // first, look to see if a template is specified by ID.
        String templateId = parentElement.getAttribute("templateId");
        if (templateId.length() > 0)
        {
            templateElement = parentElement.getOwnerDocument().getElementById(templateId);
        }
        else
        {
            // then look for any contained "Template" elements
            NodeList templateNodes = parentElement.getElementsByTagName("Template");
            if (templateNodes.getLength() > 0)
            {
                templateElement = (Element) templateNodes.item(0);
            }
        }

        // If a template element was found, create a template object of the
        // appropriate type, and set it as the "data" for the given TestData
        // object.
        if (templateElement != null)
        {
            if (templateElement.getAttribute("type").indexOf("Scene") >= 0)
                template = createSceneTemplate(templateElement);
            else
                template = createScreenTemplate(templateElement);

            testData.data = template;
            testData.misc = templateElement.getAttribute("device");
        }
    }

    /**
     * Builds and returns an <code>Object</code> if one is specified within the
     * given <code>Element</code>.
     * 
     * @param parentElement
     *            The <code>Element</code> to try to find an "Object"
     *            <code>Element</code> in.
     * @return An <code>Object</code> created using a child "Object"
     *         <code>Element</code>, or null if no such <code>Element</code> is
     *         found.
     */
    public static Object loadObject(Element parentElement)
    {
        Object object = null;

        // check for template
        Element objectElement = null;

        // first, look to see if an objct is specified by ID.
        String objectId = parentElement.getAttribute("objectId");
        if (objectId.length() > 0)
        {
            objectElement = parentElement.getOwnerDocument().getElementById(objectId);
        }
        else
        {
            // then look for any contained "Object" elements
            NodeList objectNodes = parentElement.getElementsByTagName("Object");
            if (objectNodes.getLength() > 0)
            {
                objectElement = (Element) objectNodes.item(0);
            }
        }

        // If an object element was found, create the object and assign it to
        // the "object" variable for return.
        if (objectElement != null)
        {
            object = buildObject(objectElement);
        }

        return object;
    }

    /**
     * Creates a single <code>HScreenConfigTemplate</code> based on the contents
     * of the specified "Template" <code>Element</code>.
     * 
     * @param templateElement
     *            The "Template" <code>Element</code> containing all of the data
     *            needed to construct the desired
     *            <code>HScreenConfigTemplate</code>.
     * @return An <code>HScreenConfigTemplate</code> object created from the
     *         provided <code>Element</code>.
     */
    public static HScreenConfigTemplate createScreenTemplate(Element templateElement)
    {
        HScreenConfigTemplate template;
        Element preferenceElement;
        Preference pref;

        try
        {
            // get template class from "type" attribute
            Class templateClass = Class.forName(templateElement.getAttribute("type"));

            // create new instance of that template type
            template = (HScreenConfigTemplate) templateClass.newInstance();

            // get "preference" elements for the current template
            NodeList preferenceNodes = templateElement.getElementsByTagName("Preference");

            // iterate through all of the preferences for that template
            // definition,
            // and set the appropriate preferences on the current template
            // instance
            for (int i = 0; i < preferenceNodes.getLength(); i++)
            {
                // set all of the preferences on the template
                pref = getPreference(templateClass, (Element) preferenceNodes.item(i));

                if (pref.object != null)
                {
                    // allow for a null object by looking the special case of a
                    // string object of "null"
                    if (pref.object.equals("null"))
                        template.setPreference(pref.preference, null, pref.priority);
                    else
                        template.setPreference(pref.preference, pref.object, pref.priority);
                }
                else
                    template.setPreference(pref.preference, pref.priority);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            template = null;
        }

        return (template);
    }

    /**
     * Creates a single <code>HSceneTemplate</code> based on the contents of the
     * specified "Template" <code>Element</code>.
     * 
     * @param templateElement
     *            The "Template" <code>Element</code> containing all of the data
     *            needed to construct the desired <code>HSceneTemplate</code>.
     * @return An <code>HSceneTemplate</code> object created from the provided
     *         <code>Element</code>.
     */
    public static HSceneTemplate createSceneTemplate(Element element)
    {
        HSceneTemplate template;
        Element preference;
        Preference pref;

        try
        {
            // get template class from "type" attribute
            Class templateClass = Class.forName(element.getAttribute("type"));

            // create new instance of that template type
            template = (HSceneTemplate) templateClass.newInstance();

            // can't use getElementsByTagName here because it finds the
            // "preference" nodes all the
            // way down it's subnode tree which includes "preference" nodes that
            // are in the "config" node
            // // get "preference" elements for the current template
            // NodeList preferenceNodes =
            // element.getElementsByTagName("preference");

            // get "preference" elements for the current template
            NodeList preferenceNodes = element.getChildNodes();

            // iterate through all of the preferences for that template
            // definition,
            // and set the appropriate preferences on the current template
            // instance
            for (int i = 0; i < preferenceNodes.getLength(); i++)
            {
                if (preferenceNodes.item(i) instanceof Element
                        && ((Element) preferenceNodes.item(i)).getTagName().equals("Preference"))
                {
                    // set all of the preferences on the template
                    pref = getPreference(templateClass, (Element) preferenceNodes.item(i));
                    if (pref.object != null && pref.object.equals("null"))
                        template.setPreference(pref.preference, null, pref.priority);
                    else
                        template.setPreference(pref.preference, pref.object, pref.priority);
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            template = null;
        }

        return (template);
    }

    /**
     * Creates a <code>Preference</code> object based on the contents of the
     * specified "Preference" <code>Element</code>.
     * 
     * @param templateClass
     *            The <code>Class</code> object describing the type of template
     *            we're building a preference for. This <code>Class</code> is
     *            used to obtain the preference constant values, and priority
     *            constant values.
     * @param prefElement
     *            The "Preference" <code>Element</code> containing all of the
     *            data needed to construct the desired <code>Preference</code>
     *            object.
     * @return A <code>Preference</code> object created from the provided
     *         <code>Element</code>.
     */
    public static Preference getPreference(Class templateClass, Element prefElement) throws NoSuchFieldException,
            IllegalAccessException
    {
        Preference pref = new Preference();
        Object object = null;

        // retrieve the preference and priority values
        pref.preference = ((Integer) templateClass.getField(prefElement.getAttribute("name")).get(null)).intValue();

        pref.priority = ((Integer) templateClass.getField(prefElement.getAttribute("priority")).get(null)).intValue();

        // check for object and instantiate if present (1 max)
        String objectId = prefElement.getAttribute("objectId");
        Element objectElement = null;
        if (objectId.length() > 0)
        {
            objectElement = prefElement.getOwnerDocument().getElementById(objectId);
        }
        else
        {
            NodeList objectNodes = prefElement.getElementsByTagName("Object");
            if (objectNodes.getLength() > 0)
            {
                objectElement = (Element) objectNodes.item(0);
            }
        }

        if (objectElement != null)
        {
            object = buildObject(objectElement);
            pref.object = object;
        }

        return (pref);
    }

    /**
     * Creates an <code>Object</code> based on the contents of the specified
     * "Object" <code>Element</code>.
     * 
     * @param objectElement
     *            The "Object" <code>Element</code> containing all of the data
     *            needed to construct the desired <code>Object</code>.
     * @return An <code>Object</code> created from the provided
     *         <code>Element</code>.
     */
    public static Object buildObject(Element objectElement)
    {
        Object object = null;
        String className = objectElement.getAttribute("type");

        if (className.equals("null"))
        {
            object = "null";
        }
        else if (className.equals("LARGEST_PIXEL_DIMENSION")) // total kludge!
        {
            object = HSceneTemplate.LARGEST_PIXEL_DIMENSION;
        }
        else if (className.equals("org.havi.ui.HGraphicsConfiguration"))
        {
            String templateId = objectElement.getAttribute("templateId");
            Element templateElement;

            if (templateId.length() > 0)
            {
                templateElement = objectElement.getOwnerDocument().getElementById(templateId);
            }
            else
            {
                // need to get the best config for the ScreenTemplate node
                // definition
                NodeList templateNodes = objectElement.getElementsByTagName("Template");

                templateElement = (Element) templateNodes.item(0);
            }

            // get the device to look for the config on.
            String templateType = templateElement.getAttribute("type").toLowerCase();
            String deviceId = templateElement.getAttribute("device");
            HScreenDevice device = findNamedDevice(templateType, deviceId);

            // get the template to make "get best" call with
            HScreenConfigTemplate template = createScreenTemplate(templateElement);

            if (device != null && template != null) object = getBestConfig(device, template);
        }
        else
        {
            int counter = 0;
            Vector paramTypes = new Vector(4);
            Vector paramValues = new Vector(4);

            NodeList paramNodes = objectElement.getElementsByTagName("Parameter");

            for (int i = 0; i < paramNodes.getLength(); i++)
            {
                Element paramElement = (Element) paramNodes.item(i);
                String paramType = paramElement.getAttribute("type");
                String paramValue = paramElement.getAttribute("value");

                if (paramType.equals("int"))
                {
                    paramTypes.addElement(int.class);
                    paramValues.addElement(new Integer(paramValue));
                }
                else if (paramType.equals("float"))
                {
                    paramTypes.addElement(float.class);
                    paramValues.addElement(new Float(paramValue));
                }
                else if (paramType.equals("boolean"))
                {
                    paramTypes.addElement(boolean.class);
                    paramValues.addElement(new Boolean(paramValue));
                }
                else if (paramType.equals("String"))
                {
                    paramTypes.addElement(String.class);
                    paramValues.addElement(new String(paramValue));
                }
            }

            if (paramTypes.size() > 0)
            {
                Class[] types = new Class[paramTypes.size()];
                paramTypes.copyInto(types);
                Object[] values = new Object[paramValues.size()];
                paramValues.copyInto(values);

                try
                {
                    Constructor constructor = Class.forName(className).getConstructor(types);
                    if (constructor != null)
                    {
                        object = constructor.newInstance(values);
                    }
                }
                catch (Exception e)
                {
                    // fail("Object could not be created <" + className + ">");
                    return null;
                }
            }
        }

        return object;
    }

    /**
     * Retrieves an <code>HScreenDevice</code> of the specified type, with the
     * specified device id.
     * 
     * @param type
     *            The type of <code>HScreenDevice</code> to look for.
     * @param deviceId
     *            The id of the device to find. This id is compared to the value
     *            returned from the HScreenDevice.getIDstring() call.
     */
    public static HScreenDevice findNamedDevice(String type, String deviceId)
    {
        HScreenDevice device = null;
        HScreen[] screens = HScreen.getHScreens();

        type = type.toLowerCase();

        if (deviceId.length() > 0)
        {
            if (type.indexOf("graphics") > -1)
            {
                // iterate through all Screens and all HGraphicsDevices until
                // we find a match
                HGraphicsDevice[] graphicsDevices;
                for (int i = 0; i < screens.length && device == null; i++)
                {
                    graphicsDevices = screens[i].getHGraphicsDevices();

                    // try to find the src device by id
                    for (int j = 0; j < graphicsDevices.length; j++)
                    {
                        if (graphicsDevices[j].getIDstring().equals(deviceId))
                        {
                            device = graphicsDevices[j];
                            break;
                        }
                    }
                }
            }
            else if (type.indexOf("video") > -1)
            {
                // iterate through all Screens and all HVideoDevices until
                // we find a match
                HVideoDevice[] videoDevices;
                for (int i = 0; i < screens.length && device == null; i++)
                {
                    videoDevices = screens[i].getHVideoDevices();

                    // try to find the src device by id
                    for (int j = 0; j < videoDevices.length; j++)
                    {
                        if (videoDevices[j].getIDstring().equals(deviceId))
                        {
                            device = videoDevices[j];
                            break;
                        }
                    }
                }
            }
            else if (type.indexOf("background") > -1)
            {
                // iterate through all Screens and all HBackgroundDevices until
                // we find a match
                HBackgroundDevice[] backgroundDevices;
                for (int i = 0; i < screens.length && device == null; i++)
                {
                    backgroundDevices = screens[i].getHBackgroundDevices();

                    // try to find the src device by id
                    for (int j = 0; j < backgroundDevices.length; j++)
                    {
                        if (backgroundDevices[j].getIDstring().equals(deviceId))
                        {
                            device = backgroundDevices[j];
                            break;
                        }
                    }
                }
            }
        }
        return device;
    }

    /**
     * Utility method used to get the best <code>HScreenConfiguration</code>
     * from the specified <code>HScreenDevice</code> using the specified
     * <code>HScreenConfigTemplate</code> without knowing the specific type that
     * we're working with (i.e. Background, Graphics, or Video).
     * 
     * @param device
     *            The <code>HScreenDevice</code> to find the best
     *            <code>HScreenConfiguration</code> for.
     * @param template
     *            The template to use in finding the "best" configuration.
     * 
     * @return The <code>HScreenConfiguration</code> that best matches the input
     *         <code>HScreenConfigTemplate</code>.
     */
    public static HScreenConfiguration getBestConfig(HScreenDevice device, HScreenConfigTemplate template)
    {
        HScreenConfiguration config = null;
        if (device instanceof HGraphicsDevice)
        {
            config = ((HGraphicsDevice) device).getBestConfiguration((HGraphicsConfigTemplate) template);
        }
        else if (device instanceof HVideoDevice)
        {
            config = ((HVideoDevice) device).getBestConfiguration((HVideoConfigTemplate) template);
        }
        else if (device instanceof HBackgroundDevice)
        {
            config = ((HBackgroundDevice) device).getBestConfiguration((HBackgroundConfigTemplate) template);
        }

        return (config);
    }

    /**
     * Utility method used to get the best <code>HScreenConfiguration</code>
     * from the specified <code>HScreenDevice</code> using the specified array
     * of <code>HScreenConfigTemplate</code>s without knowing the specific type
     * that we're working with (i.e. Background, Graphics, or Video).
     * 
     * @param device
     *            The <code>HScreenDevice</code> to find the best
     *            <code>HScreenConfiguration</code> for.
     * @param templates
     *            The array of templates to use in finding the "best"
     *            configuration.
     * 
     * @return The <code>HScreenConfiguration</code> that best matches the input
     *         <code>HScreenConfigTemplate</code>s.
     */
    public static HScreenConfiguration getBestConfig(HScreenDevice device, HScreenConfigTemplate[] templates)
    {
        HScreenConfiguration config = null;
        if (device instanceof HGraphicsDevice)
        {
            config = ((HGraphicsDevice) device).getBestConfiguration((HGraphicsConfigTemplate[]) templates);
        }
        else if (device instanceof HVideoDevice)
        {
            config = ((HVideoDevice) device).getBestConfiguration((HVideoConfigTemplate[]) templates);
        }
        else if (device instanceof HBackgroundDevice)
        {
            config = ((HBackgroundDevice) device).getBestConfiguration((HBackgroundConfigTemplate[]) templates);
        }

        return (config);
    }

    /**
     * Utility method used to set the current <code>HScreenConfiguration</code>
     * on the specified <code>HScreenDevice</code> without knowing the specific
     * type that we're working with (i.e. Background, Graphics, or Video).
     * 
     * @param device
     *            The <code>HScreenDevice</code> to set the current
     *            <code>HScreenConfiguration</code> on.
     * @param config
     *            The <code>HScreenConfiguration</code> to set as the current
     *            configuration.
     */
    public static void setCurrentConfig(HScreenDevice device, HScreenConfiguration config)
    {
        try
        {
            if (device instanceof HGraphicsDevice)
            {
                ((HGraphicsDevice) device).setGraphicsConfiguration((HGraphicsConfiguration) config);
            }
            else if (device instanceof HVideoDevice)
            {
                ((HVideoDevice) device).setVideoConfiguration((HVideoConfiguration) config);
            }
            else if (device instanceof HBackgroundDevice)
            {
                ((HBackgroundDevice) device).setBackgroundConfiguration((HBackgroundConfiguration) config);
            }
        }
        catch (org.havi.ui.HPermissionDeniedException e)
        {
            e.printStackTrace();
        }
        catch (org.havi.ui.HConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This class represents the components of a single "preference" within a
     * template (<code>HScreenConfigTemplate</code> or
     * <code>HSceneTemplate</code>).
     */
    public static class Preference
    {
        /**
         * The constant value for the specified preference as defined in the
         * relevant class.
         */
        public int preference;

        /**
         * The constant value for the specified preference priority as defined
         * in the relevant class.
         */
        public int priority;

        /**
         * An optional <code>Object</code> associated with the preference.
         */
        public Object object;
    }

    /**
     * This class represents a single piece of test data to be used within a
     * test. It is used to represent any data described in a "Setup", "Intput",
     * or "Result" <code>Element</code>.
     */
    public static class TestData
    {
        /**
         * A <code>String</code> describing what this <code>TestData</code>
         * object is for. The test code is responsible for recognizing the
         * command, and knowing what to do what to do with any contained
         * <code>Object</code> data. (example: compareequal, comparecompatible,
         * comparelarger...)
         */
        public String command;

        /**
         * An optional <code>String</code> used to further clarify the action to
         * take for the given command, or to give more context to the
         * <code>Object</code> data.
         */
        public String misc;

        /**
         * An optional data <code>Object</code> to use within the test.
         */
        public Object data;
    }

    /**
     * This class is used by the DOM parser to report any errors in parsing the
     * XML document.
     */
    public static class Errors implements org.xml.sax.ErrorHandler
    {
        public void error(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
        {
        }

        public void fatalError(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
        {
        }

        void store(SAXParseException ex, String type)
        {
        }

        public void warning(org.xml.sax.SAXParseException arg1) throws org.xml.sax.SAXException
        {
        }
    }

}
