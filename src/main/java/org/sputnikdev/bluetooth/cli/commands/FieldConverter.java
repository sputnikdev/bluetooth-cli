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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class FieldConverter implements Converter<FieldHolder> {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return type.equals(FieldHolder.class);
    }

    @Override
    public FieldHolder convertFromText(String value, Class<?> targetType, String optionContext) {
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        CharacteristicGovernor characteristicGovernor =
                (CharacteristicGovernor) bluetoothManagerCli.getSelected();
        GattRequest gattRequest = parser.prepare(characteristicGovernor.getURL().getCharacteristicUUID());
        return gattRequest.getFieldHolder(value);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType,
            String existingData, String optionContext, MethodTarget target) {
        BluetoothGattParser parser = bluetoothManagerCli.getGattParser();
        CharacteristicGovernor characteristicGovernor =
                (CharacteristicGovernor) bluetoothManagerCli.getSelected();

        GattRequest gattRequest = parser.prepare(characteristicGovernor.getURL().getCharacteristicUUID());

        for (FieldHolder fieldHolder : gattRequest.getAllFieldHolders()) {
            completions.add(new Completion(fieldHolder.getField().getName()));
        }

        return true;
    }

}
