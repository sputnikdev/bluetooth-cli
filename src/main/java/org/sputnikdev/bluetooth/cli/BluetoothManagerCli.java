package org.sputnikdev.bluetooth.cli;

/*-
 * #%L
 * org.sputnikdev:bluetooth-cli
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import gnu.io.NativeResourceException;
import gnu.io.SerialManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.AdapterDiscoveryListener;
import org.sputnikdev.bluetooth.manager.AdapterGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceDiscoveryListener;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredAdapter;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerFactory;
import org.sputnikdev.bluetooth.manager.impl.BluetoothObjectFactoryProvider;
import org.sputnikdev.bluetooth.manager.transport.bluegiga.BluegigaFactory;
import org.sputnikdev.bluetooth.manager.transport.tinyb.TinyBFactory;

import javax.annotation.PreDestroy;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class BluetoothManagerCli implements DeviceDiscoveryListener, AdapterDiscoveryListener {

    protected final java.util.logging.Logger logger = HandlerUtils.getLogger(getClass());

    public static BluetoothManagerCli instance;

    private final BluetoothManager bluetoothManager;
    private final BluetoothGattParser gattParser;

    private BluetoothGovernor selected;

    public BluetoothManagerCli() {
        registerTransports();
        bluetoothManager = BluetoothManagerFactory.getManager();
        bluetoothManager.addDeviceDiscoveryListener(this);
        bluetoothManager.addAdapterDiscoveryListener(this);
        bluetoothManager.start(true);

        gattParser = BluetoothGattParserFactory.getDefault();
        String extensionFolder = System.getProperty("user.home") + File.separator + ".bluetooth_smart";
        File extensionFolderFile = new File(extensionFolder);
        if (extensionFolderFile.exists() && extensionFolderFile.isDirectory()) {
            gattParser.loadExtensionsFromFolder(extensionFolder);
        }
    }

    @PreDestroy
    public void shutDown() {
        logger.info("Shutting down / disposing Bluetooth Manager");
        bluetoothManager.dispose();
    }

    @Override
    public void discovered(DiscoveredDevice discoveredDevice) {
        logger.info("Device discovered: " + discoveredDevice);
    }

    @Override
    public void discovered(DiscoveredAdapter adapter) {
        logger.info("Adapter discovered: " + adapter);
    }

    @Override
    public void deviceLost(URL url) {
        logger.info("Device lost: " + url);
    }

    @Override
    public void adapterLost(URL url) {
        logger.info("Adapter lost: " + url);
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothGattParser getGattParser() {
        return gattParser;
    }

    public static BluetoothManagerCli getInstance() {
        if (instance == null) {
            synchronized (BluetoothManagerCli.class) {
                if (instance == null) {
                    instance = new BluetoothManagerCli();
                }
            }
        }
        return instance;
    }

    public Set<DiscoveredDevice> getDiscoveredDevices() {
        return bluetoothManager.getDiscoveredDevices();
    }

    public Set<DiscoveredAdapter> getDiscoveredAdapters() {
        return bluetoothManager.getDiscoveredAdapters();
    }

    public BluetoothGovernor getSelected() {
        return selected;
    }

    public void setSelected(BluetoothGovernor selected) {
        this.selected = selected;
    }

    public List<BluetoothGovernor> getSelectedDescendants() {
        if (selected == null || !selected.isReady()) {
            return Collections.EMPTY_LIST;
        }
        if (selected instanceof AdapterGovernor) {
            return (List) ((AdapterGovernor) selected).getDeviceGovernors();
        } else if (selected instanceof DeviceGovernor) {
            return (List) ((DeviceGovernor) selected).getCharacteristicGovernors();
        }
        return Collections.EMPTY_LIST;
    }

    public static void main(String[] args) throws IOException {
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add("--disableInternalCommands");
        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);
        Bootstrap.main(argsArray);
    }

    public Class getSelectedGovernorInterface() {
        if (selected == null) {
            return null;
        }
        List<Class> interfaces = Arrays.asList(selected.getClass().getInterfaces());
        List<Class> supported = new ArrayList<>(
                Arrays.asList(AdapterGovernor.class, DeviceGovernor.class, CharacteristicGovernor.class));

        supported.retainAll(interfaces);

        return supported.get(0);
    }

    public Map<String, PropertyDescriptor> getSelectedGovernorProperties(boolean writeOnly) {
        Map<String, PropertyDescriptor> properties = new HashMap<>();
        Class selectedInterface = getSelectedGovernorInterface();
        try {
            for (PropertyDescriptor propertyDescriptor :
                    Introspector.getBeanInfo(selectedInterface).getPropertyDescriptors()) {

                if (writeOnly && propertyDescriptor.getWriteMethod() == null) {
                    continue;
                }

                Class propertyClass = propertyDescriptor.getPropertyType();
                if (propertyClass.isPrimitive() || propertyClass.isAssignableFrom(Number.class)
                        || propertyClass.equals(String.class)) {

                    String propertyName = String.join(" ",
                            StringUtils.splitByCharacterTypeCamelCase(propertyDescriptor.getDisplayName()));

                    properties.put(StringUtils.capitalize(propertyName), propertyDescriptor);
                }
            }
        } catch (IntrospectionException e) { }
        return properties;
    }

    private void registerTransports() {

        if (TinyBFactory.loadNativeLibraries()) {
            BluetoothObjectFactoryProvider.registerFactory(new TinyBFactory());
        } else {
            logger.warning("Could not load tinyb library. TinyB transport is not registered.");
        }

        try {
            SerialManager.getInstance();
            BluetoothObjectFactoryProvider.registerFactory(new BluegigaFactory());
        } catch (NativeResourceException ex) {
            logger.warning("Could not load bluegiga library. Bluegiga transport is not registered: " + ex.getMessage());
        }
    }

}
