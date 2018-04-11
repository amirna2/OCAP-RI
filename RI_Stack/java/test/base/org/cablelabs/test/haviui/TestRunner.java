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

package org.cablelabs.test.haviui;

//package junit.haviui;

import org.havi.ui.*;
import org.havi.ui.event.*;
import java.util.*;
import junit.framework.*;
import junit.runner.*;
import java.awt.Font;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.SystemColor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.event.*;
import java.awt.IllegalComponentStateException;

/**
 * A HAVi UI based user interface to run tests. Enter the name of a class which
 * either provides a static suite method or is a sublcass of TestCase. This is a
 * modified version of the awtui TestRunner class.
 * 
 * <pre>
 * Synopsis: java org.cablelabs.test.haviui.TestRunner [-noloading] [TestCase]
 * </pre>
 * 
 * TestRunner takes as an optional argument the name of the testcase class to be
 * run.
 */
public class TestRunner extends BaseTestRunner
{
    protected HScene fFrame;

    protected Vector fExceptions;

    protected Vector fFailedTests;

    protected Thread fRunner;

    protected TestResult fTestResult;

    protected TextArea fTraceArea;

    protected TextField fSuiteField;

    protected Button fRun;

    protected ProgressBar fProgressIndicator;

    protected List fFailureList;

    protected Logo fLogo;

    protected Label fNumberOfErrors;

    protected Label fNumberOfFailures;

    protected Label fNumberOfRuns;

    protected Button fQuitButton;

    protected Button fRerunButton;

    protected Label fStatusLine;

    // protected Checkbox fUseLoadingRunner;

    protected Vector components;

    protected static final Font PLAIN_FONT = new Font("sansserif", Font.PLAIN, 12);

    protected static final Font ROOT_FONT = new Font("sansserif", Font.PLAIN, 14);

    private static final int GAP = 4;

    public TestRunner()
    {
    }

    /*
     * private void about() { AboutDialog about= new AboutDialog(fFrame);
     * about.setModal(true); about.setLocation(300, 300);
     * about.setVisible(true); }
     */

    /**
     * Always use the StandardTestSuiteLoader. Overridden from BaseTestRunner.
     */
    public TestSuiteLoader getLoader()
    {
        return new StandardTestSuiteLoader();
    }

    public void testStarted(String testName)
    {
        showInfo("Running: " + testName);
    }

    public void testEnded(String testName)
    {
        setLabelValue(fNumberOfRuns, fTestResult.runCount());
        synchronized (this)
        {
            fProgressIndicator.step(fTestResult.wasSuccessful());
        }
    }

    public void testFailed(int status, Test test, Throwable t)
    {
        switch (status)
        {
            case TestRunListener.STATUS_ERROR:
                fNumberOfErrors.setText(Integer.toString(fTestResult.errorCount()));
                appendFailure("Error", test, t);
                break;
            case TestRunListener.STATUS_FAILURE:
                fNumberOfFailures.setText(Integer.toString(fTestResult.failureCount()));
                appendFailure("Failure", test, t);
                break;
        }
    }

