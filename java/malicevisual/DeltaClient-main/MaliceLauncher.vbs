Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
exe = root & "\MaliceVisuals.exe"
Set sh = CreateObject("Wscript.Shell")
sh.CurrentDirectory = root
If fso.FileExists(exe) Then
  sh.Run """" & exe & """", 1, False
Else
  ps1 = root & "\MaliceLauncher.ps1"
  cmd = "powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & ps1 & """"
  sh.Run cmd, 0, False
End If
