/*
 *  Java HTML Tidy - JTidy
 *  HTML parser and pretty printer
 *
 *  Copyright (c) 1998-2000 World Wide Web Consortium (Massachusetts
 *  Institute of Technology, Institut National de Recherche en
 *  Informatique et en Automatique, Keio University). All Rights
 *  Reserved.
 *
 *  Contributing Author(s):
 *
 *     Dave Raggett <dsr@w3.org>
 *     Andy Quick <ac.quick@sympatico.ca> (translation to Java)
 *     Gary L Peskin <garyp@firstech.com> (Java development)
 *     Sami Lempinen <sami@lempinen.net> (release management)
 *     Fabrizio Giustina <fgiust at users.sourceforge.net>
 *
 *  The contributing author(s) would like to thank all those who
 *  helped with testing, bug fixes, and patience.  This wouldn't
 *  have been possible without all of you.
 *
 *  COPYRIGHT NOTICE:
 * 
 *  This software and documentation is provided "as is," and
 *  the copyright holders and contributing author(s) make no
 *  representations or warranties, express or implied, including
 *  but not limited to, warranties of merchantability or fitness
 *  for any particular purpose or that the use of the software or
 *  documentation will not infringe any third party patents,
 *  copyrights, trademarks or other rights. 
 *
 *  The copyright holders and contributing author(s) will not be
 *  liable for any direct, indirect, special or consequential damages
 *  arising out of any use of the software or documentation, even if
 *  advised of the possibility of such damage.
 *
 *  Permission is hereby granted to use, copy, modify, and distribute
 *  this source code, or portions hereof, documentation and executables,
 *  for any purpose, without fee, subject to the following restrictions:
 *
 *  1. The origin of this source code must not be misrepresented.
 *  2. Altered versions must be plainly marked as such and must
 *     not be misrepresented as being the original source.
 *  3. This Copyright notice may not be removed or altered from any
 *     source or altered source distribution.
 * 
 *  The copyright holders and contributing author(s) specifically
 *  permit, without fee, and encourage the use of this source code
 *  as a component for supporting the Hypertext Markup Language in
 *  commercial products. If you use this source code in a product,
 *  acknowledgment is not required but would be appreciated.
 *
 */
package org.w3c.tidy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.tidy.TidyMessage.Level;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author fgiust
 * @version $Revision$ ($Author$)
 */
public class TidyTestCase extends TestCase
{

    /**
     * Tidy executable name, if you want to produce output files for comparison.
     */
    private static final String TIDY_EXECUTABLE = "tidy.exe";

    /**
     * Logger used to enable/disable output file generation using tidy c executable. Setting this logger to
     * <code>debug</code> in your log4j configuration file will cause the TIDY_EXECUTABLE to be run against the actual
     * test file. If set to false the command line used to manually run tidy will appear in the log.
     */
    private static final Logger RUN_TIDY_EXECUTABLE = LoggerFactory.getLogger("runtidy");

    /**
     * Tidy test instance.
     */
    protected Tidy tidy;

    /**
     * message listener.
     */
    protected TestMessageListener messageListener;

    /**
     * Error out.
     */
    protected StringWriter errorLog;

    /**
     * Tidy output.
     */
    protected String tidyOut;

    /**
     * logger.
     */
    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * write directly to out. Useful for debugging (but it will make the test fail!).
     */
    private boolean writeToOut;

    /**
     * Instantiate a new Test case.
     * @param name test name
     */
    public TidyTestCase(final String name)
    {
        super(name);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // creates a new Tidy
        this.tidy = new Tidy();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        this.tidy = null;
        this.errorLog = null;
        this.tidyOut = null;

        super.tearDown();
    }

