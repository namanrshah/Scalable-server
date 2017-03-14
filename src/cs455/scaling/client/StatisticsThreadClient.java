package cs455.scaling.client;

import cs455.scaling.server.*;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author namanrs
 */
public class StatisticsThreadClient implements Runnable {

    private Client client;
    private long timeoutInMilli;
    private boolean isRunning;

    public StatisticsThreadClient(Client client, long timeoutInMilli) {
        this.client = client;
        this.timeoutInMilli = timeoutInMilli;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(timeoutInMilli);
            } catch (InterruptedException ex) {
                Logger.getLogger(StatisticsThreadClient.class.getName()).log(Level.SEVERE, "-Interrupted Thread.sleep()-");
            }
            System.out.println("[" + new Timestamp(System.currentTimeMillis()) + "]" + " Total Sent Count: " + client.getRecentSentResetAndupdateTotal() + " Total Received Count: " + client.getRecentReceivedResetAndUpdateTotal());            
        }
    }

    public void stopRunning() {
        isRunning = false;
    }

}
