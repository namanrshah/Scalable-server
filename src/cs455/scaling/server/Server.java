package cs455.scaling.server;

import cs455.scaling.task.ReadTask;
import cs455.scaling.task.Task;
import cs455.scaling.task.TaskType;
import cs455.scaling.task.WriteTask;
import cs455.scaling.threadpool.ThreadPoolManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class Server {

    private static int listeningPort;
    private Selector selector;
    private ThreadPoolManager threadPoolManager;
    private AtomicLong totalSent;
    private AtomicLong totalReceived;
    private AtomicLong recentSent;
    private AtomicLong recentReceived;
    private List<Task> currentTasks;

    public Server() {
        this.totalSent = new AtomicLong(0);
        this.totalReceived = new AtomicLong(0);
        this.recentSent = new AtomicLong(0);
        this.recentReceived = new AtomicLong(0);
        this.currentTasks = new ArrayList<>();
    }

    public static void main(String[] args) {
        listeningPort = Integer.parseInt(args[0]);
        Server server = new Server();
        server.startServer();
    }

    private void startServer() {
        try {
            System.out.println("-Hostname-" + InetAddress.getLocalHost().getHostName());
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(listeningPort));
            Logger.getLogger(Server.class.getName()).log(Level.INFO, "Server started listening.");

            //Start ThreadPoolManager which will start WorkerThreads
            threadPoolManager = new ThreadPoolManager(10, this);
            threadPoolManager.initializeWorkerThreads();
            Thread threadPoolManagerThread = new Thread(threadPoolManager);
            threadPoolManagerThread.start();

            //Start statistics thread
            StatisticsThreadServer statsObj = new StatisticsThreadServer(this, threadPoolManager, 5000);
            Thread statsThread = new Thread(statsObj);
            statsThread.start();

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext();) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
//                    System.out.println("-ready ops-" + key.readyOps());
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            //code to accept the connection
                            this.acceptConnection(key);
                        }
                        if (key.isReadable()) {
                            //code to read data
                            this.read(key);
                        }
                        if (key.isWritable()) {
                            //code to write data
//                            System.out.println("-It's writable-");
                            WriteTask writeTask = (WriteTask) key.attachment();
                            this.write(key, writeTask);

                        }
                    }
                }
//                Logger.getLogger(Server.class.getName()).log(Level.INFO, "Size-" + (selector.keys().size() - 1));
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Opening server socket channel.", ex);
        }
    }

    public void acceptConnection(SelectionKey key) {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel connectedChannel = null;
        try {
            connectedChannel = channel.accept();
            connectedChannel.configureBlocking(false);
//            Logger.getLogger(Server.class.getName()).log(Level.INFO, "Remote port-" + connectedChannel.socket().getPort());
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.clear();
            connectedChannel.register(selector, SelectionKey.OP_READ, buffer);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Accepting connection.", ex);
        }
    }

    public void read(SelectionKey key) {
        //create a new Read task and submit that to taskQueue
        Task readTask = new ReadTask(TaskType.READ, key);
        synchronized (currentTasks) {
            if (!currentTasks.contains(readTask)) {
                currentTasks.add(readTask);
                threadPoolManager.checkAndAddTask(readTask, null);
            }
        }
        ///////////////////////////////////////////////////        
    }

    public void write(SelectionKey key, WriteTask writeTask) {
        //create a new Write Task and add to the taskQueue of ThreadPoolManager
//        Task writeTask = new WriteTask(TaskType.WRITE, key);
//        threadPoolManager.checkAndAddTask(writeTask, data);
        synchronized (currentTasks) {
            if (!currentTasks.contains(writeTask)) {
                currentTasks.add(writeTask);
                threadPoolManager.addWriteTaskToQueue(writeTask);
            }
        }
        ////////////////////////////////////////////////////////////        
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

    public int getActiveConnections() {
        return this.selector.keys().size() - 1;
    }

    public int getThroughputAndUpdateStatsVariable() {
        long rcvd = this.recentReceived.getAndSet(0);
        long sent = this.recentSent.getAndSet(0);
        this.incrementTotalReceived(rcvd);
        this.incrementTotalSent(sent);
        return (int) (Math.min(sent, rcvd)/5);
//        return (int) ((rcvd + sent) / 2);
    }

    public void removeTask(Task task) {
        synchronized (currentTasks) {
            currentTasks.remove(task);
        }
    }
}
