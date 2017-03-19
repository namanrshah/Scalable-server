package proj.scaling.client;

import proj.scaling.client.Client;
import proj.scaling.util.Constants;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class Writer implements Runnable {

    private SelectionKey key;
    private Client client;
    private boolean isRunning;

    public Writer(SelectionKey key, Client client) {
        this.key = key;
        this.client = client;
        isRunning = true;
    }

    @Override
    public void run() {
        Random rand = new Random();
        while (isRunning) {
            SocketChannel channel = (SocketChannel) key.channel();
//            ByteBuffer buffer = (ByteBuffer) key.attachment();
            byte[] dataToWrite = generateData(rand, Constants.DATA_SIZE);
            try {
                String hash = SHA1FromBytes(dataToWrite);
                client.addHash(hash);
//                System.out.println("-hash-" + hash);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, "-While hashing-");
            }
            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
//                            buffer.position(0);
//                            if(buffer.hasRemaining()){
//                                channel.write(buffer);
//                            }                            
//            buffer.clear();
//            int intToWrite = rand.nextInt();
//            buffer.putInt(intToWrite);
//            buffer.rewind();
            try {
                int write = channel.write(buffer);
                if (write == buffer.position()) {
                    client.incrementRecentSent(1l);
                } else if (write < buffer.position()) {
                    Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, "-Send buffer on client side is full-");
                }
            } catch (IOException ex) {
                Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, "-Writing data from client-");
            }
//            try {
//                //                            System.out.println("-Writing-");
//                Logger.getLogger(Client.class.getName()).log(Level.INFO, "Writing-" + intToWrite + " from " + channel.getLocalAddress());
//            } catch (IOException ex) {
//                Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            buffer.clear();
//                            key.interestOps(0);
//            key.interestOps(SelectionKey.OP_READ);
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static byte[] generateData(Random rng, int length) {
//        long start = System.currentTimeMillis();
        byte[] text = new byte[length];
        for (int i = 0; i < length; i++) {
            text[i] = (byte) rng.nextInt(256);
        }
        long end = System.currentTimeMillis();
//        System.out.println(end - start + "ms");
//        System.out.println(text.length);
        return text;
    }

    public static String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] hash = digest.digest(data);
        BigInteger hashInt = new BigInteger(1, hash);
        return hashInt.toString(16);
    }

    public void stopRunning() {
        isRunning = false;
    }
}
