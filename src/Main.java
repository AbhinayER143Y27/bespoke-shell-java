import java.io.File;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            String parts[] = input.split("\\s+");
            String command = parts[0];
            if(input.equals("exit") || input.equals("exit "))
            {
                break;
            }

            else if(input.startsWith("type "))
            {
                String target = input.substring(5).trim();
                if(isBuiltIn(target)) System.out.println(target + " is a shell builtin");
                else {
                    String execPath = getExecutablePath(target);
                    if(execPath != null)
                    {
                        System.out.println(target + " is " + execPath);
                    }
                    else {System.out.println(target + ": not found");}
                }
            }

            else if(input.startsWith("echo "))
            {
                System.out.println(input.substring(5));
            }
            else {
                String execPath = getExecutablePath(command);
                if(execPath != null) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(System.getProperty("user.dir")));
                        pb.inheritIO(); // connect the child process with your shells in and op.
                        //Java shell -> stdin -> Process -> stdout -> terminal without this the output on the terminal might not exists.
                        Process process = pb.start();

                        process.waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    System.out.println(input + " : not found");
                }
            }
        }
    }
    private static boolean isBuiltIn(String target)
    {
        return target.equals("echo") || target.equals("exit") || target.equals("type");
    }

    private static String getExecutablePath(String command)
    {
        String pathEnv = System.getenv("PATH"); //Specifying a lookup key in the operating system dictionary
        if(pathEnv == null || pathEnv.isEmpty())
        {
            return null;
        }
        String[] directories = pathEnv.split(File.pathSeparator);

        for(String dir : directories)
        {
            File file = new File(dir, command);

            if(file.exists() && file.isFile() && file.canExecute())
            {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}