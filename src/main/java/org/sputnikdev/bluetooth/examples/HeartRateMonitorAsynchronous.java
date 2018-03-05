package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

/**
 * A test application to display readings from a standard bluetooth heart rate monitor.
 * This example demonstrates how to use the Bluetooth Manager in asynchronous way (listening to events).
 *
 * @author Vlad Kolotov
 */
public final class HeartRateMonitorAsynchronous {

    private static final String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    private static final String HRM_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String BATTERY_LEVEL_CHAR = "00002a19-0000-1000-8000-00805f9b34fb";
    private static final String HRM_CHAR = "00002a37-0000-1000-8000-00805f9b34fb";

    private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            .withDiscovering(true)
            .withRediscover(true)
            .build();
    private final BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();
    private final URL url;

    /**
     * An entry point for the application.
     * A single argument (device address) is required in the following format:
     * <br>/adapter address/device address
     * <br>E.g.: /88:6B:0F:30:63:AD/F7:EC:62:B9:CF:1F
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException {
        new HeartRateMonitorAsynchronous(new URL(args[0])).run();
    }

    private HeartRateMonitorAsynchronous(URL url) {
        this.url = url;
    }

    private void run() throws InterruptedException {
        CharacteristicGovernor battery =
                bluetoothManager.getCharacteristicGovernor(url.copyWith(BATTERY_SERVICE, BATTERY_LEVEL_CHAR));

        // just a one-off battery level print
        battery.whenReady(CharacteristicGovernor::read).thenAccept(this::printBatteryLevel);

        // subscribing for HRM notification
        bluetoothManager.getCharacteristicGovernor(url.copyWith(HRM_SERVICE, HRM_CHAR))
                .addValueListener(this::printHeartRate);

        // making the HRM device governor to connect to the device and keep connection open
        bluetoothManager.getDeviceGovernor(url).setConnectionControl(true);

        // do your application logic here
        Thread.currentThread().join();
    }

    private void printBatteryLevel(byte[] data) {
        System.out.println("Battery level: "
                + gattParser.parse("2A19", data).get("Level").getInteger());
    }

    private void printHeartRate(byte[] data) {
        System.out.println("Heart rate: " + gattParser.parse("2A37", data)
                .get("Heart Rate Measurement Value (uint8)").getInteger());
    }

}
