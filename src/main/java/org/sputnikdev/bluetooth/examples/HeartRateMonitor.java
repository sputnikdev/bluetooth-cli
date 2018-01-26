package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.util.List;

/**
 * A test application to display readings from a standard bluetooth heart rate monitor.
 *
 * @author Vlad Kolotov
 */
public final class HeartRateMonitor {

    private static final String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    private static final String HRM_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String BATTERY_LEVEL_CHAR = "00002a19-0000-1000-8000-00805f9b34fb";
    private static final String HRM_CHAR = "00002a37-0000-1000-8000-00805f9b34fb";

    private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            .build();
    private final BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();

    private final CharacteristicGovernor batteryLevel;
    private final CharacteristicGovernor heartRate;

    /**
     * An entry point for the application.
     * A single argument (device address) is required in the following format:
     * <br>/adapter address/device address
     * <br>E.g.: /88:6B:0F:30:63:AD/F7:EC:62:B9:CF:1F
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException {
        new HeartRateMonitor(new URL(args[0])).run();
    }

    private HeartRateMonitor(URL url) {
        bluetoothManager.getDeviceGovernor(url).addBluetoothSmartDeviceListener(this::printAvailableFields);
        batteryLevel = bluetoothManager.getCharacteristicGovernor(
                url.copyWith(BATTERY_SERVICE, BATTERY_LEVEL_CHAR),true);
        heartRate = bluetoothManager.getCharacteristicGovernor(url.copyWith(HRM_SERVICE, HRM_CHAR));
    }

    private void run() throws InterruptedException {
        batteryLevel.addValueListener(this::printBatteryLevel);
        heartRate.addValueListener(this::printHeartRate);
        printBatteryLevel(batteryLevel.read());
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

    private void printAvailableFields(List<GattService> gattService) {
        Utils.printAvailableFields(bluetoothManager, gattParser, gattService);
    }

}
