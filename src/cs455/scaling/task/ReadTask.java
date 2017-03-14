
package cs455.scaling.task;

import java.nio.channels.SelectionKey;

/**
 *
 * @author namanrs
 */
public class ReadTask extends Task{

    public ReadTask(byte type, SelectionKey key) {
        super(type, key);
    }
    
}
