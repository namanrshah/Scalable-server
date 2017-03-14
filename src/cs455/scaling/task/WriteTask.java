package cs455.scaling.task;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author namanrs
 */
public class WriteTask extends Task {

    private Queue<byte[]> dataToWrite;

    public WriteTask(byte type, SelectionKey key) {
        super(type, key);
        dataToWrite = new LinkedList<>();
    }

    public boolean addData(byte[] data) {
        try {
            dataToWrite.add(data);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public Queue<byte[]> getDataToWrite() {
        return dataToWrite;
    }

    public void setDataToWrite(Queue<byte[]> dataToWrite) {
        this.dataToWrite = dataToWrite;
    }

}
