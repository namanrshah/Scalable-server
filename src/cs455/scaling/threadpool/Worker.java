package cs455.scaling.threadpool;

import cs455.scaling.client.Writer;
import cs455.scaling.server.Server;
import cs455.scaling.task.Task;
import cs455.scaling.task.TaskType;
import cs455.scaling.task.WriteTask;
import cs455.scaling.util.Constants;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the actual worker/Task executor, which performs tasks There will be
 * limited number of objects of these
 *
 * @author namanrs
 */
public class Worker implements Runnable {

    private Task taskToExecute;
    private ThreadPoolManager manager;
    private boolean taskIsChanged;
    private Server server;

    public Worker(ThreadPoolManager manager, Server server) {
        this.manager = manager;
        this.server = server;
    }

    @Override
    public void run() {
        synchronized (this) {
            while (true) {
                try {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, "-Worker waiting-");
                    }
                    if (taskToExecute != null && taskIsChanged) {
                        if (taskToExecute.getType() == TaskType.READ) {
                            //Perform read task
                            //Read everything from channel
                            SelectionKey key = taskToExecute.getKey();
                            SocketChannel channel = (SocketChannel) key.channel();
                            try {
//                                while (true) {
                                ByteBuffer buffer = ByteBuffer.allocate(Constants.DATA_SIZE);
//                                    buffer.clear();
                                int read = 0;
                                while (buffer.hasRemaining() && read != -1) {
                                    read = channel.read(buffer);
//                                        if (read > 0) {
//                                            System.out.println(read + "-" + Thread.currentThread().getName());
//                                        }
                                }
//                                Logger.getLogger(Server.class.getName()).log(Level.INFO, "Bytes read-" + read);
                                if (read > 0) {
//                                        buffer.rewind();
                                    buffer.position(0);
//                                    Logger.getLogger(Server.class.getName()).log(Level.INFO, "Data read-" + buffer.getInt());
//                                    buffer.rewind();
                                    //add write task to taskqueue
//                                    Logger.getLogger(Server.class.getName()).log(Level.INFO, "-Adding write task to queue-" + key.toString());                                    
                                    String hash = Writer.SHA1FromBytes(buffer.array());
//                                        System.out.println("-hash-" + hash + "-" + Thread.currentThread().getName());
                                    Task writeTask = new WriteTask(TaskType.WRITE, key);
                                    manager.checkAndAddTask(writeTask, hash.getBytes());
                                    server.incrementRecentReceived(1l);
//                                    int write = channel.write(buffer);
//                                    Logger.getLogger(Server.class.getName()).log(Level.INFO, "Writing-");
//                                    if (write < read) {
//                                        //Send buffer is full, so wait for write
//                                        Logger.getLogger(Server.class.getName()).log(Level.INFO, "Send buffer is full; Hung until we can write; buffer position-" + buffer.position());
//                                        key.interestOps(SelectionKey.OP_WRITE);
//                                    } else {
//                                        buffer.clear();
//                                    }
                                } else if (read == -1) {
                                    synchronized (key) {
                                        key.cancel();
                                        channel.close();
                                        break;
                                    }
//                                } else {
//                                    break;
                                }
//                                }
//                                System.out.println("-breaking out of while true-");
                            } catch (IOException ex) {
                                synchronized (key) {
                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Exception while reading");
                                    key.cancel();
                                    try {
                                        channel.close();
                                    } catch (IOException ex1) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Closign channel");
                                    }
                                }
                            }
//        key.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            SelectionKey key = taskToExecute.getKey();
//                        Logger.getLogger(Server.class.getName()).log(Level.INFO, "Queue is executing write task with key-" + key.toString());
                            //Perform write task
//                            synchronized (key) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            WriteTask writeTask = (WriteTask) taskToExecute;
                            Queue<byte[]> dataToWrite = writeTask.getDataToWrite();
//                                System.out.println("-hashes to write-" + dataToWrite.size());
                            byte[] tempBuff;
                            while ((tempBuff = dataToWrite.poll()) != null) {
//                                    System.out.println("-length to write-" + tempBuff.length);
                                ByteBuffer bufferForLength = ByteBuffer.allocate(4);
                                bufferForLength.putInt(tempBuff.length);
                                bufferForLength.rewind();
                                channel.write(bufferForLength);
                                ByteBuffer buffer = ByteBuffer.wrap(tempBuff);
                                int supposedBytesToWrite = buffer.remaining();
                                if (buffer.hasRemaining()) {
                                    try {
                                        //Issue
                                        int write = channel.write(buffer);
//                                    Logger.getLogger(Server.class.getName()).log(Level.INFO, "-Supposed to write-" + supposedBytesToWrite + "-Wrote-" + write);
                                        if (write < supposedBytesToWrite) {
                                            //Couldn't write because send-buffer is full; again add this task to taskQueue
                                            //adding lastly polled byte[] before assing task
//                                        Logger.getLogger(Server.class.getName()).log(Level.INFO, "-Adding write task to queue due to no space in SendBuffer for key-" + key.toString());
                                            key.interestOps(SelectionKey.OP_WRITE);
                                            dataToWrite.add(tempBuff);
                                            key.attach(writeTask);
//                                        manager.addWriteTaskToQueue(writeTask);
                                        } else {
                                            server.incrementRecentSent(1l);
                                        }
                                    } catch (IOException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "-Writing data to client-", ex);
                                    }
//            key.interestOps(SelectionKey.OP_READ);
                                } else {
//            System.out.println("-Set reading-");
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                                buffer.clear();
                            }
                        }
                    }
//                        System.out.println("-Complete operation on-" + taskToExecute.getKey() + "-type-" + taskToExecute.getType());
                    server.removeTask(taskToExecute);
                    taskIsChanged = false;
//                    }
                } catch (Exception e) {

                } finally {
                    manager.submitWorker(this);
                }
            }
        }
    }

    public synchronized void setTask(Task task) {
        taskToExecute = task;
        taskIsChanged = true;
        this.notify();
    }
}
