$ErrorActionPreference = 'Stop'
$zip = 'C:\Users\admin\Downloads\Nursultan Alpha.zip'
$out = 'C:\Users\admin\Desktop\DeltaClient-main\tools\nursultan-ref'
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Path $out | Out-Null
Expand-Archive -LiteralPath $zip -DestinationPath $out -Force
Get-ChildItem $out -Recurse -File | Select-Object -First 80 FullName, Length | Format-Table -AutoSize
Write-Host '--- TOP ---'
Get-ChildItem $out | Select-Object Name, Mode, Length | Format-Table -AutoSize