    protected void addGrid(Panel p, Component co, int x, int y, int w, int fill, double wx, int anchor)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.anchor = anchor;
        c.weightx = wx;
        c.fill = fill;
        if (fill == GridBagConstraints.BOTH || fill == GridBagConstraints.VERTICAL) c.weighty = 1.0;
        c.insets = new Insets(y == 0 ? GAP : 0, x == 0 ? GAP : 0, GAP, GAP);
        p.add(co, c);
    }

    private void appendFailure(String kind, Test test, Throwable t)
    {
        kind += ": " + test;
        String msg = t.getMessage();
        if (msg != null)
        {
            kind += ":" + truncate(msg);
        }
        fExceptions.addElement(t);
        fFailedTests.addElement(test);
        fFailureList.add(kind);
        if (fFailureList.getItemCount() == 1)
        {
            fFailureList.select(0);
            failureSelected();
        }
    }

    /**
     * Creates the JUnit menu. Clients override this method to add additional
     * menu items.
     */
    /*
     * protected Menu createJUnitMenu() { Menu menu= new Menu("JUnit"); MenuItem
     * mi= new MenuItem("About..."); mi.addActionListener( new ActionListener()
     * { public void actionPerformed(ActionEvent event) { about(); } } );
     * menu.add(mi);
     * 
     * menu.addSeparator(); mi= new MenuItem("Exit"); mi.addActionListener( new
     * ActionListener() { public void actionPerformed(ActionEvent event) {
     * System.exit(0); } } ); menu.add(mi); return menu; }
     */

    /*
     * protected void createMenus(MenuBar mb) { mb.add(createJUnitMenu()); }
     */
    protected TestResult createTestResult()
    {
        return new TestResult();
    }

    protected void registerComponent(Component c)
    {
        System.out.println("REGISTER: " + c);
        if (components == null) components = new Vector();
        components.addElement(c);
        c.addFocusListener(new java.awt.event.FocusAdapter()
        {
            public void focusGained(java.awt.event.FocusEvent e)
            {
                System.out.println("Focus -> " + e.getSource());
            }
        });
    }

    protected HScene createUI(String suiteName)
    {
        HScene scene = HSceneFactory.getInstance().getDefaultHScene();
        /*
         * Image icon= loadFrameIcon(); if (icon != null)
         * frame.setIconImage(icon);
         */

        Color BG = Color.lightGray;
        Color MG = BG.brighter();
        Color FG = Color.black;

        scene.setLayout(new BorderLayout(0, 0));
        scene.setFont(ROOT_FONT);
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        scene.setBackground(BG);
        scene.setForeground(FG);

        /*
         * final Frame finalFrame = frame;
         * 
         * frame.addWindowListener( new WindowAdapter() { public void
         * windowClosing(WindowEvent e) { finalFrame.dispose(); System.exit(0);
         * } } );
         */

        // MenuBar mb = new MenuBar();
        // createMenus(mb);
        // frame.setMenuBar(mb);

        // ---- first section
        Label suiteLabel = new Label("Test class name:");

        fSuiteField = new TextField(suiteName != null ? suiteName : "");
        // fSuiteField.selectAll();
        // fSuiteField.requestFocus();
        fSuiteField.setFont(PLAIN_FONT);
        fSuiteField.setBackground(MG);
        /*
         * fSuiteField.addActionListener( new ActionListener() { public void
         * actionPerformed(ActionEvent e) { runSuite(); } } );
         */
        fSuiteField.addHTextListener(new HTextListener()
        {
            public void caretMoved(HTextEvent e)
            {
            }

            public void textChanged(HTextEvent e)
            {
                fRun.setEnabled(fSuiteField.getText().length() > 0);
                fStatusLine.setText("");
            }
        });
        fRun = new Button("Run");
        fRun.setTextContent("Stop", HState.DISABLED_STATE); // sizing kludge
        fRun.setBackground(MG);
        fRun.setEnabled(false);
        fRun.addHActionListener(new HActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                runSuite();
            }
        });
        /*
         * boolean useLoader= useReloadingTestSuiteLoader(); fUseLoadingRunner=
         * new Checkbox("Reload classes every run", useLoader); if (inVAJava())
         * fUseLoadingRunner.setVisible(false);
         */

        // ---- second section
        fProgressIndicator = new ProgressBar();
        fProgressIndicator.setBackground(MG);
        fProgressIndicator.setDefaultSize(new Dimension(HVisible.NO_DEFAULT_WIDTH, 10));

        // ---- third section
        fNumberOfErrors = new Label("0000", Label.RIGHT);
        fNumberOfErrors.setText("0");
        fNumberOfErrors.setFont(PLAIN_FONT);

        fNumberOfFailures = new Label("0000", Label.RIGHT);
        fNumberOfFailures.setText("0");
        fNumberOfFailures.setFont(PLAIN_FONT);

        fNumberOfRuns = new Label("0000", Label.RIGHT);
        fNumberOfRuns.setText("0");
        fNumberOfRuns.setFont(PLAIN_FONT);

        Panel numbersPanel = createCounterPanel();

        // ---- fourth section
        Label failureLabel = new Label("Errors and Failures:");

        fFailureList = new List(5);
        fFailureList.setFont(PLAIN_FONT);
        fFailureList.setBackground(MG);
        fFailureList.addItemListener(new HItemListener()
        {
            public void selectionChanged(HItemEvent e)
            {
                fFailureList.clearSelection();
            }

            public void currentItemChanged(HItemEvent e)
            {
                failureSelected();
            }
        });
        fRerunButton = new Button("Run");
        fRerunButton.setBackground(MG);
        fRerunButton.setEnabled(false);
        fRerunButton.addHActionListener(new HActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                rerun();
            }
        });

        Panel failedPanel = new Panel(new GridLayout(0, 1, 0, 2));
        failedPanel.add(fRerunButton);

        fTraceArea = new TextArea();
        fTraceArea.setBackground(MG);
        fTraceArea.setRows(5);
        fTraceArea.setColumns(60);
        fTraceArea.setFont(PLAIN_FONT);

        // ---- fifth section
        fStatusLine = new Label("...HAVi UI TestRunner...");
        fStatusLine.setFont(PLAIN_FONT);
        // fStatusLine.setEditable(false);
        fStatusLine.setForeground(Color.red);

        fQuitButton = new Button("Exit");
        fQuitButton.setBackground(MG);
        fQuitButton.addHActionListener(new HActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                System.exit(0);
            }
        });

        // ---------
        fLogo = new Logo();

        // ---- overall layout
        Panel panel = new Panel(new GridBagLayout());

        addGrid(panel, suiteLabel, 0, 0, 2, GridBagConstraints.HORIZONTAL, 1.0, GridBagConstraints.WEST);

        addGrid(panel, fSuiteField, 0, 1, 2, GridBagConstraints.HORIZONTAL, 1.0, GridBagConstraints.WEST);
        addGrid(panel, fRun, 2, 1, 1, GridBagConstraints.HORIZONTAL, 0.0, GridBagConstraints.CENTER);
        // addGrid(panel, fUseLoadingRunner, 0, 2, 2, GridBagConstraints.NONE,
        // 1.0, GridBagConstraints.WEST);
        addGrid(panel, fProgressIndicator, 0, 3, 2, GridBagConstraints.HORIZONTAL, 1.0, GridBagConstraints.WEST);
        addGrid(panel, fLogo, 2, 3, 1, GridBagConstraints.NONE, 0.0, GridBagConstraints.NORTH);

        addGrid(panel, numbersPanel, 0, 4, 2, GridBagConstraints.NONE, 0.0, GridBagConstraints.WEST);

        addGrid(panel, failureLabel, 0, 5, 2, GridBagConstraints.HORIZONTAL, 1.0, GridBagConstraints.WEST);
        addGrid(panel, fFailureList, 0, 6, 2, GridBagConstraints.BOTH, 1.0, GridBagConstraints.WEST);
        addGrid(panel, failedPanel, 2, 6, 1, GridBagConstraints.HORIZONTAL, 0.0, GridBagConstraints.CENTER);
        addGrid(panel, fTraceArea, 0, 7, 2, GridBagConstraints.BOTH, 1.0, GridBagConstraints.WEST);

        addGrid(panel, fStatusLine, 0, 8, 2, GridBagConstraints.HORIZONTAL, 1.0, GridBagConstraints.CENTER);
        addGrid(panel, fQuitButton, 2, 8, 1, GridBagConstraints.HORIZONTAL, 0.0, GridBagConstraints.CENTER);

        scene.add(panel, BorderLayout.CENTER);
        scene.setSize(640, 480);
        scene.validate();

        return scene;
    }

    protected Panel createCounterPanel()
    {
        Panel numbersPanel = new Panel(new GridBagLayout());
        addToCounterPanel(numbersPanel, new Label("Runs:"), 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0));
        addToCounterPanel(numbersPanel, fNumberOfRuns, 1, 0, 1, 1, 0.33, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 40));
        addToCounterPanel(numbersPanel, new Label("Errors:"), 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0, 8, 0, 0));
        addToCounterPanel(numbersPanel, fNumberOfErrors, 3, 0, 1, 1, 0.33, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 40));
        addToCounterPanel(numbersPanel, new Label("Failures:"), 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0, 8, 0, 0));
        addToCounterPanel(numbersPanel, fNumberOfFailures, 5, 0, 1, 1, 0.33, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0));
        return numbersPanel;
    }

    private void addToCounterPanel(Panel counter, Component comp, int gridx, int gridy, int gridwidth, int gridheight,
            double weightx, double weighty, int anchor, int fill, Insets insets)
    {

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.gridwidth = gridwidth;
        constraints.gridheight = gridheight;
        constraints.weightx = weightx;
        constraints.weighty = weighty;
        constraints.anchor = anchor;
        constraints.fill = fill;
        constraints.insets = insets;
        counter.add(comp, constraints);
    }

    public void failureSelected()
    {
        fRerunButton.setEnabled(isErrorSelected());
        showErrorTrace();
    }

    private boolean isErrorSelected()
    {
        return fFailureList.getSelectedIndex() != -1;
    }

    /*
     * private Image loadFrameIcon() { Toolkit toolkit=
     * Toolkit.getDefaultToolkit(); try { java.net.URL url=
     * BaseTestRunner.class.getResource("smalllogo.gif"); return
     * toolkit.createImage((ImageProducer) url.getContent()); } catch (Exception
     * ex) { } return null; }
     */

    public Thread getRunner()
    {
        return fRunner;
    }

    public static void main(String[] args)
    {
        new TestRunner().start(args);
    }

    public static void run(Class test)
    {
        String args[] = { test.getName() };
        main(args);
    }

    public void rerun()
    {
        int index = fFailureList.getSelectedIndex();
        if (index == -1) return;

        Test test = (Test) fFailedTests.elementAt(index);
        rerunTest(test);
    }

    private void rerunTest(Test test)
    {
        if (!(test instanceof TestCase))
        {
            showInfo("Could not reload " + test.toString());
            return;
        }
        Test reloadedTest = null;
        TestCase rerunTest = (TestCase) test;
        try
        {
            Class reloadedTestClass = getLoader().reload(test.getClass());
            reloadedTest = TestSuite.createTest(reloadedTestClass, rerunTest.getName());
        }
        catch (Exception e)
        {
            showInfo("Could not reload " + test.toString());
            return;
        }
        TestResult result = new TestResult();
        reloadedTest.run(result);

        String message = reloadedTest.toString();
        if (result.wasSuccessful())
            showInfo(message + " was successful");
        else if (result.errorCount() == 1)
            showStatus(message + " had an error");
        else
            showStatus(message + " had a failure");
    }

    protected void reset()
    {
        setLabelValue(fNumberOfErrors, 0);
        setLabelValue(fNumberOfFailures, 0);
        setLabelValue(fNumberOfRuns, 0);
        fProgressIndicator.reset();
        fRerunButton.setEnabled(false);
        fFailureList.removeAll();
        fExceptions = new Vector(10);
        fFailedTests = new Vector(10);
        fTraceArea.setText("");

    }

    protected void runFailed(String message)
    {
        showStatus(message);
        fRun.setLabel("Run");
        fRunner = null;
    }

    synchronized public void runSuite()
    {
        if (fRunner != null && fTestResult != null)
        {
            fTestResult.stop();
        }
        else
        {
            // setLoading(shouldReload());
            setLoading(false);
            fRun.setLabel("Stop");
            showInfo("Initializing...");
            reset();

            showInfo("Load Test Case...");

            final Test testSuite = getTest(fSuiteField.getText());
            if (testSuite != null)
            {
                fRunner = new Thread()
                {
                    public void run()
                    {
                        fTestResult = createTestResult();
                        fTestResult.addListener(TestRunner.this);
                        fProgressIndicator.start(testSuite.countTestCases());
                        showInfo("Running...");

                        long startTime = System.currentTimeMillis();
                        testSuite.run(fTestResult);

                        if (fTestResult.shouldStop())
                        {
                            showStatus("Stopped");
                        }
                        else
                        {
                            long endTime = System.currentTimeMillis();
                            long runTime = endTime - startTime;
                            showInfo("Finished: " + elapsedTimeAsString(runTime) + " seconds");
                        }
                        fTestResult = null;
                        fRun.setLabel("Run");
                        fRunner = null;
                        System.gc();
                    }
                };
                fRunner.start();
            }
        }
    }

    /*
     * private boolean shouldReload() { return !inVAJava() &&
     * fUseLoadingRunner.getState(); }
     */

    private void setLabelValue(Label label, int value)
    {
        label.setText(Integer.toString(value));
        label.invalidate();
        label.getParent().validate();

    }

    public void setSuiteName(String suite)
    {
        fSuiteField.setText(suite);
    }

    private void showErrorTrace()
    {
        int index = fFailureList.getSelectedIndex();
        if (index == -1) return;

        Throwable t = (Throwable) fExceptions.elementAt(index);
        fTraceArea.setText(getFilteredTrace(t));
    }

    private void showInfo(String message)
    {
        fStatusLine.setFont(PLAIN_FONT);
        fStatusLine.setForeground(Color.black);
        fStatusLine.setText(message);
    }

    protected void clearStatus()
    {
        showStatus("");
    }

    private void showStatus(String status)
    {
        fStatusLine.setFont(PLAIN_FONT);
        fStatusLine.setForeground(Color.red);
        fStatusLine.setText(status);
    }

    /**
     * Starts the TestRunner
     */
    public void start(String[] args)
    {
        String suiteName = processArguments(args);
        fFrame = createUI(suiteName);
        // fFrame.setLocation(200, 200);
        fFrame.setVisible(true);

        {
            SetupTraversals setup = new SetupTraversals();

            Component[] comps = new Component[components.size()];
            components.copyInto(comps);
            setup.setFocusTraversal(comps);
            components.copyInto(comps);

            System.out.println("===============Traversals==================");
            for (int i = 0; i < comps.length; ++i)
                System.out.println(" comps[" + i + "] = " + comps[i] + " or " + components.elementAt(i));
            for (int i = 0; i < comps.length; ++i)
            {
                System.out.println("[" + i + "] = " + comps[i]);
                System.out.println("     UP    = " + ((HNavigable) comps[i]).getMove(KeyEvent.VK_UP));
                System.out.println("     DOWN  = " + ((HNavigable) comps[i]).getMove(KeyEvent.VK_DOWN));
                System.out.println("     LEFT  = " + ((HNavigable) comps[i]).getMove(KeyEvent.VK_LEFT));
                System.out.println("     RIGHT = " + ((HNavigable) comps[i]).getMove(KeyEvent.VK_RIGHT));
            }
        }

        fRun.requestFocus();

        if (suiteName != null)
        {
            setSuiteName(suiteName);
            runSuite();
        }
    }

    protected class Label extends HStaticText
    {
        public static final int RIGHT = HVisible.HALIGN_RIGHT;

        public static final int LEFT = HVisible.HALIGN_LEFT;

        public Label()
        {
            this("");
        }

        public Label(String text)
        {
            this(text, LEFT);
        }

        public Label(String text, int alignment)
        {
            super();
            setTextContent(text, HState.ALL_STATES);
            setBackgroundMode(BACKGROUND_FILL);
            setHorizontalAlignment(alignment);
            // registerComponent(this);
        }

        public void setText(String text)
        {
            setTextContent(text, HState.NORMAL_STATE);
        }
    }

    protected class Logo extends HStaticIcon
    {
    }

    protected class Button extends HTextButton
    {
        public Button(String name)
        {
            super(name);
            setBackgroundMode(BACKGROUND_FILL);
            registerComponent(this);
        }

        public void setLabel(String str)
        {
            setTextContent(str, HState.ALL_STATES);
        }
    }

    protected class Checkbox extends HToggleButton
    {
    }

    protected class TextArea extends HMultilineEntry
    {
        private boolean edit;

        public TextArea()
        {
            setBackgroundMode(BACKGROUND_FILL);
            setMaxChars(1000000);
            setHorizontalAlignment(HALIGN_LEFT);
            setVerticalAlignment(VALIGN_TOP);
            registerComponent(this);
        }

        public void setRows(int rows)
        {
        }

        public void setColumns(int columns)
        {
        }

        public void setEditable(boolean enabled)
        {
            edit = enabled;
        }

        public void processHKeyEvent(HKeyEvent e)
        {
            if (!edit) return;
            super.processHKeyEvent(e);
        }

        public void processHTextEvent(HTextEvent e)
        {
            if (false) switch (e.getID())
            {
                case HTextEvent.TEXT_START_CHANGE:
                    if (!edit) return;
            }
            super.processHTextEvent(e);
        }

        public String getText()
        {
            return getTextContent(HState.NORMAL_STATE);
        }

        public void setText(String text)
        {
            text = text.replace('\t', ' ');
            text = text.replace('\r', ' ');
            setTextContent(text, HState.ALL_STATES);
            setCaretCharPosition(0);
        }
    }

    protected class TextField extends HSinglelineEntry
    {
        private boolean edit = true;

        public TextField()
        {
            this("");
        }

        public TextField(String text)
        {
            setBackgroundMode(BACKGROUND_FILL);
            setMaxChars(1000);
            setTextContent(text, HState.ALL_STATES);
            setHorizontalAlignment(HALIGN_LEFT);
            registerComponent(this);
        }

        public void setEditable(boolean enabled)
        {
            edit = enabled;
        }

        public String getText()
        {
            return getTextContent(HState.NORMAL_STATE);
        }

        public void setText(String text)
        {
            setTextContent(text, HState.ALL_STATES);
        }

        public void processHTextEvent(HTextEvent e)
        {
            switch (e.getID())
            {
                case HTextEvent.TEXT_START_CHANGE:
                    if (!edit) return;
            }
            super.processHTextEvent(e);
        }
    }

    protected class ProgressBar extends HStaticRange
    {
        public ProgressBar()
        {
            setBackgroundMode(BACKGROUND_FILL);
        }

        public void step(boolean flag)
        {
            if (flag) setValue(getValue() + 1);
        }

        public void reset()
        {
            setValue(getMinValue());
        }

        public void start(int max)
        {
            setRange(0, max);
        }
    }

    protected class Panel extends HContainer
    {
        public Panel()
        {
            this(new java.awt.FlowLayout());
        }

        public Panel(java.awt.LayoutManager layout)
        {
            setLayout(layout);
        }

        public void paint(Graphics g)
        {
            Dimension size = getSize();
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);

            super.paint(g);
        }
    }

    protected class List extends HListGroup
    {
        public List(int count)
        {
            super();
            setBackgroundMode(BACKGROUND_FILL);
            registerComponent(this);
            setHorizontalAlignment(HALIGN_LEFT);
        }

        public void add(String entry)
        {
            addItem(new HListElement(entry), ADD_INDEX_END);
        }

        public void select(int idx)
        {
            setCurrentItem(idx);
        }

        public int getItemCount()
        {
            return getNumItems();
        }

        public int getSelectedIndex()
        {
            return getCurrentIndex();
        }

        public void removeAll()
        {
            removeAllItems();
        }
    }
}

