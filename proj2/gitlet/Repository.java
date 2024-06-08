package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  This is where the init(create folders) and middle level logic of git commands
 *  and will call the Commit.Class and Blob.Class to do the low level(detailed) logic of commands
 *
 *  The structure of a  Repository is as follows:
 *  .gitlet/ -- top level folder for all persistent data
 *      - staging area/ -- folder containing current staged files which are represented by blobs
 *                          When files(blobs) are in this area, it means that they are tracked by gitlet and
 *                          prepare to be committed
 *      - removal area/ --folder containing gitlet rm files(blobs) just like staging area which temporary records
 *                        operations
 *      - commits/ -- folder containing all the serialized commits, file name is sha1
 *      - blobs/   -- folder containing all the serialized blobs, file name is sha1
 *      - refs/     --folder containing refs, file name is branch name, fields are blobs name(sha1)
 *      - currentBranch/ --folder containing just one file, file name is current working branch,nothing else here
 */
public class Repository {
    /*
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
    public static final File REMOVAL_DIR = join(GITLET_DIR, "removal");

    public static final File REF_DIR = join(GITLET_DIR, "refs");

    /*The file name in this folder denotes current branch*/
    public static final File CURRENT_BRANCH = join(GITLET_DIR, "currentBranch");

    /** Generate persistent blobs and if the blob's name(sha1) is new, add it to blobs dir and staging dir.
     *  User sha1(content) as the blob's name
     */
    public static void add(String filename) {
        if (!BLOBS_DIR.exists()) {
            BLOBS_DIR.mkdir();
        }

        if (!join(CWD, filename).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        addFromRemoval(filename);

        File filepath = Utils.join(CWD, filename);
        String fileContent = Utils.readContentsAsString(filepath);
        String sha1Code = sha1(fileContent, filename);

        File blobPath = Utils.join(BLOBS_DIR, sha1Code);
        if (!blobPath.exists()) {
            //only new blob will create because olds have been all recorded
            Blob blob = new Blob(filename, fileContent);
            Utils.writeObject(blobPath, blob);
            File stagePath = Utils.join(STAGING_DIR, sha1Code);
            Utils.writeObject(stagePath, blob);
        }
    }

    /*see rm function     */
    private static void addFromRemoval(String filename) {
        String blobToRestoreId = null;

        for (String removalBlob : Utils.plainFilenamesIn(REMOVAL_DIR)) {
            Blob b = Utils.readObject(Utils.join(BLOBS_DIR, removalBlob), Blob.class);
            if (b.getFileName().equals(filename)) {
                //Utils.writeContents(Utils.join(CWD, filename), b.getFileContent());
                blobToRestoreId = removalBlob;
                break;
            }
        }
        if (blobToRestoreId != null) {
            Utils.join(REMOVAL_DIR, blobToRestoreId).delete();
        }
    }


    public static void init() {
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        STAGING_DIR.mkdir();
        REMOVAL_DIR.mkdir();
        REF_DIR.mkdir();
        CURRENT_BRANCH.mkdir();
        Utils.writeContents(Utils.join(CURRENT_BRANCH, "master"), "");
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
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Commit commit = new Commit();
        commit.setMessage(msg);
        String currentBranch = Utils.plainFilenamesIn(CURRENT_BRANCH ).get(0);
        Ref ref = Utils.readObject(Utils.join(REF_DIR, currentBranch), Ref.class);
        if (Utils.plainFilenamesIn(COMMITS_DIR).size() == 0) {
            commit.setDate(new Date(0L));
        } else {
            commit.setDate(new Date());
            String lastCommitSha1 = ref.getLast();
            HashMap<String, String> bList;
            HashMap<String, String> bRevomalList;
            //System.out.println(lastCommitSha1);
            Commit lastCommit = Utils.readObject(Utils.join(COMMITS_DIR, lastCommitSha1), Commit.class);
            bList = lastCommit.getBlobs();
            bRevomalList = lastCommit.getRemovalBlobs();

            if (Utils.plainFilenamesIn(STAGING_DIR).size() == 0 && Utils.plainFilenamesIn(REMOVAL_DIR).size() == 0) {
                System.out.println("No changes added to the commit.");
                System.exit(0);
            }

            if (bList == null) {
                bList = new HashMap<>();
            }
            for (String blobSha1 : Utils.plainFilenamesIn(STAGING_DIR)) {
                Blob b = Utils.readObject(Utils.join(STAGING_DIR, blobSha1), Blob.class);
                bList.put(b.getFileName(), blobSha1);
                //System.out.println("This bulbs in staging is committing: " + blobSha1);

            }

            if (bRevomalList == null) {
                bRevomalList = new HashMap<>();
            }
            for (String blobSha1 : Utils.plainFilenamesIn(REMOVAL_DIR)) {
                Blob b = Utils.readObject(Utils.join(REMOVAL_DIR, blobSha1), Blob.class);
                bRevomalList.put(b.getFileName(), blobSha1);
                bList.remove(b.getFileName());
            }

            clearStaging();
            commit.setBlobs(bList);
            commit.setRemovalBlobs(bRevomalList);
            commit.setxParent(ref.getLast());
        }
        //System.out.println("commit date: " + dateToString(commit.getDate()));
        //System.out.println("commit msg: " + commit.getMessage());
        String sha1Code = Utils.sha1(dateToString(commit.getDate()), commit.getMessage());
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

        List<String> nameOfBranches = Utils.plainFilenamesIn(CURRENT_BRANCH );
        String shaIdOfCommit = findCommit(nameOfBranches.get(0));
        String shaIdOfBlob = findBlobInCommit(shaIdOfCommit, file);
        writeBlobToCWD(shaIdOfBlob);
    }

    public static void checkoutFile2(String commitId, String file) {
        commitId = findFullId(commitId);
        if (commitId == null || !join(COMMITS_DIR, commitId).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        String shaIdOfBlob = findBlobInCommit(commitId, file);
        writeBlobToCWD(shaIdOfBlob);
    }

    private static String findFullId(String shortId) {
        for (String id : plainFilenamesIn(COMMITS_DIR)) {
            if (id.contains(shortId)) {
                return id;
            }
        }
        return null;
    }

    public static void checkoutBranch(String branchName) {
        if (!join(REF_DIR, branchName).exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        List<String> nameOfBranches = Utils.plainFilenamesIn(CURRENT_BRANCH );
        if (nameOfBranches.get(0).equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        } else {
            clearStaging();
            String oldShaIdOfCommit = findCommit(nameOfBranches.get(0));
            Commit oldCommit = Utils.readObject(Utils.join(COMMITS_DIR, oldShaIdOfCommit), Commit.class);
            if (oldCommit.getBlobs() != null) { //check if it is init
                for (String fileName : oldCommit.getBlobs().keySet()) {
                    Utils.restrictedDelete(fileName);
                }
            }

            String newShaIdOfCommit = findCommit(branchName);
            Commit commit = Utils.readObject(Utils.join(COMMITS_DIR, newShaIdOfCommit), Commit.class);
            if (commit.getBlobs() != null) { // check if it is init
                for (String fileName : commit.getBlobs().keySet()) {
                    if (Utils.join(CWD, fileName).exists()) {
                        System.out.println("There is an untracked file in the way; "
                                + "delete it, or add and commit it first.");
                        System.exit(0);
                    } else {
                        writeBlobToCWD(commit.getBlobs().get(fileName));
                    }
                }
            }

            changeCurrentBranch(branchName);
        }
    }

    public static void log() {
        String currentBranch = Utils.plainFilenamesIn(CURRENT_BRANCH).get(0);
        String currentCommitShaId = findCommit(currentBranch);

        while (currentCommitShaId != null) {
            Commit currentCommit = readObject(Utils.join(COMMITS_DIR, currentCommitShaId), Commit.class);
            printCommit(currentCommit, currentCommitShaId);
            currentCommitShaId = currentCommit.getxParent();
        }
    }

    //not sure print all commits or print all commits of master
    public static void globalLog() {
        //print all commits

        for (String commitShaId : Utils.plainFilenamesIn(COMMITS_DIR)) {
            Commit currentCommit = readObject(Utils.join(COMMITS_DIR, commitShaId), Commit.class);
            printCommit(currentCommit, commitShaId);
        }


        //print all commits of master
        /*
        Ref ref = Utils.readObject(Utils.join(REF_DIR, "master"), Ref.class);

        String currentCommitShaId = ref.getLast();

        while (currentCommitShaId != null) {
            Commit currentCommit = readObject(Utils.join(COMMITS_DIR, currentCommitShaId), Commit.class);
            printCommit(currentCommit, currentCommitShaId);
            currentCommitShaId = currentCommit.getxParent();
        }
        */

    }

    public static void branch(String branchName) {
        if (join(REF_DIR, branchName).exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String currentBranch = plainFilenamesIn(CURRENT_BRANCH).get(0);
        String currentCommitId = findCommit(currentBranch);
        Ref ref = new Ref();
        ref.setLast(currentCommitId);
        writeObject(join(REF_DIR, branchName), ref);
    }

    private static void printCommit(Commit commit, String commitId) {
        System.out.println("===");
        System.out.println("commit " + commitId);
        System.out.println("Date: " + dateToString(commit.getDate()));
        System.out.println(commit.getMessage());
        System.out.println();
    }

    public static void find(String msg) {
        Boolean isfind = false;
        for (String commitShaId : Utils.plainFilenamesIn(COMMITS_DIR)) {
            Commit currentCommit = readObject(Utils.join(COMMITS_DIR, commitShaId), Commit.class);
            if (currentCommit.getMessage().equals(msg)) {
                System.out.println(commitShaId);
                isfind = true;
            }
        }
        if (!isfind) {
            System.out.println("Found no commit with that message.");
        }
    }

    //The command is essentially checkout of an arbitrary commit that also changes the current branch head.
    public static void reset(String commitShaId) {
        commitShaId = findFullId(commitShaId);

        if (commitShaId == null || !Utils.join(COMMITS_DIR, commitShaId).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        //If a working file is untracked in the current branch and would be overwritten by the reset
        Commit commit = Utils.readObject(Utils.join(COMMITS_DIR, commitShaId), Commit.class);
        HashSet<String> filesInThisBranch = findfilesInBranch(plainFilenamesIn(CURRENT_BRANCH).get(0));
        for (String file : plainFilenamesIn(CWD)) {
            if (!filesInThisBranch.contains(file)) {
                if (commit.getBlobs().keySet().contains(file)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        //delete tracked file which are not in this commitShaId
        List<String> blobIds = Utils.plainFilenamesIn(BLOBS_DIR);
        List<String> fileNames = readFileNames(blobIds);
        List<String> filesInCWD = new ArrayList<>(Utils.plainFilenamesIn(CWD));
        Set<String> fileNamesSet = new HashSet<>(fileNames);
        filesInCWD.removeIf(item -> fileNamesSet.contains(item));

        //write blobs of this commit
        for (String blobShaId : commit.getBlobs().values()) {
            writeBlobToCWD(blobShaId);
        }

        clearStaging();

        //moves the current branch’s head to that commit node
        String currentBranch = Utils.plainFilenamesIn(CURRENT_BRANCH).get(0);
        Ref ref = Utils.readObject(Utils.join(REF_DIR, currentBranch), Ref.class);
        ref.setCurrent(commitShaId);
        Utils.writeObject(Utils.join(REF_DIR, currentBranch), ref);

    }
    public static void rmBranch(String branchName) {
        if (!join(REF_DIR, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (plainFilenamesIn(CURRENT_BRANCH).get(0).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        join(REF_DIR, branchName).delete();
    }

    private static HashSet<String> findfilesInBranch(String branch) {
        HashSet<String> files = new HashSet<>();
        Ref ref = readObject(Utils.join(REF_DIR, branch), Ref.class);
        String masterId = ref.getLast();
        Commit current = readObject(Utils.join(COMMITS_DIR, masterId), Commit.class);
        while (current.getxParent() != null) {
            if (current.getBlobs() != null) {
                files.addAll(current.getBlobs().keySet());
            }
            current = readObject(Utils.join(COMMITS_DIR, current.getxParent()), Commit.class);
        }
        return files;
    }

    /*The 1st is little different from real git, others are same:
    * 1.If the file is in stage_dir(not commit, just add), unstage it (get it out of stage_dir)
    *   The file is still in CWD, just not tracked after rm. See GradeScope T14.
    *   This just like git rm --cache [file]
    * 2.If the file is in current commit, move it into removal_dir,and delete it in CWD.See GradeScope T13.
    * 3.If the file is in current commit then plain Unix 'rm' it, still move it into removal_dir.See GradeScope T22.
    * 4.neither staged nor tracked by the head commit, println: No reason to remove the file.
    *
    * Above realized in rm function.
    * Below realized in other function
    *
    * 5.When the file in removal_dir, create it again and "add file" will simply "unremove" the file without staging.
    *   Then the status is blank .See GradeScope T15.
    *   This should be realized in add command, see add function.
    * 6.Commit will clear the Removal_Dir(See GradeScope T20.)
    * which means the commit will record this removal, so commit should have
    *   another field to save it.
    *   This should be realized in commit command, see commit function.
    *
    * */
    public static void rm(String fileName) {
        File filepath = Utils.join(CWD, fileName);
        if (filepath.exists()) {
            String fileContent = Utils.readContentsAsString(filepath);
            String sha1Code = sha1(fileContent, fileName);

            if (!join(BLOBS_DIR, sha1Code).exists() && !join(REMOVAL_DIR, sha1Code).exists()
                    && !join(COMMITS_DIR, sha1Code).exists()) {
                System.out.println("No reason to remove the file.");
                System.exit(0);
            }
        }

        /* removal staging file*/
        String unStageFile = null;
        for (String fileInStage :Utils.plainFilenamesIn(STAGING_DIR)) {
            Blob b = Utils.readObject(Utils.join(STAGING_DIR, fileInStage), Blob.class);
            if (fileName.equals(b.getFileName())) {
                unStageFile = fileInStage;
                break;
            }
        }
        if (unStageFile != null) {
            Utils.join(STAGING_DIR, unStageFile).delete();
            return;
        }

        /*removal committed file*/
        String currentBranch = Utils.plainFilenamesIn(CURRENT_BRANCH).get(0);
        String currentCommit = findCommit(currentBranch);
        String blobShaId = findBlobInCommit(currentCommit, fileName);
        if (blobShaId != null) {
            Utils.restrictedDelete(Utils.join(CWD, fileName));
            Blob b = readObject(Utils.join(BLOBS_DIR, blobShaId), Blob.class);
            Utils.writeObject(Utils.join(REMOVAL_DIR, blobShaId), b);
        }

    }

    public static void status() {
        String currentBranch = Utils.plainFilenamesIn(CURRENT_BRANCH).get(0);
        List<String> branches = Utils.plainFilenamesIn(REF_DIR);
        List<String> stagingBlobs = Utils.plainFilenamesIn(STAGING_DIR);
        List<String> stagingFiles = readFileNames(stagingBlobs);
        List<String> removalBlobs = Utils.plainFilenamesIn(REMOVAL_DIR);
        List<String> removalFiles = readFileNames(removalBlobs);

        System.out.println("=== Branches ===");
        for (String branch : branches) {
            if (branch.equals(currentBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (String file : stagingFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String file : removalFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    private static List<String> readFileNames(List<String> blobs) {
        String[] files = new String[blobs.size()];
        for (int i = 0; i < blobs.size(); i++) {
            Blob b = readObject(Utils.join(BLOBS_DIR, blobs.get(i)), Blob.class);
            files[i] = (b.getFileName());
        }
        Arrays.sort(files);
        return Arrays.asList(files);
    }

    private static void clearStaging() {
        for (String blobSha1 : Utils.plainFilenamesIn(STAGING_DIR)) {
            Boolean b = Utils.join(STAGING_DIR, blobSha1).delete();
            //System.out.println("This bulbs in staging is deleting: " +  b);
        }

        for (String blobSha1 : Utils.plainFilenamesIn(REMOVAL_DIR)) {
            Boolean b = Utils.join(REMOVAL_DIR, blobSha1).delete();
            //System.out.println("This bulbs in staging is deleting: " +  b);
        }

    }

    private static void changeCurrentBranch(String newBranch) {
        for (String branch : Utils.plainFilenamesIn(CURRENT_BRANCH)) {
            Utils.join(CURRENT_BRANCH, branch).delete();
        }
        Utils.writeContents(Utils.join(CURRENT_BRANCH, newBranch), "");
    }
    private static void writeBlobToCWD(String shaIdOfBlob) {
        if (shaIdOfBlob != null) {
            Blob blob = Utils.readObject(Utils.join(BLOBS_DIR, shaIdOfBlob), Blob.class);
            File checkoutFile = new File(CWD, blob.getFileName());
            Utils.writeContents(checkoutFile, blob.getFileContent());
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /*return the branch's head or last*/
    private static String findCommit(String branchName) {
        Ref ref = Utils.readObject(Utils.join(REF_DIR, branchName), Ref.class);
        if (ref == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
            return null;
        }

        if (!(ref.getCurrent() == null)) {
            return ref.getCurrent();
        } else {
            return ref.getLast();
        }

    }

    private static String findBlobInCommit(String shaIdOfCommit, String fileName) {
        if (shaIdOfCommit != null) {
            Commit commit = Utils.readObject(Utils.join(COMMITS_DIR, shaIdOfCommit), Commit.class);
            HashMap<String, String> blobs = commit.getBlobs();
            return blobs.get(fileName);

        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }

    private static String dateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);
        return  dateFormat.format(date);
    }

    public static void merge(String givenBranch) {
        if (plainFilenamesIn(STAGING_DIR).size() > 0 || plainFilenamesIn(REMOVAL_DIR).size() > 0 ) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!plainFilenamesIn(REF_DIR).contains(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (plainFilenamesIn(CURRENT_BRANCH).get(0).equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        String splitPoint = splitPoint(givenBranch);
        String currentCommitId = findCommit(plainFilenamesIn(CURRENT_BRANCH).get(0));
        String givenCommitId = findCommit(givenBranch);
        if (splitPoint.equals(givenCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else if (splitPoint.equals(currentCommitId)) {
            checkoutBranch(givenBranch);
            System.out.println("Current branch fast-forwarded.");
        }
    }

    private static String splitPoint(String givenBranch) {
        String currentBranch = plainFilenamesIn(CURRENT_BRANCH).get(0);
        String lastCommitIdInCurrent = findCommit(currentBranch);
        Commit lastCommitInCurrent = readObject(join(COMMITS_DIR, lastCommitIdInCurrent), Commit.class);

        String lastCommitIdInGiven = findCommit(givenBranch);
        Commit lastCommitInGiven = readObject(join(COMMITS_DIR, lastCommitIdInGiven), Commit.class);

        ArrayList<String> parentsOfCurrent = new ArrayList<>();
        HashSet<String> parentsOfGiven = new HashSet<>();
        while (true) {
            parentsOfCurrent.add(lastCommitIdInCurrent);
            lastCommitIdInCurrent = lastCommitInCurrent.getxParent();
            if (lastCommitIdInCurrent == null) {
                break;
            }
            lastCommitInCurrent = readObject(join(COMMITS_DIR, lastCommitIdInCurrent), Commit.class);
        }

        while (true) {
            parentsOfGiven.add(lastCommitIdInGiven);
            lastCommitIdInGiven = lastCommitInGiven.getxParent();
            if (lastCommitIdInGiven == null) {
                break;
            }
            lastCommitInGiven = readObject(join(COMMITS_DIR, lastCommitIdInGiven), Commit.class);
        }

        String splitPoint = null;
        for (String c : parentsOfCurrent) {
            if (parentsOfGiven.contains(c)) {
                splitPoint =  c;
                break;
            }
        }
        return splitPoint;
    }
}
