package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/** Represents a gitlet commit object which stores every commit info
 *  includes: id, message, author, time and parent Ids and bulbs of this commit
 *  Because time(date) + msg is unique, use it to sha1
 *  Why not just date? Because when commits in testing are so quick, they may be at the same time
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**The id of this Commit*/

    private String message;

    /** The date when this Commit was created. */
    private Date date;

    /** the first parentID */
    private String xParent;

    /** the second parentID */
    private String yParent;

    /**
     * key: file's name
     * value: blob's name (sha1)
     * HashMap is good here because you can easily add file or refresh(just as add)
     * */
    private HashMap<String, String> blobs;

    private HashMap<String, String> removalBlobs;

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setxParent(String xParent) {
        this.xParent = xParent;
    }

    public void setyParent(String yParent) {
        this.yParent = yParent;
    }

    public void setBlobs(HashMap<String, String> blobs) {
        this.blobs = blobs;
    }
    public void setRemovalBlobs(HashMap<String, String> removalBlobs) {
        this.removalBlobs = removalBlobs;
    }

    public String getMessage() {
        return message;
    }

    public Date getDate() {
        return date;
    }


    public String getxParent() {
        return xParent;
    }

    public String getyParent() {
        return yParent;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }
    public HashMap<String, String> getRemovalBlobs() {
        return removalBlobs;
    }
}
