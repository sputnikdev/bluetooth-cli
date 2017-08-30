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

import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class NavigationCommands implements CommandMarker {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Autowired
    private InfoCommands infoCommands;

    @CliAvailabilityIndicator({"cd"})
    public boolean isCDAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({"ls"})
    public boolean isLSAvailable() {
        return true;
    }

    @CliCommand(value = "ls", help = "Print available bluetooth objects (dependants)")
    public String ls() {
        if (bluetoothManagerCli.getSelected() == null) {
            return Joiner.on(OsUtils.LINE_SEPARATOR).join(bluetoothManagerCli.getDiscoveredAdapters());
        } else {
            return Joiner.on(OsUtils.LINE_SEPARATOR).join(bluetoothManagerCli.getSelectedDescendants());
        }
    }

    @CliCommand(value = "cd", help = "Change device (adapter, device or characteristic)")
    public String cd(
            @CliOption(key = {"url"}, mandatory = true, help = "Bluetooth URL") final URL url) {
        BluetoothGovernor selected;
        if (url.isRoot()) {
            selected = null;
        } else {
            selected = bluetoothManagerCli.getBluetoothManager().getGovernor(url);
        }
        bluetoothManagerCli.setSelected(selected);
        return infoCommands.info(url);
    }

}
