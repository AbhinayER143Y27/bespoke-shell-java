import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            List<String> parts = parseInput(input);

            if(parts.isEmpty())
            {
                continue;
            }

            String command = parts.get(0);
            if(command.equals("exit"))
            {
                break;
            }

            else if(input.startsWith("type "))
            {
                if(parts.size() > 1){
                    String target = parts.get(1);
                    if(isBuiltIn(target)) System.out.println(target + " is a shell builtin");
                    else {
                        String execPath = getExecutablePath(target);
                        if (execPath != null) {
                            System.out.println(target + " is " + execPath);
                        } else {
                            System.out.println(target + ": not found");
                        }
                    }
                }
            }

            else if(command.equals("echo "))
            {
                for(int i = 1; i < parts.size(); i++)
                {
                    System.out.println(parts.get(i));
                    if(i < parts.size() - 1)
                    {
                        System.out.print(" ");
                    }
                }
                System.out.println();
            }
            // cd is updating a variable
            // we cannot change the physical working directory of a running java using standard codes so we have to fake it by maintaining an internal variable
            //telling rest of them where the user thinks they are.
            else if(command.equals("cd"))
            {
                String targetPath;
                if(parts.size() == 1)
                {
                    targetPath = System.getenv("HOME");
                }
                else {
                    targetPath = parts.get(1);
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

                try{
                    if(newDir.exists() && newDir.isDirectory())
                    {
                        System.setProperty("user.dir",newDir.getCanonicalPath()); //Magical Event
                    }
                    else {
                        System.out.println("cd: "+ targetPath +": No such file or directory");
                    }
                }catch (IOException e)
                {
                    System.out.println("cd: " + targetPath + ": No such file or directory");
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
        return target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd") || target.equals("cd");
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

    private static List<String> parseInput(String input)
    {
        List<String> result = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for(int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);

            if(c == '\'' && !inDoubleQuotes){
                inSingleQuotes = !inSingleQuotes;
            }
            else if(c == '\"' && !inSingleQuotes)
            {
                inDoubleQuotes = !inDoubleQuotes;
            }
            else if(c == ' ' && !inSingleQuotes && !inDoubleQuotes) //treat a space as an argument separator if i'm not inside quotes.
            {
                if(currentArg.length() > 0)
                {
                    result.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            }
            else {
                currentArg.append(c);
            }
        }

        if(currentArg.length() > 0)
        {
            result.add(currentArg.toString());
        }
        return result;
    }
}