<?php
// upload_photo.php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

$uploadDir = '/home/flasskdev/www/attachments/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['file'])) {
    $file = $_FILES['file'];
    $tmpName = $file['tmp_name'];
    
    // Hash to prevent duplicates and ensure safe filenames
    $hash = hash_file('sha256', $tmpName);
    $filename = $hash . '.webp';
    $filepath = $uploadDir . $filename;

    if (!file_exists($filepath)) {
        if (!move_uploaded_file($tmpName, $filepath)) {
            echo json_encode(['status' => 'ERROR', 'message' => 'Failed to move uploaded file']);
            exit;
        }
    }
    
    $url = 'https://flasskdev.alwaysdata.net/attachments/' . $filename;
    echo json_encode(['status' => 'SUCCESS', 'url' => $url]);
} else {
    echo json_encode(['status' => 'ERROR', 'message' => 'No file uploaded or wrong request method']);
}
?>