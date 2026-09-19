<?php
// upload_video.php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

$uploadDir     = '/home/flasskdev/www/attachments/';
$publicBaseUrl = 'https://flasskdev.alwaysdata.net/attachments/';

if (!is_dir($uploadDir) && !mkdir($uploadDir, 0755, true)) {
    http_response_code(500);
    echo json_encode(['status' => 'ERROR', 'message' => 'Unable to create upload directory']);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !isset($_FILES['file'])) {
    http_response_code(400);
    echo json_encode(['status' => 'ERROR', 'message' => 'No file uploaded or wrong request method']);
    exit;
}

$file = $_FILES['file'];
if ($file['error'] !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'])) {
    http_response_code(400);
    echo json_encode(['status' => 'ERROR', 'message' => 'Video upload failed']);
    exit;
}

$originalExt = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
$allowedExts = ['mp4', 'mov', 'avi', 'mkv', 'webm', '3gp'];
$ext = in_array($originalExt, $allowedExts, true) ? $originalExt : 'mp4';

$hash     = hash_file('sha256', $file['tmp_name']);
$filename = $hash . '.' . $ext;
$filepath = $uploadDir . $filename;

if (!file_exists($filepath) && !move_uploaded_file($file['tmp_name'], $filepath)) {
    http_response_code(500);
    echo json_encode(['status' => 'ERROR', 'message' => 'Failed to store video']);
    exit;
}

$coverFilename = $hash . '.cover.jpg';
$coverPath     = $uploadDir . $coverFilename;

// 1) Клиент прислал готовый JPEG-кадр.
if (!file_exists($coverPath)
    && isset($_FILES['cover'])
    && $_FILES['cover']['error'] === UPLOAD_ERR_OK
    && is_uploaded_file($_FILES['cover']['tmp_name'])) {

    $cover         = $_FILES['cover'];
    $coverMime     = (new finfo(FILEINFO_MIME_TYPE))->file($cover['tmp_name']);
    $maxCoverBytes = 5 * 1024 * 1024;

    if ($coverMime === 'image/jpeg' && $cover['size'] > 0 && $cover['size'] <= $maxCoverBytes) {
        move_uploaded_file($cover['tmp_name'], $coverPath);
    }
}

// 2) Фолбэк: клиент обложку не прислал (старая версия приложения, бот, веб) —
//    вытаскиваем кадр на сервере. Это гарантирует, что <hash>.cover.jpg
//    существует ВСЕГДА, и клиенту не нужно декодировать видео.
if (!file_exists($coverPath)) {
    $ffmpeg = trim((string) @shell_exec('command -v ffmpeg 2>/dev/null'));
    if ($ffmpeg !== '') {
        $tmpCover = $coverPath . '.tmp.jpg';
        $cmd = sprintf(
            '%s -y -ss 00:00:01 -i %s -frames:v 1 -vf "scale=\'min(1280,iw)\':-2" -q:v 4 %s 2>/dev/null',
            escapeshellcmd($ffmpeg),
            escapeshellarg($filepath),
            escapeshellarg($tmpCover)
        );
        @shell_exec($cmd);

        // Видео короче секунды — берём самый первый кадр.
        if (!file_exists($tmpCover) || filesize($tmpCover) === 0) {
            $cmd = sprintf(
                '%s -y -i %s -frames:v 1 -vf "scale=\'min(1280,iw)\':-2" -q:v 4 %s 2>/dev/null',
                escapeshellcmd($ffmpeg),
                escapeshellarg($filepath),
                escapeshellarg($tmpCover)
            );
            @shell_exec($cmd);
        }

        if (file_exists($tmpCover) && filesize($tmpCover) > 0) {
            @rename($tmpCover, $coverPath);
        } else {
            @unlink($tmpCover);
        }
    }
}

$response = [
    'status' => 'SUCCESS',
    'url'    => $publicBaseUrl . $filename,
];

if (file_exists($coverPath) && filesize($coverPath) > 0) {
    $response['cover_url'] = $publicBaseUrl . $coverFilename;
}

echo json_encode($response);