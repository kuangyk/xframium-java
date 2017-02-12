/*******************************************************************************
v * xFramium
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
package org.xframium.device;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.xframium.artifact.ArtifactListener;
import org.xframium.artifact.ArtifactManager;
import org.xframium.artifact.ArtifactType;
import org.xframium.device.cloud.CloudDescriptor;
import org.xframium.device.data.DataProvider.DriverType;
import org.xframium.device.data.NamedDataProvider;
import org.xframium.device.factory.DeviceWebDriver;
import org.xframium.device.factory.DriverManager;
import org.xframium.device.ng.RunContainer;
import org.xframium.device.ng.RunContainer.RunStatus;
import org.xframium.device.ng.TestContainer;
import org.xframium.device.ng.TestPackage;
import org.xframium.device.property.PropertyAdapter;
import org.xframium.exception.XFramiumException;
import org.xframium.page.ExecutionRecord;
import org.xframium.page.Page;
import org.xframium.page.data.PageData;
import org.xframium.page.keyWord.KeyWordParameter;
import org.xframium.page.keyWord.KeyWordStep;
import org.xframium.page.keyWord.KeyWordTest;
import org.xframium.page.keyWord.KeyWordToken;
import org.xframium.reporting.ExecutionContext;
import org.xframium.reporting.ExecutionContextStep;
import org.xframium.reporting.ExecutionContextTest;
import org.xframium.spi.Device;
import org.xframium.spi.RunListener;
import com.xframium.serialization.SerializationManager;
import com.xframium.serialization.json.ReflectionSerializer;

// TODO: Auto-generated Javadoc
/**
 * The Class DeviceManager.
 */
public class DeviceManager implements ArtifactListener
{
    /** The singleton. */
    private static DeviceManager singleton = new DeviceManager();

    /** The execution id. */
    private ThreadLocal<String> executionId = new ThreadLocal<String>();

    private ThreadLocal<CloudDescriptor> currentCloud = new ThreadLocal<CloudDescriptor>();

    /** Stores an instance to a log handler */
    private ThreadLocal<StringBuffer> logHandler = new ThreadLocal<StringBuffer>();

    private ThreadLocal<Map<ArtifactType, List<Object>>> artifactMap = new ThreadLocal<Map<ArtifactType, List<Object>>>();

    private String deviceInterrupts;

    private List<PropertyAdapter> propertyAdapterList = new ArrayList<PropertyAdapter>( 10 );

    private Map<String, String> configurationProperties;

    private Device selectedDevice;

    private List<WebDriver> holdList = new ArrayList<WebDriver>( 10 );

    private String[] tagNames;

    private Map<Device, Integer> failureMap = new HashMap<Device, Integer>( 10 );

    private Map<String, Boolean> initializationMap = new HashMap<String, Boolean>( 10 );

    private String initializationName;

    public String getInitializationName()
    {
        return initializationName;
    }

    public void setInitializationName( String initializationName )
    {
        this.initializationName = initializationName;
    }

    public boolean isDeviceInitialized( Device currentDevice )
    {
        if ( !initializationMap.containsKey( currentDevice.getDeviceName() ) )
            return false;

        return initializationMap.get( currentDevice.getDeviceName() );
    }

    public void setDeviceInitialized( Device currentDevice )
    {
        initializationMap.put( currentDevice.getDeviceName(), true );
    }

    public String[] getTagNames()
    {
        return tagNames;
    }

    public void setTagNames( String[] tagNames )
    {
        this.tagNames = tagNames;
    }

    public List<WebDriver> getHoldList()
    {
        return holdList;
    }

    public void setHoldList( List<WebDriver> holdList )
    {
        this.holdList = holdList;
    }

    private DriverType driverType;

    public void setDriverType( DriverType val )
    {
        this.driverType = val;
    }

    public DriverType getDriverType()
    {
        return driverType;
    }

