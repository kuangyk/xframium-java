/*******************************************************************************
 * xFramium
 *
 * Copyright 2016 by Moreland Labs, Ltd. (http://www.morelandlabs.com)
 *
 * Some open source application is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *  
 * Some open source application is distributed in the hope that it will 
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with xFramium.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @license GPL-3.0+ <http://spdx.org/licenses/GPL-3.0+>
 *******************************************************************************/
/*
 * 
 */
package org.xframium.device.ng;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.xframium.artifact.ArtifactTime;
import org.xframium.artifact.ArtifactType;
import org.xframium.content.ContentManager;
import org.xframium.device.ConnectedDevice;
import org.xframium.device.DeviceManager;
import org.xframium.device.artifact.Artifact;
import org.xframium.device.artifact.ArtifactProducer;
import org.xframium.device.cloud.CloudDescriptor;
import org.xframium.device.cloud.CloudRegistry;
import org.xframium.device.data.DataManager;
import org.xframium.device.factory.DeviceWebDriver;
import org.xframium.page.data.PageData;
import org.xframium.page.data.PageDataManager;
import org.xframium.page.keyWord.KeyWordDriver;
import org.xframium.page.keyWord.KeyWordTest;
import org.xframium.reporting.ExecutionContext;
import org.xframium.reporting.ExecutionContextTest;
import org.xframium.spi.Device;
import org.xframium.spi.driver.ReportiumProvider;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractSeleniumTest.
 */
public abstract class AbstractSeleniumTest
{

    /** The log. */
    protected static Log log = LogFactory.getLog( AbstractSeleniumTest.class );

    /** The set of devices in use on this thread. */
    private static ThreadLocal<HashMap<String, ConnectedDevice>> threadDevices = new ThreadLocal<HashMap<String, ConnectedDevice>>();

    /** The test context running on this thread. */
    private static ThreadLocal<TestContext> threadContext = new ThreadLocal<TestContext>();

    /** The name of the default device **/

    /**
     * Gets the device data.
     *
     * @return the device data
     */
    @DataProvider ( name = "deviceManager", parallel = true)
    public Object[][] getDeviceData( ITestContext testContext )
    {
        List<Device> deviceList = DeviceManager.instance().getDevices();

        return getDeviceData( deviceList, testContext );
    }

    private enum KeyType
    {
        PERSONA, CONTENT, ITERATION, TEST
    }

    private class TestKey
    {
        private String key;
        private KeyType type;

        public TestKey( String key, KeyType type )
        {
            super();
            this.key = key;
            this.type = type;
        }

        public String getKey()
        {
            return key;
        }

        public void setKey( String key )
        {
            this.key = key;
        }

        public KeyType getType()
        {
            return type;
        }

        public void setType( KeyType type )
        {
            this.type = type;
        }

    }

