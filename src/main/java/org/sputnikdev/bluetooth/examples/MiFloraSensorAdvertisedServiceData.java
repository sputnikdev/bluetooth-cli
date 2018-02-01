package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.gattparser.GattResponse;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.util.List;
import java.util.Map;

/**
 * A test application to display readings from the MiFlora sensor.
 *
 * Because of a proprietary authentication protocol that MI devices use it is not possible to keep connection open.
 * If authentication fails, device drops connection. However, during a short authentication window, it is still possible
 * to read some data of the device. This example shows how to do so.
 *
 * @author Vlad Kolotov
 */
public final class MiFloraSensorAdvertisedServiceData {

    private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            .withDiscovering(true)
            .withRediscover(true)
            .build();
    private final BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();

    /**
     * An entry point for the application.
     * A single argument (device address) is required in the following format:
     * <br>/adapter address/device address
     * //00:1A:7D:DA:71:05
     * <br>E.g.: /88:6B:0F:30:63:B3/C4:7C:8D:66:07:4B
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException {
        new MiFloraSensorAdvertisedServiceData(new URL(args[0])).run();
    }

    private MiFloraSensorAdvertisedServiceData(URL url) {
        bluetoothManager.addDeviceDiscoveryListener(discoveredDevice -> {
            System.out.println(String.format("Discovered: %s [%s]", discoveredDevice.getName(),
                    discoveredDevice.getURL().getDeviceAddress()));
        });
        bluetoothManager.getDeviceGovernor(url).addBluetoothSmartDeviceListener(new BluetoothSmartDeviceListener() {
            @Override
            public void servicesResolved(List<GattService> gattServices) {  }

            @Override
            public void serviceDataChanged(Map<String, byte[]> serviceData) {
                serviceData.forEach((key, value) -> {
                    GattResponse response = gattParser.parse(key, value);
                    response.getFieldHolders().stream()
                            .filter(holder -> !holder.getField().isUnknown())
                            .forEach(holder -> {
                                System.out.println(holder.getField().getName() + ": " + holder);
                            });
                });
            }

            @Override
            public void manufacturerDataChanged(Map<Short, byte[]> manufacturerData) {
                manufacturerData.forEach((key, value) -> System.out.println(String.format("Manufacturer data (%s): %s",
                        key, Utils.formatHex(value))));
            }
        });
    }

    private void run() throws InterruptedException {
        Thread.currentThread().join();
    }

}
