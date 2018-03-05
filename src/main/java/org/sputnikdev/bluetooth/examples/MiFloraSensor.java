package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.gattparser.GattResponse;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.util.concurrent.ExecutionException;

/**
 * A test application to display readings from the MiFlora sensor.
 *
 * Because of a proprietary authentication protocol that MI devices use it is not possible to keep connection open.
 * If authentication fails, device drops connection. However, during a short authentication window, it is still possible
 * to read some data of the device. This example shows how to do so.
 *
 * @author Vlad Kolotov
 */
public final class MiFloraSensor {

    private static final String DATA_SERVICE = "00001204-0000-1000-8000-00805f9b34fb";
    private static final String MAGIC_NUMBER_CHAR = "00001a00-0000-1000-8000-00805f9b34fb";
    private static final String DATA_CHAR = "00001a01-0000-1000-8000-00805f9b34fb";
    private static final String BATTERY_FIRMWARE_CHAR = "00001a02-0000-1000-8000-00805f9b34fb";
    // captured magic number via bluetooth sniffer
    private static final byte[] MAGIC_NUMBER = {(byte) 0xA0, 0x1F};

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
     * //00:1A:7D:DA:71:05
     * <br>E.g.: /88:6B:0F:30:63:AD/C4:7C:8D:66:07:4B
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new MiFloraSensor(new URL(args[0])).run();
    }

    private MiFloraSensor(URL url) {
        this.url = url;
    }

    private boolean authenticate(CharacteristicGovernor governor) {
        return governor.write(MAGIC_NUMBER);
    }

    private void printBatteryLevel(byte[] data) {
        GattResponse batteryFirmwareResponse = gattParser.parse(BATTERY_FIRMWARE_CHAR, data);
        System.out.println(String.format("Battery level: %s %%", batteryFirmwareResponse.get("Battery level").getInteger()));
        System.out.println("Firmware version: " + batteryFirmwareResponse.get("Firmware version").getString());
    }

    private void printReadings(byte[] data) {
        GattResponse dataResponse = gattParser.parse(DATA_CHAR, data);
        System.out.println(String.format("Temperature: %.1f °C", dataResponse.get("Temperature").getDouble()));
        System.out.println(String.format("Sunlight: %d lux", dataResponse.get("Sunlight").getInteger()));
        System.out.println(String.format("Moisture: %d %%", dataResponse.get("Moisture").getInteger()));
        System.out.println(String.format("Fertility: %d µS/cm", dataResponse.get("Fertility").getInteger()));
    }

    private void run() throws InterruptedException, ExecutionException {
        bluetoothManager.getDeviceGovernor(url).setConnectionControl(true);
        bluetoothManager.addDeviceDiscoveryListener(discoveredDevice -> {
            System.out.println(String.format("Discovered: %s [%s]", discoveredDevice.getName(),
                    discoveredDevice.getURL().getDeviceAddress()));
        });
        bluetoothManager.getCharacteristicGovernor(url.copyWith(DATA_SERVICE, MAGIC_NUMBER_CHAR))
                .whenReady(this::authenticate)
                .thenAccept(authenticated -> {
                    bluetoothManager.getCharacteristicGovernor(url.copyWith(DATA_SERVICE, BATTERY_FIRMWARE_CHAR))
                            .whenReady(CharacteristicGovernor::read).thenAccept(this::printBatteryLevel);

                    bluetoothManager.getCharacteristicGovernor(url.copyWith(DATA_SERVICE, DATA_CHAR))
                            .whenReady(CharacteristicGovernor::read).thenAccept(this::printReadings);
                }).get();
        bluetoothManager.dispose();
    }

}
