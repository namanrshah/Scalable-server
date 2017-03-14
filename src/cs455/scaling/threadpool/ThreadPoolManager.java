package cs455.scaling.threadpool;

import cs455.scaling.server.Server;
import cs455.scaling.task.Task;
import cs455.scaling.task.TaskType;
import cs455.scaling.task.WriteTask;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for managing WorkerThreads and assigning Tasks to
 * them This will maintain a Queue of tasks where Server will put the Task with
 * appropriate TaskType and it will assign those Tasks to available Workers.
 *
 * @author namanrs
 */
public class ThreadPoolManager implements Runnable {

    private Queue<Task> taskQueue;
    private Object taskQueueLock;

    private Queue<Worker> workers;
    private Object workerLock;

    private int workerThreads;
    private boolean isWorking = true;

    private Server server;

//    private Queue<> writeDataQueue
    public ThreadPoolManager(int workerThreads, Server server) {
        this.workerThreads = workerThreads;
        this.server = server;
        workers = new LinkedList<>();
        taskQueue = new LinkedList<>();
        taskQueueLock = new Object();
        workerLock = new Object();
    }

    public void initializeWorkerThreads() {
        for (int i = 0; i < workerThreads; i++) {
            Worker worker = new Worker(this, server);
            Thread t = new Thread(worker);
            t.start();
            workers.add(worker);
        }
    }

    @Override
    public void run() {
        while (isWorking) {
            Task task;
            Worker worker;
            //Poll task from taskQueue
            synchronized (taskQueueLock) {
                task = taskQueue.poll();
                if (task == null) {
                    try {
                        taskQueueLock.wait();
                        task = taskQueue.poll();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ThreadPoolManager.class.getName()).log(Level.SEVERE, "-TaskQueue waiting-");
                    }
                }
            }
            //Poll worker instance from WorkerThreads
            synchronized (workerLock) {
                worker = workers.poll();
                if (worker == null) {
                    try {
                        workerLock.wait();
                        worker = workers.poll();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ThreadPoolManager.class.getName()).log(Level.SEVERE, "-WorkersQueue waiting-");
                    }
                }
            }
            //Assign task to Worker
            worker.setTask(task);
        }
    }

    public void stopThreadPoolManager() {
        isWorking = false;
    }

    public void checkAndAddTask(Task task, byte[] data) {
        synchronized (taskQueueLock) {
            if (!taskQueue.contains(task)) {
                addTask(task);
                if (task instanceof WriteTask) {
                    WriteTask writeTask = (WriteTask) task;
                    writeTask.addData(data);
                }
            } else {
                if (task.getType() == TaskType.WRITE) {
                    for (Task taskInQueue : taskQueue) {
                        if (taskInQueue.equals(task)) {
                            WriteTask writeTask = (WriteTask) taskInQueue;
                            writeTask.addData(data);
                        }
                    }
                }
            }
        }
    }

    public void addWriteTaskToQueue(WriteTask writeTask) {
        addTask(writeTask);
    }

    private void addTask(Task task) {
        synchronized (taskQueueLock) {
            taskQueue.add(task);
            taskQueueLock.notify();
        }
    }

    public void submitWorker(Worker worker) {
        synchronized (workerLock) {
            workers.add(worker);
            workerLock.notify();
        }
    }
    
    public int getTaskQueueSize(){
        return taskQueue.size();
    }
    
    public int getFreeWorker(){
        return workers.size();
    }

}
