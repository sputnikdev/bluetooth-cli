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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class MethodConverter implements Converter<Method> {

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return type.equals(Method.class);
    }

    @Override
    public Method convertFromText(String value, Class<?> targetType, String optionContext) {
        String propertyName = StringUtils.uncapitalize(value.replaceAll("\\s",""));
        for (Map.Entry<String, PropertyDescriptor> property :
                bluetoothManagerCli.getSelectedGovernorProperties(true).entrySet()) {
            if (propertyName.equals(property.getValue().getDisplayName())) {
                return property.getValue().getWriteMethod();
            }
        }
        return null;
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType,
            String existingData, String optionContext, MethodTarget target) {
        for (Map.Entry<String, PropertyDescriptor> property :
                bluetoothManagerCli.getSelectedGovernorProperties(true).entrySet()) {
            completions.add(new Completion(property.getKey()));
        }
        return true;
    }

}
