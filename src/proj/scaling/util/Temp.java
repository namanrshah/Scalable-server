package proj.scaling.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 *
 * @author namanrs
 */
public class Temp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
//        ByteBuffer bf = ByteBuffer.allocate(10);
//        bf.putInt(26);
//        bf.position(0);
//        bf = (ByteBuffer) ByteBuffer.allocate(4 + bf.capacity()).putInt(45).put(bf.slice()).position(0);
////        bf.putInt(0, 45);        
//        System.out.println(bf.getInt());
//        System.out.println(bf.getInt());
//        System.out.println(bf.capacity());
//        System.out.println(bf.limit());
//        bf = ByteBuffer.allocate(4);
//        bf.clear();
//        for (int i = 0; i < 100; i++) {
//            int nextInt = new Random().nextInt();
//            DataOutputStream dout = new DataOutputStream(null)
        Random random = new Random();
        System.out.println(generateData(random, 8000));
//        }
    }

    public static byte[] generateData(Random rng, int length) {
        long start = System.currentTimeMillis();
        byte[] text = new byte[length];
        for (int i = 0; i < length; i++) {
            text[i] = (byte) rng.nextInt(256);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start + "ms");
        System.out.println(text.length);
        return text;
    }
}
