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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.cli.BluetoothManagerCli;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothObjectType;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.GenericBluetoothDeviceListener;
import org.sputnikdev.bluetooth.manager.ValueListener;

/**
 *
 * @author Vlad Kolotov
 */
@Component
public class NotificationCommands implements CommandMarker {

    private static final String[] DEVICE_NOTIFICATIONS = {"ONLINE", "BLOCKED", "RSSI", "CONNECTED", "SERVICES RESOLVED"};
    private static final String[] CHARACTERISTIC_NOTIFICATIONS = {"VALUE CHANGED"};

    protected final Logger logger = HandlerUtils.getLogger(getClass());

    @Autowired
    private BluetoothManagerCli bluetoothManagerCli;

    @Autowired
    private JLineShellComponent shell;
    @Autowired
    private ReadWriteCommands readWriteCommands;

    private final Map<URL, NotificationLogger> listeners = new HashMap<>();

    @CliAvailabilityIndicator({"notification"})
    public boolean isNotificationAvailable() {
        BluetoothGovernor selected = bluetoothManagerCli.getSelected();
        if (selected != null && selected.getType() == BluetoothObjectType.CHARACTERISTIC && selected.isReady()) {
            CharacteristicGovernor characteristicGovernor = (CharacteristicGovernor) selected;
            return characteristicGovernor.isNotifiable();
        }
        return false;
    }

    @CliCommand(value = "notification", help = "Enable/Disable notifications")
    public String notification(
            @CliOption(key = {""}, mandatory = true, help = "Enable/Disable notifications: on / off") final String command) {
        BluetoothGovernor selected = bluetoothManagerCli.getSelected();
        URL selectedURL = selected.getURL();
        synchronized (listeners) {
            if ("on".equals(command)) {
                if (!listeners.containsKey(selectedURL)) {
                    NotificationLogger logger = new NotificationLogger(selected);
                    listeners.put(selectedURL, logger);
                    if (selected instanceof DeviceGovernor) {
                        DeviceGovernor deviceGovernor = (DeviceGovernor) selected;
                        deviceGovernor.addBluetoothSmartDeviceListener(logger);
                        deviceGovernor.addGenericBluetoothDeviceListener(logger);
                        return "Notifications enabled: " + String.join(", ", DEVICE_NOTIFICATIONS);
                    } else if (selected instanceof CharacteristicGovernor) {
                        CharacteristicGovernor characteristicGovernor = (CharacteristicGovernor) selected;
                        characteristicGovernor.addValueListener(logger);
                        checkConnected(selectedURL.getDeviceURL());
                        return "Notifications enabled: " + String.join(", ", CHARACTERISTIC_NOTIFICATIONS);
                    }
                }
            } else if ("off".equals(command)) {
                if (listeners.containsKey(selectedURL)) {
                    listeners.remove(selectedURL);
                    if (selected instanceof DeviceGovernor) {
                        ((DeviceGovernor) selected).removeBluetoothSmartDeviceListener(listeners.get(selectedURL));
                        ((DeviceGovernor) selected).removeGenericBluetoothDeviceListener(listeners.get(selectedURL));
                        return "Notifications disabled: " + String.join(", ", DEVICE_NOTIFICATIONS);
                    } else if (selected instanceof CharacteristicGovernor) {
                        ((CharacteristicGovernor) selected).removeValueListener(listeners.get(selectedURL));
                        return "Notifications disabled: " + String.join(", ", CHARACTERISTIC_NOTIFICATIONS);
                    }
                }
            }
        }
        return "Nothing to enable";
    }

    private void checkConnected(URL deviceURL) {
        DeviceGovernor deviceGovernor =
                bluetoothManagerCli.getBluetoothManager().getDeviceGovernor(deviceURL);

        if (!deviceGovernor.isReady()) {
            NotificationCommands.this.logger.info("Device is not ready!");
            return;
        }

        if (!deviceGovernor.isConnected()) {
            NotificationCommands.this.logger.info("Device is not connected. "
                    + "Notification is enabled, but won't be notifying util the device gets connected.");
        }
    }

    private class NotificationLogger implements GenericBluetoothDeviceListener,
            BluetoothSmartDeviceListener, ValueListener {

        private final BluetoothGovernor governor;
        private LocalDateTime lastRSSINotified = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);
        private LocalDateTime lastValueNotified = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);

        private NotificationLogger(BluetoothGovernor governor) {
            this.governor = governor;
        }

        @Override
        public void online() {
            logger.info(governor + ": ONLINE");
            shell.promptLoop();
        }

        @Override
        public void offline() {
            logger.info(governor + ": OFFLINE");
        }

        @Override
        public void blocked(boolean blocked) {
            logger.info(governor + ": " + (blocked ? "BLOCKED" : "UNBLOCKED"));
        }

        @Override
        public void rssiChanged(short rssi) {
            if (LocalDateTime.now().minus(1, ChronoUnit.MINUTES).isAfter(lastRSSINotified)) {
                logger.info(governor + ": RSSI " + rssi);
                lastRSSINotified = LocalDateTime.now();
            }
        }

        @Override
        public void connected() {
            logger.info(governor + ": CONNECTED");
        }

        @Override
        public void disconnected() {
            logger.info(governor + ": DISCONNECTED");
        }

        @Override
        public void servicesResolved(List<GattService> gattServices) {
            logger.info(governor + ": SERVICES RESOLVED: " + gattServices.size());
        }

        @Override
        public void servicesUnresolved() {
            logger.info(governor + ": SERVICES UNRESOLVED");
        }

        @Override
        public void changed(byte[] value) {
            if (LocalDateTime.now().minus(1, ChronoUnit.SECONDS).isAfter(lastValueNotified)) {
                logger.info(governor + ": VALUE CHANGED:" + OsUtils.LINE_SEPARATOR +
                        readWriteCommands.parse(governor.getURL(), value));
                lastValueNotified = LocalDateTime.now();
            }
        }

    }
}
