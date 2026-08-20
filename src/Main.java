import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> 
        {
            try { ShellUtils.setRawMode(false); } catch (Exception ignored) {}
        }));
        
        Map<String, String> completeMap = new HashMap<>();
        
        while(true) {
            
            System.out.print("$ ");
            System.out.flush();
            String input;
            ShellUtils.setRawMode(true);
            
            try 
            {
                input = InputReader.readInputWithTab();
            } 
            finally
            {
                ShellUtils.setRawMode(false);
            }
            
            if (input == null) break;

            List<String> parts = CommandParser.parseInput(input);
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
                    ShellUtils.prepareFile(outputFile,isAppend);
                    parts.remove(i + 1); parts.remove(i); i--;
                }
                else if (parts.get(i).equals("2>>")) 
                {
                    errorFile = parts.get(i + 1);
                    isErrorAppend = true;
                    ShellUtils.prepareFile(errorFile,isErrorAppend);
                    parts.remove(i + 1); parts.remove(i); i--;
                } 
                else if(parts.get(i).equals(">") || parts.get(i).equals("1>")) 
                {
                    outputFile = parts.get(i + 1);
                    ShellUtils.prepareFile(outputFile,false);
                    parts.remove(i + 1); parts.remove(i); i--;
                } 
                else if(parts.get(i).equals("2>")) 
                {
                    errorFile = parts.get(i + 1);
                    ShellUtils.prepareFile(errorFile,false);
                    parts.remove(i + 1); parts.remove(i); i--;
                }
            }

            String command = parts.get(0);
            if(command.equals("exit")) 
            {
                break;
            }
            else if(input.startsWith("type")) 
            {
                if(parts.size() > 1)
                {
                    String target = parts.get(1);
                    String result;
                    if(ShellUtils.isBuiltIn(target)) System.out.println(target + " is a shell builtin");
                    else {
                        String execPath = ShellUtils.getExecutablePath(target);
                        if (execPath != null) 
                        {
                            result = target + " is " + execPath;
                        } 
                        else
                        {
                            result = target + ": not found";
                        }
                        ShellUtils.printResult(result, outputFile,false);
                    }
                }
            } 
            else if(command.equals("complete")) 
            {
                if(parts.size() >= 3 && parts.get(1).equals("-p"))
                {
                    String targetCommand = parts.get(2);
                    if(completeMap.containsKey(targetCommand)) 
                    {
                        System.out.println("complete -C '" + completeMap.get(targetCommand) + "' " + targetCommand);
                    } else 
                    {
                        System.out.println("complete: " + targetCommand + ": no completion specification");
                    }
                } 
                else if(parts.size() >= 4 && parts.get(1).equals("-C")) {
                    String commandName = parts.get(3);
                    String path = parts.get(2);
                    completeMap.put(commandName, path);
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
                ShellUtils.printResult(sb.toString(), outputFile, isAppend);
            } 
            else if(command.equals("cd"))
            {
                String targetPath;
                if(parts.size() == 1) 
                {
                    targetPath = System.getenv("HOME");
                }
                else
                {
                    targetPath = parts.get(1);
                }
                if(targetPath.equals("~")) 
                {
                    targetPath = System.getenv("HOME");
                }
                
                File newDir = new File(targetPath);
                
                if(!newDir.isAbsolute()) 
                {
                    newDir = new File(System.getProperty("user.dir"),targetPath);
                }
                try
                {
                    if(newDir.exists() && newDir.isDirectory())
                    {
                        System.setProperty("user.dir",newDir.getCanonicalPath());
                    } 
                    else
                    {
                        System.out.println("cd: "+ targetPath +": No such file or directory");
                    }
                } 
                catch (IOException e) 
                {
                    System.out.println("cd: " + targetPath + ": No such file or directory");
                }
            } 
            else if(command.equals("pwd"))
            {
                ShellUtils.printResult(System.getProperty("user.dir"), outputFile, false);
            } else
            {
                String execPath = ShellUtils.getExecutablePath(command);
                if(execPath != null) 
                {
                    try 
                    {
                        ProcessBuilder pb = new ProcessBuilder(parts);
                        pb.directory(new File(System.getProperty("user.dir")));

                        if(isAppend && outputFile != null) 
                        {
                            File logFile = new File(outputFile);
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
                        } 
                        else if(outputFile != null && !isAppend)
                        {
                            pb.redirectOutput(new File(outputFile));
                        } 
                        else
                        {
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        }

                        if(isErrorAppend && errorFile != null) 
                        {
                            File logFile = new File(errorFile);
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
                        } 
                        else if(errorFile != null && !isErrorAppend) 
                        {
                            pb.redirectError(new File(errorFile));
                        }
                        else 
                        {
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        }
                        pb.start().waitFor();
                    } 
                    catch(Exception e) 
                    {
                        e.printStackTrace();
                    }
                } else System.out.println(input + ": not found");
            }
        }
    }
}