/** Ripped from SnapLayout. */
class SetupTraversals
{
    /**
     * Computes and sets up the proper focus traversals for each
     * {@link HNavigable} component contained in the array.
     * 
     * @param array
     *            components of components
     */
    public void setFocusTraversal(Component[] components)
    {
        // Ignore non-navigable, non-visible components
        for (int i = 0; i < components.length; ++i)
        {
            if (!(components[i] instanceof HNavigable) || !components[i].isVisible()
                    || !components[i].isFocusTraversable()
            /*
             * || getConstraints(components[i]).nontraversable
             */)
            {
                components[i] = null;
            }
        }

        // We'll use the center point to measure distances
        Point center[] = new Point[components.length];
        for (int i = 0; i < center.length; ++i)
        {
            if (components[i] != null)
            {
                // center[i] = findCenter(components[i].getBounds());

                try
                {
                    center[i] = findCenter(components[i].getLocationOnScreen(), components[i].getSize());
                }
                catch (IllegalComponentStateException notOnScreen)
                {
                    center[i] = null;
                    components[i] = null;
                }
            }
        }

        // Set up focus traversals foreach component
        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null) setFocusTraversal(i, components,
            // parent,
                    center);
        }
    }

    /**
     * Calculates and sets focus traversals for the given {@link HNavigable}
     * component.
     * 
     * @param current
     *            the component to set focus traversals for
     * @param components
     *            array of all of the components in the enclosing container,
     *            including <code>current</code>. Note that entries may be
     *            <code>null</code> because the given component is not
     *            <code>HNavigable</code>.
     * @param container
     *            the enclosing <code>Container</code>.
     * @param center
     *            the component center(s)
     */
    protected void setFocusTraversal(int index, Component[] components, Point[] center)
    {
        HNavigable current = (HNavigable) components[index];
        // Dimension area = container.getSize();
        Dimension area = Toolkit.getDefaultToolkit().getScreenSize();
        Point point = center[index];
        HNavigable right = null, left = null, up = null, down = null;
        int d_up, d_down, d_left, d_right;
        HNavigable wright = null, wleft = null, wup = null, wdown = null;
        int d_wup, d_wdown, d_wleft, d_wright;

        /*
         * SnapLayoutConstraints slc = getConstraints((Component)current); //
         * Don't bother if keeping all presets if (slc.up && slc.down &&
         * slc.right && slc.left) return;
         */

        // Start with maximum distances
        d_up = d_down = d_left = d_right = Integer.MAX_VALUE;
        d_wup = d_wdown = d_wleft = d_wright = Integer.MAX_VALUE;

        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null && components[i] != current)
            {
                HNavigable x = (HNavigable) components[i];
                Point xPoint = center[i];
                int dist, d, dh, dv;

                // Find actual distance between centers
                d = distCenter(point, xPoint);
                if (wrap)
                {
                    dh = distCenterWrapHorizontal(point, xPoint, area);
                    dv = distCenterWrapVertical(point, xPoint, area);
                }
                else
                {
                    dh = 0;
                    dv = 0;
                }

                /*
                 * Try and find the best for each of the for directions. The
                 * best is the shortest distance. If a shorter direct distance
                 * isn't found, and wrapping is enabled, try to find a shorter
                 * wrapped distance. A "wrapped" traversal isn't selected unless
                 * an appropriate non-wrapped traversal cannot be found.
                 */

                // Find best up
                // if (!slc.up)
                {
                    dist = distUp(point, xPoint, d); // weighted dist up
                    if (dist < d_up)
                    {
                        d_up = dist;
                        up = x;
                    }
                    else if (wrap)
                    {
                        // Weighting down works the same as up w/wrap...
                        dist = distDown(point, xPoint, dv); // weighted wrap
                        if (dist < d_wup)
                        {
                            d_wup = dist;
                            wup = x;
                        }
                    }
                }

                // Find best down
                // if (!slc.down)
                {
                    dist = distDown(point, xPoint, d); // weighted dist down
                    if (dist < d_down)
                    {
                        d_down = dist;
                        down = x;
                    }
                    else if (wrap)
                    {
                        // Weighting up works the same as down w/wrap...
                        dist = distUp(point, xPoint, dv); // weighted wrap
                        if (dist < d_wdown)
                        {
                            d_wdown = dist;
                            wdown = x;
                        }
                    }
                }

                // Find best right
                // if (!slc.right)
                {
                    dist = distRight(point, xPoint, d); // weighted dist right
                    if (dist < d_right)
                    {
                        d_right = dist;
                        right = x;
                    }
                    else if (wrap)
                    {
                        // Weighting left works the same as right w/wrap...
                        dist = distLeft(point, xPoint, dh); // weighted wrap
                        if (dist < d_wright)
                        {
                            d_wright = dist;
                            wright = x;
                        }
                    }
                }

                // Find best left
                // if (!slc.left)
                {
                    dist = distLeft(point, xPoint, d); // weighted dist left
                    if (dist < d_left)
                    {
                        d_left = dist;
                        left = x;
                    }
                    else if (wrap)
                    {
                        // Weighting right works the same as left w/wrap...
                        dist = distRight(point, xPoint, dh); // weighted wrap
                        if (dist < d_wleft)
                        {
                            d_wleft = dist;
                            wleft = x;
                        }
                    }
                }

            } // if (components[i] != null && components[i] != current)
        } // for()

        // Figure defaults if there are any
        if (wrap)
        {
            // Choose the wrap-arounds
            if (up == null) up = wup;
            if (down == null) down = wdown;
            if (left == null) left = wleft;
            if (right == null) right = wright;
        }
        /*
         * else if (container instanceof HNavigable) { // Inherit from the
         * parent if (up == null && !slc.up) up =
         * ((HNavigable)container).getMove(UP); if (down == null && !slc.down)
         * down = ((HNavigable)container).getMove(DOWN); if (left == null &&
         * !slc.left) left = ((HNavigable)container).getMove(LEFT); if (right ==
         * null && !slc.right) right = ((HNavigable)container).getMove(RIGHT); }
         */
        // Keep current if the constraints say so
        /*
         * if (slc.up) up = current.getMove(UP); if (slc.down) down =
         * current.getMove(DOWN); if (slc.left) left = current.getMove(LEFT); if
         * (slc.right) right = current.getMove(RIGHT);
         */

        if (DEBUG)
        {
            System.out.println("Setting: " + current);
            System.out.println("  UP    " + d_up + ": " + up);
            System.out.println("  DOWN  " + d_down + ": " + down);
            System.out.println("  LEFT  " + d_left + ": " + left);
            System.out.println("  RIGHT " + d_right + ": " + right);
        }

        current.setFocusTraversal(up, down, left, right);
    }

    /**
     * Returns distance up from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distUp(Point current, Point x, int d)
    {
        if (current.y <= x.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns distance down from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distDown(Point current, Point x, int d)
    {
        if (x.y <= current.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns a vertically weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than vertical.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured vertically
     */
    protected int weighVertical(Point current, Point x, int d)
    {
        // Slope is dX/dY
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dX * straight) / (dY * close);
    }

    /**
     * Returns distance right from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distRight(Point current, Point x, int d)
    {
        if (x.x <= current.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns distance left from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distLeft(Point current, Point x, int d)
    {
        if (current.x <= x.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns a horizontally weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than horizontal.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured horizontally
     */
    protected int weighHorizontal(Point current, Point x, int d)
    {
        // Slope is dY/dX
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dY * straight) / (dX * close);
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code>
     */
    protected int distCenter(Point current, Point x)
    {
        int dX = current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapHorizontal(Point current, Point x, Dimension d)
    {
        int dX = d.width - current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapVertical(Point current, Point x, Dimension d)
    {
        int dX = current.x - x.x;
        int dY = d.height - current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the center <code>Point</code> of <code>x</code>.
     * 
     * @param x
     *            the area for which the geometric center should be found
     * @return the <code>Point</code> which specifies the geometric center of
     *         the given area
     */
    protected Point findCenter(Rectangle x)
    {
        return new Point(x.x + x.width / 2, x.y + x.height / 2);
    }

    protected Point findCenter(Point loc, Dimension size)
    {
        return new Point(loc.x + size.width / 2, loc.y + size.height / 2);
    }

    /**
     * Ratio by which straight vs close should be favored. This ratio is figured
     * in with the weighing of the distance. If the slope should not be
     * considered, straight should be 0. If the slope should be highly
     * considered, straight should be > close.
     * <p>
     * Please don't set close to 0.
     * <p>
     * The default is 1:1.
     */
    protected int straight = 1, close = 1;

    /**
     * Sets the ratio that determines how heavily the slope is taken into
     * account. The slope is multiplied by this ratio. If
     * <code>straight > close</code> then straight line traversals are favored
     * over close traversals. The reverse also holds. The extremes are given
     * when one is 0 and the other non-zero. For example, if
     * <code>close=1</code> and <code>straight=0</code>, then the closest
     * component is always chosen, regardless of the angle.
     * <p>
     * The default is 1:1.
     * 
     * @param straight
     *            the straight portion of the straight:close ratio
     * @param close
     *            the close portion of the straight:close ratio
     */
    public void setSlopeRatio(int straight, int close)
    {
        if (close == straight) close = straight = 1;
        if (close == 0)
        {
            close = 1;
            straight = 1000;
        }
        this.straight = straight;
        this.close = close;
    }

    /**
     * Returns the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @return straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getStraightWeight()
    {
        return straight;
    }

    /**
     * Sets the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @param straight
     *            straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setStraightWeight(int straight)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Returns the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @return close weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getCloseWeight()
    {
        return close;
    }

    /**
     * Sets the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @param straight
     *            close weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setCloseWeight(int close)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     */
    private boolean wrap = false;

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     * <p>
     * Note that when wrapping is enabled, traversals are <b>not</b> inherited
     * from the parent container. Also, note that a wrap-around traversal is
     * only selected if no suitable <i>non</i>-wrap-around traversal can be
     * found.
     * 
     * @param wrap
     *            if <code>true</code> then wrap-around traversals are enabled;
     *            <code>false</code> disables wrap-around traversals and is the
     *            default.
     */
    public void setWrap(boolean wrap)
    {
        this.wrap = wrap;
    }

    /**
     * @see #setWrap(boolean)
     */
    public boolean getWrap()
    {
        return wrap;
    }

    /** Convenience constant for <code>KeyEvent.VK_UP</code>. */
    private static final int UP = KeyEvent.VK_UP;

    /** Convenience constant for <code>KeyEvent.VK_DOWN</code>. */
    private static final int DOWN = KeyEvent.VK_DOWN;

    /** Convenience constant for <code>KeyEvent.VK_LEFT</code>. */
    private static final int LEFT = KeyEvent.VK_LEFT;

    /** Convenience constant for <code>KeyEvent.VK_RIGHT</code>. */
    private static final int RIGHT = KeyEvent.VK_RIGHT;

    /**
     * Convenience constant constraints object with all values set to
     * <code>false</code>.
     */
    // private static final SnapLayoutConstraints emptyConstraints = new
    // SnapLayoutConstraints();

    /**
     * Mapping of {@link HNavigable}s to {@link SnapLayoutConstraint}s.
     */
    // private Hashtable constraints;

    private static final boolean DEBUG = true;
}
