package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;

import java.util.Arrays;
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
                    ? gattParser.getService(serviceUID).getName() : serviceUID;
            service.getCharacteristics().forEach(characteristic -> {
                boolean knownCharacteristic = gattParser.isKnownCharacteristic(characteristic.getURL().getCharacteristicUUID());

                String flags = characteristic.getFlags().stream().map(Object::toString)
                        .collect(Collectors.joining(", "));
                String characteristicUID = characteristic.getURL().getCharacteristicUUID();
                if (characteristic.getFlags().contains(CharacteristicAccessType.READ)) {

                    if (knownCharacteristic) {
                        byte[] data = tryToRead(bluetoothManager.getCharacteristicGovernor(characteristic.getURL()));
                        gattParser.parse(characteristic.getURL().getCharacteristicUUID(), data).getFieldHolders().forEach(holder -> {
                            Field field = holder.getField();
                            System.out.println(String.format("%-25s %-45s %-20s: %50s", serviceName, field.getName(),
                                    field.getFormat().getName() + "; " + flags, holder.getString()));
                        });
                    } else {
                        System.out.println(String.format("%-25s %-45s %-20s: %50s", serviceName,
                                characteristic.getURL().getCharacteristicUUID(),
                                flags,
                                formatHex(tryToRead(bluetoothManager.getCharacteristicGovernor(characteristic.getURL())))));
                    }
                } else {
                    if (knownCharacteristic) {
                        List<Field> fields = gattParser.getFields(characteristic.getURL().getCharacteristicUUID());
                        fields.forEach(field -> {
                            System.out.println(String.format("%-25s %-45s %-20s", serviceName, field.getName(),
                                    field.getFormat().getName() + "; " + flags));
                        });
                    } else {
                        System.out.println(String.format("%-25s %-45s %-20s", serviceName, characteristicUID, flags));
                    }
                }

            });
        });
    }

    static String formatHex(byte[] value) {
        String[] hexFormatted = new String[value.length];
        int index = 0;
        for (byte b : value) {
            hexFormatted[index++] = String.format("%02x", b);
        }
        return Arrays.toString(hexFormatted);
    }

    private static byte[] tryToRead(CharacteristicGovernor governor) {
        try {
            return governor.read();
        } catch (Throwable ex) {
            System.out.println("Could not read characteristic: " + ex.getMessage());
            return new byte[]{ };
        }
    }

}
