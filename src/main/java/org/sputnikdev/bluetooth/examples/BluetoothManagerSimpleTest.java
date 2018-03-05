package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

/**
 * The example below shows how to set up the Bluetooth Manager and read a characteristic value (battery level)
 * in a single line of code.
 * @author Vlad Kolotov
 */
public final class BluetoothManagerSimpleTest {

    public static void main(String[] args) throws Exception {
        new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .withBlueGigaTransport("^*.$")
                .build()
                .getCharacteristicGovernor(new URL("/XX:XX:XX:XX:XX:XX/F7:EC:62:B9:CF:1F/"
                        + "0000180f-0000-1000-8000-00805f9b34fb/00002a19-0000-1000-8000-00805f9b34fb"), true)
                .whenReady(CharacteristicGovernor::read)
                .thenAccept(data -> {
                    System.out.println("Battery level: " + data[0]);
                }).get();
    }

}
