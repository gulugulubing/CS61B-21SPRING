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
            System.out.println(Utils.error("Please enter a command."));
            System.exit(0);
        }


        String firstArg = args[0];
        if (!firstArg.equals("init") && !Repository.GITLET_DIR.exists()) {
            System.out.println(Utils.error("Not in an initialized Gitlet directory."));
            System.exit(0);
        }

        switch(firstArg) {
            case "init":
                if (Repository.GITLET_DIR.exists()) {
                    System.out.println(Utils.error("A Gitlet version-control system already exists in the current directory."));
                    System.exit(0);
                }
                Repository.init();
                break;
            case "add":
                if (args.length > 2) {
                    System.out.println(Utils.error("Incorrect operands."));
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length > 2) {
                    System.out.println(Utils.error("Incorrect operands."));
                    System.exit(0);
                }
                if (args.length == 1) {
                    System.out.println(Utils.error("Please enter a commit message."));
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "checkout":
                if (args[1].equals("--")) {
                    Repository.checkoutFile1(args[2]);
                }
                if (args[2].equals("--")) {
                    Repository.checkoutFile2(args[1], args[3]);
                }
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                }
                break;
            case "log":
                Repository.log();
                break;
            default:
                System.out.println(Utils.error("No command with that name exits."));
                System.exit(0);
        }
    }
}
