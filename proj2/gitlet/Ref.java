package gitlet;

import java.io.Serializable;

/**
 * Serialized Ref's name is the branch name,like "Master".
 */
public class Ref implements Serializable {

    /* This branch's last commit (master) */
    private String last;

    /* This branch's current commit(head)
    *  If current is null, it means that head is not changed and default to last.
    *  Actually there's no need to do that, the teacher said gitlet would not have detached head state
    * */
    private String current;

    public String getLast() {
        return last;
    }

    public String getCurrent() {
        return current;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public void setCurrent(String current) {
        this.current = current;
    }
}
