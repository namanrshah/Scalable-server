//package cs455.scaling.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class EchoClient {

    private static int localPort;
    private static Selector selector;
    public static final int DATA_SIZE = 8192;

    public EchoClient() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        EchoClient client = new EchoClient();
        client.startProcessing(args);
    }

    private void startProcessing(String[] args) {
        try {
            // TODO code application logic here
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
//            socketChannel.connect(new InetSocketAddress(args[0], Integer.parseInt(args[1])));
            //Selector
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(args[0], Integer.parseInt(args[1])));
            Random rand = new Random();
//            Writer writer = null;
            int i = 0;
            while (i < 10) {
                ++i;
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
//                Logger.getLogger(Server.class.getName()).log(Level.INFO, "Size-" + selectedKeys.size());
                for (Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext();) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            channel.finishConnect();
                            localPort = channel.socket().getLocalPort();
                            Logger.getLogger(EchoClient.class.getName()).log(Level.INFO, "Local port-" + localPort);

                            //start a thread which writes after specific time interval
//                            writer = new Writer(key, this);
//                            Thread writerThread = new Thread(writer);
//                            writerThread.start();
//                            //start a thread that periodically prints stats
//                            StatisticsThreadClient statsClient = new StatisticsThreadClient(this, 10000);
//                            Thread statsThreadClient = new Thread(statsClient);
//                            statsThreadClient.start();
                            byte[] dataToWrite = generateData(rand, DATA_SIZE);
                            try {
                                String hash = SHA1FromBytes(dataToWrite);
                                System.out.println("-Sending hash-" + hash);
//                System.out.println("-hash-" + hash);
                            } catch (NoSuchAlgorithmException ex) {
                                //Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, "-While hashing-");
                            }
                            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
                            int write = channel.write(buffer);
//                            synchronized (key) {
                            key.interestOps(SelectionKey.OP_READ);
//                            }
                        }
                        if (key.isReadable()) {
                            //code to read data
                            SocketChannel channel = (SocketChannel) key.channel();
//                            ByteBuffer bufferForLength = ByteBuffer.allocate(4);
////                            if (bufferForLength.hasRemaining()) {
//////                                Logger.getLogger(Client.class.getName()).log(Level.INFO, "Buffer is not empty while reading. remaining - " + buffer.remaining() + " at " + localPort);
////                            }
//                            bufferForLength.clear();
//                            int readLength = channel.read(bufferForLength);
//                            while (readLength > 0) {
//                                int length = ((ByteBuffer) bufferForLength.rewind()).getInt();
//                                System.out.println("-length of received hash-" + length);
//                                ByteBuffer buffer = ByteBuffer.allocate(length);
                            ByteBuffer buffer = ByteBuffer.allocate(40);
                            int readData = 0;
                            while (buffer.hasRemaining() && readData != -1) {
                                readData = channel.read(buffer);
                            }
                            if (readData == -1) {
                                Logger.getLogger(EchoClient.class.getName()).log(Level.SEVERE, "Connection terminated");
                                Logger.getLogger(EchoClient.class.getName()).log(Level.INFO, "Closing client");
//                                writer.stopRunning();
                            }
                            buffer.rewind();
                            String readHash = new String(buffer.array());
                            System.out.println("-Received hash-" + readHash);
//                                synchronized (hashes) {
//                                    if (hashes.contains(readHash)) {
//                                        hashes.remove(readHash);
//                                        this.incrementRecentReceived(1l);
//                                    } else {
//                                        System.out.println("-Doesn't contain-" + readHash);
//                                    }
//                                }
//                                bufferForLength.clear();
//                                readLength = channel.read(bufferForLength);
//                            }
//                            if (readLength == -1) {
//                                Logger.getLogger(EchoClient.class.getName()).log(Level.SEVERE, "Connection terminated");
//                                Logger.getLogger(EchoClient.class.getName()).log(Level.INFO, "Closing client");
//                                writer.stopRunning();
//                            }

                            //Writing data                            
                            byte[] dataToWrite = generateData(rand, DATA_SIZE);
                            try {
                                String hash = SHA1FromBytes(dataToWrite);
                                System.out.println("-Sending hash-" + hash);
//                System.out.println("-hash-" + hash);
                            } catch (NoSuchAlgorithmException ex) {
                                //Logger.getLogger(Writer.class.getName()).log(Level.SEVERE, "-While hashing-");
                            }
                            ByteBuffer bufferW = ByteBuffer.wrap(dataToWrite);
                            int write = channel.write(bufferW);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(EchoClient.class.getName()).log(Level.SEVERE, "-Connecting to server channel-", ex);
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

}
