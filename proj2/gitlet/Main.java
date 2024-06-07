package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  receive user's inputs, dispatch jobs to Repository, and do same upper level logic
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }


        String firstArg = args[0];
        if (!firstArg.equals("init") && !Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        switch (firstArg) {
            case "init":
                if (Repository.GITLET_DIR.exists()) {
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                }
                Repository.init();
                break;
            case "add":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "checkout":
                if (args[1].equals("--")) {
                    Repository.checkoutFile1(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutFile2(args[1], args[3]);
                } else if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "log":
                Repository.log();
                break;
            case "rm":
                Repository.rm(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "reset":
                Repository.reset(args[1]);
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.rmBranch(args[1]);
                break;
            default:
                System.out.println("No command with that name exits.");
                System.exit(0);
        }
    }
}
