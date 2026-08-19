import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Safety net: if the JVM exits abnormally (Ctrl+C, uncaught
        // exception) while mid-read, this guarantees the terminal gets put
        // back into normal (cooked/echo) mode instead of staying stuck.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { setRawMode(false); } catch (Exception ignored) {}
        }));

        while(true) {
            System.out.print("$ ");
            System.out.flush();
            String input;
            setRawMode(true);
            try
            {
                input = readInputWithTab();
            }finally {
                setRawMode(false);
            }
            // This will work after the enter has been placed and before this all the tab related work is done
            if (input == null) break; // Ctrl-D / real EOF on stdin

            List<String> parts = parseInput(input);

            if(parts.isEmpty()) continue;
            String outputFile = null;
            String errorFile = null;
            boolean isAppend = false;
            boolean isErrorAppend = false;
            for(int i = 0; i < parts.size(); i++)
            {
                if(parts.get(i).equals(">>") || parts.get(i).equals("1>>"))
                {
                    outputFile = parts.get(i + 1);
                    isAppend = true;
                    prepareFile(outputFile,isAppend);
                    parts.remove(i + 1);
                    parts.remove(i);
                    i--;
                } else if (parts.get(i).equals("2>>"))
                {
                    errorFile = parts.get(i + 1);
                    isErrorAppend = true;
                    prepareFile(errorFile,isErrorAppend);
                    parts.remove(i + 1);
                    parts.remove(i);
                    i--;
                }
                else if(parts.get(i).equals(">") || parts.get(i).equals("1>"))
                {
                    outputFile = parts.get(i + 1);

                    prepareFile(outputFile,false);
                    parts.remove(i + 1);
                    parts.remove(i);
                    i--;
                }
                else if(parts.get(i).equals("2>"))
                {
                    errorFile = parts.get(i + 1);

                    prepareFile(errorFile,false);
                    parts.remove(i + 1);
                    parts.remove(i);
                    i--;
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
                        printResult(result, outputFile,false);
                    }
                }
            }

            else if(command.equals("complete"))
            {
                if(parts.size() >= 3 && parts.get(1).equals("-p"))
                {
                    String targetCommand = parts.get(2);
                    System.out.println("complete: " + targetCommand + ": no completion specification");
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
                printResult(sb.toString(), outputFile, isAppend);
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

                if(!newDir.isAbsolute()) // this is there to tell that the document is related to what actually
                {
                    newDir = new File(System.getProperty("user.dir"),targetPath); // /Users/abhinay/Documents - absolute path
                }                                                                 // Documents is a relative path or cd .. is relative

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
            { // here is append is false obv we are just looking for the location why care about the add ons.
                printResult(System.getProperty("user.dir"), outputFile, false); //The shell doesn't look for the external programs to tell where it is and the user.dir in java property
            }// that always tell where the JVM was started.

//         <<<<<<<<<<------------------------------------------------------------------------------------------------------------------->>>>>>>>>
//                                                                        External Commands
//         <<<<<<<<<<------------------------------------------------------------------------------------------------------------------->>>>>>>>>
            else{
                String execPath = getExecutablePath(command);
                if(execPath != null)
                {
                    try
                    {
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(System.getProperty("user.dir"))); // this new File(System.getProperty("user.dir")) becomes this new File("/Users/abhinay/my-shell") and it also represents the path and the pb.directory this one make this its working directory.

                        if(isAppend && outputFile != null)//if it is true then it will access else it will just do what it was doing
                        {
                            File logFile = new File(outputFile);
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile)); // we will open the file i dont know about that  but it will be something like this ig :) ;
                        }
                        else if(outputFile != null && !isAppend)
                        {
                            pb.redirectOutput(new File(outputFile));
                        }
                        else {
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);  // this allows errors to still show on the screen
                        }

                        if(isErrorAppend && errorFile != null)
                        {
                            File logFile = new File(errorFile);
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
                        }
                        else if(errorFile != null && !isErrorAppend)
                        {
                            pb.redirectError(new File(errorFile)); // instead of showing hte error on the terminal write them in the error file.
                        }
                        else {
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT); // if there is no file that is given
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
        return target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd") || target.equals("cd") || target.equals("complete");
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
            File file = new File(dir, command); // dir = "/usr/bin" and the command = "cat" it doesn't create a file it creates a java file object representing a possible path., not anything actual filesystem,

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

    private static void printResult(String message, String outputFile, boolean isAppend)
    {
        if(outputFile != null)
        {
            try(PrintWriter writer = new PrintWriter(new FileWriter(outputFile,isAppend))) // is append is there so directly the value could be written in here;
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

    private static void prepareFile(String path, boolean isAppend)
    {
        try
        {
            File f = new File(path);
            if(f.getParentFile() != null)
            {
                f.getParentFile().mkdirs(); // the program will not crash if the user redirects to a path that hasn't been created.
            }
            f.createNewFile();

            if(isAppend)
            {

            }
            else
            {
                new FileOutputStream(f).close();
            }
        }
        catch (IOException e)
        {

        }
    }

    private static String readInputWithTab() throws IOException{
        StringBuilder inputBuffer = new StringBuilder();
        List<String> builtins = List.of("echo","exit","pwd","type","cd");
        boolean lastTabWasMultiMatch = false;
        String lastTabBuffer = null;
        while(true)
        {
            int inChar = System.in.read();

            if(inChar == -1)
            {
                return inputBuffer.length() == 0 ? null : inputBuffer.toString();
            }

            if(inChar == 10 || inChar == 13)
            {
                System.out.print("\n");
                break;
            }

            if(inChar == 9)
            {
                String fullBufferText = inputBuffer.toString();
                boolean hasSpace = fullBufferText.contains(" ");
                int spaceIndex = fullBufferText.lastIndexOf(' ');
                String argument = hasSpace ? fullBufferText.substring(spaceIndex + 1) : fullBufferText;

                int slashIndex = argument.lastIndexOf("/");
                String dirPath = (slashIndex >= 0) ? argument.substring(0,slashIndex + 1) : "";
                String currentBufferText = (slashIndex >= 0) ? argument.substring(slashIndex + 1) : argument;

                List<String> matches = new ArrayList<>();

                //Find which builtin starts with what we typed
                Set<String> allCandidates = hasSpace ? getFilenameCandidates(dirPath ,currentBufferText) : getCompletionCandidates(currentBufferText);

                for(String b : allCandidates)
                {
                    if(b.startsWith(currentBufferText))
                    {
                        matches.add(b);
                    }
                }
                Collections.sort(matches);

                if(matches.size() == 1)
                {
                    String match = matches.get(0);
                    String completionPart = match.substring(currentBufferText.length());

                    if(!match.endsWith("/"))
                    {
                        completionPart += " ";
                    }
                    inputBuffer.append(completionPart);
                    System.out.print(completionPart);
                    System.out.flush();

                    lastTabWasMultiMatch = false;
                    lastTabBuffer = null;
                }
                else if(matches.size() > 1)
                {
                    String lcp = longestCommonPrefix(matches);
                    if(lcp.length() > currentBufferText.length())
                    {
                        String completionPart = lcp.substring(currentBufferText.length());
                        inputBuffer.append(completionPart);
                        System.out.print(completionPart);
                        System.out.flush();

                        lastTabWasMultiMatch = false;
                        lastTabBuffer = null;
                    }
                    else if(lastTabWasMultiMatch && currentBufferText.equals(lastTabBuffer))
                    {
                        System.out.print("\n");
                        System.out.print(String.join("  ",matches));
                        System.out.print("\n");
                        System.out.print("$ " + fullBufferText);
                        System.out.flush();

                        lastTabWasMultiMatch = false;
                        lastTabBuffer = null;
                    }
                    else {
                        System.out.print("\u0007");
                        System.out.flush();
                        lastTabWasMultiMatch = true;
                        lastTabBuffer = currentBufferText;
                    }
                }
                else{
                    // I should do nothing but i got something on the internet
                    System.out.print("\u0007");
                    System.out.flush();
                    lastTabWasMultiMatch = false;
                    lastTabBuffer = null;
                }
                continue;
            }

            if((inChar == 127 || inChar == 8) && inputBuffer.length() > 0)
            {
                inputBuffer.setLength(inputBuffer.length() - 1);
                System.out.print("\b \b");
                System.out.flush();
                lastTabWasMultiMatch = false;
                lastTabBuffer = null;
                continue;
            }

            if(inChar >= 32 && inChar <= 126)
            {
                char ch = (char) inChar;
                inputBuffer.append(ch);
                System.out.print(ch);
                System.out.flush();
                lastTabWasMultiMatch = false;
                lastTabBuffer = null;
            }
        }
        return inputBuffer.toString();
    }
    private static void setRawMode(boolean on) throws Exception
    {
        String state = on ? "-icanon -echo" : "icanon echo";
        String cmd = "stty " + state + " < /dev/tty";
        int exitCode = new ProcessBuilder("sh","-c",cmd).inheritIO().start().waitFor();
        if (exitCode != 0)
        {
            System.err.println("warning: stty " + state + " failed (exit " + exitCode + ")");
        }
    }

    private static String longestCommonPrefix(List<String> strings)
    {
        if(strings == null || strings.isEmpty()) return "";

        String prefix = strings.get(0);
        for(int i = 1; i < strings.size(); i++)
        {
            String other = strings.get(i);
            int maxLen = Math.min(prefix.length(), other.length());
            int j = 0;
            while(j < maxLen && prefix.charAt(j) == other.charAt(j))
            {
                j++;
            }
            prefix = prefix.substring(0,j);
            if(prefix.isEmpty()) break;
        }
        return prefix;
    }

    // This one is restricted to the current directory only

    private static Set<String> getFilenameCandidates(String dirPath,String prefix)
    {
        Set<String> candidates = new TreeSet<>();
        File currentDir = dirPath.isEmpty() ? new File(System.getProperty("user.dir")) : new File(new File(System.getProperty("user.dir")),dirPath);
        File[] localFiles = currentDir.listFiles();
        if(localFiles != null)
        {
            for(File f : localFiles)
            {
                if(f.getName().startsWith(prefix))
                {
                    candidates.add(f.isDirectory() ? f.getName() + "/" : f.getName());
                }
            }
        }
        return candidates;
    }

    private static Set<String> getCompletionCandidates(String prefix)
    {
        if(prefix == null) return new HashSet<>();
        File currentDir = new File(System.getProperty("user.dir"));
        File[] localFiles = currentDir.listFiles();
        Set<String> candidates = new TreeSet<>();

        List<String> builtins = List.of("echo","exit","pwd","type","cd");
        for(String b : builtins)
        {
            if(b.startsWith(prefix)) candidates.add(b);
        }
        String pathEnv = System.getenv("PATH");

        if(localFiles != null)
        {
            for(File f : localFiles)
            {
                if(f.isFile() && f.getName().startsWith(prefix)) // dont add the f.canexecute() invested 2 hours already + 20 min
                {
                    candidates.add(f.getName());
                }
            }
        }

        if(pathEnv != null)
        {
            String[] directories = pathEnv.split(File.pathSeparator);
            for(String dir : directories)
            {
                File folder = new File(dir);
                if(folder.exists() && folder.isDirectory())
                {
                    File[] files = folder.listFiles();
                    if(files != null)
                    {
                        for(File f : files)
                        {
                            if(f.isFile() && f.getName().startsWith(prefix))
                            {
                                candidates.add(f.getName());
                            }
                        }
                    }
                }
            }
        }
        return candidates;
    }
}