    public CloudDescriptor getCurrentCloud()
    {
        return currentCloud.get();
    }

    public void setCurrentCloud( CloudDescriptor currentCloud )
    {
        this.currentCloud.set( currentCloud );
    }

    public void setSelectedDevice( Device selectedDevice )
    {
        this.selectedDevice = selectedDevice;
    }

    public Device getSelectedDevice()
    {
        return selectedDevice;
    }

    public Map<String, String> getConfigurationProperties()
    {
        return configurationProperties;
    }

    public void setConfigurationProperties( Map<String, String> configurationProperties )
    {
        this.configurationProperties = configurationProperties;
    }

    public String getDeviceInterrupts()
    {
        return deviceInterrupts;
    }

    public void setDeviceInterrupts( String deviceInterrupts )
    {
        this.deviceInterrupts = deviceInterrupts;
    }

    public void registerPropertyAdapter( PropertyAdapter propertyAdapter )
    {
        propertyAdapterList.add( propertyAdapter );
    }

    public void notifyPropertyAdapter( Map<String, String> configurationProperties )
    {
        if ( configurationProperties != null )
        {
            for ( PropertyAdapter p : propertyAdapterList )
                p.applyProperties( configurationProperties );
        }
    }

    public void notifyPropertyAdapter( Map<String, String> configurationProperties, Object webDriver )
    {
        if ( configurationProperties != null )
        {
            for ( PropertyAdapter p : propertyAdapterList )
                p.applyInstanceProperties( configurationProperties, webDriver );
        }
    }

    public void addArtifactRecord( ArtifactType aType, Object value )
    {
        if ( artifactMap.get() == null )
            artifactMap.set( new HashMap<ArtifactType, List<Object>>( 10 ) );

        List<Object> artifactList = artifactMap.get().get( aType );
        if ( artifactList == null )
        {
            artifactList = new ArrayList<Object>( 10 );
            artifactMap.get().put( aType, artifactList );
        }

        artifactList.add( value );
    }

    public List<Object> getArtifacts( ArtifactType aType )
    {
        if ( artifactMap.get() == null )
            return null;

        return artifactMap.get().get( aType );
    }

    public void clearArtifacts( ArtifactType aType )
    {
        if ( artifactMap.get() == null )
            return;

        if ( artifactMap.get().get( aType ) != null )
            artifactMap.get().get( aType ).clear();
    }

    public void clearAllArtifacts()
    {
        artifactMap.set( new HashMap<ArtifactType, List<Object>>( 10 ) );
    }

    public void addLog( String logMessage )
    {
        if ( logHandler.get() == null )
            logHandler.set( new StringBuffer() );

        logHandler.get().append( logMessage );
    }

    public void writeLog( File fileName )
    {
        try
        {
            fileName.getParentFile().mkdirs();
            FileWriter outputStream = new FileWriter( fileName );
            outputStream.write( logHandler.get().toString() );
            logHandler.set( new StringBuffer() );
            outputStream.close();
        }
        catch ( Exception e )
        {
            log.error( "Error writing console log", e );
        }
    }

    public String getLog()
    {
        if ( logHandler.get() != null )
            return logHandler.get().toString();
        else
            return "";
    }

    public void clearLog()
    {
        logHandler.set( new StringBuffer() );
    }

    /**
     * Instance.
     *
     * @return the device manager
     */
    public static DeviceManager instance()
    {
        return singleton;
    }

