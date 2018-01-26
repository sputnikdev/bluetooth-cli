package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class Utils {

    private Utils() { }


    static void printAvailableFields(BluetoothManager bluetoothManager, BluetoothGattParser gattParser,
                                      List<GattService> gattService) {
        System.out.println("========================= Discovered GATT fields =========================");
        gattService.forEach(service -> {
            String serviceUID = service.getURL().getServiceUUID();
            final String serviceName = gattParser.isKnownService(serviceUID)
                    ? gattParser.getService(serviceUID).getName() : "Unknown service";
            service.getCharacteristics().forEach(characteristic -> {
                if (gattParser.isKnownCharacteristic(characteristic.getURL().getCharacteristicUUID())) {
                    String flags = characteristic.getFlags().stream().map(Object::toString)
                            .collect(Collectors.joining(", "));
                    if (characteristic.getFlags().contains(CharacteristicAccessType.READ)) {
                        byte[] data = bluetoothManager.getCharacteristicGovernor(characteristic.getURL()).read();
                        gattParser.parse(characteristic.getURL().getCharacteristicUUID(), data).getFieldHolders().forEach(holder -> {
                            Field field = holder.getField();
                            System.out.println(String.format("%s / %s (%s; %s): %s", serviceName, field.getName(),
                                    field.getFormat().getName(), flags, holder.getString()));
                        });
                    } else {
                        List<Field> fields = gattParser.getFields(characteristic.getURL().getCharacteristicUUID());
                        fields.forEach(field -> {
                            System.out.println(String.format("%s / %s (%s; %s)", serviceName, field.getName(),
                                    field.getFormat().getName(), flags));
                        });
                    }
                } else {
                    System.out.println("Unknown characteristic: " + characteristic.getURL());
                }
            });
        });
    }

}
