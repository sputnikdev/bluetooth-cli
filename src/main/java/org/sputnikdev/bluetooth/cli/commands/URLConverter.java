package org.sputnikdev.bluetooth.cli.commands;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.manager.AdapterGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredAdapter;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class URLConverter implements Converter<URL> {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return type.equals(URL.class);
    }

    @Override
    public URL convertFromText(String value, Class<?> targetType, String optionContext) {
        if ("../".equals(value) && bluetoothManagerCli.getSelected() != null) {
            URL selectedURL = bluetoothManagerCli.getSelected().getURL();
            if (selectedURL.isAdapter()) {
                return new URL();
            }
            URL parent = selectedURL.getParent();
            return parent.isService() ? parent.getParent() : parent;
        }
        return new URL(value);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType,
            String existingData, String optionContext, MethodTarget target) {
        BluetoothGovernor selected = bluetoothManagerCli.getSelected();
        if (selected == null) {
            completions.addAll(getAdapters());
        } else if (selected instanceof AdapterGovernor) {
            completions.addAll(getDevices((AdapterGovernor) selected));
        } else if (selected instanceof DeviceGovernor) {
            completions.addAll(getCharacteristics((DeviceGovernor) selected));
        }
        return true;
    }

    private List<Completion> getAdapters() {
        List<Completion> result = new ArrayList<>();
        for (DiscoveredAdapter adapter : bluetoothManagerCli.getDiscoveredAdapters()) {
            result.add(new Completion(adapter.getURL().toString()));
        }
        return result;
    }

    private List<Completion> getDevices(AdapterGovernor adapterGovernor) {
        return adapterGovernor.isReady() ?
                getCompletions(adapterGovernor.getDeviceGovernors()) : Collections.emptyList();
    }

    private List<Completion> getCharacteristics(DeviceGovernor deviceGovernor) {
        return deviceGovernor.isReady() ?
                getCompletions(deviceGovernor.getCharacteristicGovernors()) : Collections.emptyList();
    }

    private List<Completion> getCompletions(List<? extends BluetoothGovernor> governors) {
        return governors.stream().map(governor -> new Completion(governor.getURL().toString())).collect(Collectors.toList());
    }
}
