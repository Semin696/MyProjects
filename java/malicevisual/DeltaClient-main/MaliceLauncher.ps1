$ErrorActionPreference = 'Stop'
if ([Threading.Thread]::CurrentThread.GetApartmentState() -ne 'STA') {
    $extra = @($args)
    & powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -File $MyInvocation.MyCommand.Path @extra
    exit $LASTEXITCODE
}

Add-Type -AssemblyName PresentationFramework, PresentationCore, WindowsBase, System.Drawing
if (-not ('MaliceNative' -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class MaliceNative {
    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);
    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    public static extern int SetCurrentProcessExplicitAppUserModelID(string appID);
}
"@
}

$root = $null
if ($MyInvocation.MyCommand.Path) {
    $root = Split-Path -Parent $MyInvocation.MyCommand.Path
}
if (-not $root -and $PSScriptRoot) { $root = $PSScriptRoot }
if (-not $root -and $env:MALICE_ROOT) { $root = $env:MALICE_ROOT }
if (-not $root) { $root = (Get-Location).Path }
if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) {
    $parent = Split-Path -Parent $root
    if ($parent -and (Test-Path (Join-Path $parent 'gradlew.bat'))) { $root = $parent }
}
Set-Location $root

$script:cfgPath = Join-Path $root 'malice-launcher.json'
$script:keysPath = Join-Path $root 'malice-keys.json'
$script:licensePath = Join-Path $root 'malice-license.json'
$script:ramMb = 4096
$script:proc = $null
$script:page = 'home'
$script:windowIcon = $null
$script:license = $null
$script:gatePlan = 30
$script:closeOnLaunch = $false
$script:launchAt = $null
$script:launchPct = 0
$script:launchNote = ''
$script:gameStarted = $false
$script:launchTimer = $null
$script:logQueue = New-Object 'System.Collections.Concurrent.ConcurrentQueue[string]'
$script:launchLogPath = $null
$script:autoPlay = $false
if ($env:MALICE_AUTO_LAUNCH -eq '1') { $script:autoPlay = $true }
foreach ($a in @($args)) {
    if ([string]$a -match '^(?i)[-/]{0,2}play$') { $script:autoPlay = $true }
}
$autoPlayFlag = Join-Path $root 'run\malice-autoplay'
if (Test-Path -LiteralPath $autoPlayFlag) {
    $script:autoPlay = $true
    Remove-Item -LiteralPath $autoPlayFlag -Force -ErrorAction SilentlyContinue
}
$script:launchLogPos = [int64]0
$script:launchLogCarry = ''
$script:friends = New-Object System.Collections.Generic.List[object]
$script:friendNoDamage = $true
$script:friendHighlight = $true
$script:friendNoPush = $true
$script:friendSkipEsp = $true
$script:brushes = New-Object System.Windows.Media.BrushConverter
$script:accent = '#6B7394'

function Get-JavaMajorVersion([string]$javaHomeDir) {
    if (-not $javaHomeDir) { return 0 }
    $rel = Join-Path $javaHomeDir 'release'
    if (Test-Path -LiteralPath $rel) {
        foreach ($line in [IO.File]::ReadAllLines($rel)) {
            if ($line -match 'JAVA_VERSION="?(\d+)') { return [int]$Matches[1] }
        }
    }
    $exe = Join-Path $javaHomeDir 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $exe)) { return 0 }
    try {
        $out = & $exe -version 2>&1 | Out-String
        if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    } catch {}
    return 0
}

function Find-JavaHome {
    $raw = New-Object System.Collections.Generic.List[string]
    foreach ($d in @(
        $env:JAVA_HOME,
        (Join-Path $env:USERPROFILE '.jdks\ms-21.0.11'),
        (Join-Path $env:USERPROFILE '.jdks\ms-21.0.10'),
        (Join-Path $env:USERPROFILE '.jdks\openjdk-26'),
        (Join-Path $env:USERPROFILE '.jdks\ms-25.0.2'),
        'C:\Program Files\Java\jdk-26.0.1',
        'C:\Program Files\Java\jdk-24',
        'C:\Program Files\Java\latest'
    )) {
        if ($d) { [void]$raw.Add([string]$d) }
    }
    foreach ($rootDir in @(
        (Join-Path $env:USERPROFILE '.jdks'),
        'C:\Program Files\Java',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\BellSoft'
    )) {
        if (Test-Path $rootDir) {
            Get-ChildItem $rootDir -Directory -ErrorAction SilentlyContinue | ForEach-Object { [void]$raw.Add($_.FullName) }
        }
    }

    $ok = New-Object System.Collections.Generic.List[object]
    $seen = @{}
    foreach ($dir in $raw) {
        $jdkDir = ([string]$dir).TrimEnd('\', '/')
        if (-not $jdkDir) { continue }
        $key = $jdkDir.ToLowerInvariant()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        if (-not (Test-Path -LiteralPath (Join-Path $jdkDir 'bin\java.exe'))) { continue }
        $maj = Get-JavaMajorVersion $jdkDir
        if ($maj -ge 21) {
            [void]$ok.Add([pscustomobject]@{ Dir = $jdkDir; Major = $maj })
        }
    }
    if ($ok.Count -eq 0) { return $null }
    $j21 = @($ok | Where-Object { $_.Major -eq 21 } | Sort-Object Dir -Descending)
    if ($j21.Count -gt 0) { return [string]$j21[0].Dir }
    return [string](@($ok | Sort-Object Major -Descending)[0].Dir)
}

function Get-MalicePng {
    $named = Join-Path $env:USERPROFILE '.cursor\projects\c-Users-admin-Desktop-DeltaClient-main\assets\c__Users_admin_AppData_Roaming_Cursor_User_workspaceStorage_debe26d56c6328f0001cac9d3d28e03e_images_image-2986cd45-4e8c-498f-ba8f-e2f0a1eb60eb.png'
    $candidates = @(
        (Join-Path $root 'malice.png'),
        (Join-Path $root 'src\main\resources\assets\skeleton\pictures\avatar.png'),
        (Join-Path $root 'src\main\resources\assets\skeleton\icon.png'),
        $named
    )
    foreach ($p in $candidates) {
        if ($p -and (Test-Path -LiteralPath $p)) { return $p }
    }
    return $null
}

function Ensure-MalicePng {
    $png = Join-Path $root 'malice.png'
    if (Test-Path -LiteralPath $png) { return $png }
    $src = Get-MalicePng
    if ($src -and ($src -ne $png)) {
        Copy-Item -LiteralPath $src -Destination $png -Force
        return $png
    }
    return $src
}

function Convert-PngToIco([string]$pngPath, [string]$icoPath) {
    Add-Type -AssemblyName System.Drawing | Out-Null
    $fsIn = [IO.File]::OpenRead($pngPath)
    try {
        $src = New-Object System.Drawing.Bitmap $fsIn
        try {
            $sizes = @(16, 32, 48, 256)
            $chunks = New-Object 'System.Collections.Generic.List[object]'
            foreach ($s in $sizes) {
                $bmp = New-Object System.Drawing.Bitmap $s, $s, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
                $g = [System.Drawing.Graphics]::FromImage($bmp)
                $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $g.Clear([System.Drawing.Color]::Transparent)
                $scale = [Math]::Min($s / [double]$src.Width, $s / [double]$src.Height)
                $nw = [int][Math]::Max(1, [Math]::Round($src.Width * $scale))
                $nh = [int][Math]::Max(1, [Math]::Round($src.Height * $scale))
                $g.DrawImage($src, [int](($s - $nw) / 2), [int](($s - $nh) / 2), $nw, $nh)
                $g.Dispose()
                $ms = New-Object IO.MemoryStream
                $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
                $chunks.Add(@{ Size = $s; Data = $ms.ToArray() })
                $bmp.Dispose()
                $ms.Dispose()
            }
        } finally {
            $src.Dispose()
        }
    } finally {
        $fsIn.Close()
    }

    $msOut = New-Object IO.MemoryStream
    $bw = New-Object IO.BinaryWriter $msOut
    $bw.Write([uint16]0)
    $bw.Write([uint16]1)
    $bw.Write([uint16]$chunks.Count)
    $offset = 6 + (16 * $chunks.Count)
    foreach ($chunk in $chunks) {
        $size = [int]$chunk.Size
        $data = [byte[]]$chunk.Data
        $dim = [byte]$(if ($size -ge 256) { 0 } else { $size })
        $bw.Write($dim)
        $bw.Write($dim)
        $bw.Write([byte]0)
        $bw.Write([byte]0)
        $bw.Write([uint16]1)
        $bw.Write([uint16]32)
        $bw.Write([uint32]$data.Length)
        $bw.Write([uint32]$offset)
        $offset += $data.Length
    }
    foreach ($chunk in $chunks) {
        $bw.Write([byte[]]$chunk.Data)
    }
    $bw.Flush()
    [IO.File]::WriteAllBytes($icoPath, $msOut.ToArray())
    $bw.Close()
}

function Build-LauncherExe([string]$icoPath) {
    $csc = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    $stub = Join-Path $root 'tools\MaliceVisualsStub.cs'
    $exe = Join-Path $root 'MaliceVisuals.exe'
    if (-not (Test-Path $csc) -or -not (Test-Path $stub)) { return $null }
    $sma = @(
        (Join-Path $env:WINDIR 'Microsoft.NET\assembly\GAC_MSIL\System.Management.Automation\v4.0_3.0.0.0__31bf3856ad364e35\System.Management.Automation.dll'),
        (Join-Path ${env:ProgramFiles(x86)} 'Reference Assemblies\Microsoft\WindowsPowerShell\3.0\System.Management.Automation.dll')
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if (-not $sma) { return $null }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $csc
    $psi.Arguments = "/nologo /target:winexe /platform:x64 /win32icon:`"$icoPath`" /reference:`"$sma`" /out:`"$exe`" `"$stub`""
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $p = [Diagnostics.Process]::Start($psi)
    $p.WaitForExit()
    if ($p.ExitCode -ne 0 -or -not (Test-Path $exe)) { return $null }
    return $exe
}

function Install-DesktopShortcut {
    $png = Ensure-MalicePng
    $ico = Join-Path $root 'malice.ico'
    if (-not $png) { throw 'malice.png not found' }
    Convert-PngToIco $png $ico
    $exe = Build-LauncherExe $ico
    $ps1 = Join-Path $root 'MaliceLauncher.ps1'
    $desktop = [Environment]::GetFolderPath('Desktop')
    $lnkPath = Join-Path $desktop 'Malice Visuals.lnk'
    if (Test-Path $lnkPath) { Remove-Item $lnkPath -Force }
    $shell = New-Object -ComObject WScript.Shell
    $lnk = $shell.CreateShortcut($lnkPath)
    if ($exe) {
        $lnk.TargetPath = $exe
        $lnk.Arguments = ''
        $lnk.IconLocation = "$exe,0"
    } else {
        $lnk.TargetPath = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
        $lnk.Arguments = "-STA -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$ps1`""
        $lnk.IconLocation = "$ico,0"
    }
    $lnk.WorkingDirectory = $root
    $lnk.WindowStyle = 1
    $lnk.Description = 'Malice Visuals'
    $lnk.Save()
    return $lnkPath
}

function Load-LauncherConfig {
    $script:ramMb = 4096
    $script:username = $env:USERNAME
    if (-not $script:username) { $script:username = 'Player' }
    $script:uid = Get-Random -Minimum 10000 -Maximum 99999
    if (Test-Path $script:cfgPath) {
        try {
            $j = Get-Content -LiteralPath $script:cfgPath -Raw -Encoding UTF8 | ConvertFrom-Json
            if ($j.ramMb) { $script:ramMb = [int]$j.ramMb }
            if ($j.username) { $script:username = [string]$j.username }
            if ($j.uid) { $script:uid = [string]$j.uid }
            if ($null -ne $j.closeOnLaunch) { $script:closeOnLaunch = [bool]$j.closeOnLaunch }
        } catch {}
    }
}

function Save-LauncherConfig {
    $json = @{
        ramMb         = [int]$script:ramMb
        username      = [string]$script:username
        uid           = [string]$script:uid
        closeOnLaunch = [bool]$script:closeOnLaunch
    } | ConvertTo-Json
    [IO.File]::WriteAllText($script:cfgPath, $json, (New-Object System.Text.UTF8Encoding $false))
}

function Get-ModsDir {
    $d = Join-Path $root 'run\mods'
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
    return $d
}

function Get-VoiceChatJars {
    Get-ChildItem (Get-ModsDir) -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '(?i)voicechat' }
}

