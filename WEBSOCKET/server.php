<?php
// www/php/websocket/server.php
date_default_timezone_set('Europe/Paris');

use Ratchet\Server\IoServer;
use Ratchet\Http\HttpServer;
use Ratchet\WebSocket\WsServer;
use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;

if (file_exists('/home/flasskdev/vendor/autoload.php')) {
    require '/home/flasskdev/vendor/autoload.php';
} elseif (file_exists('/home/flasskdev/www/vendor/autoload.php')) {
    require '/home/flasskdev/www/vendor/autoload.php';
} else {
    require dirname(dirname(__DIR__)) . '/vendor/autoload.php';
}

// Конфигурация БД
define('DB_HOST', 'mysql-flasskdev.alwaysdata.net');
define('DB_USER', 'flasskdev');
define('DB_PASS', '31052019RoG+');
define('DB_NAME', 'flasskdev_mobile');

// Конфигурация RabbitMQ
define('RABBITMQ_HOST', 'rabbitmq-flasskdev.alwaysdata.net');
define('RABBITMQ_PORT', 5672);
define('RABBITMQ_USER', 'flasskdev');
define('RABBITMQ_PASS', '31052019RoG+');
define('RABBITMQ_VHOST', 'flasskdev_mobile');

require_once __DIR__ . '/Chat.php';


// Принудительно слушаем все интерфейсы (0.0.0.0) на выданном порту

$chat = new Chat();
$server = IoServer::factory(
    new HttpServer(
        new WsServer(
            $chat
        )
    ),
    $port,
    $address
);

if (isset($server->loop) && method_exists($server->loop, 'addPeriodicTimer')) {
    $server->loop->addPeriodicTimer(0.1, function() use ($chat) {
        $chat->checkNewMessages();
    });
}

echo "🚀 Сервер запущен на {$address}:{$port}\n";
$server->run();