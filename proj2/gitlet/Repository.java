package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  This is where the init(create folders) and middle level logic of git commands
 *  and will call the Commit.Class and Blob.Class to do the low level(detailed) logic of commands
 *
 *
 *  The structure of a  Repository is as follows:
 *  .gitlet/ -- top level folder for all persistent data
 *      - staging area/ -- folder containing current staged files which are represented by blobs
 *                          When files(blobs) are in this area, it means that they are tracked by gitlet and
 *                          prepare to be committed
 *      - commits/ -- folder containing all the serialized commits, file name is sha1
 *      - blobs/   -- folder containing all the serialized blobs, file name is sha1
 *      - refs/     --folder containing refs, file name is branch name, fields are blobs name(sha1)
 *      - currentBranch/ --folder containing just one file, file name is current working branch,nothing else here
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");

    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    public static final File REF_DIR = join(GITLET_DIR, "refs");

    /*The file name in this folder denotes current branch*/
    public static final File CurrentBranch = join(GITLET_DIR, "currentBranch");

    /** Generate persistent blobs and if the blob's name(sha1) is new, add it to blobs dir and staging dir.
     *  User sha1(content) as the blob's name
     */
    public static void add(String filename) {
        if (!BLOBS_DIR.exists()) {
            BLOBS_DIR.mkdir();
        }
        File filepath = Utils.join(CWD, filename);
        String fileContent = Utils.readContentsAsString(filepath);
        String sha1Code = sha1(fileContent);

        File blobPath = Utils.join(BLOBS_DIR, sha1Code);
        if (!blobPath.exists()) {
            Blob blob = new Blob(filename, fileContent);
            Utils.writeObject(blobPath, blob);
            File stagePath = Utils.join(STAGING_DIR, sha1Code);
            Utils.writeObject(stagePath, blob);
        }
    }

    public static void init() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        STAGING_DIR.mkdir();
        REF_DIR.mkdir();
        CurrentBranch.mkdir();
        Utils.writeContents(Utils.join(CurrentBranch, "master"), "");
        Ref firstRef = new Ref();
        Utils.writeObject(Utils.join(REF_DIR, "master"), firstRef);
        commit("initial commit");
    }

    /** Generate persistent commits.
     *  User sha1(date) as the commit's name because date is unique
     *  Every commit will read last commit's blobs which is HashMap
     *  Add the staging blobs to the map
     */
    public static void commit(String msg) {
        Commit commit = new Commit();
        commit.setMessage(msg);
        String currentBranch = Utils.plainFilenamesIn(CurrentBranch).get(0);
        Ref ref = Utils.readObject(Utils.join(REF_DIR, currentBranch), Ref.class);
        if (Utils.plainFilenamesIn(COMMITS_DIR).size() == 0) {
            commit.setDate(new Date(0L));
        } else {
            commit.setDate(new Date());
            String lastCommitSha1 = ref.getLast();
            HashMap<String, String> bList;
            //System.out.println(lastCommitSha1);
            Commit lastCommit = Utils.readObject(Utils.join(COMMITS_DIR, lastCommitSha1), Commit.class);
            bList = lastCommit.getBlobs();
            if (bList == null) {
                bList = new HashMap<>();
            }

            if (Utils.plainFilenamesIn(STAGING_DIR).size() == 0) {
                System.out.println(Utils.error("No changes added to the commit."));
                System.exit(0);
            }
            for (String blobSha1 : Utils.plainFilenamesIn(STAGING_DIR)) {
                Blob b = Utils.readObject(Utils.join(STAGING_DIR, blobSha1), Blob.class);
                bList.put(b.getFileName(), blobSha1);
                //System.out.println("This bulbs in staging is committing: " + blobSha1);

            }


            clearStaging();
            commit.setBlobs(bList);
            commit.setxParent(ref.getLast());
        }
        //System.out.println("commit date: " + dateToString(commit.getDate()));
        //System.out.println("commit msg: " + commit.getMessage());
        String sha1Code = Utils.sha1(dateToString(commit.getDate()));
        File commitPath = Utils.join(COMMITS_DIR, sha1Code);
        Utils.writeObject(commitPath, commit);

        /*update the ref*/
        ref.setLast(sha1Code);
        Utils.writeObject(Utils.join(REF_DIR, currentBranch), ref);

    }

    /*get the current branch name in currentBranch, file's name in it tells you
    * get the branch in refs, it tells you last commit's name
    * get the last commit in commits, it tells blobs' name it has
    * get the blob in blobs, read it and write it to CWD
    */
    public static void checkoutFile1(String file) {

        List<String> nameOfBranches = Utils.plainFilenamesIn(CurrentBranch);
        String shaIdOfCommit = findCommit(nameOfBranches.get(0));
        String shaIdOfBlob = findBlobInCommit(shaIdOfCommit, file);
        writeBlobToCWD(shaIdOfBlob);
    }

    public static void checkoutFile2(String commitId, String file) {
        String shaIdOfBlob = findBlobInCommit(commitId, file);
        writeBlobToCWD(shaIdOfBlob);
    }

    public static void checkoutBranch(String branchName) {
        List<String> nameOfBranches = Utils.plainFilenamesIn(CurrentBranch);
        if (nameOfBranches.get(0).equals(branchName)) {
           System.out.println(Utils.error("No need to checkout the current branch."));
           System.exit(0);
        } else {
           clearStaging();
           String oldBranchName = Utils.plainFilenamesIn(CurrentBranch).get(0);
           String oldShaIdOfCommit = findCommit(oldBranchName);
           Commit oldCommit = Utils.readObject(Utils.join(COMMITS_DIR, oldShaIdOfCommit), Commit.class);
           for (String b : oldCommit.getBlobs().keySet()) {
                Utils.restrictedDelete(b);
           }

           String newShaIdOfCommit = findCommit(branchName);
           Commit commit = Utils.readObject(Utils.join(COMMITS_DIR, newShaIdOfCommit), Commit.class);
           for (String b : commit.getBlobs().values()) {
               if (Utils.join(CWD, b).exists()) {
                   System.out.println(Utils.error("There is an untracked file in the way; " +
                           "delete it, or add and commit it first."));
                   System.exit(0);
               } else {
                   writeBlobToCWD(b);
               }
           }

           changeCurrentBranch(branchName);
        }
    }

    public static void log() {
        String currentBranch = Utils.plainFilenamesIn(CurrentBranch).get(0);
        String currentCommitShaId = findCommit(currentBranch);
        while (currentCommitShaId != null) {
            Commit currentCommit = readObject(Utils.join(COMMITS_DIR, currentCommitShaId), Commit.class);

            System.out.println("===");
            System.out.println("commit " + currentCommitShaId);
            System.out.println("Date: " + dateToString(currentCommit.getDate()));
            System.out.println(currentCommit.getMessage());
            System.out.println();

            currentCommitShaId = currentCommit.getxParent();
        }
    }
    private static void clearStaging() {
        for (String blobSha1 : Utils.plainFilenamesIn(STAGING_DIR)) {
            Boolean b = Utils.join(STAGING_DIR,blobSha1).delete();
            //System.out.println("This bulbs in staging is deleting: " +  b);
        }
    }

    private static void  changeCurrentBranch(String newBranch) {
        for (String branch : Utils.plainFilenamesIn(CurrentBranch)) {
            Utils.join(CurrentBranch, branch).delete();
        }
        Utils.writeContents(Utils.join(CurrentBranch, newBranch), "");
    }
    private static void writeBlobToCWD (String shaIdOfBlob) {
        if (shaIdOfBlob != null) {
            Blob blob = Utils.readObject(Utils.join(BLOBS_DIR, shaIdOfBlob), Blob.class);
            File checkoutFile = new File(CWD, blob.getFileName());
            Utils.writeContents(checkoutFile, blob.getFileContent());
        } else {
            System.out.println(Utils.error("File does not exist in that commit."));
            System.exit(0);
        }
    }

    private static String findCommit(String BranchName) {
        List<String> nameOfRefs = Utils.plainFilenamesIn(REF_DIR);
        for (String nameOfRef : nameOfRefs) {
            if (nameOfRef.equals(BranchName)) {
                Ref ref = Utils.readObject(Utils.join(REF_DIR, nameOfRef), Ref.class);
                if (!(ref.getCurrent() == null)) {
                    return ref.getCurrent();
                } else {
                   return ref.getLast();
                }
            }
        }
        System.out.println(Utils.error("No such branch exists."));
        System.exit(0);
        return null;
    }

    private static String findBlobInCommit(String ShaIdOfCommit, String fileName) {
        List<String> shaIdOfCommits = Utils.plainFilenamesIn(COMMITS_DIR);
        if (ShaIdOfCommit != null) {
            for (String shaId : shaIdOfCommits) {
                if (shaId.equals(ShaIdOfCommit)) {
                    Commit commit = Utils.readObject(Utils.join(COMMITS_DIR, shaId), Commit.class);
                    HashMap<String, String> blobs = commit.getBlobs();
                    return blobs.get(fileName);
                }
            }
        }
        System.out.println(Utils.error("No commit with that id exists"));
        System.exit(0);
        return null;
    }


    private static String dateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);
        return  dateFormat.format(date);
    }
}
