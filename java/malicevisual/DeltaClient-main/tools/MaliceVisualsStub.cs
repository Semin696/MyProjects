using System;
using System.IO;
using System.Management.Automation;
using System.Management.Automation.Runspaces;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;

internal static class Program
{
    private const string AppId = "Malice.Visuals.Launcher";

    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    private static extern int SetCurrentProcessExplicitAppUserModelID(string appID);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int MessageBoxW(IntPtr hWnd, string text, string caption, uint type);

    [STAThread]
    private static int Main()
    {
        try
        {
            SetCurrentProcessExplicitAppUserModelID(AppId);
        }
        catch
        {
        }

        string dir = AppDomain.CurrentDomain.BaseDirectory;
        if (string.IsNullOrEmpty(dir))
        {
            dir = Environment.CurrentDirectory;
        }

        dir = dir.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        string ps1 = Path.Combine(dir, "MaliceLauncher.ps1");
        if (!File.Exists(ps1))
        {
            Fail("Не найден MaliceLauncher.ps1 рядом с лаунчером.");
            return 1;
        }

        Environment.CurrentDirectory = dir;
        Environment.SetEnvironmentVariable("MALICE_ROOT", dir);

        try
        {
            InitialSessionState iss = InitialSessionState.CreateDefault();
            iss.LanguageMode = PSLanguageMode.FullLanguage;
            try
            {
                iss.ExecutionPolicy = Microsoft.PowerShell.ExecutionPolicy.Bypass;
            }
            catch
            {
            }

            using (Runspace runspace = RunspaceFactory.CreateRunspace(iss))
            {
                runspace.ApartmentState = ApartmentState.STA;
                runspace.ThreadOptions = PSThreadOptions.UseCurrentThread;
                runspace.Open();

                using (PowerShell ps = PowerShell.Create())
                {
                    ps.Runspace = runspace;
                    ps.AddCommand("Set-Location").AddParameter("LiteralPath", dir);
                    ps.Invoke();
                    ThrowIfFailed(ps);
                    ps.Commands.Clear();
                    string escaped = ps1.Replace("'", "''");
                    ps.AddScript("& '" + escaped + "'", false);
                    ps.Invoke();
                }
            }
        }
        catch (Exception ex)
        {
            Fail(ex.Message);
            return 1;
        }

        return 0;
    }

    private static void ThrowIfFailed(PowerShell ps)
    {
        if (ps.Streams.Error == null || ps.Streams.Error.Count == 0)
        {
            return;
        }

        StringBuilder sb = new StringBuilder();
        foreach (ErrorRecord err in ps.Streams.Error)
        {
            sb.AppendLine(err.ToString());
        }

        throw new InvalidOperationException(sb.ToString());
    }

    private static void Fail(string message)
    {
        MessageBoxW(IntPtr.Zero, message, "Malice Visuals", 0x10);
    }
}
