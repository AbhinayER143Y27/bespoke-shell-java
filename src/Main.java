import java.util.Scanner;

public class Main
{
    public static void main(String args[]) throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        while(true)
        {
            System.out.print("$ ");
            String input = scanner.nextLine();
            if(input.startsWith("type ")) {
                String target = input.substring(5).trim();
                if (isBuiltIn(target)) {
                    System.out.println(input + " is a shell builtin");
                }
                else
                {
                    System.out.println(input + ": not found");
                }
            }
            else if(input.equals("exit"))
            {
                break;
            }
            else if(input.startsWith("echo "))
            {
                System.out.println(input.substring(5).trim());
            }
            else
            System.out.println(input + ": command not found");
        }
    }

    private static boolean isBuiltIn(String target)
    {
        return target.equals("echo") || target.equals("exit") || target.equals("type");
    }
}