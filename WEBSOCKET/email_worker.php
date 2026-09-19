<?php
// email_worker.php
// Воркер для обработки очереди отправки email через RabbitMQ
date_default_timezone_set('Europe/Paris');

if (file_exists('/home/flasskdev/vendor/autoload.php')) {
    require '/home/flasskdev/vendor/autoload.php';
} elseif (file_exists('/home/flasskdev/www/vendor/autoload.php')) {
    require '/home/flasskdev/www/vendor/autoload.php';
} else {
    require dirname(dirname(__DIR__)) . '/vendor/autoload.php';
}

use PhpAmqpLib\Connection\AMQPStreamConnection;

$host = defined('RABBITMQ_HOST') ? RABBITMQ_HOST : 'rabbitmq-flasskdev.alwaysdata.net';
$port = defined('RABBITMQ_PORT') ? RABBITMQ_PORT : 5672;
$user = defined('RABBITMQ_USER') ? RABBITMQ_USER : 'flasskdev';
$pass = defined('RABBITMQ_PASS') ? RABBITMQ_PASS : '31052019RoG+';
$vhost = defined('RABBITMQ_VHOST') ? RABBITMQ_VHOST : 'flasskdev_mobile';

try {
    $connection = new AMQPStreamConnection($host, $port, $user, $pass, $vhost);
    $channel = $connection->channel();
    
    $queueName = 'email_queue';
    $channel->queue_declare($queueName, false, true, false, false);
    
    echo " [*] Ожидание сообщений в очереди '{$queueName}'. Для выхода нажмите CTRL+C\n";
    
    $callback = function ($msg) {
        $data = json_decode($msg->body, true);
        if ($data) {
            $email = $data['email'] ?? '';
            $subject = $data['subject'] ?? '';
            $message = $data['message'] ?? '';
            $headers = $data['headers'] ?? '';
            
            if ($email && $subject && $message) {
                echo " [x] Отправка письма на {$email}...\n";
                // MIME кодирование темы для корректного отображения и прохождения спам-фильтров
                $encodedSubject = '=?UTF-8?B?' . base64_encode($subject) . '?=';
                // Отправляем письмо с указанием envelope sender (-f) для SPF
                $sent = @mail($email, $encodedSubject, $message, $headers, "-f noreply@flasskdev.alwaysdata.net")
                     || @mail($email, $encodedSubject, $message, $headers);
                if ($sent) {
                    echo " [v] Успешно отправлено.\n";
                } else {
                    echo " [!] Ошибка при отправке письма.\n";
                }
            }
        }
        
        $msg->delivery_info['channel']->basic_ack($msg->delivery_info['delivery_tag']);
    };
    
    $channel->basic_qos(null, 1, null);
    $channel->basic_consume($queueName, '', false, false, false, false, $callback);
    
    while ($channel->is_open()) {
        $channel->wait();
    }
    
    $channel->close();
    $connection->close();

} catch (\Exception $e) {
    echo "Ошибка RabbitMQ: " . $e->getMessage() . "\n";
}