    /**
     * Instantiates a new device manager.
     */
    private DeviceManager()
    {
        ArtifactManager.instance().addArtifactListener( this );

        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( ExecutionContext.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( ExecutionContextTest.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( ExecutionContextStep.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( Device.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( KeyWordTest.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( KeyWordStep.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( KeyWordParameter.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( KeyWordToken.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( CloudDescriptor.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( Page.class, new ReflectionSerializer() );
        SerializationManager.instance().getAdapter( SerializationManager.JSON_SERIALIZATION ).addCustomMapping( PageData.class, new ReflectionSerializer() );

    }

    /** The log. */
    private Log log = LogFactory.getLog( DeviceManager.class );

    /** The manager lock. */
    //private Lock managerLock = new ReentrantLock();

    /** The device map. */
    private Map<String, Device> deviceMap = new HashMap<String, Device>( 20 );

    /** The device list. */
    private List<Device> deviceList = new LinkedList<Device>();


    /** The retry count. */
    private int retryCount = 3;

    

    /** The run listeners. */
    private List<RunListener> runListeners = new ArrayList<RunListener>( 20 );

    /** The caching enabled. */
    private boolean cachingEnabled = false;

    /** The dry run. */
    private boolean dryRun = false;

    public void clear()
    {
        propertyAdapterList.clear();
        if ( configurationProperties != null )
            configurationProperties.clear();
        selectedDevice = null;
        holdList.clear();
        tagNames = null;
        failureMap.clear();
        initializationMap.clear();
        deviceMap.clear();
        deviceList.clear();
        failureMap.clear();
        initializationMap.clear();
    }

    /**
     * Checks if is dry run.
     *
     * @return true, if is dry run
     */
    public boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * Sets the dry run.
     *
     * @param dryRun
     *            the new dry run
     */
    public void setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
    }

    /**
     * Sets the execution id.
     *
     * @param executionId
     *            the new execution id
     */
    public void setExecutionId( String executionId )
    {
        this.executionId.set( executionId );
    }

    /**
     * Gets the execution id.
     *
     * @return the execution id
     */
    public String getExecutionId()
    {
        return executionId.get();
    }

    /**
     * Adds the run listener.
     *
     * @param runListener
     *            the run listener
     */
    public void addRunListener( RunListener runListener )
    {
        runListeners.add( runListener );
    }

    /**
     * Removes the run listener.
     *
     * @param runListener
     *            the run listener
     */
    public void removeRunListener( RunListener runListener )
    {
        runListeners.remove( runListener );
    }

    /**
     * Notify before run.
     *
     * @param currentDevice
     *            the current device
     * @param runKey
     *            the run key
     * @return true, if successful
     */
    private boolean notifyBeforeRun( Device currentDevice, String runKey )
    {
        for ( RunListener runListener : runListeners )
        {
            try
            {
                if ( !runListener.beforeRun( currentDevice, runKey ) )
                    return false;
            }
            catch ( Exception e )
            {
                log.error( "Error executing run listener", e );
            }
        }

        return true;
    }

    private boolean notifyValidateDevice( Device currentDevice, String runKey )
    {
        for ( RunListener runListener : runListeners )
        {
            try
            {
                if ( !runListener.validateDevice( currentDevice, runKey ) )
                    return false;
            }
            catch ( Exception e )
            {
                log.error( "Error executing run listener", e );
            }
        }

        return true;
    }

    private void notifySkipRun( Device currentDevice, String runKey )
    {
        for ( RunListener runListener : runListeners )
        {
            try
            {
                runListener.skipRun( currentDevice, runKey );
            }
            catch ( Exception e )
            {
                log.error( "Error executing run listener", e );
            }
        }
    }

    /**
     * Notify after run.
     *
     * @param currentDevice
     *            the current device
     * @param runKey
     *            the run key
     * @param successful
     *            the successful
     */
    private void notifyAfterRun( Device currentDevice, String runKey, boolean successful )
    {
        for ( RunListener runListener : runListeners )
        {
            try
            {
                int stepsPassed = 0;
                int stepsFailed = 0;
                int stepsIgnored = 0;
                long startTime = 0;
                long stopTime = 0;
                int scriptFailures = 0;
                int appFailures = 0;
                int cloudFailures = 0;
                int configFailures = 0;
                int filteredTests = 0;

                int successFlag = successful ? 1 : 0;

                if ( DeviceManager.instance().getArtifacts( ArtifactType.EXECUTION_RECORD ) != null && !DeviceManager.instance().getArtifacts( ArtifactType.EXECUTION_RECORD ).isEmpty() )
                {

                    for ( Object item : DeviceManager.instance().getArtifacts( ArtifactType.EXECUTION_RECORD ) )
                    {
                        ExecutionRecord eItem = (ExecutionRecord) item;

                        if ( startTime == 0 )
                            startTime = eItem.getTimeStamp();

                        if ( eItem.getTimeStamp() + eItem.getRunTime() > stopTime )
                            stopTime = eItem.getTimeStamp() + eItem.getRunTime();

                        switch ( eItem.getStatus() )
                        {
                            case FAILURE:
                                stepsFailed++;

                                if ( eItem.getT() != null )
                                {
                                    if ( eItem.getT() instanceof XFramiumException )
                                    {
                                        switch ( ((XFramiumException) eItem.getT()).getType() )
                                        {

                                            case CLOUD:
                                                cloudFailures++;
                                                break;

                                            case CONFIGURATION:
                                                configFailures++;
                                                break;

                                            case SCRIPT:
                                                scriptFailures++;
                                                break;

                                            case FILTERED:
                                                successFlag = 2;
                                                filteredTests++;
                                                break;
                                        }
                                    }
                                }

                                break;

                            case FAILURE_IGNORED:
                                stepsIgnored++;
                                break;
                            case REPORT:
                            case SUCCESS:
                                stepsPassed++;
                                break;
                        }
                    }
                }

                runListener.afterRun( currentDevice, runKey, successFlag, stepsPassed, stepsFailed, stepsIgnored, startTime, stopTime, scriptFailures, configFailures, appFailures, cloudFailures, filteredTests );
            }
            catch ( Exception e )
            {
                log.error( "Error executing run listener", e );
            }
        }
    }

    /**
     * Sets the retry count.
     *
     * @param retryCount
     *            the new retry count
     */
    public void setRetryCount( int retryCount )
    {
        this.retryCount = retryCount;
    }

    /**
     * Gets the device.
     *
     * @param deviceKey
     *            the device key
     * @return the device
     */
    public Device getDevice( String deviceKey )
    {
        return deviceMap.get( deviceKey );

    }

    

    /**
     * This will cycle through the available devices, by weight, to locate a
     * device that has not run this method yet.
     *
     * @param currentMethod
     *            The current test method
     * @param testContext
     *            the test context
     * @param attachDevice
     *            the attach device
     * @param personaName
     *            the persona name
     * @return The next available device or null if no device are available
     */
    
    public boolean isDeviceInvalid( Device currentDevice )
    {
        Integer deviceFailures = failureMap.get( currentDevice );
        if ( deviceFailures == null )
            return false;
        
        if ( deviceFailures >= (currentDevice.getAvailableDevices() * retryCount) )
            return true;
        
        return false;
    }
    
    public ConnectedDevice getDevice( TestPackage testPackage, boolean attachDevice )
    {

        String runKey = null;

        try
        {
            Device currentDevice = testPackage.getDevice();
            int currentFailures = 0;
            Integer deviceFailures = failureMap.get( currentDevice );
            if ( deviceFailures != null )
                currentFailures = deviceFailures;

            if ( currentFailures >= (currentDevice.getAvailableDevices() * retryCount) )
            {
                //
                // This device has failed too many times
                //
                return null;
            }

            //
            // Notify any listeners about this device acquisition and
            // allow them to cancel it
            //
            if ( !notifyValidateDevice( currentDevice, runKey ) )
            {
                return null;
            }

            if ( attachDevice && !dryRun )
            {
                //
                // Create the WebDriver here if we are attaching
                // this device
                //
                DeviceWebDriver webDriver = null;
                try
                {
                    if ( log.isDebugEnabled() )
                        log.debug( "Attempting to create a connected instance for " + currentDevice );

                    if ( testPackage.getTestName().getPersonaName() != null && !testPackage.getTestName().getPersonaName().isEmpty() )
                        currentDevice.addCapability( "windTunnelPersona", testPackage.getTestName().getPersonaName(), "STRING" );

                    webDriver = DriverManager.instance().getDriverFactory( currentDevice.getDriverType() ).createDriver( currentDevice );

                    if ( webDriver != null )
                    {
                        notifyBeforeRun( currentDevice, testPackage.getRunKey() );

                        if ( log.isDebugEnabled() )
                            log.debug( "WebDriver Created - Creating Connected Device for " + currentDevice );

                        DeviceManager.instance().notifyPropertyAdapter( configurationProperties, webDriver );

                        return new ConnectedDevice( webDriver, currentDevice, testPackage.getTestName().getPersonaName() );
                    }
                    else
                    {
                        //
                        // We got a null web driver here
                        //
                        Integer failureCount = failureMap.get( currentDevice );
                        if ( failureCount == null )
                            failureMap.put( currentDevice, 1 );
                        else
                            failureMap.put( currentDevice, failureCount + 1 );
                    }
                }
                catch ( Exception e )
                {
                    try
                    {

                    }
                    catch ( Exception e2 )
                    {
                    }
                    log.error( "Error creating factory instance", e );
                    try
                    {
                        webDriver.close();
                    }
                    catch ( Exception e2 )
                    {
                    }
                    try
                    {
                        webDriver.quit();
                    }
                    catch ( Exception e2 )
                    {
                    }
                    Integer failureCount = failureMap.get( currentDevice );
                    if ( failureCount == null )
                        failureMap.put( currentDevice, 1 );
                    else
                        failureMap.put( currentDevice, failureCount + 1 );
                }
            }
            else
            {
                return new ConnectedDevice( null, currentDevice, testPackage.getTestName().getPersonaName() );
            }
        }
        catch( Exception e )
        {
            
        }
        
        return null;

    }

    /**
     * This will cycle through the available devices, by weight, to locate a
     * device that has not run this method yet.
     *
     * @param currentMethod
     *            The current test method
     * @param testContext
     *            the test context
     * @param attachDevice
     *            the attach device
     * @param personaName
     *            the persona name
     * @return The next available device or null if no device are available
     */
    public ConnectedDevice getUnconfiguredDevice( Method currentMethod, String testContext, String personaName, String deviceId )
    {
        ConnectedDevice rtn = null;
        Device currentDevice = NamedDataProvider.lookupDeviceById( deviceId, driverType );

        String runKey = currentMethod.getDeclaringClass().getSimpleName() + "." + currentMethod.getName() + (testContext != null ? ("." + testContext) : "");
        if ( personaName != null && !personaName.isEmpty() )
            runKey = runKey + "." + personaName;

        //if ( ((analyticsMap.get( currentDevice.getKey() ) == null) || (!analyticsMap.get( currentDevice.getKey() ).hasRun( runKey ))) && (!activeRuns.containsKey( currentDevice.getKey() + "." + runKey )) )
        //{
            DeviceWebDriver webDriver = null;
            try
            {
                if ( log.isDebugEnabled() )
                    log.debug( "Attempting to create WebDriver instance for " + currentDevice );

                if ( personaName != null && !personaName.isEmpty() )
                    currentDevice.addCapability( "windTunnelPersona", personaName, "STRING" );

                webDriver = DriverManager.instance().getDriverFactory( currentDevice.getDriverType() ).createDriver( currentDevice );

                if ( webDriver != null )
                {
                    if ( log.isDebugEnabled() )
                        log.debug( "WebDriver Created - Creating Connected Device for " + currentDevice );

                    DeviceManager.instance().notifyPropertyAdapter( configurationProperties, webDriver );

                    //activeRuns.put( currentDevice.getKey() + "." + runKey, true );

                    rtn = new ConnectedDevice( webDriver, currentDevice, personaName );
                }
            }
            catch ( Exception e )
            {
                log.error( "Error creating factory instance", e );
                try
                {
                    webDriver.close();
                }
                catch ( Exception e2 )
                {
                }
                try
                {
                    webDriver.quit();
                }
                catch ( Exception e2 )
                {
                }
            }
        //}
//        else
//        {
//            if ( log.isDebugEnabled() )
//                log.debug( Thread.currentThread().getName() + ": Releasing unused Device Semaphore for " + currentDevice );
//        }

        return rtn;
    }

    /**
     * This will cycle through the available devices, by weight, to locate a
     * device that has not run this method yet.
     *
     * @param deviceName
     *            name of the device which has to be connected
     * @return The next available device or null if no device are available
     */
    public ConnectedDevice getInactiveDevice( String deviceName )
    {
        ConnectedDevice rtn = null;

        Device currentDevice = deviceMap.get( deviceName );
        DeviceWebDriver webDriver = null;

        try
        {
            webDriver = DriverManager.instance().getDriverFactory( currentDevice.getDriverType() ).createDriver( currentDevice );

            if ( webDriver != null )
            {

                if ( log.isDebugEnabled() )
                    log.debug( "WebDriver Created - Creating Connected Device for " + currentDevice );

                DeviceManager.instance().notifyPropertyAdapter( configurationProperties, webDriver );
                rtn = new ConnectedDevice( webDriver, currentDevice, null );
            }
        }
        catch ( Exception e )
        {
            log.error( "Error creating factory instance", e );
            try
            {
                webDriver.close();
            }
            catch ( Exception e2 )
            {
            }
            try
            {
                webDriver.quit();
            }
            catch ( Exception e2 )
            {
            }

            if ( log.isDebugEnabled() )
                log.debug( Thread.currentThread().getName() + ": Releasing unused Device Semaphore for " + currentDevice );
        }
        finally
        {
            if ( log.isDebugEnabled() )
                log.debug( Thread.currentThread().getName() + ": Releasing Device Manager Lock" );
        }
        return rtn;
    }


    

    /**
     * Adds the run.
     *
     * @param currentDevice
     *            the current device
     * @param currentMethod
     *            the current method
     * @param testContext
     *            the test context
     * @param success
     *            the success
     * @param personaName
     *            the persona name
     */
    public void addRun( Device currentDevice, TestPackage testPackage, TestContainer testContainer, boolean success )
    {
        try
        {
            testContainer.completeTest( testPackage.getTestName(), testPackage.getRunKey(), success ? RunStatus.COMPLETED : RunStatus.FAILED );
            notifyAfterRun( currentDevice, testPackage.getRunKey(), success );

        }
        finally
        {
        }
    }

    /**
     * Gets the devices.
     *
     * @return the devices
     */
    public List<Device> getDevices()
    {
        return Collections.unmodifiableList( deviceList );
    }

    /**
     * Register device.
     *
     * @param currentDevice
     *            the current device
     */
    public void registerDevice( Device currentDevice )
    {
        if ( log.isInfoEnabled() )
            log.info( "Registering Device " + currentDevice );
        deviceMap.put( currentDevice.getKey(), currentDevice );
        deviceList.add( currentDevice );
    }

    public void registerInactiveDevice( Device currentDevice )
    {
        if ( log.isInfoEnabled() )
            log.info( "Registering Inactive Device " + currentDevice );
        deviceMap.put( currentDevice.getKey(), currentDevice );
    }

    /**
     * Checks if is caching enabled.
     *
     * @return true, if is caching enabled
     */
    public boolean isCachingEnabled()
    {
        return cachingEnabled;
    }

    /**
     * Sets the caching enabled.
     *
     * @param cachingEnabled
     *            the new caching enabled
     */
    public void setCachingEnabled( boolean cachingEnabled )
    {
        this.cachingEnabled = cachingEnabled;
    }

}
