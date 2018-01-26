package org.sputnikdev.bluetooth.cli;

import org.junit.Test;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

public class BluetoothManagerWahooHRM {

    @Test
    public void testMinimal() throws InterruptedException {
        System.out.println("Battery level: " +
                new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .withBlueGigaTransport("^*.$")
                .withIgnoreTransportInitErrors(true)
                .build()
                .getCharacteristicGovernor(new URL("/XX:XX:XX:XX:XX:XX/F7:EC:62:B9:CF:1F/" +
                        "0000180f-0000-1000-8000-00805f9b34fb/00002a19-0000-1000-8000-00805f9b34fb"), true)
                .read()[0]);
    }

    @Test
    public void testDirectRead() throws InterruptedException {
        // instantiate the Bluetooth Manager
        BluetoothManager manager = new BluetoothManagerBuilder()
                //.withTinyBTransport(true)
                .withBlueGigaTransport("^*.$")
                .build();
        // don't forget to start the Bluetooth Manager processes
        manager.start(false);

        // define a URL pointing to the target characteristic
        URL url = new URL("/XX:XX:XX:XX:XX:XX/F7:EC:62:B9:CF:1F/" +
            "0000180f-0000-1000-8000-00805f9b34fb/00002a19-0000-1000-8000-00805f9b34fb");
        // get the characteristic governor and read its value
        byte[] data = manager.getCharacteristicGovernor(url, true).read();
        System.out.println("Battery level: " + data[0]);
        // when we are done (just about to exit application) it is advisable to dispose the Bluetooth Manager,
        // so that it automatically releases all resources (disconnects devices etc)
        manager.dispose();
    }

}