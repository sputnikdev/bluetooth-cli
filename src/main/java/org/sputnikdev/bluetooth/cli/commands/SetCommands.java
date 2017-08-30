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

import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class SetCommands implements CommandMarker {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Autowired
    private InfoCommands infoCommands;

    @CliAvailabilityIndicator({"set"})
    public boolean isSetAvailable() {
        return bluetoothManagerCli.getSelected() != null;
    }

    @CliCommand(value = "set", help = "Modifies an attribute of a bluetooth object")
    public String set(@CliOption(key = {"name"}, mandatory = true, help = "Attribute name") final Method method,
            @CliOption(key = {"value"}, mandatory = true, help = "Attribute value") final String value) {

        try {
            method.invoke(bluetoothManagerCli.getSelected(),
                    ConvertUtils.convert(value, method.getParameterTypes()[0]));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return infoCommands.info(bluetoothManagerCli.getSelected().getURL());
    }


}
