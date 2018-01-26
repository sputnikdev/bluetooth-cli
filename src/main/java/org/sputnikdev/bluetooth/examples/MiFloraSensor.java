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
public final class MiFloraSensor {

    private static final String AUTH_SERVICE = "00001204-0000-1000-8000-00805f9b34fb";
    private static final String AUTH_CHAR = "00001a00-0000-1000-8000-00805f9b34fb";
    private static final byte[] AUTH_CODE = {(byte) 0xA0, 0x1F};

    private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            //.withDiscovering(true)
            //.withRediscover(true)
            .build();
    private final BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();

    private CharacteristicGovernor auth;

    /**
     * An entry point for the application.
     * A single argument (device address) is required in the following format:
     * <br>/adapter address/device address
     * <br>E.g.: /88:6B:0F:30:63:AD/C4:7C:8D:66:07:4B
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException {
        new MiFloraSensor(new URL(args[0])).run();
    }

    private MiFloraSensor(URL url) {
        auth = bluetoothManager.getCharacteristicGovernor(url.copyWith(AUTH_SERVICE, AUTH_CHAR));
        bluetoothManager.getDeviceGovernor(url).addBluetoothSmartDeviceListener(services -> {
            auth.write(AUTH_CODE);
            printAvailableFields(services);
        });
        bluetoothManager.getDeviceGovernor(url).setConnectionControl(true);
    }

    private void run() throws InterruptedException {
        //bluetoothManager.getAdapterGovernor(new URL("/88:6B:0F:30:63:AD")).setDiscoveringControl(true);
        //bluetoothManager.getAdapterGovernor(new URL("/88:6B:0F:30:63:B3")).setDiscoveringControl(true);
        bluetoothManager.addDeviceDiscoveryListener(device -> {
            System.out.println(device.getURL() + " " + device.getName());
        });
        Thread.currentThread().join();
    }

    private void printAvailableFields(List<GattService> gattService) {
        Utils.printAvailableFields(bluetoothManager, gattParser, gattService);
    }

}
