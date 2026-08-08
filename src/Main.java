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
            if(command.equals("exit"))
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
            // cd is updating a variable
            // we cannot change the physical working directory of a running java using standard codes so we have to fake it by maintaining an internal variable
            //telling rest of them where the user thinks they are.
            else if(command.equals("cd"))
            {
                String targetPath = parts[1];
                if(parts.length == 1)
                {
                    targetPath = System.getenv("HOME");
                }
                else if(parts.length == 2){targetPath = parts[1];}
                else {
                    System.out.println("cd: too many arguments");
                    return;
                }

                if(targetPath.equals("~"))
                {
                    targetPath = System.getenv("HOME"); //OS's environment dictionary
                }

                File newDir = new File(targetPath);

                if(!newDir.isAbsolute())
                {
                    newDir = new File(System.getProperty("user.dir"),targetPath);
                }

                if(newDir.exists() && newDir.isDirectory())
                {
                    System.setProperty("user.dir",newDir.getCanonicalPath()); //Magical Event
                }
                else {
                    System.out.println("cd: "+ targetPath +": No such file or directory");
                }
            }

            else if(command.equals("pwd"))
            {
                System.out.println(System.getProperty("user.dir")); //The shell doesn't look for the external programs to tell where it is and the user.dir in java property
            }// that always tell where the JVM was started.
            else{
                String execPath = getExecutablePath(command);
                if(execPath != null)
                {
                    try
                    {
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(System.getProperty("user.dir")));
                        pb.inheritIO(); //send its output to the terminal.

                        Process process = pb.start();

                        process.waitFor();
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else System.out.println(input + ": not found");
            }
        }
    }
    private static boolean isBuiltIn(String target)
    {
        return target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd");
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