function Test-VoiceChatInstalled { return [bool](Get-VoiceChatJars) }

function Get-FriendsFile {
    $dir = Join-Path $root 'run\configs\general'
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    Join-Path $dir 'friends.json'
}

function Json-Esc([string]$s) {
    if ($null -eq $s) { $s = '' }
    return $s.Replace('\', '\\').Replace('"', '\"').Replace("`r", '').Replace("`n", ' ')
}

function Json-Bool([bool]$b) { if ($b) { 'true' } else { 'false' } }

function Read-FriendsState {
    $script:friends = New-Object System.Collections.Generic.List[object]
    $script:friendNoDamage = $true
    $script:friendHighlight = $true
    $script:friendNoPush = $true
    $script:friendSkipEsp = $true
    $file = Get-FriendsFile
    if (-not (Test-Path $file)) { return }
    try {
        $j = Get-Content -LiteralPath $file -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($null -ne $j.noDamage) { $script:friendNoDamage = [bool]$j.noDamage }
        if ($null -ne $j.highlight) { $script:friendHighlight = [bool]$j.highlight }
        if ($null -ne $j.noPush) { $script:friendNoPush = [bool]$j.noPush }
        if ($null -ne $j.skipEsp) { $script:friendSkipEsp = [bool]$j.skipEsp }
        foreach ($f in @($j.friends)) {
            $n = [string]$f.name
            if ($n) {
                $script:friends.Add([pscustomobject]@{
                    name     = $n
                    note     = $(if ($f.PSObject.Properties['note']) { [string]$f.note } else { '' })
                    favorite = [bool]$(if ($f.PSObject.Properties['favorite']) { $f.favorite } else { $false })
                })
            }
        }
    } catch {}
}

function Write-FriendsState {
    $parts = New-Object System.Collections.Generic.List[string]
    foreach ($f in $script:friends) {
        $parts.Add(('    { "name": "' + (Json-Esc $f.name) + '", "note": "' + (Json-Esc $f.note) + '", "favorite": ' + (Json-Bool ([bool]$f.favorite)) + ' }'))
    }
    $json = @(
        '{',
        ('  "noDamage": ' + (Json-Bool $script:friendNoDamage) + ','),
        ('  "highlight": ' + (Json-Bool $script:friendHighlight) + ','),
        ('  "noPush": ' + (Json-Bool $script:friendNoPush) + ','),
        ('  "skipEsp": ' + (Json-Bool $script:friendSkipEsp) + ','),
        '  "friends": [',
        ($parts -join ",`r`n"),
        '  ]',
        '}'
    ) -join "`r`n"
    [IO.File]::WriteAllText((Get-FriendsFile), $json, (New-Object System.Text.UTF8Encoding $false))
}

function Get-LauncherFont {
    $ttf = Join-Path $root 'tools\fonts\Outfit.ttf'
    if (Test-Path $ttf) {
        $uri = New-Object System.Uri ((Resolve-Path $ttf).Path)
        return New-Object System.Windows.Media.FontFamily ($uri.AbsoluteUri + '#Outfit')
    }
    return New-Object System.Windows.Media.FontFamily 'Bahnschrift, Segoe UI Variable Display, Segoe UI'
}

function Ensure-KeyCatalog {
    if (Test-Path -LiteralPath $script:keysPath) { return }
    $json = @'
{
  "keys": [
    { "code": "MV-30-968B-231C-A08D", "days": 30 },
    { "code": "MV-90-EA40-FD78-3216", "days": 90 },
    { "code": "MV-180-E343-A12B-8A39", "days": 180 }
  ]
}
'@
    [IO.File]::WriteAllText($script:keysPath, $json.Trim() + "`r`n", (New-Object System.Text.UTF8Encoding $false))
}

function Normalize-Key([string]$s) {
    if ([string]::IsNullOrWhiteSpace($s)) { return '' }
    return (($s.ToUpperInvariant()) -replace '[^A-Z0-9]', '')
}

function Read-KeyCatalog {
    Ensure-KeyCatalog
    try {
        $j = Get-Content -LiteralPath $script:keysPath -Raw -Encoding UTF8 | ConvertFrom-Json
        return @($j.keys)
    } catch {
        return @()
    }
}

function Find-CatalogKey([string]$raw) {
    $n = Normalize-Key $raw
    if (-not $n) { return $null }
    foreach ($k in (Read-KeyCatalog)) {
        if ((Normalize-Key ([string]$k.code)) -eq $n) { return $k }
    }
    return $null
}

function Convert-LicenseTime($v) {
    if ($null -eq $v) { return [datetime]::MinValue }
    if ($v -is [datetime]) { return ([datetime]$v).ToUniversalTime() }
    return [datetime]::Parse([string]$v, $null, [Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
}

function Read-License {
    $script:license = $null
    if (-not (Test-Path -LiteralPath $script:licensePath)) { return $null }
    try {
        $script:license = Get-Content -LiteralPath $script:licensePath -Raw -Encoding UTF8 | ConvertFrom-Json
        return $script:license
    } catch {
        return $null
    }
}

function Test-LicenseValid {
    $lic = Read-License
    if (-not $lic) { return $false }
    if (-not (Find-CatalogKey ([string]$lic.key))) { return $false }
    return ((Convert-LicenseTime $lic.expiresAt) -gt [datetime]::UtcNow)
}

function Save-License($keyObj) {
    $now = [datetime]::UtcNow
    $days = [int]$keyObj.days
    $payload = @(
        '{',
        ('  "key": "' + (Json-Esc ([string]$keyObj.code)) + '",'),
        ('  "days": ' + $days + ','),
        ('  "activatedAt": "' + $now.ToString('o') + '",'),
        ('  "expiresAt": "' + $now.AddDays($days).ToString('o') + '"'),
        '}'
    ) -join "`r`n"
    [IO.File]::WriteAllText($script:licensePath, $payload, (New-Object System.Text.UTF8Encoding $false))
    Read-License | Out-Null
}

function Get-LicenseRemainingDays {
    if (-not $script:license) { return 0 }
    $span = (Convert-LicenseTime $script:license.expiresAt) - [datetime]::UtcNow
    if ($span.TotalDays -le 0) { return 0 }
    return [int][Math]::Ceiling($span.TotalDays)
}

if ($args -contains '-InstallShortcut') {
    Write-Host (Install-DesktopShortcut)
    exit 0
}

Load-LauncherConfig
Ensure-KeyCatalog
Read-FriendsState
$javaHome = Find-JavaHome
$pngPath = Ensure-MalicePng

[xml]$xaml = @"
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="Malice Visuals" ShowInTaskbar="True"
        Width="1180" Height="720" MinWidth="980" MinHeight="620"
        WindowStartupLocation="CenterScreen" WindowStyle="None"
        ResizeMode="CanResizeWithGrip" UseLayoutRounding="True" SnapsToDevicePixels="True"
        TextOptions.TextFormattingMode="Display" Background="#0C0C0E">
  <Window.Resources>
    <Style x:Key="NavBtnH" TargetType="Button">
      <Setter Property="Width" Value="32"/>
      <Setter Property="Height" Value="32"/>
      <Setter Property="MinWidth" Value="32"/>
      <Setter Property="Margin" Value="2,0"/>
      <Setter Property="Background" Value="Transparent"/>
      <Setter Property="Foreground" Value="#8B8B93"/>
      <Setter Property="Cursor" Value="Hand"/>
      <Setter Property="BorderThickness" Value="0"/>
      <Setter Property="FontFamily" Value="Segoe MDL2 Assets"/>
      <Setter Property="FontSize" Value="14"/>
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border Background="{TemplateBinding Background}" BorderThickness="0">
              <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
            </Border>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="CardBtn" TargetType="Button">
      <Setter Property="Foreground" Value="#F0F0F2"/>
      <Setter Property="Cursor" Value="Hand"/>
      <Setter Property="BorderThickness" Value="0"/>
      <Setter Property="Padding" Value="0"/>
      <Setter Property="HorizontalContentAlignment" Value="Stretch"/>
      <Setter Property="VerticalContentAlignment" Value="Stretch"/>
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border x:Name="bd" CornerRadius="12" Padding="{TemplateBinding Padding}" BorderThickness="1" BorderBrush="#2C2C32">
              <Border.Background>
                <SolidColorBrush x:Name="bg" Color="#161618"/>
              </Border.Background>
              <ContentPresenter HorizontalAlignment="Stretch" VerticalAlignment="Stretch"/>
            </Border>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Trigger.EnterActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#1E1E22" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.EnterActions>
                <Trigger.ExitActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#161618" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.ExitActions>
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="GhostBtn" TargetType="Button">
      <Setter Property="Foreground" Value="#F0F0F2"/>
      <Setter Property="Cursor" Value="Hand"/>
      <Setter Property="BorderThickness" Value="0"/>
      <Setter Property="Padding" Value="14,9"/>
      <Setter Property="FontWeight" Value="SemiBold"/>
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border x:Name="bd" CornerRadius="10" Padding="{TemplateBinding Padding}" BorderThickness="1" BorderBrush="#2C2C32">
              <Border.Background>
                <SolidColorBrush x:Name="bg" Color="#1A1A1D"/>
              </Border.Background>
              <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
            </Border>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Trigger.EnterActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#242428" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.EnterActions>
                <Trigger.ExitActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#1A1A1D" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.ExitActions>
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="PrimaryBtn" TargetType="Button">
      <Setter Property="Foreground" Value="White"/>
      <Setter Property="Cursor" Value="Hand"/>
      <Setter Property="BorderThickness" Value="0"/>
      <Setter Property="Padding" Value="18,11"/>
      <Setter Property="FontWeight" Value="SemiBold"/>
      <Setter Property="Template">
        <Setter.Value>
          <ControlTemplate TargetType="Button">
            <Border x:Name="bd" CornerRadius="10" Padding="{TemplateBinding Padding}">
              <Border.Background>
                <SolidColorBrush x:Name="bg" Color="#6B7394"/>
              </Border.Background>
              <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
            </Border>
            <ControlTemplate.Triggers>
              <Trigger Property="IsMouseOver" Value="True">
                <Trigger.EnterActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#7C84A6" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.EnterActions>
                <Trigger.ExitActions>
                  <BeginStoryboard>
                    <Storyboard>
                      <ColorAnimation Storyboard.TargetName="bg" Storyboard.TargetProperty="Color" To="#6B7394" Duration="0:0:0.12"/>
                    </Storyboard>
                  </BeginStoryboard>
                </Trigger.ExitActions>
              </Trigger>
            </ControlTemplate.Triggers>
          </ControlTemplate>
        </Setter.Value>
      </Setter>
    </Style>
    <Style x:Key="FieldBox" TargetType="TextBox">
      <Setter Property="Background" Value="#1A1A1D"/>
      <Setter Property="Foreground" Value="#F0F0F2"/>
      <Setter Property="BorderBrush" Value="#2C2C32"/>
      <Setter Property="BorderThickness" Value="1"/>
      <Setter Property="Padding" Value="12,10"/>
      <Setter Property="CaretBrush" Value="#6B7394"/>
      <Setter Property="FontSize" Value="14"/>
    </Style>
  </Window.Resources>
  <Grid>
    <Grid x:Name="MainShell">
    <Grid.ColumnDefinitions>
      <ColumnDefinition Width="400"/>
      <ColumnDefinition Width="*"/>
    </Grid.ColumnDefinitions>

    <Grid Grid.Column="0" Background="#0A0A0C">
      <Image x:Name="BrandBg" Stretch="UniformToFill" Opacity="0.16"/>
      <Border Background="#AA0A0A0C"/>
      <StackPanel VerticalAlignment="Center" HorizontalAlignment="Center" Width="300">
        <Border Width="92" Height="92" CornerRadius="20" HorizontalAlignment="Center" ClipToBounds="True" Background="#141416">
          <Image x:Name="BrandLogo" Stretch="UniformToFill"/>
        </Border>
        <TextBlock Text="malice visuals" FontSize="28" FontWeight="Bold" Foreground="#F5F5F5"
                   HorizontalAlignment="Center" TextAlignment="Center" Margin="0,22,0,10"/>
        <TextBlock Text="Контроль, скорость и точность — всё уже внутри."
                   TextWrapping="Wrap" TextAlignment="Center" FontSize="13" Foreground="#8B8B93" LineHeight="20"/>
      </StackPanel>
    </Grid>

    <Grid Grid.Column="1" Background="#101012">
      <Grid.RowDefinitions>
        <RowDefinition Height="48"/>
        <RowDefinition Height="*"/>
        <RowDefinition Height="44"/>
      </Grid.RowDefinitions>

      <Border x:Name="TitleBar" Grid.Row="0" Background="#101012">
        <Button x:Name="CloseBtn" Style="{StaticResource GhostBtn}" Content="&#xE711;" FontFamily="Segoe MDL2 Assets"
                HorizontalAlignment="Right" Width="40" Height="40" Padding="0" Margin="0,4,8,0"
                Foreground="#8B8B93"/>
      </Border>
      <Button x:Name="MinBtn" Visibility="Collapsed"/>

      <Grid Grid.Row="1">
        <ScrollViewer x:Name="PageHome" Padding="48,8,48,24" VerticalScrollBarVisibility="Auto">
          <StackPanel MaxWidth="560" HorizontalAlignment="Left">
            <TextBlock x:Name="HomeHello" FontSize="13" Foreground="#8B8B93" Margin="0,0,0,8"/>
            <TextBlock Text="Клиент" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
            <TextBlock Text="Выбери сборку и запусти. Моды и друзья — в панели внизу." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,28"/>
            <Button x:Name="VersionCard" Style="{StaticResource CardBtn}" HorizontalAlignment="Stretch" ToolTip="Запустить">
              <Grid IsHitTestVisible="False">
                <Grid.ColumnDefinitions>
                  <ColumnDefinition Width="120"/>
                  <ColumnDefinition Width="*"/>
                </Grid.ColumnDefinitions>
                <Border Width="88" Height="88" CornerRadius="16" Margin="16" ClipToBounds="True" Background="#0C0C0E">
                  <Image x:Name="CardAvatarImg" Stretch="UniformToFill"/>
                </Border>
                <StackPanel Grid.Column="1" VerticalAlignment="Center" Margin="0,16,18,16">
                  <TextBlock Text="malice visuals" FontSize="12" Foreground="#8B8B93"/>
                  <TextBlock Text="1.21.4" FontSize="22" FontWeight="Bold" Foreground="#F5F5F5" Margin="0,4,0,8"/>
                  <TextBlock x:Name="CardArrow" Text="&#xE768;" FontFamily="Segoe MDL2 Assets" FontSize="16" Foreground="#8B8B93"/>
                </StackPanel>
              </Grid>
            </Button>
            <UniformGrid Columns="2" Margin="0,16,0,0">
              <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="14" Margin="0,0,8,8">
                <StackPanel>
                  <TextBlock Text="память" FontSize="11" Foreground="#8B8B93"/>
                  <TextBlock x:Name="StatRam" FontSize="16" Foreground="#F0F0F2" Margin="0,4,0,0"/>
                </StackPanel>
              </Border>
              <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="14" Margin="8,0,0,8">
                <StackPanel>
                  <TextBlock Text="друзья" FontSize="11" Foreground="#8B8B93"/>
                  <TextBlock x:Name="StatFriends" FontSize="16" Foreground="#F0F0F2" Margin="0,4,0,0"/>
                </StackPanel>
              </Border>
              <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="14" Margin="0,8,8,0">
                <StackPanel>
                  <TextBlock Text="voice chat" FontSize="11" Foreground="#8B8B93"/>
                  <TextBlock x:Name="StatVoice" FontSize="16" Foreground="#F0F0F2" Margin="0,4,0,0"/>
                </StackPanel>
              </Border>
              <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="14" Margin="8,8,0,0">
                <StackPanel>
                  <TextBlock Text="тариф" FontSize="11" Foreground="#8B8B93"/>
                  <TextBlock x:Name="StatPlan" FontSize="16" Foreground="#F0F0F2" Margin="0,4,0,0"/>
                </StackPanel>
              </Border>
            </UniformGrid>
            <TextBlock x:Name="HomeNickHint" FontSize="12" Foreground="#6B6B73" Margin="0,16,0,0"/>
          </StackPanel>
        </ScrollViewer>

        <ScrollViewer x:Name="PageFriends" Padding="48,8,48,24" VerticalScrollBarVisibility="Auto" Visibility="Collapsed">
          <StackPanel MaxWidth="560" HorizontalAlignment="Left">
            <TextBlock Text="Друзья" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
            <TextBlock Text="Список общий с клиентом." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,22"/>
            <DockPanel Margin="0,0,0,16">
              <Button x:Name="AddFriendBtn" DockPanel.Dock="Right" Style="{StaticResource PrimaryBtn}" FontFamily="Segoe MDL2 Assets" Content="&#xE710;" FontSize="14" Width="48" Padding="0" Margin="10,0,0,0" ToolTip="Добавить"/>
              <TextBox x:Name="FriendNickBox" Style="{StaticResource FieldBox}"/>
            </DockPanel>
            <TextBlock x:Name="FriendsEmpty" Text="Пока никого нет." Foreground="#8B8B93" Margin="0,0,0,8"/>
            <StackPanel x:Name="FriendsList"/>
          </StackPanel>
        </ScrollViewer>

        <ScrollViewer x:Name="PageMods" Padding="48,8,48,24" VerticalScrollBarVisibility="Auto" Visibility="Collapsed">
          <StackPanel MaxWidth="560" HorizontalAlignment="Left">
            <TextBlock Text="Моды" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
            <TextBlock Text="Установка в run\mods для Fabric 1.21.4." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,22"/>
            <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="18">
              <Grid>
                <Grid.ColumnDefinitions>
                  <ColumnDefinition Width="*"/>
                  <ColumnDefinition Width="Auto"/>
                </Grid.ColumnDefinitions>
                <StackPanel>
                  <TextBlock Text="Simple Voice Chat" FontSize="16" FontWeight="SemiBold" Foreground="#F5F5F5"/>
                  <TextBlock TextWrapping="Wrap" Foreground="#8B8B93" FontSize="13" Margin="0,6,12,8"
                             Text="Голосовой чат. Пока доступен только этот мод."/>
                  <TextBlock x:Name="ModStatus" FontSize="12" Foreground="#8B8B93" Text="Проверка..."/>
                </StackPanel>
                <Button x:Name="ModBtn" Grid.Column="1" Style="{StaticResource PrimaryBtn}" Content="Установить" VerticalAlignment="Center"/>
              </Grid>
            </Border>
          </StackPanel>
        </ScrollViewer>

        <ScrollViewer x:Name="PageProfile" Padding="48,8,48,24" VerticalScrollBarVisibility="Auto" Visibility="Collapsed">
          <StackPanel MaxWidth="420" HorizontalAlignment="Left">
            <TextBlock Text="Профиль" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
            <TextBlock Text="Локальные данные лаунчера." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,24"/>
            <Grid Width="88" Height="88" HorizontalAlignment="Left" Margin="0,0,0,16">
              <Border CornerRadius="16" Background="#1A1A1D" ClipToBounds="True">
                <Image x:Name="ProfileAvatarImg" Stretch="UniformToFill"/>
              </Border>
            </Grid>
            <Button x:Name="AvatarBtn" Style="{StaticResource GhostBtn}" Content="Загрузить аватар" HorizontalAlignment="Left" Margin="0,0,0,8"/>
            <TextBlock x:Name="AvatarStatus" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,20"/>
            <TextBlock Text="имя пользователя" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,6"/>
            <TextBox x:Name="NickBox" Style="{StaticResource FieldBox}" Margin="0,0,0,14"/>
            <Grid Margin="0,0,0,10"><TextBlock Text="UID" Foreground="#8B8B93"/><TextBlock x:Name="UidLabel" HorizontalAlignment="Right" Foreground="#F0F0F2"/></Grid>
            <Grid Margin="0,0,0,10"><TextBlock Text="клиент" Foreground="#8B8B93"/><TextBlock Text="Fabric 1.21.4" HorizontalAlignment="Right" Foreground="#F0F0F2"/></Grid>
            <Grid><TextBlock Text="тариф" Foreground="#8B8B93"/><TextBlock x:Name="PlanLabel" HorizontalAlignment="Right" Foreground="#F0F0F2"/></Grid>
          </StackPanel>
        </ScrollViewer>

        <ScrollViewer x:Name="PageSettings" Padding="48,8,48,24" VerticalScrollBarVisibility="Auto" Visibility="Collapsed">
          <StackPanel MaxWidth="520" HorizontalAlignment="Left">
            <TextBlock Text="Настройки" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
            <TextBlock Text="Память, путь и ярлык." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,24"/>
            <TextBlock Text="оперативная память" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,8"/>
            <Grid Margin="0,0,0,20">
              <Grid.ColumnDefinitions>
                <ColumnDefinition Width="*"/>
                <ColumnDefinition Width="80"/>
                <ColumnDefinition Width="Auto"/>
              </Grid.ColumnDefinitions>
              <Slider x:Name="RamSlider" Minimum="1024" Maximum="16384" TickFrequency="256" IsSnapToTickEnabled="True" VerticalAlignment="Center"/>
              <TextBox x:Name="RamBox" Grid.Column="1" Style="{StaticResource FieldBox}" Margin="10,0,8,0" Padding="8,6"/>
              <TextBlock Grid.Column="2" Text="МБ" Foreground="#8B8B93" VerticalAlignment="Center"/>
            </Grid>
            <TextBlock Text="расположение" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,8"/>
            <DockPanel Margin="0,0,0,20">
              <Button x:Name="OpenFolderBtn" DockPanel.Dock="Right" Style="{StaticResource GhostBtn}" Content="Открыть" Margin="8,0,0,0"/>
              <TextBox x:Name="PathBox" Style="{StaticResource FieldBox}" IsReadOnly="True"/>
            </DockPanel>
            <Button x:Name="ShortcutBtn" Style="{StaticResource PrimaryBtn}" Content="Создать ярлык  →" HorizontalAlignment="Left"/>
            <TextBlock x:Name="ShortcutStatus" FontSize="12" Foreground="#8B8B93" Margin="0,10,0,16"/>
            <Button x:Name="CloseOnLaunchBtn" Style="{StaticResource CardBtn}" Padding="14" HorizontalAlignment="Stretch" Margin="0,0,0,20">
              <Grid IsHitTestVisible="False">
                <StackPanel>
                  <TextBlock Text="при запуске игры" FontSize="11" Foreground="#8B8B93"/>
                  <TextBlock x:Name="CloseOnLaunchValue" FontSize="16" Foreground="#F0F0F2" Margin="0,4,0,0"/>
                </StackPanel>
              </Grid>
            </Button>
            <TextBlock Text="java" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,6"/>
            <TextBlock x:Name="JavaLabel" TextWrapping="Wrap" Foreground="#F0F0F2" FontSize="13"/>
          </StackPanel>
        </ScrollViewer>

        <Grid x:Name="LaunchOverlay" Visibility="Collapsed" Opacity="0" Background="#CC0C0C0E">
          <Border Width="460" Background="#101012" CornerRadius="14" Padding="28" HorizontalAlignment="Center" VerticalAlignment="Center" BorderBrush="#2C2C32" BorderThickness="1">
            <StackPanel>
              <Grid Margin="0,0,0,18">
                <TextBlock Text="Запуск" FontSize="24" FontWeight="Bold" Foreground="#F5F5F5"/>
                <Button x:Name="BackBtn" Style="{StaticResource GhostBtn}" Content="назад" HorizontalAlignment="Right" Padding="12,6"/>
              </Grid>
              <Border Width="72" Height="72" CornerRadius="16" ClipToBounds="True" HorizontalAlignment="Left" Margin="0,0,0,16" Background="#161618">
                <Image x:Name="ModalAvatarImg" Stretch="UniformToFill"/>
              </Border>
              <TextBlock Text="malice visuals  ·  1.21.4" FontSize="13" Foreground="#8B8B93" Margin="0,0,0,14"/>
              <Border Background="#161618" CornerRadius="12" BorderBrush="#2C2C32" BorderThickness="1" Padding="14" Margin="0,0,0,12">
                <StackPanel>
                  <Grid>
                    <TextBlock Text="статус" FontSize="11" Foreground="#8B8B93"/>
                    <TextBlock x:Name="LaunchPercent" Text="0%" FontSize="16" FontWeight="SemiBold" Foreground="#F5F5F5" HorizontalAlignment="Right"/>
                  </Grid>
                  <Grid x:Name="LaunchBarTrack" Height="8" Margin="0,10,0,10">
                    <Border Background="#1A1A1D" CornerRadius="4"/>
                    <Border x:Name="LaunchBarFill" Background="#6B7394" CornerRadius="4" HorizontalAlignment="Left" Width="0"/>
                  </Grid>
                  <TextBlock x:Name="LaunchNotify" Text="ожидание запуска" FontSize="13" Foreground="#F0F0F2" TextWrapping="Wrap"/>
                </StackPanel>
              </Border>
              <StackPanel x:Name="LaunchNotes" Margin="0,0,0,12"/>
              <TextBlock x:Name="LaunchStatus" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,10"/>
              <TextBox x:Name="LogBox" Height="120" Background="#0C0C0E" Foreground="#8B8B93"
                       BorderThickness="1" BorderBrush="#2C2C32" IsReadOnly="True" TextWrapping="Wrap"
                       VerticalScrollBarVisibility="Auto" FontFamily="Consolas" FontSize="11" Padding="8"/>
            </StackPanel>
          </Border>
        </Grid>
      </Grid>

      <Border Grid.Row="2" Background="#0C0C0E" BorderBrush="#1F1F22" BorderThickness="0,1,0,0">
        <Grid Margin="12,0">
          <StackPanel Orientation="Horizontal" VerticalAlignment="Center">
            <Button x:Name="NavHome" Style="{StaticResource NavBtnH}" Content="&#xE80F;" ToolTip="Главная"/>
            <Button x:Name="NavFriends" Style="{StaticResource NavBtnH}" Content="&#xE716;" ToolTip="Друзья"/>
            <Button x:Name="NavMods" Style="{StaticResource NavBtnH}" Content="&#xE7B8;" ToolTip="Моды"/>
            <Button x:Name="NavSettings" Style="{StaticResource NavBtnH}" Content="&#xE713;" ToolTip="Настройки"/>
          </StackPanel>
          <StackPanel Orientation="Horizontal" HorizontalAlignment="Right" VerticalAlignment="Center">
            <Button x:Name="NavProfile" Style="{StaticResource NavBtnH}" ToolTip="Профиль" Padding="0">
              <Border Width="16" Height="16" CornerRadius="4" ClipToBounds="True" Background="Transparent" IsHitTestVisible="False">
                <Image x:Name="NavAvatarImg" Stretch="UniformToFill"/>
              </Border>
            </Button>
            <Button x:Name="NavExit" Style="{StaticResource NavBtnH}" Content="&#xE7E8;" ToolTip="Выход"/>
          </StackPanel>
        </Grid>
      </Border>
    </Grid>
    </Grid>

    <Grid x:Name="GatePanel" Background="#0C0C0E">
      <Grid.ColumnDefinitions>
        <ColumnDefinition Width="400"/>
        <ColumnDefinition Width="*"/>
      </Grid.ColumnDefinitions>
      <Grid x:Name="GateSide" Grid.Column="0" Background="#0A0A0C">
        <Image x:Name="GateBrandBg" Stretch="UniformToFill" Opacity="0.16"/>
        <Border Background="#AA0A0A0C"/>
        <StackPanel VerticalAlignment="Center" HorizontalAlignment="Center" Width="300">
          <Border Width="92" Height="92" CornerRadius="20" HorizontalAlignment="Center" ClipToBounds="True" Background="#141416">
            <Image x:Name="GateBrandLogo" Stretch="UniformToFill"/>
          </Border>
          <TextBlock Text="malice visuals" FontSize="28" FontWeight="Bold" Foreground="#F5F5F5"
                     HorizontalAlignment="Center" TextAlignment="Center" Margin="0,22,0,10"/>
          <TextBlock Text="Контроль, скорость и точность — всё уже внутри."
                     TextWrapping="Wrap" TextAlignment="Center" FontSize="13" Foreground="#8B8B93" LineHeight="20"/>
        </StackPanel>
      </Grid>
      <Grid Grid.Column="1" Background="#101012">
        <Button x:Name="GateCloseBtn" Style="{StaticResource GhostBtn}" Content="&#xE711;" FontFamily="Segoe MDL2 Assets"
                HorizontalAlignment="Right" VerticalAlignment="Top" Width="40" Height="40" Padding="0" Margin="0,8,12,0"
                Foreground="#8B8B93"/>
        <StackPanel VerticalAlignment="Center" MaxWidth="420" Margin="48,0,48,0" HorizontalAlignment="Left">
          <TextBlock x:Name="GateHello" FontSize="13" Foreground="#8B8B93" Margin="0,0,0,8" Text="доступ к клиенту"/>
          <TextBlock Text="Тариф" FontSize="32" FontWeight="Bold" Foreground="#F5F5F5"/>
          <TextBlock Text="Выбери срок и введи ключ — после этого откроется лаунчер." FontSize="13" Foreground="#8B8B93" Margin="0,8,0,22" TextWrapping="Wrap"/>
          <UniformGrid Columns="3" Margin="0,0,0,18">
            <Button x:Name="Plan30Btn" Style="{StaticResource CardBtn}" Padding="14" Margin="0,0,6,0" Tag="30">
              <StackPanel IsHitTestVisible="False">
                <TextBlock Text="тариф" FontSize="11" Foreground="#8B8B93"/>
                <TextBlock Text="30 дней" FontSize="16" FontWeight="SemiBold" Foreground="#F5F5F5" Margin="0,4,0,0"/>
              </StackPanel>
            </Button>
            <Button x:Name="Plan90Btn" Style="{StaticResource CardBtn}" Padding="14" Margin="6,0" Tag="90">
              <StackPanel IsHitTestVisible="False">
                <TextBlock Text="тариф" FontSize="11" Foreground="#8B8B93"/>
                <TextBlock Text="90 дней" FontSize="16" FontWeight="SemiBold" Foreground="#F5F5F5" Margin="0,4,0,0"/>
              </StackPanel>
            </Button>
            <Button x:Name="Plan180Btn" Style="{StaticResource CardBtn}" Padding="14" Margin="6,0,0,0" Tag="180">
              <StackPanel IsHitTestVisible="False">
                <TextBlock Text="тариф" FontSize="11" Foreground="#8B8B93"/>
                <TextBlock Text="180 дней" FontSize="16" FontWeight="SemiBold" Foreground="#F5F5F5" Margin="0,4,0,0"/>
              </StackPanel>
            </Button>
          </UniformGrid>
          <TextBlock Text="ключ" FontSize="12" Foreground="#8B8B93" Margin="0,0,0,6"/>
          <TextBox x:Name="KeyBox" Style="{StaticResource FieldBox}" Margin="0,0,0,14"/>
          <Button x:Name="ActivateBtn" Style="{StaticResource CardBtn}" HorizontalAlignment="Stretch" Padding="16,14">
            <Grid IsHitTestVisible="False">
              <TextBlock Text="открыть лаунчер" FontSize="12" Foreground="#8B8B93" VerticalAlignment="Center"/>
              <TextBlock Text="&#xE72A;" FontFamily="Segoe MDL2 Assets" FontSize="14" Foreground="#F5F5F5"
                         HorizontalAlignment="Right" VerticalAlignment="Center"/>
            </Grid>
          </Button>
          <TextBlock x:Name="GateError" FontSize="12" Foreground="#8B8B93" Margin="0,12,0,0" TextWrapping="Wrap"/>
        </StackPanel>
      </Grid>
    </Grid>
  </Grid>
</Window>
"@

$window = [Windows.Markup.XamlReader]::Load((New-Object System.Xml.XmlNodeReader $xaml))
$window.FontFamily = Get-LauncherFont
$navHome = $window.FindName('NavHome')
$navFriends = $window.FindName('NavFriends')
$navMods = $window.FindName('NavMods')
$navSettings = $window.FindName('NavSettings')
$navProfile = $window.FindName('NavProfile')
$navExit = $window.FindName('NavExit')
$pageHome = $window.FindName('PageHome')
$pageFriends = $window.FindName('PageFriends')
$pageMods = $window.FindName('PageMods')
$pageProfile = $window.FindName('PageProfile')
$pageSettings = $window.FindName('PageSettings')
$versionCard = $window.FindName('VersionCard')
$cardArrow = $window.FindName('CardArrow')
$overlay = $window.FindName('LaunchOverlay')
$backBtn = $window.FindName('BackBtn')
$launchStatus = $window.FindName('LaunchStatus')
$launchNotify = $window.FindName('LaunchNotify')
$launchPercent = $window.FindName('LaunchPercent')
$launchBarTrack = $window.FindName('LaunchBarTrack')
$launchBarFill = $window.FindName('LaunchBarFill')
$launchNotes = $window.FindName('LaunchNotes')
$logBox = $window.FindName('LogBox')
$modBtn = $window.FindName('ModBtn')
$modStatus = $window.FindName('ModStatus')
$ramSlider = $window.FindName('RamSlider')
$ramBox = $window.FindName('RamBox')
$pathBox = $window.FindName('PathBox')
$openFolderBtn = $window.FindName('OpenFolderBtn')
$shortcutBtn = $window.FindName('ShortcutBtn')
$shortcutStatus = $window.FindName('ShortcutStatus')
$closeOnLaunchBtn = $window.FindName('CloseOnLaunchBtn')
$closeOnLaunchValue = $window.FindName('CloseOnLaunchValue')
$javaLabel = $window.FindName('JavaLabel')
$titleBar = $window.FindName('TitleBar')
$minBtn = $window.FindName('MinBtn')
$closeBtn = $window.FindName('CloseBtn')
$brandLogo = $window.FindName('BrandLogo')
$brandBg = $window.FindName('BrandBg')
$navAvatarImg = $window.FindName('NavAvatarImg')
$cardAvatarImg = $window.FindName('CardAvatarImg')
$modalAvatarImg = $window.FindName('ModalAvatarImg')
$profileAvatarImg = $window.FindName('ProfileAvatarImg')
$avatarBtn = $window.FindName('AvatarBtn')
$avatarStatus = $window.FindName('AvatarStatus')
$uidLabel = $window.FindName('UidLabel')
$nickBox = $window.FindName('NickBox')
$homeHello = $window.FindName('HomeHello')
$homeNickHint = $window.FindName('HomeNickHint')
$friendsList = $window.FindName('FriendsList')
$friendsEmpty = $window.FindName('FriendsEmpty')
$friendNickBox = $window.FindName('FriendNickBox')
$addFriendBtn = $window.FindName('AddFriendBtn')
$statRam = $window.FindName('StatRam')
$statFriends = $window.FindName('StatFriends')
$statVoice = $window.FindName('StatVoice')
$statPlan = $window.FindName('StatPlan')
$planLabel = $window.FindName('PlanLabel')
$mainShell = $window.FindName('MainShell')
$gatePanel = $window.FindName('GatePanel')
$gateSide = $window.FindName('GateSide')
$gateBrandLogo = $window.FindName('GateBrandLogo')
$gateBrandBg = $window.FindName('GateBrandBg')
$gateCloseBtn = $window.FindName('GateCloseBtn')
$gateHello = $window.FindName('GateHello')
$gateError = $window.FindName('GateError')
$keyBox = $window.FindName('KeyBox')
$activateBtn = $window.FindName('ActivateBtn')
$plan30Btn = $window.FindName('Plan30Btn')
$plan90Btn = $window.FindName('Plan90Btn')
$plan180Btn = $window.FindName('Plan180Btn')

function New-Fade([double]$from, [double]$to, [int]$ms) {
    $a = New-Object System.Windows.Media.Animation.DoubleAnimation
    $a.From = $from
    $a.To = $to
    $a.Duration = [System.Windows.Duration]::new([TimeSpan]::FromMilliseconds($ms))
    return $a
}

function Show-PageAnim($panel) {
    $panel.Opacity = 0
    $panel.Visibility = [Windows.Visibility]::Visible
    $panel.BeginAnimation([Windows.UIElement]::OpacityProperty, (New-Fade 0 1 140))
}

function Load-Bitmap([string]$path) {
    if (-not $path -or -not (Test-Path -LiteralPath $path)) { return $null }
    $bmp = New-Object System.Windows.Media.Imaging.BitmapImage
    $bmp.BeginInit()
    $bmp.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bmp.UriSource = New-Object System.Uri ((Resolve-Path -LiteralPath $path).Path)
    $bmp.EndInit()
    $bmp.Freeze()
    return $bmp
}

function Apply-TaskbarIcon {
    try { [void][MaliceNative]::SetCurrentProcessExplicitAppUserModelID('Malice.Visuals.Launcher') } catch {}
    $ico = Join-Path $root 'malice.ico'
    $png = Ensure-MalicePng
    if ($png -and -not (Test-Path $ico)) { Convert-PngToIco $png $ico }
    if (-not (Test-Path $ico)) { return }
    try {
        $uri = New-Object System.Uri ((Resolve-Path $ico).Path)
        $dec = New-Object System.Windows.Media.Imaging.IconBitmapDecoder($uri,
            [System.Windows.Media.Imaging.BitmapCreateOptions]::None,
            [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad)
        $window.Icon = $dec.Frames[0]
    } catch {}
    $helper = New-Object System.Windows.Interop.WindowInteropHelper $window
    $hwnd = $helper.EnsureHandle()
    if ($hwnd -ne [IntPtr]::Zero) {
        if ($script:windowIcon) { $script:windowIcon.Dispose() }
        $script:windowIcon = New-Object System.Drawing.Icon $ico
        [void][MaliceNative]::SendMessage($hwnd, 0x80, [IntPtr]0, $script:windowIcon.Handle)
        [void][MaliceNative]::SendMessage($hwnd, 0x80, [IntPtr]1, $script:windowIcon.Handle)
    }
}

function Apply-LogoImages([string]$path) {
    $bmp = Load-Bitmap $path
    if ($bmp) {
        foreach ($img in @($brandLogo, $brandBg, $navAvatarImg, $cardAvatarImg, $modalAvatarImg, $profileAvatarImg, $gateBrandLogo, $gateBrandBg)) {
            if ($img) { $img.Source = $bmp }
        }
        $window.Icon = $bmp
    }
    Apply-TaskbarIcon
}

function Set-NavLook($btn, [bool]$on) {
    $btn.Background = [System.Windows.Media.Brushes]::Transparent
    if ($on) {
        $btn.Foreground = $script:brushes.ConvertFrom($script:accent)
    } else {
        $btn.Foreground = $script:brushes.ConvertFrom('#8B8B93')
    }
}

function Set-Page([string]$name) {
    $script:page = $name
    foreach ($p in @($pageHome, $pageFriends, $pageMods, $pageProfile, $pageSettings)) {
        $p.Visibility = [Windows.Visibility]::Collapsed
        $p.BeginAnimation([Windows.UIElement]::OpacityProperty, $null)
    }
    $overlay.Visibility = [Windows.Visibility]::Collapsed
    $overlay.Opacity = 0
    Set-NavLook $navHome ($name -eq 'home')
    Set-NavLook $navFriends ($name -eq 'friends')
    Set-NavLook $navMods ($name -eq 'mods')
    Set-NavLook $navSettings ($name -eq 'settings')
    Set-NavLook $navProfile ($name -eq 'profile')
    switch ($name) {
        'home' { Show-PageAnim $pageHome }
        'friends' { Show-PageAnim $pageFriends }
        'mods' { Show-PageAnim $pageMods }
        'profile' { Show-PageAnim $pageProfile }
        'settings' { Show-PageAnim $pageSettings }
    }
}

function Show-LaunchOverlay {
    $overlay.Opacity = 0
    $overlay.Visibility = [Windows.Visibility]::Visible
    $overlay.BeginAnimation([Windows.UIElement]::OpacityProperty, (New-Fade 0 1 140))
}

function Hide-LaunchOverlay {
    $anim = New-Fade 1 0 120
    $anim.Add_Completed({ $overlay.Visibility = [Windows.Visibility]::Collapsed })
    $overlay.BeginAnimation([Windows.UIElement]::OpacityProperty, $anim)
}

function Add-Log([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return }
    $act = {
        $logBox.AppendText(("{0}`r`n" -f $text))
        $logBox.ScrollToEnd()
    }
    if ($window.Dispatcher.CheckAccess()) { & $act }
    else { [void]$window.Dispatcher.BeginInvoke($act) }
}

function Set-LaunchPercent([int]$pct) {
    if ($pct -lt 0) { $pct = 0 }
    if ($pct -gt 100) { $pct = 100 }
    if ($pct -lt $script:launchPct -and $script:launchPct -lt 100) { $pct = $script:launchPct }
    $script:launchPct = $pct
    $launchPercent.Text = ('{0}%' -f $pct)
    $tw = $launchBarTrack.ActualWidth
    if ($tw -lt 8) { $tw = 400 }
    $launchBarFill.Width = [Math]::Round($tw * $pct / 100.0)
}

function Add-LaunchNote([string]$text) {
    if (-not $launchNotes -or [string]::IsNullOrWhiteSpace($text)) { return }
    $tb = New-Object System.Windows.Controls.TextBlock
    $tb.Text = $text
    $tb.FontSize = 12
    $tb.Foreground = $script:brushes.ConvertFrom('#8B8B93')
    $tb.Margin = New-Object System.Windows.Thickness 0,0,0,4
    [void]$launchNotes.Children.Add($tb)
    while ($launchNotes.Children.Count -gt 5) { $launchNotes.Children.RemoveAt(0) }
}

function Set-LaunchNotify([string]$text, [bool]$isError = $false) {
    if ([string]::IsNullOrWhiteSpace($text)) { return }
    if ($text -eq $script:launchNote) {
        $launchStatus.Text = $text
        return
    }
    $script:launchNote = $text
    $launchNotify.Text = $text
    $launchStatus.Text = $text
    if ($isError) {
        $launchNotify.Foreground = $script:brushes.ConvertFrom('#F0F0F2')
        $launchBarFill.Background = $script:brushes.ConvertFrom('#5A5A62')
    } else {
        $launchNotify.Foreground = $script:brushes.ConvertFrom('#F0F0F2')
        $launchBarFill.Background = $script:brushes.ConvertFrom($script:accent)
    }
    Add-LaunchNote $text
}

function Apply-LaunchLine([string]$line) {
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith('__EXIT__:')) {
        $code = 0
        [void][int]::TryParse($line.Substring(9), [ref]$code)
        if ($script:gameStarted) {
            Set-LaunchNotify 'Клиент закрыт'
            Set-LaunchPercent 100
            $versionCard.IsEnabled = $true
            if ($script:closeOnLaunch) { $window.Close() }
        } elseif ($code -ne 0) {
            Set-LaunchNotify ('Ошибка запуска  ·  код {0}' -f $code) $true
            $versionCard.IsEnabled = $true
        } else {
            Set-LaunchNotify 'Процесс Gradle завершился'
            $versionCard.IsEnabled = $true
        }
        return
    }

    if ($line -match '(\d+)\s*%\s*(CONFIGURING|EXECUTING|INITIALIZING|WAITING)') {
        $g = [int]$Matches[1]
        Set-LaunchPercent ([Math]::Min(70, 8 + [int]($g * 0.6)))
    }
    if ($line -match 'Starting a Gradle Daemon|Starting Gradle Daemon') {
        Set-LaunchPercent 8
        Set-LaunchNotify 'Запуск Gradle'
    }
    if ($line -match 'Configure project|Evaluating settings|Settings evaluated') {
        Set-LaunchPercent 16
        Set-LaunchNotify 'Настройка проекта'
    }
    if ($line -match 'Task :compileJava|Compil(ing|e)') {
        Set-LaunchPercent 32
        Set-LaunchNotify 'Компиляция'
    }
    if ($line -match 'Task :processResources|Task :classes') {
        Set-LaunchPercent 45
        Set-LaunchNotify 'Обработка ресурсов'
    }
    if ($line -match 'Task :remap|Task :runClient') {
        Set-LaunchPercent 58
        Set-LaunchNotify 'Сборка клиента'
    }
    if ($line -match 'Loading Minecraft') {
        Set-LaunchPercent 78
        Set-LaunchNotify 'Загрузка Minecraft'
    }
    if ($line -match 'Fabric Loader|Loading \d+ mods') {
        Set-LaunchPercent 88
        Set-LaunchNotify 'Загрузка модов'
    }
    if ($line -match 'Setting user:|Reloading ResourceManager|OpenAL|LWJGL|Backend pipeline|Narrator library') {
        $script:gameStarted = $true
        Set-LaunchPercent 100
        Set-LaunchNotify 'Клиент запущен'
        $versionCard.IsEnabled = $true
        if ($script:closeOnLaunch) {
            $window.Hide()
            $window.ShowInTaskbar = $false
        }
    }
    if ($line -match 'BUILD FAILED|FAILURE:\s+Build failed|What went wrong') {
        Set-LaunchNotify 'Сборка не удалась' $true
        $versionCard.IsEnabled = $true
    }
}

function Read-LaunchLogTail {
    if (-not $script:launchLogPath -or -not (Test-Path -LiteralPath $script:launchLogPath)) { return }
    try {
        $fs = [IO.File]::Open($script:launchLogPath, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
        try {
            if ($fs.Length -lt $script:launchLogPos) { $script:launchLogPos = 0 }
            if ($fs.Length -le $script:launchLogPos) { return }
            [void]$fs.Seek($script:launchLogPos, [IO.SeekOrigin]::Begin)
            $n = [int]($fs.Length - $script:launchLogPos)
            $buf = New-Object byte[] $n
            $got = $fs.Read($buf, 0, $n)
            $script:launchLogPos += $got
            $chunk = [Text.Encoding]::Default.GetString($buf, 0, $got)
            $chunk = $chunk.Replace("`r`n", "`n").Replace("`r", "`n")
            $text = $script:launchLogCarry + $chunk
            $parts = $text -split "`n", -1
            if ($chunk.EndsWith("`n")) {
                $script:launchLogCarry = ''
            } else {
                $script:launchLogCarry = [string]$parts[-1]
                if ($parts.Length -le 1) { return }
                $parts = $parts[0..($parts.Length - 2)]
            }
            foreach ($line in $parts) {
                $s = [string]$line
                if ([string]::IsNullOrWhiteSpace($s)) { continue }
                Add-Log $s
                Apply-LaunchLine $s
            }
        } finally { $fs.Dispose() }
    } catch {}
}

function Drain-LaunchQueue {
    try {
        Read-LaunchLogTail
        $line = $null
        $n = 0
        while ($n -lt 80 -and $script:logQueue.TryDequeue([ref]$line)) {
            Apply-LaunchLine ([string]$line)
            $n++
            $line = $null
        }
        if ($script:proc -and -not $script:proc.HasExited -and -not $script:gameStarted) {
            $elapsed = 0
            if ($script:launchAt) { $elapsed = ((Get-Date) - $script:launchAt).TotalSeconds }
            $soft = [Math]::Min(68, 4 + [int]($elapsed * 0.9))
            if ($soft -gt $script:launchPct) { Set-LaunchPercent $soft }
            if ($elapsed -gt 3 -and $script:launchNote -eq 'Запуск Gradle') {
                Set-LaunchNotify 'Сборка  ·  подожди немного'
            }
        }
        if ($script:proc -and $script:proc.HasExited) {
            if ($script:launchLogPath -and (Test-Path -LiteralPath $script:launchLogPath)) {
                try {
                    if ((Get-Item -LiteralPath $script:launchLogPath).Length -gt $script:launchLogPos) { return }
                } catch {}
            }
            Apply-LaunchLine ('__EXIT__:{0}' -f $script:proc.ExitCode)
            $script:proc = $null
        }
    } catch {}
}

function Ensure-LaunchTimer {
    if ($script:launchTimer) { return }
    $script:launchTimer = New-Object System.Windows.Threading.DispatcherTimer
    $script:launchTimer.Interval = [TimeSpan]::FromMilliseconds(120)
    $script:launchTimer.Add_Tick({ Drain-LaunchQueue })
    $script:launchTimer.Start()
}

function Refresh-LicenseUi {
    $left = Get-LicenseRemainingDays
    $days = 0
    if ($script:license) { $days = [int]$script:license.days }
    if ($statPlan) { $statPlan.Text = ("{0} дн." -f $left) }
    if ($planLabel) {
        if ($days -gt 0) {
            $planLabel.Text = ("{0} дней  ·  ещё {1}" -f $days, $left)
        } else {
            $planLabel.Text = 'нет'
        }
    }
}

function Set-GatePlan([int]$days) {
    $script:gatePlan = $days
    foreach ($b in @($plan30Btn, $plan90Btn, $plan180Btn)) {
        if ([int]$b.Tag -eq $days) { $b.Opacity = 1 } else { $b.Opacity = 0.45 }
    }
    $gateHello.Text = ("тариф  ·  {0} дней" -f $days)
}

function Show-LicenseGate {
    $mainShell.Visibility = [Windows.Visibility]::Collapsed
    $gatePanel.Visibility = [Windows.Visibility]::Visible
    Set-GatePlan $script:gatePlan
    $lic = Read-License
    if ($lic -and -not (Test-LicenseValid)) {
        $gateHello.Text = 'срок ключа истёк — введи новый'
    }
}

function Open-LauncherShell {
    $gatePanel.Visibility = [Windows.Visibility]::Collapsed
    $mainShell.Visibility = [Windows.Visibility]::Visible
    Refresh-LicenseUi
}

function Activate-FromBox {
    $k = Find-CatalogKey $keyBox.Text
    if (-not $k) {
        $gateError.Text = 'Неверный ключ'
        return
    }
    Save-License $k
    if (-not (Test-LicenseValid)) {
        $gateError.Text = 'Не удалось активировать ключ'
        return
    }
    $gateError.Text = ''
    Open-LauncherShell
}

function Refresh-HomeStats {
    $statRam.Text = ("{0} МБ" -f $script:ramMb)
    $statFriends.Text = [string]$script:friends.Count
    if (Test-VoiceChatInstalled) { $statVoice.Text = 'установлен' } else { $statVoice.Text = 'нет' }
    Refresh-LicenseUi
    $homeNickHint.Text = $script:username
}

function Refresh-ModUi {
    if (Test-VoiceChatInstalled) {
        $modStatus.Text = 'Установлен в run\mods'
        $modStatus.Foreground = $script:brushes.ConvertFrom($script:accent)
        $modBtn.Content = 'Удалить'
    } else {
        $modStatus.Text = 'Не установлен'
        $modStatus.Foreground = $script:brushes.ConvertFrom('#8B8B93')
        $modBtn.Content = 'Установить'
    }
    Refresh-HomeStats
}

function New-FriendCard($friend) {
    $card = New-Object System.Windows.Controls.Border
    $card.Background = $script:brushes.ConvertFrom('#161618')
    $card.BorderBrush = $script:brushes.ConvertFrom('#2C2C32')
    $card.BorderThickness = New-Object System.Windows.Thickness 1
    $card.CornerRadius = New-Object System.Windows.CornerRadius 12
    $card.Padding = New-Object System.Windows.Thickness 14
    $card.Margin = New-Object System.Windows.Thickness 0,0,0,8

    $grid = New-Object System.Windows.Controls.Grid
    $c0 = New-Object System.Windows.Controls.ColumnDefinition
    $c0.Width = New-Object System.Windows.GridLength (1, [System.Windows.GridUnitType]::Star)
    $c1 = New-Object System.Windows.Controls.ColumnDefinition
    $c1.Width = [System.Windows.GridLength]::Auto
    [void]$grid.ColumnDefinitions.Add($c0)
    [void]$grid.ColumnDefinitions.Add($c1)

    $info = New-Object System.Windows.Controls.StackPanel
    $nameTb = New-Object System.Windows.Controls.TextBlock
    $nameTb.Text = $friend.name
    $nameTb.FontSize = 15
    $nameTb.FontWeight = 'SemiBold'
    $nameTb.Foreground = $script:brushes.ConvertFrom('#F0F0F2')
    $meta = New-Object System.Windows.Controls.TextBlock
    $meta.FontSize = 12
    $meta.Foreground = $script:brushes.ConvertFrom('#8B8B93')
    if ($friend.favorite) { $meta.Text = 'избранный' } else { $meta.Text = 'друг' }
    [void]$info.Children.Add($nameTb)
    [void]$info.Children.Add($meta)
    [Windows.Controls.Grid]::SetColumn($info, 0)

    $btns = New-Object System.Windows.Controls.StackPanel
    $btns.Orientation = 'Horizontal'
    $star = New-Object System.Windows.Controls.Button
    $star.Style = $window.Resources['GhostBtn']
    $star.FontFamily = New-Object System.Windows.Media.FontFamily 'Segoe MDL2 Assets'
    $star.FontSize = 13
    if ($friend.favorite) { $star.Content = [char]0xE735 } else { $star.Content = [char]0xE734 }
    $star.ToolTip = 'Избранный'
    $star.Margin = New-Object System.Windows.Thickness 0,0,8,0
    $star.Padding = New-Object System.Windows.Thickness 10,6,10,6
    $star.Tag = $friend.name
    $star.Add_Click({
        $nm = $this.Tag
        foreach ($f in $script:friends) {
            if ($f.name -eq $nm) { $f.favorite = -not [bool]$f.favorite; break }
        }
        Write-FriendsState
        Refresh-FriendsUi
    })
    $del = New-Object System.Windows.Controls.Button
    $del.Style = $window.Resources['GhostBtn']
    $del.FontFamily = New-Object System.Windows.Media.FontFamily 'Segoe MDL2 Assets'
    $del.FontSize = 13
    $del.Content = [char]0xE74D
    $del.ToolTip = 'Удалить'
    $del.Padding = New-Object System.Windows.Thickness 10,6,10,6
    $del.Tag = $friend.name
    $del.Add_Click({
        $nm = $this.Tag
        $keep = New-Object System.Collections.Generic.List[object]
        foreach ($f in $script:friends) { if ($f.name -ne $nm) { $keep.Add($f) } }
        $script:friends = $keep
        Write-FriendsState
        Refresh-FriendsUi
    })
    [void]$btns.Children.Add($star)
    [void]$btns.Children.Add($del)
    [Windows.Controls.Grid]::SetColumn($btns, 1)

    [void]$grid.Children.Add($info)
    [void]$grid.Children.Add($btns)
    $card.Child = $grid
    return $card
}

function Refresh-FriendsUi {
    $friendsList.Children.Clear()
    if ($script:friends.Count -eq 0) {
        $friendsEmpty.Visibility = [Windows.Visibility]::Visible
        Refresh-HomeStats
        return
    }
    $friendsEmpty.Visibility = [Windows.Visibility]::Collapsed
    foreach ($f in $script:friends) {
        [void]$friendsList.Children.Add((New-FriendCard $f))
    }
    Refresh-HomeStats
}

function Add-FriendFromBox {
    $n = $friendNickBox.Text.Trim()
    if (-not $n) { return }
    foreach ($f in $script:friends) {
        if ($f.name -eq $n) { $friendNickBox.Text = ''; return }
    }
    $script:friends.Add([pscustomobject]@{ name = $n; note = ''; favorite = $false })
    $friendNickBox.Text = ''
    Write-FriendsState
    Refresh-FriendsUi
}

function Refresh-CloseOnLaunchUi {
    if ($script:closeOnLaunch) {
        $closeOnLaunchValue.Text = 'закрывать лаунчер'
    } else {
        $closeOnLaunchValue.Text = 'оставить открытым'
    }
}

function Start-Client {
    if (-not (Test-LicenseValid)) {
        Show-LicenseGate
        return
    }
    Set-Page 'home'
    Show-LaunchOverlay
    Ensure-LaunchTimer

    if ($script:proc -and -not $script:proc.HasExited) {
        Set-LaunchNotify 'Клиент уже запускается'
        return
    }

    $gradlew = Join-Path $root 'gradlew.bat'
    if (-not (Test-Path $gradlew)) {
        Set-LaunchNotify 'Не найден gradlew.bat' $true
        return
    }
    if (-not $javaHome) {
        Set-LaunchNotify 'JDK не найден. Установи Java 21+' $true
        return
    }

    try {
        $script:launchPct = 0
        $script:launchNote = ''
        $script:gameStarted = $false
        $script:launchAt = Get-Date
        $script:logQueue = New-Object 'System.Collections.Concurrent.ConcurrentQueue[string]'
        $runDir = Join-Path $root 'run'
        if (-not (Test-Path $runDir)) { New-Item -ItemType Directory -Path $runDir -Force | Out-Null }
        $script:launchLogPath = Join-Path $runDir 'launcher-last.log'
        $script:launchLogPos = [int64]0
        $script:launchLogCarry = ''
        [IO.File]::WriteAllText($script:launchLogPath, '')
        $logBox.Clear()
        $launchNotes.Children.Clear()
        $launchBarFill.Background = $script:brushes.ConvertFrom($script:accent)
        Set-LaunchPercent 2
        Set-LaunchNotify 'Запуск Gradle'
        $versionCard.IsEnabled = $false

        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = Join-Path $env:WINDIR 'System32\cmd.exe'
        $offline = ''
        try {
            $req = [Net.HttpWebRequest]::Create('https://maven.fabricmc.net/')
            $req.Method = 'HEAD'
            $req.Timeout = 4000
            $req.ReadWriteTimeout = 4000
            $req.UserAgent = 'MaliceVisuals'
            $resp = $req.GetResponse()
            $resp.Close()
        } catch {
            $offline = ' --offline'
            Add-Log 'Сеть недоступна — запуск в offline-режиме'
        }
        $psi.Arguments = '/c gradlew.bat' + $offline + ' runClient -PmaliceRam=' + [int]$script:ramMb + ' 1> "' + $script:launchLogPath + '" 2>&1'
        $psi.WorkingDirectory = $root
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true
        $psi.EnvironmentVariables['GRADLE_OPTS'] = '-Xmx2G'
        $psi.EnvironmentVariables['JAVA_HOME'] = $javaHome
        $psi.EnvironmentVariables['PATH'] = (Join-Path $javaHome 'bin') + ';' + $psi.EnvironmentVariables['PATH']

        $p = New-Object System.Diagnostics.Process
        $p.StartInfo = $psi
        if (-not $p.Start()) { throw 'Не удалось стартовать процесс' }
        $script:proc = $p
        Add-Log ("JAVA_HOME={0}  (Java {1})" -f $javaHome, (Get-JavaMajorVersion $javaHome))
        Add-Log ("gradlew.bat runClient  RAM={0}MB" -f $script:ramMb)
    } catch {
        $versionCard.IsEnabled = $true
        Set-LaunchNotify ("Ошибка: {0}" -f $_) $true
        Add-Log ("Ошибка: {0}" -f $_)
    }
}

$pathBox.Text = $root
$ramSlider.Value = $script:ramMb
$ramBox.Text = [string]$script:ramMb
Refresh-CloseOnLaunchUi
$uidLabel.Text = [string]$script:uid
$nickBox.Text = [string]$script:username
$homeHello.Text = "привет, $($script:username)"
Apply-LogoImages $pngPath
Save-LauncherConfig
if ($javaHome) {
    $javaLabel.Text = ("{0}  ·  Java {1}" -f $javaHome, (Get-JavaMajorVersion $javaHome))
} else {
    $javaLabel.Text = 'JDK 21+ не найден.'
}

Set-Page 'home'
Refresh-ModUi
Refresh-FriendsUi
if (Test-LicenseValid) { Open-LauncherShell } else { Show-LicenseGate }

$navHome.Add_Click({ Set-Page 'home' })
$navFriends.Add_Click({ Set-Page 'friends' })
$navMods.Add_Click({ Set-Page 'mods' })
$navSettings.Add_Click({ Set-Page 'settings' })
$navProfile.Add_Click({ Set-Page 'profile' })
$navExit.Add_Click({ $window.Close() })
$closeBtn.Add_Click({ $window.Close() })
$gateCloseBtn.Add_Click({ $window.Close() })
$minBtn.Add_Click({ $window.WindowState = 'Minimized' })
$titleBar.Add_MouseLeftButtonDown({ $window.DragMove() })
$gateSide.Add_MouseLeftButtonDown({ $window.DragMove() })

$versionCard.Add_MouseEnter({ $cardArrow.Foreground = $script:brushes.ConvertFrom($script:accent) })
$versionCard.Add_MouseLeave({ $cardArrow.Foreground = $script:brushes.ConvertFrom('#8B8B93') })
$versionCard.Add_Click({
    try { Start-Client } catch { Set-LaunchNotify ("Ошибка: {0}" -f $_) $true }
})
$backBtn.Add_Click({ Hide-LaunchOverlay })
$launchBarTrack.Add_SizeChanged({ Set-LaunchPercent $script:launchPct })
$activateBtn.Add_Click({ Activate-FromBox })
$keyBox.Add_KeyDown({
    if ($_.Key -eq 'Return') { Activate-FromBox }
})
$plan30Btn.Add_Click({ Set-GatePlan 30 })
$plan90Btn.Add_Click({ Set-GatePlan 90 })
$plan180Btn.Add_Click({ Set-GatePlan 180 })

$ramSlider.Add_ValueChanged({
    $script:ramMb = [int]$ramSlider.Value
    $ramBox.Text = [string]$script:ramMb
    Save-LauncherConfig
    Refresh-HomeStats
})
$ramBox.Add_LostFocus({
    $n = 0
    if ([int]::TryParse($ramBox.Text, [ref]$n)) {
        if ($n -lt 1024) { $n = 1024 }
        if ($n -gt 32768) { $n = 32768 }
        $script:ramMb = $n
        $ramSlider.Value = [Math]::Min(16384, $n)
        $ramBox.Text = [string]$n
        Save-LauncherConfig
        Refresh-HomeStats
    } else { $ramBox.Text = [string]$script:ramMb }
})

$closeOnLaunchBtn.Add_Click({
    $script:closeOnLaunch = -not $script:closeOnLaunch
    Save-LauncherConfig
    Refresh-CloseOnLaunchUi
})
$openFolderBtn.Add_Click({ Start-Process explorer.exe $root })
$nickBox.Add_LostFocus({
    $n = $nickBox.Text.Trim()
    if (-not $n) { $n = 'Player' }
    $script:username = $n
    $nickBox.Text = $n
    $homeHello.Text = "привет, $n"
    Save-LauncherConfig
    Refresh-HomeStats
})
$avatarBtn.Add_Click({
    $dialog = New-Object Microsoft.Win32.OpenFileDialog
    $dialog.Title = 'Выберите аватар'
    $dialog.Filter = 'Изображения|*.png;*.jpg;*.jpeg;*.bmp;*.webp|Все файлы|*.*'
    if ($dialog.ShowDialog() -eq $true) {
        try {
            $dest = Join-Path $root 'malice.png'
            Copy-Item -LiteralPath $dialog.FileName -Destination $dest -Force
            $avatarRes = Join-Path $root 'src\main\resources\assets\skeleton\pictures\avatar.png'
            $iconRes = Join-Path $root 'src\main\resources\assets\skeleton\icon.png'
            if (Test-Path (Split-Path $avatarRes)) { Copy-Item -LiteralPath $dest -Destination $avatarRes -Force }
            if (Test-Path (Split-Path $iconRes)) { Copy-Item -LiteralPath $dest -Destination $iconRes -Force }
            Convert-PngToIco $dest (Join-Path $root 'malice.ico')
            Apply-LogoImages $dest
            $avatarStatus.Text = 'Аватар обновлён'
            $avatarStatus.Foreground = $script:brushes.ConvertFrom($script:accent)
        } catch {
            $avatarStatus.Text = "Ошибка: $_"
            $avatarStatus.Foreground = $script:brushes.ConvertFrom('#8B8B93')
        }
    }
})
$shortcutBtn.Add_Click({
    try {
        $p = Install-DesktopShortcut
        $shortcutStatus.Text = "Готово: $p"
        $shortcutStatus.Foreground = $script:brushes.ConvertFrom($script:accent)
    } catch {
        $shortcutStatus.Text = "Ошибка: $_"
        $shortcutStatus.Foreground = $script:brushes.ConvertFrom('#8B8B93')
    }
})
$addFriendBtn.Add_Click({ Add-FriendFromBox })
$friendNickBox.Add_KeyDown({
    if ($_.Key -eq 'Return') { Add-FriendFromBox }
})
$modBtn.Add_Click({
    try {
        if (Test-VoiceChatInstalled) {
            Get-VoiceChatJars | Remove-Item -Force
            Refresh-ModUi
            return
        }
        $modBtn.IsEnabled = $false
        $modStatus.Text = 'Скачивание...'
        $window.Dispatcher.Invoke([Windows.Threading.DispatcherPriority]::Render, [action]{})
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $ua = 'MaliceVisuals/1.0 (launcher)'
        $api = 'https://api.modrinth.com/v2/project/simple-voice-chat/version?game_versions=%5B%221.21.4%22%5D&loaders=%5B%22fabric%22%5D'
        $versions = Invoke-RestMethod -Uri $api -Headers @{ 'User-Agent' = $ua }
        $file = $null
        foreach ($v in @($versions)) {
            foreach ($f in @($v.files)) {
                $name = [string]$f.filename
                if ($name -match '(?i)fabric' -and $name -notmatch '(?i)forge|neoforge|quilt|sources') {
                    $file = $f
                    break
                }
            }
            if ($file) { break }
        }
        if (-not $file) { throw 'Нет сборки Simple Voice Chat для Fabric 1.21.4' }
        $dest = Join-Path (Get-ModsDir) $file.filename
        $wc = New-Object Net.WebClient
        $wc.Headers['User-Agent'] = $ua
        $wc.DownloadFile([string]$file.url, $dest)
        Refresh-ModUi
    } catch {
        $modStatus.Text = "Ошибка: $_"
        $modStatus.Foreground = $script:brushes.ConvertFrom('#8B8B93')
    } finally {
        $modBtn.IsEnabled = $true
        if (Test-VoiceChatInstalled) { Refresh-ModUi }
    }
})

$window.Add_Loaded({
    Apply-TaskbarIcon
    Ensure-LaunchTimer
    if ($script:autoPlay -and (Test-LicenseValid)) {
        $script:autoPlay = $false
        Show-LaunchOverlay
        Set-LaunchNotify 'Ожидание закрытия клиента'
        $delay = New-Object System.Windows.Threading.DispatcherTimer
        $delay.Interval = [TimeSpan]::FromSeconds(5)
        $script:autoPlayTimer = $delay
        $delay.Add_Tick({
            if ($script:autoPlayTimer) { $script:autoPlayTimer.Stop() }
            try { Start-Client } catch { Set-LaunchNotify ("Ошибка: {0}" -f $_) $true }
        })
        $delay.Start()
    }
})
$window.Add_KeyDown({
    if ($_.Key -eq 'Escape' -and $overlay.Visibility -eq [Windows.Visibility]::Visible) {
        Hide-LaunchOverlay
    }
})
$window.Add_Closed({
    if ($script:windowIcon) { $script:windowIcon.Dispose() }
})

[void]$window.ShowDialog()
