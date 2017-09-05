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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.*;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class InfoCommands implements CommandMarker {

    private static final String FORMAT_TWO_COLUMNS = "%-30s%-50s";
    private static final String FORMAT_THREE_COLUMNS = "%-15s%-30s%-30s";

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @CliAvailabilityIndicator({"info"})
    public boolean isInfoAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({"pwd"})
    public boolean isPWDAvailable() {
        return bluetoothManagerCli.getSelected() != null;
    }

    @CliCommand(value = "pwd", help = "Print details about selected bluetooth object")
    public String pwd() {
        return info(bluetoothManagerCli.getSelected().getURL());
    }

    @CliCommand(value = "info", help = "Print details about bluetooth object")
    public String info(@CliOption(key = {"url"}, mandatory = false, help = "Bluetooth URL") final URL url) {
        URL objectURL = url;

        if (url == null || url.isRoot()) {
            if (bluetoothManagerCli.getSelected() != null) {
                objectURL = bluetoothManagerCli.getSelected().getURL();
            } else {
                return "Select a bluetooth object (see 'cd' command) or specify --url parameter";
            }
        }
        return getBluetoothGovernorInfo(bluetoothManagerCli.getBluetoothManager().getGovernor(objectURL));
    }

    private void format(StringBuilder builder, String colum1, String column2) {
        builder.append(String.format(FORMAT_TWO_COLUMNS, colum1, column2)).append(OsUtils.LINE_SEPARATOR);
    }

    private void format(StringBuilder builder, String colum1, String column2, String column3) {
        builder.append(String.format(FORMAT_THREE_COLUMNS, colum1, column2, column3)).append(OsUtils.LINE_SEPARATOR);
    }

    private String getBluetoothGovernorInfo(BluetoothGovernor bluetoothGovernor) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, PropertyDescriptor> property :
                bluetoothManagerCli.getSelectedGovernorProperties(false).entrySet()) {
            try {
                format(builder, property.getKey() + ":",
                        String.valueOf(property.getValue().getReadMethod().invoke(bluetoothGovernor)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        printChildren(builder, bluetoothGovernor);

        return builder.toString();
    }

    private void printChildren(StringBuilder builder, BluetoothGovernor governor) {
        try {
            governor.accept(new BluetoothObjectVisitor() {
                @Override public void visit(AdapterGovernor governor) throws NotReadyException {
                    printDevices(builder, governor.getDeviceGovernors());
                }

                @Override public void visit(DeviceGovernor governor) throws NotReadyException {
                    printServicesAndCharacteristics(builder, governor);
                }

                @Override public void visit(CharacteristicGovernor governor) throws NotReadyException {
                    printCharacteristic(builder, governor);
                }
            });
        } catch (Exception e) {
            format(builder, "Siblings:", "Not ready");
        }
    }

    private void print(StringBuilder builder, List<? extends BluetoothGovernor> governors) {
        for (BluetoothGovernor governor : governors) {
            format(builder, "", governor.toString());
        }
    }

    private void printDevices(StringBuilder builder, List<? extends BluetoothGovernor> governors) {
        format(builder, "Devices:", "");
        print(builder, governors);
    }

    private void printServicesAndCharacteristics(StringBuilder builder, DeviceGovernor governor)
            throws NotReadyException {
        format(builder, "Services:", "");
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        Map<URL, List<CharacteristicGovernor>> services = governor.getServicesToCharacteristicsMap();
        for (Map.Entry<URL, List<CharacteristicGovernor>> service : services.entrySet()) {
            String serviceUUID = service.getKey().getServiceUUID();
            String serviceName = parser.isKnownService(service.getKey().getServiceUUID()) ?
                    parser.getService(serviceUUID).getName() : "Unrecognised";
            format(builder, "", serviceUUID + " [" + serviceName + "]", "");
            printCharacteristics(builder, service.getValue());
        }
    }

    private void printCharacteristics(StringBuilder builder, List<CharacteristicGovernor> characteristics) {
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        format(builder, "", "Characteristics:");
        for (CharacteristicGovernor characteristic : characteristics) {
            String characteristicUUID = characteristic.getURL().getCharacteristicUUID();
            String characteristicName = parser.isKnownCharacteristic(characteristicUUID) ?
                    parser.getCharacteristic(characteristicUUID).getName() : "Unrecognised";
            format(builder, "", "", characteristicUUID + " [" + characteristicName + "] [" +
                    characteristic.getFlags().stream().map(Enum::toString).collect(Collectors.joining(", ")) + "]");
        }
    }

    private void printCharacteristic(StringBuilder builder, CharacteristicGovernor governor) throws NotReadyException {
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        String characteristicUUID = governor.getURL().getCharacteristicUUID();
        format(builder, "Name:", parser.isKnownCharacteristic(characteristicUUID) ?
                parser.getCharacteristic(characteristicUUID).getName() : "Unrecognised");
        format(builder, "Flags:", governor.isReady() ?
                governor.getFlags().stream().map(Enum::toString).collect(Collectors.joining(", ")) : "Not ready");
        if (parser.isKnownCharacteristic(governor.getURL().getCharacteristicUUID())) {
            format(builder, "Fields:", "");
            List<Field> fields = parser.getFields(governor.getURL().getCharacteristicUUID());
            for (Field field : fields) {
                format(builder, "", field.getName() + " [" + field.getFormat().getName() + "]");
            }
        } else {
            builder.append("Unrecognised characteristic");
        }
    }

}
