$ErrorActionPreference = 'Stop'
$src = 'C:\Users\admin\.cursor\projects\c-Users-admin-Desktop-DeltaClient-main\assets\c__Users_admin_AppData_Roaming_Cursor_User_workspaceStorage_debe26d56c6328f0001cac9d3d28e03e_images_image-2986cd45-4e8c-498f-ba8f-e2f0a1eb60eb.png'
$root = 'C:\Users\admin\Desktop\DeltaClient-main'
if (-not (Test-Path -LiteralPath $src)) {
    throw "Source avatar not found: $src"
}

Copy-Item -LiteralPath $src -Destination (Join-Path $root 'malice.png') -Force
Copy-Item -LiteralPath $src -Destination (Join-Path $root 'src\main\resources\assets\skeleton\pictures\avatar.png') -Force
Copy-Item -LiteralPath $src -Destination (Join-Path $root 'src\main\resources\assets\skeleton\icon.png') -Force

$pngPath = Join-Path $root 'malice.png'
$icoPath = Join-Path $root 'malice.ico'
$png = [IO.File]::ReadAllBytes($pngPath)
$fs = [IO.File]::Create($icoPath)
$bw = New-Object IO.BinaryWriter $fs
$bw.Write([uint16]0)
$bw.Write([uint16]1)
$bw.Write([uint16]1)
$bw.Write([byte]0)
$bw.Write([byte]0)
$bw.Write([byte]0)
$bw.Write([byte]0)
$bw.Write([uint16]1)
$bw.Write([uint16]32)
$bw.Write([uint32]$png.Length)
$bw.Write([uint32]22)
$bw.Write($png)
$bw.Flush()
$bw.Close()

Write-Host ("png=" + (Get-Item $pngPath).Length)
Write-Host ("ico=" + (Get-Item $icoPath).Length)
Write-Host ("avatar=" + (Get-Item (Join-Path $root 'src\main\resources\assets\skeleton\pictures\avatar.png')).Length)
Write-Host ("icon=" + (Get-Item (Join-Path $root 'src\main\resources\assets\skeleton\icon.png')).Length)
