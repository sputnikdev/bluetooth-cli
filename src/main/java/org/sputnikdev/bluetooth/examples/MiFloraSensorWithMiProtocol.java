package org.sputnikdev.bluetooth.examples;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.*;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A test application to display readings from the MiFlora sensor.
 * <p>Unfortunately MiFlor sensor (and probably other Mi sensors) uses a proprietary authentication mechanism that involves
 * a handshake, security token generation and exchange.
 * <p>Here are some steps that have been captured by a bluetooth sniffer while a MiFlora sensor was getting connected:
 *
 * <br>Step 1. Authentication process begins
 * <br>write 	0x0013 01 00 				- enable notification for 0x0012
 * <br>Step 2. Session opens (a magic number {00 BC 43 CD} is written to 0x001B)
 * <br>write 	0x001B 00 BC 43 CD          - 00000010-0000-1000-8000-00805f9b34fb
 * <br>notify 	0x0012 CD 9B 00 FA			- 00000001-0000-1000-8000-00805f9b34fb
 * <br>Step 3. Security tokens is generated and written
 * <br>write 	0x0012 10 A1 FB D5			- 00000001-0000-1000-8000-00805f9b34fb
 * <br>some kind of response after sending an auth token
 * <br>notify 	0x0012 D0 55 DE 70			- 00000001-0000-1000-8000-00805f9b34fb
 * <br>notify 	0x0012 D0 55 DE 70			- 00000001-0000-1000-8000-00805f9b34fb
 * <br>write 	0x0013 00 00                - disable notification for 0x0012
 * <br>authentication complete
 * -----
 * <br>read 	0x0038 -> 63 27 33 2E 31 2E 38         - 00001a02-0000-1000-8000-00805f9b34fb
 * <br>write 	0x0036 01 00                - enable notif for data char 0x0035 (00001a01-0000-1000-8000-00805f9b34fb)
 * <br>write	0x003F 01 00                - enable notif for 0x003E (00001a10-0000-1000-8000-00805f9b34fb)
 * <br>write	0x0033 A0 1F  				- set the magic number (00001a00-0000-1000-8000-00805f9b34fb) to be able to read the data char (00001a01-0000-1000-8000-00805f9b34fb))
 * <br>notify   0x0035 0E 01 00 4E 01 00 00 00 00 00 02 3C 00 FB 34 9B - data char
 * <br>read	    0x0041 -> 1C 04 08 00       - (00001a12-0000-1000-8000-00805f9b34fb)
 * <br>write  	0x003E A0 00 00             - 00001a10-0000-1000-8000-00805f9b34fb
 * <br>notify   0x003E A0 00 00             - 00001a10-0000-1000-8000-00805f9b34fb
 * <br>read	    0x003C -> 00 00 00 00 C6 13 08 00 C8 15 08 00 00 00 00 00 (00001a11-0000-1000-8000-00805f9b34fb)
 * <br>write    0x003E A2 00 00
 * <br>write    0x003F 00 00 				- disable for 0x003e
 * <br>notify   0x0021 00 					- 00001001-0000-1000-8000-00805f9b34fb
 * <br>notify 	0x0035 0D 01 00 97 01 00 00 00 00 00 02 3C 00 FB 34 9B - data
 * <br>write    0x0036 00 00 				- disable for 0x0035
 * <br>write	0x0033 C0 1F 				- set the magic number - different to previous one
 * <br>read	    0x0038 -> 63 27 33 2E 31 2E 38         00001a02-0000-1000-8000-00805f9b34fb
 * <br>write	0x0036 -> 01 00 			- enable notif for data
 * <br>write	0x0033 -> A0 1F 			- magic number again
 * <br>notify   0x0035 0C 01 00 2B 01 00 00 00 00 00 02 3C 00 FB 34 9B - data
 * <br>notify   0x0021 00 					- ??? 00001001-0000-1000-8000-00805f9b34fb
 * <br>notify   0x0035 ...
 * <br>notify   0x0035 ...
 *
 * <p>Unfortunately there is not any easy way to generate security tokens, the Flower Care android mobile app uses
 * a JNI library to generate/encrypt tokens.
 *
 * <p>The authentication process is happening in BleRegisterConnector class (Flower Care app).
 * There are three steps in total (see above). Security tokens are generated by BLECipher class which it turns uses
 * a JNI library "blecipher". It is possible to extract that library from the mobile app
 * (see {@link org.sputnikdev.bluetooth.miot.BLECipher} and {@link #generateToken()}),
 * however it won't work in linux. Looks like android libs are not compatible with other distributions of linux.
 * The following error happens when you try to use it in ubuntu:
 * <p>java.lang.UnsatisfiedLinkError: /home/user/projects/libblecipher.so: /usr/lib/x86_64-linux-gnu/libm.so: invalid ELF header
 *
 * <p>Furthermore, in order to read the data characteristic ("00001a01-0000-1000-8000-00805f9b34fb"), a special magic
 * number ({0xA0, 0x1F}) must be set to a characteristic first ("00001a00-0000-1000-8000-00805f9b34fb").
 *
 * <p>If authentication token is not validated by device, device props connection. However, fortunately it is still
 * possible to quickly read data characteristic before the disconnection.
 *
 * @author Vlad Kolotov
 */
public final class MiFloraSensorWithMiProtocol {

    private static final String DATA_SERVICE = "00001204-0000-1000-8000-00805f9b34fb";
    private static final String MAGIC_NUMBER_CHAR = "00001a00-0000-1000-8000-00805f9b34fb";
    private static final String DATA_CHAR = "00001a01-0000-1000-8000-00805f9b34fb";
    // captured magic number via bluetooth sniffer
    private static final byte[] MAGIC_NUMBER = {(byte) 0xA0, 0x1F};

    private static final String CONTROL_POINT_SERVICE = "0000fe95-0000-1000-8000-00805f9b34fb";
    private static final String CONTROL_POINT_CHAR = "00000010-0000-1000-8000-00805f9b34fb";
    private static final String TOKEN_CHAR = "00000001-0000-1000-8000-00805f9b34fb";
    private static final byte[] SESSION_START = {0x00, (byte) 0xBC, 0x43, (byte) 0xCD};

    // captured token via bluetooth sniffer
    private static final byte[] TOKEN = {0x10, (byte) 0xA1, (byte) 0xFB, (byte) 0xD5};


    private final BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
            .withTinyBTransport(true)
            .withBlueGigaTransport("^*.$")
            .withIgnoreTransportInitErrors(true)
            .withDiscovering(true)
            .withRediscover(true)
            .build();
    private final BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();

    private CharacteristicGovernor magicNumber;
    private CharacteristicGovernor data;

    private CharacteristicGovernor controlPoint;
    private CharacteristicGovernor token;

    private int authenticationStepNumber = 0;

    private final ValueListener valueListener = new ValueListener() {
        @Override
        public void changed(byte[] value) {
            authenticationStepNumber++;
            System.out.println("Token notific: " + Utils.formatHex(value));

            if (authenticationStepNumber == 1) {
                System.out.println("Writing token...");
                if (!token.write(generateToken())) {
                    System.out.println("Could not write token!");
                }
            }
            if (authenticationStepNumber == 2) {
                token.removeValueListener(valueListener);
            }
        }
    };

    /**
     * An entry point for the application.
     * A single argument (device address) is required in the following format:
     * <br>/adapter address/device address
     * //00:1A:7D:DA:71:05
     * <br>E.g.: /88:6B:0F:30:63:AD/C4:7C:8D:66:07:4B
     * @param args a single argument in the following format: /XX:XX:XX:XX:XX:XX/YY:YY:YY:YY:YY:YY
     */
    public static void main(String[] args) throws InterruptedException {
        new MiFloraSensorWithMiProtocol(new URL(args[0])).run();
    }

    private MiFloraSensorWithMiProtocol(URL url) throws InterruptedException {
        controlPoint = bluetoothManager.getCharacteristicGovernor(url.copyWith(CONTROL_POINT_SERVICE, CONTROL_POINT_CHAR));
        magicNumber = bluetoothManager.getCharacteristicGovernor(url.copyWith(DATA_SERVICE, MAGIC_NUMBER_CHAR));
        data = bluetoothManager.getCharacteristicGovernor(url.copyWith(DATA_SERVICE, DATA_CHAR));
        token = bluetoothManager.getCharacteristicGovernor(url.copyWith(CONTROL_POINT_SERVICE, TOKEN_CHAR));
        token.addValueListener(valueListener);
        bluetoothManager.getDeviceGovernor(url).addBluetoothSmartDeviceListener(new BluetoothSmartDeviceListener() {
            @Override
            public void servicesResolved(List<GattService> gattServices) {
                System.out.println("Trying to authenticate...");

                if (controlPoint.write(SESSION_START)) {
                    System.out.println("Session started...");
                    authenticationStepNumber = 0;
                }
                magicNumber.write(MAGIC_NUMBER);
                printAvailableFields(gattServices);
            }

            @Override
            public void connected() {

            }
        });
        bluetoothManager.getDeviceGovernor(url).setConnectionControl(true);
    }

    private void run() throws InterruptedException {
        bluetoothManager.addDeviceDiscoveryListener(device -> {
            System.out.println(device.getURL() + " " + device.getName());
        });
        Thread.currentThread().join();
    }

    private void printAvailableFields(List<GattService> gattService) {
        Utils.printAvailableFields(bluetoothManager, gattParser, gattService);
    }

    private static byte[] fromInt(int i) {
        byte[] result = new byte[4];
        for (int i2 = 0; i2 < 4; i2++) {
            result[i2] = (byte) (i >>> (i2 * 8));
        }
        return result;
    }

    private static byte[] generateToken() {
        long currentTimeMillis = System.currentTimeMillis();
        double randFloat = randFloat();
        return MD5_12(String.format("token.%d.%f", currentTimeMillis, randFloat));
    }

    private static double randFloat() {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        return new Random().nextDouble();
    }

    private static byte[] MD5_12(String str) {
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(str.getBytes(), 0, str.length());
            byte[] digest = instance.digest();
            int length = digest.length;
            if (length >= 12) {
                return Arrays.copyOfRange(digest, (length / 2) - 6, (length / 2) + 6);
            }
            return new byte[0];
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
