package proj.scaling.server;

import proj.scaling.threadpool.ThreadPoolManager;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class StatisticsThreadServer implements Runnable {

    private Server server;
    private ThreadPoolManager threadPoolManager;
    private long timeoutInMilli;
    private boolean isRunning;

    public StatisticsThreadServer(Server server, ThreadPoolManager threadPoolManager, long timeoutInMilli) {
        this.server = server;
        this.threadPoolManager = threadPoolManager;
        this.timeoutInMilli = timeoutInMilli;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(timeoutInMilli);
            } catch (InterruptedException ex) {
                Logger.getLogger(StatisticsThreadServer.class.getName()).log(Level.SEVERE, "-Interrupted Thread.sleep()-");
            }
            System.out.println("[" + new Timestamp(System.currentTimeMillis()) + "]" + " Current Server Throughput: " + server.getThroughputAndUpdateStatsVariable() + " messages/s, Active Client Connections: " + server.getActiveConnections() + ", TaskQueue: " + threadPoolManager.getTaskQueueSize() + ", Workers: " + threadPoolManager.getFreeWorker());
        }
    }

    public void stopRunning() {
        isRunning = false;
    }

}