    public Object[][] getDeviceData( List<Device> deviceList, ITestContext testContext )
    {
        List<TestName> rawList = new ArrayList<TestName>( 10 );
        List<TestName> finalList = new ArrayList<TestName>( 10 );
        List<TestKey> personaList = new ArrayList<TestKey>( 10 );
        List<TestKey> testList = new ArrayList<TestKey>( 10 );

        if ( DataManager.instance().getPersonas() != null && DataManager.instance().getPersonas().length > 0 )
        {
            for ( String pN : DataManager.instance().getPersonas() )
                personaList.add( new TestKey( pN, KeyType.PERSONA ) );
        }

        if ( DataManager.instance().getTests() != null && DataManager.instance().getTests().length > 0 )
        {
            for ( String pN : DataManager.instance().getTests() )
            {
                testList.add( new TestKey( pN, KeyType.TEST ) );
            }
        }
        else
        {
            for ( String pN : KeyWordDriver.instance().getTestNames() )
            {
                testList.add( new TestKey( pN, KeyType.TEST ) );
            }
        }

        for ( TestKey tK : testList )
        {
            KeyWordTest kT = KeyWordDriver.instance().getTest( tK.getKey() );
            if ( kT.getContentKeys() != null && kT.getContentKeys().length > 0 )
            {
                for ( String contentKey : kT.getContentKeys() )
                {
                    if ( kT.getCount() > 1 )
                    {
                        for ( int i = 0; i < kT.getCount(); i++ )
                        {
                            if ( kT.getDataDriver() != null && kT.getDataDriver().trim().length() > 0 )
                            {
                                PageData[] pageData = PageDataManager.instance().getRecords( kT.getDataDriver() );
                                for ( PageData pD : pageData )
                                {
                                    TestName testName = new TestName( tK.getKey() );
                                    testName.setIteration( i + 1 );
                                    testName.setTestContext( contentKey );
                                    testName.setDataDriven( pD );
                                    rawList.add( testName );
                                }
                            }
                            else
                            {
                                TestName testName = new TestName( tK.getKey() );
                                testName.setIteration( i + 1 );
                                testName.setTestContext( contentKey );
                                rawList.add( testName );
                            }
                        }
                    }
                    else
                    {
                        if ( kT.getDataDriver() != null && kT.getDataDriver().trim().length() > 0 )
                        {
                            PageData[] pageData = PageDataManager.instance().getRecords( kT.getDataDriver() );
                            for ( PageData pD : pageData )
                            {
                                TestName testName = new TestName( tK.getKey() );
                                testName.setTestContext( contentKey );
                                testName.setDataDriven( pD );
                                rawList.add( testName );
                            }
                        }
                        else
                        {
                            TestName testName = new TestName( tK.getKey() );
                            testName.setTestContext( contentKey );
                            rawList.add( testName );
                        }
                    }
                }
            }
            else
            {
                if ( kT.getCount() > 1 )
                {
                    for ( int i = 0; i < kT.getCount(); i++ )
                    {
                        if ( kT.getDataDriver() != null && kT.getDataDriver().trim().length() > 0 )
                        {
                            PageData[] pageData = PageDataManager.instance().getRecords( kT.getDataDriver() );
                            for ( PageData pD : pageData )
                            {
                                TestName testName = new TestName( tK.getKey() );
                                testName.setIteration( i );
                                testName.setDataDriven( pD );
                                rawList.add( testName );
                            }
                        }
                        else
                        {
                            TestName testName = new TestName( tK.getKey() );
                            testName.setIteration( i );
                            rawList.add( testName );
                        }
                    }
                }
                else
                {
                    if ( kT.getDataDriver() != null && kT.getDataDriver().trim().length() > 0 )
                    {
                        PageData[] pageData = PageDataManager.instance().getRecords( kT.getDataDriver() );
                        for ( PageData pD : pageData )
                        {
                            TestName testName = new TestName( tK.getKey() );
                            testName.setDataDriven( pD );
                            rawList.add( testName );
                        }
                    }
                    else
                    {
                        TestName testName = new TestName( tK.getKey() );
                        rawList.add( testName );
                    }
                }
            }
        }

        if ( personaList.size() > 0 )
        {
            for ( TestKey tK : personaList )
            {
                for ( TestName tN : rawList )
                {
                    TestName testName = new TestName( tN.getRawName() );
                    testName.setIteration( tN.getIteration() );
                    testName.setContentKey( tN.getContentKey() );
                    testName.setPersonaName( tK.getKey() );
                    testName.setDataDriven( tN.getDataDriven() );
                    finalList.add( testName );
                }
            }
        }
        else
            finalList.addAll( rawList );

        TestName[] newArray = null;

        newArray = new TestName[(finalList.size() == 0 ? 1 : finalList.size()) * deviceList.size()];

        if ( finalList.size() > 0 )
        {
            for ( int i = 0; i < finalList.size(); ++i )
            {
                for ( int j = 0; j < deviceList.size(); j++ )

                    newArray[i * deviceList.size() + j] = finalList.get( i );
            }
        }
        else
        {
            for ( int i = 0; i < deviceList.size(); i++ )

                newArray[i] = new TestName();
        }

        List<Device> fullDeviceList = new ArrayList<Device>( 10 );
        for ( Device d : deviceList )
        {
            for ( int i = 0; i < d.getAvailableDevices(); i++ )
                fullDeviceList.add( d.cloneDevice() );
        }

        log.warn( "Thread count configuration as " + fullDeviceList.size() );
        testContext.getSuite().getXmlSuite().setDataProviderThreadCount( fullDeviceList.size() );
        testContext.getSuite().getXmlSuite().setThreadCount( fullDeviceList.size() );

        TestContainer testContainer = new TestContainer( newArray, fullDeviceList.toArray( new Device[0] ) );

        Object[][] returnArray = new Object[newArray.length][1];
        for ( int i = 0; i < returnArray.length; i++ )
            returnArray[i][0] = testContainer;

        return returnArray;
    }

    /**
     * Adds the capabilities.
     *
     * @param dc
     *            the dc
     */
    public void addCapabilities( DesiredCapabilities dc )
    {

    }

