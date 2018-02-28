package chat.tox.antox.wrapper;


import chat.tox.antox.utils.HexJ;

/**
 * Created by Nechypurenko on 05.02.2018.
 */

public class ToxAddressJ {

    private final String fixedAddress;

    public ToxAddressJ(String address) {
        this.fixedAddress = removePrefix(address.toUpperCase());
        if (!isAddressValid(fixedAddress)) {
            throw new IllegalArgumentException("address must be $ToxAddress.MAX_ADDRESS_LENGTH hex chars long");
        }
    }

    public ToxAddressJ(byte[] bytes) {
        this(HexJ.bytesToHexString(bytes));
    }

    @Override
    public String toString() {
        return fixedAddress;
    }

    public byte[] bytes() {
        return HexJ.hexStringToBytes(fixedAddress);
    }

//    public FriendKey key() {
//        return new FriendKey(fixedAddress.substring(0, ToxKey..MODULE$.MAX_KEY_LENGTH()));
//    }

    private static final int MAX_ADDRESS_LENGTH = 76;

    //test value
    //1D407BCA3614E144A624C993842D6EFAB72682015E4D8DDF0C8C8D76C2A8470DA2CCD1EDA571
    public static boolean isAddressValid(String address) {
        if (address.length() == MAX_ADDRESS_LENGTH && address.matches("^[0-9A-F]+$")) {
            String[] data = address.split("(?<=\\G.{4})");
            int[] values = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                values[i] = Integer.parseInt(data[i], 16);
            }
            int res = values[0];
            for (int i = 1; i < values.length; i++) {
                res = res ^ values[i];
            }
            return res == 0;
        }
        return false;
    }

    public static String removePrefix(String address) {
        String prefix = "tox:";
        if (address.toLowerCase().contains(prefix)) {
            return address.substring(prefix.length());
        } else {
            return address;
        }
    }

}
