param([string]$pngPath, [string]$icoPath)
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($pngPath)
$sizes = @(256, 64, 48, 32, 16)
$ms = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter $ms
$bw.Write([UInt16]0)
$bw.Write([UInt16]1)
$bw.Write([UInt16]$sizes.Count)
$offset = 6 + $sizes.Count * 16
$dataStreams = @()
foreach ($s in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap $s, $s
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.DrawImage($img, 0, 0, $s, $s)
    $g.Dispose()
    $pngMs = New-Object System.IO.MemoryStream
    $bmp.Save($pngMs, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $dataStreams += $pngMs.ToArray()
}
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = $sizes[$i]
    if ($s -ge 256) { $w = 0 } else { $w = $s }
    if ($s -ge 256) { $h = 0 } else { $h = $s }
    $bw.Write([Byte]$w)
    $bw.Write([Byte]$h)
    $bw.Write([Byte]0)
    $bw.Write([Byte]0)
    $bw.Write([UInt16]1)
    $bw.Write([UInt16]32)
    $bw.Write([UInt32]$dataStreams[$i].Length)
    $bw.Write([UInt32]$offset)
    $offset += $dataStreams[$i].Length
}
foreach ($d in $dataStreams) { $bw.Write($d) }
$bw.Flush()
[System.IO.File]::WriteAllBytes($icoPath, $ms.ToArray())
$bw.Dispose()
$ms.Dispose()
$img.Dispose()
Write-Host "ICO created: $((Get-Item $icoPath).Length) bytes"
