package cs455.scaling.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class Client {

    private static int localPort;
    private static Selector selector;
    private AtomicLong totalSent;
    private AtomicLong totalReceived;
    private AtomicLong recentSent;
    private AtomicLong recentReceived;
    private List<String> hashes;

    public Client() {
        this.totalSent = new AtomicLong(0);
        this.totalReceived = new AtomicLong(0);
        this.recentSent = new AtomicLong(0);
        this.recentReceived = new AtomicLong(0);
        this.hashes = new ArrayList<>();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Client client = new Client();
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
            Writer writer = null;
            while (true) {
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
                            Logger.getLogger(Client.class.getName()).log(Level.INFO, "Local port-" + localPort);

                            //start a thread which writes after specific time interval
                            writer = new Writer(key, this);
                            Thread writerThread = new Thread(writer);
                            writerThread.start();
                            //start a thread that periodically prints stats
                            StatisticsThreadClient statsClient = new StatisticsThreadClient(this, 10000);
                            Thread statsThreadClient = new Thread(statsClient);
                            statsThreadClient.start();
//                            synchronized (key) {
                            key.interestOps(SelectionKey.OP_READ);
//                            }
                        }
                        if (key.isReadable()) {
                            //code to read data
                            SocketChannel channel = (SocketChannel) key.channel();
                            ByteBuffer bufferForLength = ByteBuffer.allocate(4);
//                            if (bufferForLength.hasRemaining()) {
////                                Logger.getLogger(Client.class.getName()).log(Level.INFO, "Buffer is not empty while reading. remaining - " + buffer.remaining() + " at " + localPort);
//                            }
                            bufferForLength.clear();
                            int readLength = channel.read(bufferForLength);
                            while (readLength > 0) {
                                int length = ((ByteBuffer) bufferForLength.rewind()).getInt();
//                                System.out.println("-length of received hash-" + length);
                                ByteBuffer buffer = ByteBuffer.allocate(length);
                                int readData = 0;
                                while (buffer.hasRemaining() && readData != -1) {
                                    readData = channel.read(buffer);
                                }
                                if (readData == -1) {
                                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Connection terminated");
                                    Logger.getLogger(Client.class.getName()).log(Level.INFO, "Closing client");
                                    writer.stopRunning();
                                }
                                buffer.rewind();
                                String readHash = new String(buffer.array());
//                                System.out.println("-hash-" + readHash);
                                synchronized (hashes) {
                                    if (hashes.contains(readHash)) {
                                        hashes.remove(readHash);
                                        this.incrementRecentReceived(1l);
                                    } else {
                                        System.out.println("-Doesn't contain-" + readHash);
                                    }
                                }
                                bufferForLength.clear();
                                readLength = channel.read(bufferForLength);
                            }
                            if (readLength == -1) {
                                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "Connection terminated");
                                Logger.getLogger(Client.class.getName()).log(Level.INFO, "Closing client");
                                writer.stopRunning();
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, "-Connecting to server channel-", ex);
        }
    }

    public void incrementRecentSent(long val) {
        this.recentSent.addAndGet(val);
    }

    public void incrementRecentReceived(long val) {
        this.recentReceived.addAndGet(val);
    }

    private void incrementTotalSent(long val) {
        this.totalSent.addAndGet(val);
    }

    private void incrementTotalReceived(long val) {
        this.totalReceived.addAndGet(val);
    }

    public int getRecentSentResetAndupdateTotal() {
        long sent = this.recentSent.getAndSet(0);
        this.incrementTotalSent(sent);
        return (int) sent;
    }

    public int getRecentReceivedResetAndUpdateTotal() {
        long rcvd = this.recentReceived.getAndSet(0);
        this.incrementTotalReceived(rcvd);
        return (int) rcvd;
    }

    public void addHash(String hash) {
        synchronized (hashes) {
            hashes.add(hash);
        }
    }

}