    /**
     * Gets the web driver.
     *
     * @return the web driver
     */
    protected WebDriver getWebDriver()
    {
        if ( getConnectedDevice( TestName.DEFAULT ) != null )
        {
            return getConnectedDevice( TestName.DEFAULT ).getWebDriver();
        }
        else
            return null;
    }

    /**
     * Gets the device.
     *
     * @return the device
     */
    protected Device getDevice()
    {
        if ( getConnectedDevice( TestName.DEFAULT ) != null )
            return getConnectedDevice( TestName.DEFAULT ).getDevice();
        else
            return null;
    }

    /**
     * Before method.
     *
     * @param currentMethod
     *            the current method
     * @param testArgs
     *            the test args
     */
    @BeforeMethod ( alwaysRun = true)
    public void beforeMethod( Method currentMethod, Object[] testArgs, ITestContext testContext )
    {
        try
        {
            TestContainer tC = ((TestContainer) testArgs[0]);

            TestPackage testPackage = tC.getTestPackage( currentMethod, true );

            testContext.setAttribute( "testPackage", testPackage );

            TestName testName = testPackage.getTestName();
            ConnectedDevice connectedDevice = testPackage.getConnectedDevice();

            String contentKey = testName.getContentKey();

            if ( (contentKey != null) && (contentKey.length() > 0) )
            {
                ContentManager.instance().setCurrentContentKey( contentKey );
            }
            else
            {
                ContentManager.instance().setCurrentContentKey( null );
            }

            if ( connectedDevice != null )
            {
                putConnectedDevice( TestName.DEFAULT, connectedDevice );

                if ( testName.getTestName() == null || testName.getTestName().isEmpty() )
                    testName.setTestName( currentMethod.getDeclaringClass().getSimpleName() + "." + currentMethod.getName() );

                testName.setFullName( testArgs[0].toString() );

                if ( log.isInfoEnabled() )
                    log.info( Thread.currentThread().getName() + ": acquired for " + currentMethod.getName() );
            }

            if ( connectedDevice != null && connectedDevice.getWebDriver() != null && connectedDevice.getWebDriver().isConnected() )
            {
                try
                {
                    if ( DataManager.instance().isArtifactEnabled( ArtifactType.DEVICE_LOG ) )
                        connectedDevice.getWebDriver().getCloud().getCloudActionProvider().enabledLogging( connectedDevice.getWebDriver() );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }

            TestContext ctx = new TestContext();
            ctx.currentMethod = currentMethod;
            ctx.testArgs = testArgs;

            threadContext.set( ctx );
        }
        catch ( Exception e )
        {
            log.fatal( Thread.currentThread().getName() + ": Fatal error configuring test", e );
        }
    }

    public static ConnectedDevice getConnectedDevice( String name )
    {
        HashMap<String, ConnectedDevice> map = getDeviceMap();

        return map.get( name );
    }

    public static void registerSecondaryDeviceOnName( String name, String deviceId )
    {
        TestContext ctx = threadContext.get();

        if ( ctx != null )
        {
            if ( log.isInfoEnabled() )
                log.info( "Attempting to acquire device for " + ctx.currentMethod.getName() );

            ConnectedDevice connectedDevice = DeviceManager.instance().getUnconfiguredDevice( ctx.currentMethod, ((TestName) ctx.testArgs[0]).getTestName(), ((TestName) ctx.testArgs[0]).getPersonaName(), deviceId );

            if ( connectedDevice != null )
            {
                putConnectedDevice( name, connectedDevice );
            }
        }
    }

    public static void registerInactiveDeviceOnName( String name )
    {
        TestContext ctx = threadContext.get();

        if ( ctx != null )
        {
            if ( log.isInfoEnabled() )
                log.info( "Attempting to acquire Inactive device for " + ctx.currentMethod.getName() );

            ConnectedDevice connectedDevice = DeviceManager.instance().getInactiveDevice( name );

            if ( connectedDevice != null )
            {
                putConnectedDevice( name, connectedDevice );
            }
        }
    }

    /**
     * After method.
     *
     * @param currentMethod
     *            the current method
     * @param testArgs
     *            the test args
     * @param testResult
     *            the test result
     */
    @AfterMethod ( alwaysRun = true)
    public void afterMethod( Method currentMethod, Object[] testArgs, ITestResult testResult, ITestContext testContext )
    {
        try
        {
            HashMap<String, ConnectedDevice> map = getDevicesToCleanUp();
            threadContext.set( null );
            Iterator<String> keys = ((map != null) ? map.keySet().iterator() : null);

            if ( map.get( TestName.DEFAULT ) == null )
                return;

            if ( map.get( TestName.DEFAULT ).getWebDriver() != null && map.get( TestName.DEFAULT ).getWebDriver().isConnected() )
            {
                try
                {
                    if ( DataManager.instance().isArtifactEnabled( ArtifactType.DEVICE_LOG ) )
                        map.get( TestName.DEFAULT ).getWebDriver().getCloud().getCloudActionProvider().disableLogging( map.get( TestName.DEFAULT ).getWebDriver() );

                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }

            while ( (keys != null) && (keys.hasNext()) )
            {
                String name = keys.next();
                ConnectedDevice device = map.get( name );

                cleanUpConnectedDevice( name, device, currentMethod, testArgs, testResult, testContext, (TestContainer) testArgs[0] );
            }

            try
            {
                DeviceManager.instance().clearAllArtifacts();
                Thread.currentThread().setName( "xF-Idle Thread" );
            }
            catch ( Exception e )
            {
                log.error( Thread.currentThread() + ": Error flushing artifacts", e );
            }
        }
        catch ( Exception e )
        {
            log.fatal( Thread.currentThread().getName() + ": Fatal error completing test", e );
        }
    }

    private void cleanUpConnectedDevice( String name, ConnectedDevice device, Method currentMethod, Object[] testArgs, ITestResult testResult, ITestContext testContext, TestContainer testContainer )
    {
        WebDriver webDriver = device.getWebDriver();
        Device currentDevice = device.getDevice();
        ExecutionContextTest test = null;
        TestPackage testPackage = (TestPackage) testContext.getAttribute( "testPackage" );
        TestName testName = testPackage.getTestName();

        try
        {
            if ( webDriver != null )
            {

                String runKey = ((TestName.DEFAULT.equals( name )) ? testName.getTestName() : testName.getTestName() + "-" + name);

                if ( TestName.DEFAULT.equals( name ) )
                {
                    test = testName.getTest();
                }

                File rootFolder = ExecutionContext.instance().getReportFolder();
                rootFolder.mkdirs();

                try
                {
                    if ( !testResult.isSuccess() )
                    {

                        if ( DataManager.instance().getAutomaticDownloads() != null )
                        {
                            if ( webDriver instanceof ArtifactProducer )
                            {
                                if ( DataManager.instance().getReportFolder() == null )
                                    DataManager.instance().setReportFolder( new File( "." ) );

                                for ( ArtifactType aType : DataManager.instance().getAutomaticDownloads() )
                                {
                                    if ( aType.getTime() == ArtifactTime.ON_FAILURE )
                                    {
                                        try
                                        {
                                            Artifact currentArtifact = ((ArtifactProducer) webDriver).getArtifact( webDriver, aType, device, runKey, testResult.getStatus() == ITestResult.SUCCESS, test );
                                            if ( currentArtifact != null )
                                            {
                                                if ( test != null )
                                                {
                                                    test.addExecutionParameter( aType.name(), currentArtifact.getArtifactName() );
                                                    test.addExecutionParameter( aType.name() + "_FILE", new File( currentArtifact.getArtifactName() ).getName() );
                                                }
                                                currentArtifact.writeToDisk( rootFolder );
                                            }
                                        }
                                        catch ( Exception e )
                                        {
                                            log.error( "Error acquiring Artifacts ", e );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                finally
                {
                    try
                    {
                        webDriver.close();
                    }
                    catch ( Exception e )
                    {
                    }
                }

                if ( DataManager.instance().getAutomaticDownloads() != null )
                {
                    if ( webDriver instanceof ArtifactProducer )
                    {
                        if ( DataManager.instance().getReportFolder() == null )
                            DataManager.instance().setReportFolder( new File( "." ) );

                        for ( ArtifactType aType : DataManager.instance().getAutomaticDownloads() )
                        {
                            if ( aType.getTime() == ArtifactTime.AFTER_TEST )
                            {
                                try
                                {
                                    Artifact currentArtifact = ((ArtifactProducer) webDriver).getArtifact( webDriver, aType, device, runKey, testResult.getStatus() == ITestResult.SUCCESS, test );
                                    if ( currentArtifact != null )
                                    {
                                        if ( test != null )
                                        {
                                            test.addExecutionParameter( aType.name(), currentArtifact.getArtifactName() );
                                            test.addExecutionParameter( aType.name() + "_FILE", new File( currentArtifact.getArtifactName() ).getName() );
                                        }
                                        currentArtifact.writeToDisk( rootFolder );
                                    }
                                }
                                catch ( Exception e )
                                {
                                    log.error( "Error acquiring Artifacts ", e );
                                }
                            }
                        }
                    }

                    //
                    // Cloud specific artifacts
                    //
                    CloudDescriptor currentCloud = CloudRegistry.instance().getCloud();
                    if ( device.getDevice().getCloud() != null && !device.getDevice().getCloud().isEmpty() )
                        currentCloud = CloudRegistry.instance().getCloud( device.getDevice().getCloud() );
                    String cloudProvider = currentCloud.getProvider();

                    //
                    // Perfecto Wind Tunnel
                    //
                    String wtUrl = ((DeviceWebDriver) webDriver).getWindTunnelReport();
                    if ( test != null && cloudProvider.equals( "PERFECTO" ) && wtUrl != null && !wtUrl.isEmpty() )
                        test.addExecutionParameter( "PERFECTO_WT", wtUrl );

                    //
                    // Saucelabs integration
                    //
                    if ( test != null )
                    {
                        if ( DataManager.instance().isArtifactEnabled( ArtifactType.SAUCE_LABS ) && cloudProvider.equals( "SAUCELABS" ) )
                            test.addExecutionParameter( "SAUCELABS", "https://saucelabs.com/beta/tests/" + test.getSessionId() + "/commands#0" );

                        if ( DataManager.instance().isArtifactEnabled( ArtifactType.REPORTIUM ) && ((DeviceWebDriver) webDriver).isConnected() && cloudProvider.equals( "PERFECTO" ) )
                        {
                            if ( ((ReportiumProvider) webDriver).getReportiumClient() != null )
                            {
                                test.addExecutionParameter( "SAUCELABS", ((ReportiumProvider) webDriver).getReportiumClient().getReportUrl() );
                            }
                        }
                    }

                    if ( test != null )
                    {
                        try
                        {
                            test.popupateSystemProperties();
                            Artifact currentArtifact = ((ArtifactProducer) webDriver).getArtifact( webDriver, ArtifactType.EXECUTION_RECORD_JSON, device, runKey, testResult.getStatus() == ITestResult.SUCCESS, test );
                            if ( currentArtifact != null )
                                currentArtifact.writeToDisk( rootFolder );

                            currentArtifact = ((ArtifactProducer) webDriver).getArtifact( webDriver, ArtifactType.EXECUTION_RECORD_HTML, device, runKey, testResult.getStatus() == ITestResult.SUCCESS, test );
                            if ( currentArtifact != null )
                                currentArtifact.writeToDisk( rootFolder );

                        }
                        catch ( Exception e )
                        {
                            log.error( "Error acquiring Artifacts - " + e );
                        }
                    }
                }
            }

            if ( currentDevice != null )
            {
                if ( webDriver instanceof DeviceWebDriver )
                    DeviceManager.instance().addRun( ((DeviceWebDriver) webDriver).getPopulatedDevice(), testPackage, testContainer, testResult.isSuccess() );
                else
                    DeviceManager.instance().addRun( currentDevice, testPackage, testContainer, testResult.isSuccess() );
            }
        }
        finally
        {
            try
            {
                webDriver.quit();
            }
            catch ( Exception e )
            {
            }

            if ( currentDevice != null && TestName.DEFAULT.equals( name ) )
                testContainer.returnDevice( currentDevice );

            if ( test != null && TestName.DEFAULT.equals( name ) )
            {
                ExecutionContext.instance().addExecution( test );
            }
        }

    }

    //
    // Helpers
    //

    private static class TestContext
    {
        public Method currentMethod = null;
        public Object[] testArgs = null;
    }

    private static void putConnectedDevice( String name, ConnectedDevice device )
    {
        HashMap<String, ConnectedDevice> map = getDeviceMap();

        map.put( name, device );
    }

    private static HashMap<String, ConnectedDevice> getDeviceMap()
    {
        HashMap<String, ConnectedDevice> map = threadDevices.get();

        if ( map == null )
        {
            map = new HashMap<String, ConnectedDevice>();

            threadDevices.set( map );
        }

        return map;
    }

    private HashMap<String, ConnectedDevice> getDevicesToCleanUp()
    {
        HashMap<String, ConnectedDevice> map = threadDevices.get();

        threadDevices.set( null );

        return map;
    }
}
