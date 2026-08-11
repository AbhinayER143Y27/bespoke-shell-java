import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
            String outputFile = null;
            for(int i = 0; i < parts.size(); i++)
            {
                if(parts.get(i).equals(">") || parts.get(i).equals("1>"))
                {
                    outputFile = parts.get(i + 1);
                    parts.remove(i + 1);
                    parts.remove(i);
                    break;
                }
            }

            String command = parts.get(0);
            if(command.equals("exit"))
            {
                break;
            }

            else if(input.startsWith("type"))
            {
                if(parts.size() > 1){
                    String target = parts.get(1);
                    String result;
                    if(isBuiltIn(target)) System.out.println(target + " is a shell builtin");
                    else {
                        String execPath = getExecutablePath(target);
                        if (execPath != null) {
                            result = target + " is " + execPath;
                        } else {
                            result = target + ": not found";
                        }
                        printResult(result, outputFile);
                    }
                }
            }

            else if(command.equals("echo"))
            {
                StringBuilder sb = new StringBuilder();
                for(int i = 1; i < parts.size(); i++)
                {
                    sb.append(parts.get(i));
                    if(i < parts.size() - 1) sb.append(" ");
                }
                printResult(sb.toString(), outputFile);
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
                printResult(System.getProperty("user.dir"), outputFile); //The shell doesn't look for the external programs to tell where it is and the user.dir in java property
            }// that always tell where the JVM was started.
            else{
                String execPath = getExecutablePath(command);
                if(execPath != null)
                {
                    try
                    {
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(System.getProperty("user.dir")));

                        if(outputFile != null)
                        {
                            pb.redirectOutput(new File(outputFile)); // this allows errors to still show on the screen
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        }
                        else {
                            pb.inheritIO();
                        }
                        pb.start().waitFor();
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

        for(String dir : directories) {
            File file = new File(dir, command);

            if (file.exists() && file.isFile() && file.canExecute()) {
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
        boolean inEscaped = false;

        for(int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);

            if(inEscaped)
            {
                currentArg.append(c);
                inEscaped = false;
                continue;
            }

            //Look ahead buffering
            else if(c == '\\' && inDoubleQuotes)
            {
                if(i+1 < input.length())
                {
                    char next = input.charAt(i+1);
                    if(next == '\"' || next == '\\' || next == '$')
                    {
                        currentArg.append(next);
                        i++;
                    }
                    else
                    {
                        currentArg.append(c);
                    }
                }
            }

            else if(c == '\\' && !inSingleQuotes && !inDoubleQuotes)
            {
                inEscaped = true;
            }

            else if(c == '\'' && !inDoubleQuotes){
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

    private static void printResult(String message, String outputFile)
    {
        if(outputFile != null)
        {
            try(PrintWriter writer = new PrintWriter(new FileWriter(outputFile,false)))
            {
                writer.println(message);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        else {
            System.out.println(message);
        }
    }
}