package cs455.scaling.task;

import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 *
 * @author namanrs
 */
public abstract class Task {

    private byte type;
    private SelectionKey key;

    public Task(byte type, SelectionKey key) {
        this.type = type;
        this.key = key;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public SelectionKey getKey() {
        return key;
    }

    public void setKey(SelectionKey key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.type;
        hash = 83 * hash + Objects.hashCode(this.key);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Task other = (Task) obj;
        if (this.type != other.type) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        return true;
    }
    
    

}