    /**
     * Executes a tidy test. This method simply requires the input file name. If a file with the same name but with a
     * ".cfg" extension is found is used as configuration file for the test, otherwise the default config will be used.
     * If a file with the same name, but with the ".out" extension is found, tidy will the result with the content of
     * such file.
     * @param fileName input file name
     * @throws Exception any exception generated during the test
     */
    protected void executeTidyTest(final String fileName) throws Exception
    {

        // set up Tidy using supplied configuration
        setUpTidy(fileName);

        // input file
        final URL inputURL = getClass().getClassLoader().getResource(fileName);
        assertNotNull("Can't find input file [" + fileName + "]", inputURL);

        OutputStream out;
        // out
        if (!writeToOut)
        {
            out = new ByteArrayOutputStream();
        }
        else
        {
            out = System.out;
        }

        // go!
        this.tidy.parse(inputURL.openStream(), out);

        if (log.isDebugEnabled())
        {
            log.debug("out:\n---- out ----\n" + out + "\n---- out ----");
            log.debug("log:\n---- log ----\n" + this.errorLog + "\n---- log ----");
        }

        // existing file for comparison
        final String outFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".out";
        final URL outFile = getClass().getClassLoader().getResource(outFileName);

        this.tidyOut = out.toString();

        if (outFile != null)
        {
            log.debug("Comparing file using [" + outFileName + "]");
            assertEquals(this.tidyOut, outFile);
        }

        // check messages
        final String messagesFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".msg";
        final URL messagesFile = getClass().getClassLoader().getResource(messagesFileName);

        // save messages
        if (messagesFile == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Messages file doesn't exists, generating [" + messagesFileName + "] for reference");
            }
            final FileWriter fw = new FileWriter(inputURL.getFile().substring(0, inputURL.getFile().lastIndexOf("."))
                + ".msg");
            fw.write(this.messageListener.messagesToXml());
            fw.close();
        }
        else
        {
            // compare result to expected messages
            if (log.isDebugEnabled())
            {
                log.debug("Comparing messages using [" + messagesFileName + "]");
            }
            compareMsgXml(messagesFile);
        }
    }

    /**
     * Parse an existing msg file and assert that content is identical to current output.
     * @param messagesFile URL to mesage file
     * @throws Exception any exception generated during the test
     */
    protected void compareMsgXml(final URL messagesFile) throws Exception
    {

        // first parse existing file
        // avoid using DOM since if will need forking junit execution in maven (too slow)
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();

        final MsgXmlHandler handler = new MsgXmlHandler();
        saxParser.parse(new InputSource(messagesFile.openStream()), handler);
        final List<TidyMessage> expectedMsgs = handler.getMessages();

        final List<TidyMessage> tidyMsgs = this.messageListener.getReceived();

        // assert size
        if (expectedMsgs.size() != tidyMsgs.size())
        {
            final StringBuffer messagesAsString = new StringBuffer();

            for (final TidyMessage message : tidyMsgs)
            {
                messagesAsString.append("\n");
                messagesAsString.append(message.getMessage());
            }

            fail("Expected "
                + expectedMsgs.size()
                + " messages but got "
                + tidyMsgs.size()
                + ". Messages:"
                + messagesAsString.toString());
        }

        // compare messages
        final Iterator<TidyMessage> expectedMsgIt = expectedMsgs.iterator();
        final Iterator<TidyMessage> tidyMsgIt = tidyMsgs.iterator();
        int count = 0;
        while (tidyMsgIt.hasNext())
        {
            final TidyMessage expectedOne = expectedMsgIt.next();
            final TidyMessage tidyOne = tidyMsgIt.next();

            assertEquals("Error code for message [" + count + "] is different from expected", expectedOne
                .getErrorCode(), tidyOne.getErrorCode());

            assertEquals(
                "Level for message [" + count + "] is different from expected",
                expectedOne.getLevel(),
                tidyOne.getLevel());

            assertEquals("Line for message ["
                + count
                + "] is different from expected. Expected position: ["
                + expectedOne.getLine()
                + ":"
                + expectedOne.getColumn()
                + "] , current ["
                + tidyOne.getLine()
                + ":"
                + tidyOne.getColumn()
                + "]", expectedOne.getLine(), tidyOne.getLine());

            assertEquals("Column for message ["
                + count
                + "] is different from expected. Expected position: ["
                + expectedOne.getLine()
                + ":"
                + expectedOne.getColumn()
                + "] , current ["
                + tidyOne.getLine()
                + ":"
                + tidyOne.getColumn()
                + "]", expectedOne.getColumn(), tidyOne.getColumn());

            // don't assert text in respect for i18n

            count++;
        }

    }

    /**
     * Basic test for DOM parser. Test is set up using [fileName.cfg] configuration if the file exists. Calls
     * tidy.parseDOM and returns the Document to the caller.
     * @param fileName input file name
     * @return parsed Document
     * @throws Exception any exception generated during the test
     */
    protected Document parseDomTest(final String fileName) throws Exception
    {
        // creates a new Tidy
        setUpTidy(fileName);

        // input file
        final URL inputURL = getClass().getClassLoader().getResource(fileName);
        assertNotNull("Can't find input file [" + fileName + "]", inputURL);

        // out
        final OutputStream out = new ByteArrayOutputStream();

        final Document doc = this.tidy.parseDOM(inputURL.openStream(), out);
        this.tidyOut = out.toString();

        return doc;
    }

    /**
     * assert generated output and test file are equals.
     * @param tidyOutput tidy output as string
     * @param correctFile URL used to load the file for comparison
     * @throws FileNotFoundException if test file is not found
     * @throws IOException in reading file
     */
    protected void assertEquals(final String tidyOutput, final URL correctFile) throws FileNotFoundException, IOException
    {
        // assume the expected output has the same encoding tidy has in its configuration
        final String encodingName = tidy.getConfiguration().getOutCharEncodingName();

        diff(
            new BufferedReader(new InputStreamReader(new ByteArrayInputStream(tidyOutput.getBytes()), encodingName)),
            new BufferedReader(new InputStreamReader(new FileInputStream(correctFile.getPath()), encodingName)));
    }

    /**
     * Utility method: assert no warnings were reported in the last tidy run.
     */
    protected void assertNoWarnings()
    {
        final int warningNum = this.tidy.getParseWarnings();
        if (warningNum != 0)
        {
            fail("Test failed, [" + warningNum + "] false warnings were reported");
        }
    }

    /**
     * Utility method: assert no errors were reported in the last tidy run.
     */
    protected void assertNoErrors()
    {
        final int errorNum = this.tidy.getParseErrors();
        if (errorNum != 0)
        {
            fail("Test failed, [" + errorNum + "] false errors were reported");
        }
    }

    /**
     * Utility method: assert no warnings were reported in the last tidy run.
     * @param expectedNumber expected number of warnings.
     */
    protected void assertWarnings(final int expectedNumber)
    {
        final int warningNum = this.tidy.getParseWarnings();
        if (warningNum != expectedNumber)
        {
            fail("Test failed, [" + expectedNumber + "] warnings expected, [" + warningNum + "] were reported");
        }
    }

    /**
     * Utility method: assert no errors were reported in the last tidy run.
     * @param expectedNumber expected number of errors.
     */
    protected void assertErrors(final int expectedNumber)
    {
        final int errorNum = this.tidy.getParseErrors();
        if (errorNum != expectedNumber)
        {
            fail("Test failed, [" + expectedNumber + "] errors expected, [" + errorNum + "] were reported");
        }
    }

    /**
     * Utility method: asserts a given String can be found in the error log.
     * @param expectedString expected String in error log.
     */
    protected void assertLogContains(final String expectedString)
    {
        final String logString = this.errorLog.toString();

        if (logString.indexOf(expectedString) == -1)
        {
            fail("Test failed, expected [" + expectedString + "] couldn't be found in error log.");
        }
    }

    /**
     * Utility method: asserts a given String can't be found in the error log.
     * @param expectedString expected String in error log.
     */
    protected void assertLogDoesntContains(final String expectedString)
    {
        final String logString = this.errorLog.toString();

        if (logString.indexOf(expectedString) != -1)
        {
            fail("Test failed, [" + expectedString + "] was found in error log.");
        }
    }

    /**
     * set up the tidy instance.
     * @param fileName input file name (needed to determine configuration file name)
     * @throws IOException in reading configuration file
     */
    private void setUpTidy(final String fileName) throws IOException
    {
        // config file names
        final String configFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".cfg";
        final String messagesFileName = fileName.substring(0, fileName.lastIndexOf("."));

        // input file
        final URL inputURL = getClass().getClassLoader().getResource(fileName);
        assertNotNull("Can't find input file [" + fileName + "]", inputURL);

        // configuration file
        URL configurationFile = getClass().getClassLoader().getResource(configFileName);

        // debug runing test info
        if (log.isDebugEnabled())
        {
            final StringBuffer message = new StringBuffer();
            message.append("Testing [" + fileName + "]");
            if (configurationFile != null)
            {
                message.append(" using configuration file [" + configFileName + "]");
            }
            log.debug(message.toString());
        }

        if (configurationFile == null)
        {
            configurationFile = getClass().getClassLoader().getResource("default.cfg");
        }

        generateOutputUsingTidyC(inputURL.getFile(), configurationFile.getFile(), RUN_TIDY_EXECUTABLE.isDebugEnabled());

        // if configuration file exists load and set it
        final Properties testProperties = new Properties();
        testProperties.load(configurationFile.openStream());
        this.tidy.setConfigurationFromProps(testProperties);

        // set up error log
        this.errorLog = new StringWriter();
        this.tidy.setErrout(new PrintWriter(this.errorLog));

        this.messageListener = new TestMessageListener(messagesFileName);
        this.tidy.setMessageListener(messageListener);
    }

    /**
     * Diff between two buffered readers. If comparison fails an AssertionFailedException is thrown with the line
     * number, actual and expected output. Content is tested to be identical (same wrapping).
     * @param tidyOutput reader for tidy generated output
     * @param correctFile reader for test file
     * @throws IOException in reading from readers
     */
    private static void diff(final BufferedReader tidyOutput, final BufferedReader correctFile) throws IOException
    {
        String tidyLine, testLine;
        int i = 1;
        do
        {
            tidyLine = tidyOutput.readLine();
            testLine = correctFile.readLine();
            i++;
        }
        while (tidyLine != null && testLine != null && tidyLine.equals(testLine));
        tidyOutput.close();
        correctFile.close();

        if (tidyLine != null || testLine != null)
        {
            assertEquals("Wrong output, file comparison failed at line [" + (i - 1) + "]", testLine, tidyLine);
        }
        return;
    }

    /**
     * Run TIDY_EXECUTABLE to produce an output file. Used to generates output files using tidy c for comparison with
     * jtidy. A file ".out" will be written in the same folder of the input file.
     * @param inputFileName input file for tidy.
     * @param configurationFileName configuration file name (default if there is no not test-specific file).
     * @param runIt if true the output is generated using tidy, if false simply output the command line.
     */
    private void generateOutputUsingTidyC(final String inputFileName, final String configurationFileName, final boolean runIt)
    {

        final String outputFileName = inputFileName.substring(0, inputFileName.lastIndexOf(".")) + ".out";

        final String strCmd = TIDY_EXECUTABLE
            + " -config \""
            + cleanUpFilePath(configurationFileName)
            + "\" -o \""
            + cleanUpFilePath(outputFileName)
            + "\" \""
            + cleanUpFilePath(inputFileName)
            + "\"";

        log.debug("cmd line:\n***\n"
            + strCmd
            + "\nw/o output:\n"
            + TIDY_EXECUTABLE
            + " -config \""
            + cleanUpFilePath(configurationFileName)
            + "\" \""
            + cleanUpFilePath(inputFileName)
            + "\""
            + "\n***");

        if (runIt)
        {
            log.debug("running " + TIDY_EXECUTABLE);
            try
            {
                Runtime.getRuntime().exec(strCmd);
            }
            catch (final IOException e)
            {
                log.warn("Error running [" + strCmd + "] cmd: " + e.getMessage());
            }
        }

    }

    /**
     * Utility method to clean up file path returned by URLs.
     * @param fileName file name as given by URL.getFile()
     * @return String fileName
     */
    protected String cleanUpFilePath(final String fileName)
    {
        if (fileName.length() > 3 && fileName.charAt(2) == ':')
        {
            // assuming something like ""/C:/program files/..."
            return fileName.substring(1);
        }
        else if (fileName.startsWith("file://"))
        {
            return fileName.substring(7);
        }

        return fileName;

    }

    /**
     * A simple SAX Content Handler used to parse .msg files.
     */
    static class MsgXmlHandler extends DefaultHandler
    {

        /**
         * Parsed messages.
         */
        private final List<TidyMessage> messages = new ArrayList<TidyMessage>();

        /**
         * Error code for the current message.
         */
        private int code;

        /**
         * Level for the current message.
         */
        private Level level;

        /**
         * Column for the current message.
         */
        private int column;

        /**
         * Line for the current message.
         */
        private int line;

        /**
         * Message the current message.
         */
        private StringBuffer textbuffer;

        /**
         * Actual parsing position.
         */
        private int parsePosition = -100;

        /**
         * actually parsing a detail tag.
         */
        private boolean intag;

        /**
         * @see org.xml.sax.ContentHandler#startElement(String, String, String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
        {
            if ("message".equals(qName))
            {
                parsePosition = 0;
                textbuffer = new StringBuffer();
            }
            else
            {
                parsePosition++;
                intag = true;
            }
        }

        /**
         * @see org.xml.sax.ContentHandler#endElement(String, String, String)
         */
        @Override
        public void endElement(final String uri, final String localName, final String qName)
        {
            if ("message".equals(qName))
            {
                final TidyMessage message = new TidyMessage(code, line, column, level, textbuffer
                    .toString());
                messages.add(message);
            }
            intag = false;
        }

        /**
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters(final char[] ch, final int start, final int length)
        {
            if (!intag)
            {
                return;
            }

            switch (parsePosition)
            {
                case 1 :
                    this.code = Integer.parseInt(new String(ch, start, length));
                    break;
                case 2 :
                    this.level =  Level.values()[Integer.parseInt(new String(ch, start, length))];
                    break;
                case 3 :
                    this.line = Integer.parseInt(new String(ch, start, length));
                    break;
                case 4 :
                    this.column = Integer.parseInt(new String(ch, start, length));
                    break;
                case 5 :
                    textbuffer.append(new String(ch, start, length));
                    break;
                default :
                    break;
            }
        }

        /**
         * Returns the list of parsed messages.
         * @return List containing TidyMessage elements
         */
        public List<TidyMessage> getMessages()
        {
            return messages;
        }
    }
}
