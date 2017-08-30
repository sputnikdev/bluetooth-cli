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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.gattparser.GattResponse;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothObjectType;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class ReadWriteCommands implements CommandMarker {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Autowired
    private InfoCommands infoCommands;

    @CliAvailabilityIndicator({"read"})
    public boolean isReadAvailable() {
        BluetoothGovernor selected = bluetoothManagerCli.getSelected();
        if (selected != null && selected.getType() == BluetoothObjectType.CHARACTERISTIC && selected.isReady()) {
            CharacteristicGovernor characteristicGovernor = (CharacteristicGovernor) selected;
            return characteristicGovernor.isReadable();
        }
        return false;
    }

    @CliAvailabilityIndicator({"write"})
    public boolean isWriteAvailable() {
        BluetoothGovernor selected = bluetoothManagerCli.getSelected();
        if (selected != null && selected.getType() == BluetoothObjectType.CHARACTERISTIC && selected.isReady()) {
            CharacteristicGovernor characteristicGovernor = (CharacteristicGovernor) selected;
            return characteristicGovernor.isWritable();
        }
        return false;
    }

    @CliCommand(value = "read", help = "Reads from selected characteristics")
    public String read() {
        CharacteristicGovernor characteristicGovernor =
                (CharacteristicGovernor) bluetoothManagerCli.getSelected();
        return parse(characteristicGovernor.getURL(), characteristicGovernor.read());
    }

    @CliCommand(value = "write", help = "Writes to selected characteristics")
    public String write(@CliOption(key = {"fieldName"}, mandatory = true, help = "Field name") final FieldHolder holder,
            @CliOption(key = {"value"}, mandatory = true, help = "Field value") final String value) {
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        CharacteristicGovernor characteristicGovernor =
                (CharacteristicGovernor) bluetoothManagerCli.getSelected();

        GattRequest gattRequest = parser.prepare(characteristicGovernor.getURL().getCharacteristicUUID());

        gattRequest.setField(holder.getField().getName(), value);

        characteristicGovernor.write(parser.serialize(gattRequest));

        return "OK";
    }

    String parse(URL url, byte[] raw) {
        StringBuilder builder = new StringBuilder();

        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        String characteristicUUID = url.getCharacteristicUUID();
        if (parser.isKnownCharacteristic(url.getCharacteristicUUID())) {
            GattResponse gattResponse = parser.parse(characteristicUUID, raw);
            builder.append(gattResponse.getFieldHolders().stream().map(
                    holder -> String.format("%-30s%-50s", holder.getField().getName() + ":", holder.getString(null))).
                    collect(Collectors.joining(",")));
        } else {
            builder.append("[");
            builder.append(IntStream.range(0, raw.length).mapToObj(
                    i -> String.format("%02x", raw[i] & 0xff)).collect(Collectors.joining(", ")));
            builder.append("]");
        }

        return builder.toString();
    }


}
