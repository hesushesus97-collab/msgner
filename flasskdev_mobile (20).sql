-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: mysql-flasskdev.alwaysdata.net
-- Generation Time: Sep 12, 2026 at 02:27 AM
-- Server version: 10.11.18-MariaDB
-- PHP Version: 8.4.25

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `flasskdev_mobile`
--

-- --------------------------------------------------------

--
-- Table structure for table `auth_challenges`
--

CREATE TABLE `auth_challenges` (
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `user_id` int(11) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `auth_sessions`
--

CREATE TABLE `auth_sessions` (
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `user_id` int(11) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `login_notified` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `auth_sessions`
--

INSERT INTO `auth_sessions` (`token_hash`, `user_id`, `device_id`, `expires_at`, `login_notified`) VALUES
('f992dea7280194a103f4935ad75b03ee908f629d2722911f01254cb2edb4faff', 113, '990492f9-5942-41b1-aa3d-980b8ccc8c64', '2026-10-12 01:42:44', 1);

-- --------------------------------------------------------

--
-- Table structure for table `banned_devices`
--

CREATE TABLE `banned_devices` (
  `device_id` varchar(255) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `banned_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `blocked_users`
--

CREATE TABLE `blocked_users` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `blocked_id` int(11) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bots`
--

CREATE TABLE `bots` (
  `id` int(11) NOT NULL,
  `name` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  `bot_token` varchar(128) DEFAULT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `about` varchar(64) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bots`
--

INSERT INTO `bots` (`id`, `name`, `username`, `bot_token`, `is_verified`, `about`) VALUES
(1, 'VibeCat', 'vibe', 'VIBEBOT-7q9XkL2mP8wR4zY5tB1vN3hF6dC0sJ9aH2fG5bK8xQ1mL4pZ7tW0yC3vN6hF9dC2sJ5aH8fG1bK4xQ7mL0pZ3tW6yC9vN2hF5dC8sJ1aH4fG7bK0xQ3mL6pZ', 1, 'Люблю помогать <3'),
(2, 'Spam Info', 'SpamInfo', NULL, 1, 'Информация о статусе SpamBlock.');

-- --------------------------------------------------------

--
-- Table structure for table `bot_callbacks`
--

CREATE TABLE `bot_callbacks` (
  `id` int(11) NOT NULL,
  `bot_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `message_id` int(11) NOT NULL,
  `data` text NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bot_callbacks`
--

INSERT INTO `bot_callbacks` (`id`, `bot_id`, `user_id`, `message_id`, `data`, `created_at`) VALUES
(1, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:12'),
(2, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:17'),
(3, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:18'),
(4, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:18'),
(5, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:19'),
(6, 1, 106, 12416, 'btn_ping', '2026-08-18 11:23:20'),
(7, 1, 106, 12416, 'btn_info', '2026-08-18 11:23:20'),
(8, 1, 106, 12416, 'btn_ping', '2026-08-18 11:36:06'),
(9, 1, 106, 12416, 'btn_info', '2026-08-18 11:36:07'),
(10, 1, 106, 12416, 'btn_ping', '2026-08-18 11:36:09'),
(11, 1, 106, 12416, 'btn_ping', '2026-08-18 11:36:10'),
(12, 1, 106, 12416, 'btn_info', '2026-08-18 11:50:19'),
(13, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:20'),
(14, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:24'),
(15, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:24'),
(16, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:25'),
(17, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:26'),
(18, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:26'),
(19, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:27'),
(20, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:27'),
(21, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:28'),
(22, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:29'),
(23, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:29'),
(24, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:29'),
(25, 1, 106, 12416, 'btn_ping', '2026-08-18 11:50:30'),
(26, 1, 106, 12416, 'btn_info', '2026-08-18 11:50:43'),
(27, 1, 106, 12416, 'btn_info', '2026-08-18 11:50:43'),
(28, 1, 106, 12416, 'btn_info', '2026-08-18 11:50:44'),
(29, 1, 106, 12416, 'btn_ping', '2026-08-18 11:51:51'),
(30, 1, 106, 12416, 'btn_ping', '2026-08-18 11:51:52'),
(31, 1, 106, 12416, 'btn_info', '2026-08-18 11:56:29'),
(32, 1, 106, 12416, 'btn_info', '2026-08-18 11:56:29'),
(33, 1, 106, 12416, 'btn_ping', '2026-08-18 11:56:31'),
(34, 1, 106, 12416, 'btn_help', '2026-08-18 11:56:32'),
(35, 1, 106, 12416, 'btn_help', '2026-08-18 11:56:32'),
(36, 1, 106, 12416, 'btn_help', '2026-08-18 11:56:33'),
(37, 1, 106, 12416, 'btn_ping', '2026-08-18 11:59:48'),
(38, 1, 106, 12416, 'btn_info', '2026-08-18 12:00:44'),
(39, 1, 106, 12416, 'btn_info', '2026-08-18 12:00:45'),
(40, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:04'),
(41, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:04'),
(42, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:05'),
(43, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:05'),
(44, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:06'),
(45, 1, 106, 12416, 'btn_ping', '2026-08-18 12:01:06'),
(46, 1, 106, 12416, 'btn_help', '2026-08-18 12:01:08'),
(47, 1, 106, 12416, 'btn_ping', '2026-08-18 12:16:19'),
(48, 1, 106, 12498, 'btn_info', '2026-08-18 12:39:32'),
(49, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:34'),
(50, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:37'),
(51, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:41'),
(52, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:44'),
(53, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:47'),
(54, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:48'),
(55, 1, 106, 12498, 'btn_ping', '2026-08-18 12:39:48'),
(56, 1, 106, 12498, 'btn_info', '2026-08-18 12:39:51'),
(57, 1, 106, 12498, 'btn_info', '2026-08-18 12:39:53'),
(58, 1, 106, 12498, 'btn_info', '2026-08-18 12:39:54'),
(59, 1, 106, 12498, 'btn_info', '2026-08-18 12:39:59'),
(60, 1, 106, 12498, 'btn_info', '2026-08-18 12:40:00'),
(61, 1, 106, 12498, 'btn_info', '2026-08-18 12:40:05'),
(62, 1, 106, 12498, 'btn_help', '2026-08-18 12:40:08'),
(63, 1, 106, 12498, 'btn_help', '2026-08-18 12:40:09'),
(64, 1, 106, 12498, 'btn_help', '2026-08-18 12:40:11'),
(65, 1, 106, 12501, 'btn_info', '2026-08-18 12:44:04'),
(66, 1, 106, 12501, 'btn_ping', '2026-08-18 12:44:07'),
(67, 1, 106, 12501, 'btn_ping', '2026-08-18 12:44:11'),
(68, 1, 106, 12501, 'btn_help', '2026-08-18 12:44:14'),
(69, 1, 106, 12501, 'btn_help', '2026-08-18 12:44:15'),
(70, 1, 106, 12498, 'btn_info', '2026-08-18 18:06:17'),
(71, 1, 106, 12498, 'btn_info', '2026-08-18 18:06:18'),
(72, 1, 106, 12498, 'btn_info', '2026-08-18 18:06:20'),
(73, 1, 106, 12498, 'btn_info', '2026-08-18 18:06:21'),
(74, 1, 106, 12498, 'btn_info', '2026-08-18 18:06:23'),
(75, 1, 106, 12507, 'diag:start:connection', '2026-08-18 18:31:13'),
(76, 1, 106, 12507, 'diag:start:notifications', '2026-08-18 18:31:16'),
(77, 1, 106, 12508, 'diag:answer:notifications:permission:запрещены', '2026-08-18 18:31:26'),
(78, 1, 106, 12508, 'diag:answer:notifications:permission:запрещены', '2026-08-18 18:31:32'),
(79, 1, 106, 12509, 'diag:answer:notifications:chat_muted:нет', '2026-08-18 18:31:41'),
(80, 1, 106, 12509, 'diag:answer:notifications:chat_muted:нет', '2026-08-18 18:31:50'),
(81, 1, 106, 12510, 'diag:answer:notifications:battery:нет', '2026-08-18 18:32:00'),
(82, 1, 106, 12510, 'diag:answer:notifications:battery:нет', '2026-08-18 18:32:05'),
(83, 1, 106, 12511, 'diag:report', '2026-08-18 18:32:16'),
(84, 1, 106, 12511, 'diag:report', '2026-08-18 18:32:19'),
(85, 1, 106, 12512, 'diag:report', '2026-08-18 18:56:12'),
(86, 1, 106, 12515, 'diag:start:notifications', '2026-08-18 18:57:17'),
(87, 1, 106, 12515, 'diag:report', '2026-08-18 18:57:35'),
(88, 1, 106, 12516, 'diag:menu', '2026-08-18 19:02:21'),
(89, 1, 106, 12516, 'diag:menu', '2026-08-18 19:02:33'),
(90, 1, 106, 12516, 'diag:menu', '2026-08-18 19:02:42'),
(91, 1, 106, 12517, 'diag:start:notifications', '2026-08-18 20:43:25'),
(92, 1, 106, 12516, 'diag:report', '2026-08-18 20:43:34'),
(93, 1, 106, 12517, 'diag:start:connection', '2026-08-18 20:44:26'),
(94, 1, 106, 12518, 'diag:answer:connection:network:Wi-Fi', '2026-08-18 20:44:33'),
(95, 1, 106, 12518, 'diag:answer:connection:network:Wi-Fi', '2026-08-18 20:44:50'),
(96, 1, 106, 12519, 'diag:answer:connection:internet:нестабильно', '2026-08-18 20:44:59'),
(97, 1, 106, 12519, 'diag:answer:connection:internet:да', '2026-08-18 20:53:12'),
(98, 1, 106, 12519, 'diag:answer:connection:internet:да', '2026-08-18 21:07:32'),
(99, 1, 106, 12519, 'diag:answer:connection:internet:да', '2026-08-18 21:10:01'),
(100, 1, 106, 12519, 'diag:answer:connection:internet:нет', '2026-08-20 01:11:23');

-- --------------------------------------------------------

--
-- Table structure for table `bot_callback_answers`
--

CREATE TABLE `bot_callback_answers` (
  `id` int(11) NOT NULL,
  `callback_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `text` text DEFAULT NULL,
  `show_alert` tinyint(1) DEFAULT 0,
  `is_delivered` tinyint(1) DEFAULT 0,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bot_callback_answers`
--

INSERT INTO `bot_callback_answers` (`id`, `callback_id`, `user_id`, `text`, `show_alert`, `is_delivered`, `created_at`) VALUES
(1, 49, 106, 'Pong! 🏓 Бот работает штатно.', 1, 1, '2026-08-18 12:39:34'),
(2, 51, 106, 'Pong! 🏓 Бот работает штатно.', 1, 1, '2026-08-18 12:39:42'),
(3, 53, 106, 'Pong! 🏓 Бот работает штатно.', 1, 1, '2026-08-18 12:39:48'),
(4, 55, 106, 'Pong! 🏓 Бот работает штатно.', 1, 1, '2026-08-18 12:39:49'),
(5, 63, 106, 'Вы можете задать боту любой вопрос текстом прямо в этом чате.', 1, 1, '2026-08-18 12:40:10'),
(6, 65, 106, 'Vibe Messenger — стильный и современный мессенджер с открытым Bot API!', 1, 1, '2026-08-18 12:44:04'),
(7, 67, 106, 'Pong! 🏓 Бот работает штатно.', 1, 1, '2026-08-18 12:44:12'),
(8, 69, 106, 'Вы можете задать боту любой вопрос текстом прямо в этом чате.', 1, 1, '2026-08-18 12:44:16'),
(9, 76, 106, 'Принято', 0, 1, '2026-08-18 18:31:16'),
(10, 78, 106, 'Принято', 0, 1, '2026-08-18 18:31:33'),
(11, 80, 106, 'Принято', 0, 1, '2026-08-18 18:31:51'),
(12, 82, 106, 'Принято', 0, 1, '2026-08-18 18:32:05'),
(13, 84, 106, 'Принято', 0, 1, '2026-08-18 18:32:20'),
(14, 87, 106, 'Принято', 0, 1, '2026-08-18 18:57:36'),
(15, 90, 106, 'Принято', 0, 1, '2026-08-18 19:02:42'),
(16, 93, 106, 'Принято', 0, 1, '2026-08-18 20:44:27'),
(17, 95, 106, 'Принято', 0, 1, '2026-08-18 20:44:51');

-- --------------------------------------------------------

--
-- Table structure for table `bot_outgoing_events`
--

CREATE TABLE `bot_outgoing_events` (
  `id` int(11) NOT NULL,
  `bot_id` int(11) NOT NULL,
  `event_type` varchar(32) NOT NULL,
  `target_user_id` int(11) NOT NULL,
  `payload` longtext NOT NULL,
  `is_delivered` tinyint(1) DEFAULT 0,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bot_polling_locks`
--

CREATE TABLE `bot_polling_locks` (
  `bot_id` int(11) NOT NULL,
  `client_id` varchar(64) NOT NULL,
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bot_polling_locks`
--

INSERT INTO `bot_polling_locks` (`bot_id`, `client_id`, `updated_at`) VALUES
(1, '43c934bc90630a1c00b2e77228a377ee', '2026-08-25 15:57:52');

-- --------------------------------------------------------

--
-- Table structure for table `bot_updates`
--

CREATE TABLE `bot_updates` (
  `id` int(11) NOT NULL,
  `bot_id` int(11) NOT NULL,
  `update_type` varchar(32) NOT NULL,
  `reference_id` int(11) NOT NULL,
  `payload` longtext NOT NULL,
  `message_id` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bot_updates`
--

INSERT INTO `bot_updates` (`id`, `bot_id`, `update_type`, `reference_id`, `payload`, `message_id`, `created_at`) VALUES
(1, 1, 'message', 12576, '{}', NULL, '2026-08-25 15:54:36'),
(2, 1, 'callback_query', 100, '{}', NULL, '2026-08-25 15:54:36'),
(3, 1, 'message', 12577, '{\"id\":12577,\"message_id\":12577,\"type\":\"message\",\"sender_id\":106,\"sender_info\":{\"id\":106,\"name\":\"Locked\",\"username\":\"locked\",\"avatar_url\":\"https:\\/\\/flasskdev.alwaysdata.net\\/avatars\\/4297e122fc20a8ba92f2f445dd77cdf843fb8fb761a5706d7f550ef97a6f567d.webp\",\"about\":\"Вопросы в поддержку.\",\"is_online\":true,\"last_seen\":\"2026-08-25 17:55:25\",\"is_developer\":true,\"is_verified\":false,\"is_bot\":false},\"content\":\"здравствуйте\",\"timestamp\":\"2026-08-25 17:55:34\",\"reply_to_id\":null,\"is_bot\":0,\"is_media\":0,\"attachments\":null,\"reply_markup\":null}', 12577, '2026-08-25 15:55:34');

-- --------------------------------------------------------

--
-- Table structure for table `chat_actions`
--

CREATE TABLE `chat_actions` (
  `id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `sender_type` varchar(20) NOT NULL,
  `action` varchar(20) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `chat_actions`
--

INSERT INTO `chat_actions` (`id`, `sender_id`, `receiver_id`, `sender_type`, `action`, `created_at`) VALUES
(1, 1, 106, 'bot', 'typing', '2026-08-17 11:51:53'),
(2, 1, 106, 'bot', 'typing', '2026-08-17 11:52:21'),
(3, 1, 106, 'bot', 'typing', '2026-08-17 11:56:23'),
(4, 1, 106, 'bot', 'typing', '2026-08-17 11:56:36'),
(5, 1, 106, 'bot', 'typing', '2026-08-17 11:56:48'),
(6, 1, 106, 'bot', 'typing', '2026-08-17 11:56:57'),
(7, 1, 106, 'bot', 'typing', '2026-08-17 11:57:56'),
(8, 1, 106, 'bot', 'typing', '2026-08-17 11:58:11'),
(9, 1, 106, 'bot', 'typing', '2026-08-17 12:01:56'),
(10, 1, 106, 'bot', 'typing', '2026-08-17 12:02:15'),
(11, 1, 106, 'bot', 'typing', '2026-08-18 00:33:03'),
(12, 1, 106, 'bot', 'typing', '2026-08-18 00:33:20'),
(13, 1, 106, 'bot', 'typing', '2026-08-18 00:33:44'),
(14, 1, 106, 'bot', 'typing', '2026-08-18 10:59:30'),
(15, 1, 106, 'bot', 'typing', '2026-08-18 11:00:32'),
(16, 1, 106, 'bot', 'typing', '2026-08-18 11:01:05'),
(17, 1, 106, 'bot', 'typing', '2026-08-18 11:02:54'),
(18, 1, 106, 'bot', 'typing', '2026-08-18 11:11:34'),
(19, 1, 106, 'bot', 'typing', '2026-08-18 11:22:06'),
(20, 1, 103, 'bot', 'typing', '2026-08-18 12:17:42'),
(21, 1, 106, 'bot', 'typing', '2026-08-18 12:17:44'),
(22, 1, 106, 'bot', 'typing', '2026-08-18 12:17:46'),
(23, 1, 106, 'bot', 'typing', '2026-08-18 12:17:47'),
(24, 1, 106, 'bot', 'typing', '2026-08-18 12:17:49'),
(25, 1, 106, 'bot', 'typing', '2026-08-18 12:17:50'),
(26, 1, 103, 'bot', 'typing', '2026-08-18 12:17:56'),
(27, 1, 106, 'bot', 'typing', '2026-08-18 12:17:58'),
(28, 1, 106, 'bot', 'typing', '2026-08-18 12:18:00'),
(29, 1, 106, 'bot', 'typing', '2026-08-18 12:18:02'),
(30, 1, 106, 'bot', 'typing', '2026-08-18 12:18:04'),
(31, 1, 106, 'bot', 'typing', '2026-08-18 12:18:05'),
(32, 1, 106, 'bot', 'typing', '2026-08-18 12:18:08'),
(33, 1, 106, 'bot', 'typing', '2026-08-18 12:18:10'),
(34, 1, 106, 'bot', 'typing', '2026-08-18 12:18:12'),
(35, 1, 106, 'bot', 'typing', '2026-08-18 12:18:14'),
(36, 1, 106, 'bot', 'typing', '2026-08-18 12:18:16'),
(37, 1, 103, 'bot', 'typing', '2026-08-18 12:18:21'),
(38, 1, 103, 'bot', 'typing', '2026-08-18 12:19:37'),
(39, 1, 106, 'bot', 'typing', '2026-08-18 12:19:39'),
(40, 1, 106, 'bot', 'typing', '2026-08-18 12:19:40'),
(41, 1, 106, 'bot', 'typing', '2026-08-18 12:19:42'),
(42, 1, 106, 'bot', 'typing', '2026-08-18 12:19:44'),
(43, 1, 106, 'bot', 'typing', '2026-08-18 12:19:46'),
(44, 1, 106, 'bot', 'typing', '2026-08-18 12:19:47'),
(45, 1, 106, 'bot', 'typing', '2026-08-18 12:19:50'),
(46, 1, 106, 'bot', 'typing', '2026-08-18 12:19:51'),
(47, 1, 103, 'bot', 'typing', '2026-08-18 12:30:25'),
(48, 1, 106, 'bot', 'typing', '2026-08-18 12:30:26'),
(49, 1, 106, 'bot', 'typing', '2026-08-18 12:30:28'),
(50, 1, 106, 'bot', 'typing', '2026-08-18 12:30:30'),
(51, 1, 106, 'bot', 'typing', '2026-08-18 12:30:33'),
(52, 1, 106, 'bot', 'typing', '2026-08-18 12:30:35'),
(53, 1, 106, 'bot', 'typing', '2026-08-18 12:30:37'),
(54, 1, 106, 'bot', 'typing', '2026-08-18 12:30:39'),
(55, 1, 106, 'bot', 'typing', '2026-08-18 12:30:41'),
(56, 1, 106, 'bot', 'typing', '2026-08-18 12:30:43'),
(57, 1, 106, 'bot', 'typing', '2026-08-18 12:30:46'),
(58, 1, 106, 'bot', 'typing', '2026-08-18 12:30:47'),
(59, 1, 106, 'bot', 'typing', '2026-08-18 12:30:50'),
(60, 1, 106, 'bot', 'typing', '2026-08-18 12:30:52'),
(61, 1, 106, 'bot', 'typing', '2026-08-18 12:30:53'),
(62, 1, 106, 'bot', 'typing', '2026-08-18 12:30:56'),
(63, 1, 106, 'bot', 'typing', '2026-08-18 12:30:58'),
(64, 1, 106, 'bot', 'typing', '2026-08-18 12:30:59'),
(65, 1, 106, 'bot', 'typing', '2026-08-18 12:31:01'),
(66, 1, 106, 'bot', 'typing', '2026-08-18 12:31:04'),
(67, 1, 106, 'bot', 'typing', '2026-08-18 12:31:05'),
(68, 1, 106, 'bot', 'typing', '2026-08-18 12:31:07'),
(69, 1, 106, 'bot', 'typing', '2026-08-18 12:31:09'),
(70, 1, 106, 'bot', 'typing', '2026-08-18 12:31:10'),
(71, 1, 106, 'bot', 'typing', '2026-08-18 12:31:12'),
(72, 1, 106, 'bot', 'typing', '2026-08-18 12:31:14'),
(73, 1, 106, 'bot', 'typing', '2026-08-18 12:31:16'),
(74, 1, 106, 'bot', 'typing', '2026-08-18 12:31:18'),
(75, 1, 106, 'bot', 'typing', '2026-08-18 12:31:21'),
(76, 1, 106, 'bot', 'typing', '2026-08-18 12:31:22'),
(77, 1, 106, 'bot', 'typing', '2026-08-18 12:31:25'),
(78, 1, 106, 'bot', 'typing', '2026-08-18 12:31:27'),
(79, 1, 106, 'bot', 'typing', '2026-08-18 12:31:28'),
(80, 1, 106, 'bot', 'typing', '2026-08-18 12:31:31'),
(81, 1, 106, 'bot', 'typing', '2026-08-18 12:31:33'),
(82, 1, 106, 'bot', 'typing', '2026-08-18 12:31:35'),
(83, 1, 106, 'bot', 'typing', '2026-08-18 12:31:37'),
(84, 1, 106, 'bot', 'typing', '2026-08-18 12:31:39'),
(85, 1, 106, 'bot', 'typing', '2026-08-18 12:31:40'),
(86, 1, 106, 'bot', 'typing', '2026-08-18 12:31:42'),
(87, 1, 106, 'bot', 'typing', '2026-08-18 12:31:44'),
(88, 1, 106, 'bot', 'typing', '2026-08-18 12:31:46'),
(89, 1, 106, 'bot', 'typing', '2026-08-18 12:31:48'),
(90, 1, 106, 'bot', 'typing', '2026-08-18 12:31:50'),
(91, 1, 106, 'bot', 'typing', '2026-08-18 12:31:52'),
(92, 1, 106, 'bot', 'typing', '2026-08-18 12:39:28'),
(93, 1, 106, 'bot', 'typing', '2026-08-18 12:43:40'),
(94, 1, 106, 'bot', 'typing', '2026-08-18 12:44:52'),
(95, 1, 106, 'bot', 'typing', '2026-08-18 18:30:56'),
(96, 1, 106, 'bot', 'typing', '2026-08-18 18:56:49'),
(97, 1, 106, 'bot', 'typing', '2026-08-25 15:55:35');

-- --------------------------------------------------------

--
-- Table structure for table `device_tokens`
--

CREATE TABLE `device_tokens` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `token` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `device_tokens`
--

INSERT INTO `device_tokens` (`id`, `user_id`, `token`, `created_at`) VALUES
(1197, 106, 'eNKWVUTjSeqNxg_GNcy36b:APA91bERA0SleOa7NsQKEqxBzzauarkDY8sqerecTSYvpPuvG_DqlKHhH05qDa8MwA4j6UgG8M3EsImPvt-uGiM7-Ij71kt32gtzdHeVAgJFQUJX35FYPUo', '2026-08-27 20:52:24'),
(1219, 106, 'dMYULVU4_F7jd5ehxm8ddf:APA91bGzRr9aQKRXJgmOcIsoUddXCEcbJLvibGKEs97mxT_gksn9TAMczaA7KnDn3jNtZScYseEbLkYkpTjsNBQ-gZepCtGr0snmr-pAhp9adblLVcbhS1Q', '2026-08-28 14:33:23'),
(1309, 106, 'ff8c1-lCS5mnoMP3KFo0vE:APA91bFXrnAUhGBBl3UZYoVDe3qT-C8vhYPX-BZ_87wnBLZ9pF-mc0ZRiaA3GMFNm8wqQzIop12hLG296DeyO_tuf28q2CO6gP67cJkv-fURLghjLM00_Zc', '2026-09-04 19:24:43'),
(1386, 105, 'cEBs9RRIRr2lC1Fr15OccA:APA91bHrCnk96N7fRgy-d_lytWV7SZpIhT9EWsK-equ72fMpi00fRV4ZfM2y0Zchjhg2Kh4UzuP4JT-WWT4SRtuiEuUElmsPALc37QSRsX9MAeKrC4Og2wA', '2026-09-06 20:46:35'),
(1458, 113, 'cbZbKAc4SFGz08kOazTIHa:APA91bGwua8sduONZhYaEYSDFVVXIQXyWX920015V67omonDaemXq762E6AjSLDF7QzzyvZkG3U5XZQ2p9pdJRcAe3xDnDW6Ky0mS1SmHR9emsHvtpjUIXo', '2026-09-12 00:27:15');

-- --------------------------------------------------------

--
-- Table structure for table `flasskdev_mobilestickerpacks`
--

CREATE TABLE `flasskdev_mobilestickerpacks` (
  `id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `owner` int(11) NOT NULL,
  `stickers` longtext NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `flasskdev_mobilestickerpacks`
--

INSERT INTO `flasskdev_mobilestickerpacks` (`id`, `name`, `owner`, `stickers`, `created_at`) VALUES
(1, 'Test', 107, '', '2026-08-21 11:05:04');

-- --------------------------------------------------------

--
-- Table structure for table `flasskdev_mobileuserstickerpacks`
--

CREATE TABLE `flasskdev_mobileuserstickerpacks` (
  `user_id` int(11) NOT NULL,
  `pack_id` int(11) NOT NULL,
  `position` int(11) NOT NULL DEFAULT 0,
  `added_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `flasskdev_mobileuserstickerpacks`
--

INSERT INTO `flasskdev_mobileuserstickerpacks` (`user_id`, `pack_id`, `position`, `added_at`) VALUES
(106, 1, 1, '2026-08-22 09:19:02');

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `sender_type` enum('user','bot') NOT NULL DEFAULT 'user',
  `content` text NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `reply_to_id` int(11) DEFAULT NULL,
  `is_edited` tinyint(1) NOT NULL DEFAULT 0,
  `deleted_by_sender` tinyint(1) DEFAULT 0,
  `deleted_by_receiver` tinyint(1) DEFAULT 0,
  `forwarded_from_id` int(11) DEFAULT NULL,
  `attachments` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`attachments`)),
  `reactions` text DEFAULT NULL,
  `reply_markup` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`reply_markup`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `messages`
--

INSERT INTO `messages` (`id`, `sender_id`, `receiver_id`, `sender_type`, `content`, `timestamp`, `is_read`, `reply_to_id`, `is_edited`, `deleted_by_sender`, `deleted_by_receiver`, `forwarded_from_id`, `attachments`, `reactions`, `reply_markup`) VALUES
(12601, 98, 106, 'user', 'Здравствуйте! Опишите, с чем нужна помощь в VibeMessenger. Укажите, если уже проводили диагностику.', '2026-08-25 15:55:47', 1, NULL, 0, 0, 0, NULL, NULL, NULL, '{\"inline_keyboard\":[[{\"text\":\"Проверить отправку\",\"callback_data\":\"diag:start:connection\"},{\"text\":\"Нет уведомлений\",\"callback_data\":\"diag:start:notifications\"}],[{\"text\":\"Не отправляются медиа\",\"callback_data\":\"diag:start:media\"},{\"text\":\"Ошибка в чате\",\"callback_data\":\"diag:start:messages\"}],[{\"text\":\"Аккаунт и безопасность\",\"callback_data\":\"diag:start:account\"},{\"text\":\"Подготовить обращение\",\"callback_data\":\"diag:report\"}],[{\"text\":\"Написать в поддержку\",\"url\":\"https:\\/\\/t.me\\/vibemsgchat\",\"bg_color\":\"#4F46E5\",\"text_color\":\"#FFFFFF\"}]]}'),
(12602, 1, 110, 'bot', 'Добро пожаловать в семейство Vibe!\nЗадавай мне любые вопросы, я подскажу что да как!\nМяу.', '2026-09-06 20:17:56', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12603, 110, 1, 'user', 'пон', '2026-09-06 20:38:54', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12604, 1, 111, 'bot', 'Добро пожаловать в семейство Vibe!\nЗадавай мне любые вопросы, я подскажу что да как!\nМяу.', '2026-09-06 20:39:53', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12605, 111, 106, 'user', 'ты сын бляди', '2026-09-06 20:41:06', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12606, 105, 111, 'user', '99392', '2026-09-06 20:47:17', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12607, 111, 105, 'user', 'пр шлюхиня ебаная', '2026-09-06 20:47:36', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12608, 105, 111, 'user', '534543444343345434353543535343535344', '2026-09-06 20:47:48', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12609, 111, 105, 'user', 'писч', '2026-09-06 20:49:19', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12610, 111, 105, 'user', 'го кекс', '2026-09-06 20:49:22', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12611, 111, 105, 'user', '😆', '2026-09-06 20:49:28', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12612, 106, 111, 'user', '😀😀', '2026-09-06 22:29:07', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12613, 1, 106, 'bot', '**В ваш аккаунт совершён вход**\n\nУстройство: Xiaomi 15T Pro · Android 17\nЛокация: Харьков, Украина\n\nЕсли это не вы, завершите сессию в разделе [Устройства](vibe://settings/devices).', '2026-09-07 15:48:22', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12614, 1, 106, 'bot', '**В ваш аккаунт совершён вход**\n\nУстройство: Xiaomi 15T Pro · Android 17\nЛокация: Харьков, Украина\n\nЕсли это не вы, завершите сессию в разделе [Устройства](vibe://settings/devices).', '2026-09-07 17:22:05', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12615, 106, 1, 'user', 'video_message:11024', '2026-09-08 13:57:35', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12616, 1, 106, 'bot', '**В ваш аккаунт совершён вход**\n\nУстройство: Xiaomi 15T Pro · Android 17\nЛокация: Харьков, Украина\n\nЕсли это не вы, завершите сессию в разделе [Устройства](vibe://settings/devices).', '2026-09-08 14:00:04', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12617, 1, 106, 'bot', '**В ваш аккаунт совершён вход**\n\nУстройство: ||Xiaomi 15T Pro · Android 17||\nЛокация: ||Харьков, Украина||\n\nЕсли это не вы, завершите сессию в разделе [Устройства](vibe://settings/devices).', '2026-09-08 14:02:53', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12618, 1, 106, 'bot', '**В ваш аккаунт совершён вход**\n\nУстройство: ||Xiaomi 15T Pro · Android 17||\nЛокация: ||Харьков, Украина||\n\nЕсли это не вы, завершите сессию в разделе [Устройства](vibe://settings/devices).', '2026-09-08 14:20:59', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12619, 106, 1, 'user', 'duration:9021', '2026-09-08 14:22:26', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12620, 106, 98, 'user', 'duration:600', '2026-09-10 22:50:22', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12621, 106, 98, 'user', 'video_message:13161', '2026-09-10 22:50:22', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12622, 106, 98, 'user', '📌📝🖊️✏️📖📚🔐🔓', '2026-09-11 17:38:28', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12623, 106, 98, 'user', '😍🥺✌️', '2026-09-11 17:38:35', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12624, 1, 112, 'bot', 'Добро пожаловать в семейство Vibe!\nЗадавай мне любые вопросы, я подскажу что да как!\nМяу.', '2026-09-11 23:26:51', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12625, 1, 113, 'bot', 'Добро пожаловать в семейство Vibe!\nЗадавай мне любые вопросы, я подскажу что да как!\nМяу.', '2026-09-11 23:42:44', 1, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12626, 113, 1, 'user', 'video_message:11487', '2026-09-12 00:05:29', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL),
(12627, 113, 1, 'user', 'video_message:8904', '2026-09-12 00:27:15', 0, NULL, 0, 0, 0, NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `muted_users`
--

CREATE TABLE `muted_users` (
  `user_id` int(11) NOT NULL,
  `muted_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `muted_users`
--

INSERT INTO `muted_users` (`user_id`, `muted_id`) VALUES
(106, 103),
(107, 1),
(107, 106);

-- --------------------------------------------------------

--
-- Table structure for table `notification_contacts`
--

CREATE TABLE `notification_contacts` (
  `user_id` int(11) NOT NULL,
  `peer_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notification_contacts`
--

INSERT INTO `notification_contacts` (`user_id`, `peer_id`) VALUES
(98, 106),
(105, 111),
(106, 1),
(106, 98),
(106, 111),
(111, 1),
(111, 105),
(111, 106),
(112, 1),
(113, 1);

-- --------------------------------------------------------

--
-- Table structure for table `notification_settings`
--

CREATE TABLE `notification_settings` (
  `user_id` int(11) NOT NULL,
  `mute_all` tinyint(1) NOT NULL DEFAULT 0,
  `auto_mute_new` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notification_settings`
--

INSERT INTO `notification_settings` (`user_id`, `mute_all`, `auto_mute_new`) VALUES
(113, 0, 0);

-- --------------------------------------------------------

--
-- Table structure for table `pending_users`
--

CREATE TABLE `pending_users` (
  `id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `code` varchar(10) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `attempts` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `pending_users`
--

INSERT INTO `pending_users` (`id`, `email`, `username`, `code`, `created_at`, `attempts`) VALUES
(157, 'flassk@proton.me', 'pidor', '087822', '2026-09-08 14:14:11', 0);

-- --------------------------------------------------------

--
-- Table structure for table `pinned_messages`
--

CREATE TABLE `pinned_messages` (
  `id` int(11) NOT NULL,
  `message_id` int(11) NOT NULL,
  `pinned_by_id` int(11) NOT NULL,
  `pinned_for_id` int(11) NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `pinned_messages`
--

INSERT INTO `pinned_messages` (`id`, `message_id`, `pinned_by_id`, `pinned_for_id`, `created_at`) VALUES
(45, 12619, 106, 106, '2026-09-08 14:23:24');

-- --------------------------------------------------------

--
-- Table structure for table `privacy_settings`
--

CREATE TABLE `privacy_settings` (
  `user_id` int(11) NOT NULL,
  `activity` varchar(20) DEFAULT 'EVERYONE',
  `activity_users` text DEFAULT NULL,
  `avatar` varchar(20) DEFAULT 'EVERYONE',
  `avatar_users` text DEFAULT NULL,
  `forwarded` varchar(20) DEFAULT 'EVERYONE',
  `forwarded_users` text DEFAULT NULL,
  `messages` varchar(20) DEFAULT 'EVERYONE',
  `messages_users` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'EVERYONE',
  `status_users` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `privacy_settings`
--

INSERT INTO `privacy_settings` (`user_id`, `activity`, `activity_users`, `avatar`, `avatar_users`, `forwarded`, `forwarded_users`, `messages`, `messages_users`, `status`, `status_users`) VALUES
(106, 'NOBODY', '[107]', 'EVERYONE', '[107]', 'NOBODY', '[]', 'EVERYONE', '[107]', 'EVERYONE', '[107]'),
(107, 'EVERYONE', '[]', 'EVERYONE', '[]', 'NOBODY', '[]', 'EVERYONE', '[]', 'EVERYONE', '[]');

-- --------------------------------------------------------

--
-- Table structure for table `reports`
--

CREATE TABLE `reports` (
  `id` int(11) NOT NULL,
  `theme` enum('SPAM','SCAM','DRUGS','WEAPONS','PORNOGRAPHY','CSAM','VIOLENCE','HARASSMENT','HATESPEECH','IMPERSONATION','MISINFORMATION') NOT NULL,
  `from_user` int(11) NOT NULL,
  `to_user` int(11) NOT NULL,
  `message_id` int(11) NOT NULL,
  `comment` varchar(512) DEFAULT NULL,
  `time` timestamp NULL DEFAULT current_timestamp(),
  `punishment` set('ban','freeze','spamblock') DEFAULT NULL,
  `punishment_time` timestamp NULL DEFAULT NULL,
  `status` enum('pending','accepted','declined') DEFAULT 'pending',
  `resolved_by` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reports`
--

INSERT INTO `reports` (`id`, `theme`, `from_user`, `to_user`, `message_id`, `comment`, `time`, `punishment`, `punishment_time`, `status`, `resolved_by`) VALUES
(1, '', 107, 108, 12122, '', '2026-08-09 13:40:47', NULL, NULL, 'accepted', 108);

-- --------------------------------------------------------

--
-- Table structure for table `sessions`
--

CREATE TABLE `sessions` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `os_version` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `last_active` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `sessions`
--

INSERT INTO `sessions` (`id`, `user_id`, `device_id`, `device_name`, `os_version`, `location`, `last_active`) VALUES
(1, 107, '17615384-0f53-4e93-b6d6-6df40dfb9979', 'sdk_gphone64_x86_64', 'Android 17', 'Харьков, Украина', '2026-08-09 15:20:16'),
(1061, 107, 'a8d1a530-0392-4003-9f6a-fac48b937549', 'sdk_gphone64_x86_64', 'Android 17', 'Харьков, Украина', '2026-08-10 21:18:46'),
(1220, 105, '781b713e-e738-415d-b8fe-2a9cc6fd583d', 'sdk_gphone64_x86_64', 'Android 17', 'Харьков, Украина', '2026-09-06 20:46:49'),
(2518, 109, '7418a6a6-8a79-4145-a057-836242e503eb', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-08-27 18:22:49'),
(3192, 111, 'd6a7941f-f4ad-43d0-972b-b1caf41b1a12', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-06 20:53:13'),
(3218, 106, 'd6a7941f-f4ad-43d0-972b-b1caf41b1a12', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-06 22:23:17'),
(3241, 106, 'b4ce75e1-1026-4cee-94c3-d57b322c8587', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-07 12:04:12'),
(3277, 106, '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-11 23:24:56'),
(3437, 112, '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-11 23:42:20'),
(3447, 113, '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'Xiaomi 15T Pro', 'Android 17', 'Харьков, Украина', '2026-09-12 00:27:15');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(32) NOT NULL,
  `username` varchar(32) NOT NULL,
  `register_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_online` tinyint(1) DEFAULT 0,
  `last_seen` datetime DEFAULT current_timestamp(),
  `is_developer` tinyint(1) DEFAULT 0,
  `is_verified` tinyint(1) DEFAULT 0,
  `avatar_url` varchar(255) DEFAULT NULL,
  `about` text DEFAULT NULL,
  `is_banned` tinyint(1) NOT NULL DEFAULT 0,
  `is_freezed` tinyint(1) NOT NULL DEFAULT 0,
  `freeze_time` timestamp NOT NULL,
  `is_spamblock` tinyint(1) NOT NULL DEFAULT 0,
  `spamblock_time` timestamp NOT NULL,
  `last_device_id` varchar(255) DEFAULT NULL,
  `language` varchar(8) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `name`, `username`, `register_date`, `is_online`, `last_seen`, `is_developer`, `is_verified`, `avatar_url`, `about`, `is_banned`, `is_freezed`, `freeze_time`, `is_spamblock`, `spamblock_time`, `last_device_id`, `language`) VALUES
(98, 'hesushesus97@gmail.com', '', 'hesus', '2026-07-28 18:57:33', 1, '2024-07-10 20:05:26', 1, 1, NULL, NULL, 1, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(99, 'snzjsjd@mail.ru', 'snsjdj', 'sjskznx', '2026-07-28 20:28:01', 0, '2026-07-28 22:28:01', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(100, 'snzjsjdj@mail.ru', 'snzjzs', 'snskdn', '2026-07-28 20:29:05', 0, '2026-07-28 22:48:15', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(101, 'snzjdndjdj@mail.ru', 'znsjejd', 'snskdndn', '2026-07-28 20:48:36', 0, '2026-07-28 22:48:36', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(102, 'sjsjdnnd@mail.ru', 'shzjsne', 'smxkjdd', '2026-07-28 21:00:08', 0, '2026-07-28 23:00:08', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(103, 'xbzjdjdnbz@mail.ru', 'snzjdjr', 'znzkdjd', '2026-07-28 21:06:16', 0, '2026-07-29 00:08:44', 1, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(104, 'sbxjendndj@gmail.com', 'andrei', 'dnxjendnd', '2026-07-28 22:09:13', 0, '2026-07-29 00:20:05', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', NULL, NULL),
(105, 'ammask@mail.ru', 'sbsjsndnd', 'snxjndxn', '2026-07-28 22:19:15', 0, '2026-09-06 23:23:40', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', '781b713e-e738-415d-b8fe-2a9cc6fd583d', NULL),
(106, 'flasskdev@gmail.com', 'Locked', 'locked', '2026-07-28 22:28:21', 0, '2026-09-12 01:24:59', 1, 0, 'https://flasskdev.alwaysdata.net/avatars/4297e122fc20a8ba92f2f445dd77cdf843fb8fb761a5706d7f550ef97a6f567d.webp', 'Вопросы в поддержку.', 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'ru'),
(107, 'kskskskks@gmail.com', 'asdasaasdasaasdasaasdasaasdasasd', 'asdasaasdasaasdasaasdasaasdasasd', '2026-07-29 01:47:51', 0, '2026-08-10 23:20:04', 1, 1, '', '9223', 1, 0, '0000-00-00 00:00:00', 0, '2026-08-26 14:42:24', 'a8d1a530-0392-4003-9f6a-fac48b937549', NULL),
(108, 'ejsksnsj@gmail.com', 'dayn', 'dalbaebsban', '2026-08-09 10:14:49', 0, '2026-08-09 17:28:18', 1, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 1, '0000-00-00 00:00:00', 'f75aa2ab-0d26-47ae-8111-d4c277a40d83', NULL),
(109, 'sjxkdndj@gmail.com', 'maks', 'sjzsnd', '2026-08-27 17:44:15', 0, '2026-08-27 20:23:55', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', '7418a6a6-8a79-4145-a057-836242e503eb', NULL),
(111, 'mama12tv@gmail.com', 'pidorsha', 'smxkdkd', '2026-09-06 20:39:53', 0, '2026-09-06 22:53:40', 0, 0, NULL, NULL, 0, 0, '0000-00-00 00:00:00', 0, '0000-00-00 00:00:00', 'd6a7941f-f4ad-43d0-972b-b1caf41b1a12', NULL),
(112, 'flassk90@gmail.com', 'sin sluhi', 'lods', '2026-09-11 23:26:51', 0, '2026-09-12 01:42:23', 0, 0, NULL, NULL, 0, 0, '2026-09-11 23:26:51', 0, '2026-09-11 23:26:51', '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'ru'),
(113, 'dbdjsndnd@mail.ru', 'pidoras', 'djdkdjd', '2026-09-11 23:42:44', 0, '2026-09-12 02:27:19', 0, 0, NULL, NULL, 0, 0, '2026-09-11 23:42:44', 0, '2026-09-11 23:42:44', '990492f9-5942-41b1-aa3d-980b8ccc8c64', 'ru');

-- --------------------------------------------------------

--
-- Table structure for table `user_security`
--

CREATE TABLE `user_security` (
  `user_id` int(11) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `hint` varchar(255) DEFAULT NULL,
  `failed_attempts` int(11) NOT NULL DEFAULT 0,
  `locked_until` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_security`
--

INSERT INTO `user_security` (`user_id`, `password_hash`, `hint`, `failed_attempts`, `locked_until`) VALUES
(106, '$2y$10$8GJohDLkOVC/wfm6KYWLq.CmIfHHu5ZWnTjJBX.n4PNzj/Wu0WRGK', '123', 0, NULL),
(112, NULL, NULL, 0, NULL),
(113, NULL, NULL, 0, NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `auth_challenges`
--
ALTER TABLE `auth_challenges`
  ADD PRIMARY KEY (`token_hash`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `auth_sessions`
--
ALTER TABLE `auth_sessions`
  ADD PRIMARY KEY (`token_hash`),
  ADD UNIQUE KEY `auth_user_device` (`user_id`,`device_id`);

--
-- Indexes for table `banned_devices`
--
ALTER TABLE `banned_devices`
  ADD PRIMARY KEY (`device_id`);

--
-- Indexes for table `blocked_users`
--
ALTER TABLE `blocked_users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`,`blocked_id`),
  ADD KEY `blocked_id` (`blocked_id`);

--
-- Indexes for table `bots`
--
ALTER TABLE `bots`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `bot_callbacks`
--
ALTER TABLE `bot_callbacks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_bot_offset` (`bot_id`,`id`);

--
-- Indexes for table `bot_callback_answers`
--
ALTER TABLE `bot_callback_answers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_delivered` (`is_delivered`,`id`),
  ADD KEY `idx_undelivered` (`is_delivered`,`id`);

--
-- Indexes for table `bot_outgoing_events`
--
ALTER TABLE `bot_outgoing_events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_undelivered` (`is_delivered`,`id`);

--
-- Indexes for table `bot_polling_locks`
--
ALTER TABLE `bot_polling_locks`
  ADD PRIMARY KEY (`bot_id`);

--
-- Indexes for table `bot_updates`
--
ALTER TABLE `bot_updates`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_bot_offset` (`bot_id`,`id`),
  ADD KEY `idx_bot_ref` (`bot_id`,`update_type`,`reference_id`);

--
-- Indexes for table `chat_actions`
--
ALTER TABLE `chat_actions`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `device_tokens`
--
ALTER TABLE `device_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `token` (`token`),
  ADD KEY `idx_user_id` (`user_id`);

--
-- Indexes for table `flasskdev_mobilestickerpacks`
--
ALTER TABLE `flasskdev_mobilestickerpacks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_owner` (`owner`),
  ADD KEY `idx_name` (`name`);

--
-- Indexes for table `flasskdev_mobileuserstickerpacks`
--
ALTER TABLE `flasskdev_mobileuserstickerpacks`
  ADD PRIMARY KEY (`user_id`,`pack_id`),
  ADD KEY `idx_pack` (`pack_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_sender` (`sender_id`),
  ADD KEY `idx_receiver` (`receiver_id`),
  ADD KEY `idx_chat` (`sender_id`,`receiver_id`,`timestamp`),
  ADD KEY `fk_message_reply` (`reply_to_id`);

--
-- Indexes for table `muted_users`
--
ALTER TABLE `muted_users`
  ADD PRIMARY KEY (`user_id`,`muted_id`);

--
-- Indexes for table `notification_contacts`
--
ALTER TABLE `notification_contacts`
  ADD PRIMARY KEY (`user_id`,`peer_id`);

--
-- Indexes for table `notification_settings`
--
ALTER TABLE `notification_settings`
  ADD PRIMARY KEY (`user_id`);

--
-- Indexes for table `pending_users`
--
ALTER TABLE `pending_users`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `pinned_messages`
--
ALTER TABLE `pinned_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `message_id` (`message_id`);

--
-- Indexes for table `privacy_settings`
--
ALTER TABLE `privacy_settings`
  ADD PRIMARY KEY (`user_id`);

--
-- Indexes for table `reports`
--
ALTER TABLE `reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_reports_from_user` (`from_user`),
  ADD KEY `fk_reports_to_user` (`to_user`);

--
-- Indexes for table `sessions`
--
ALTER TABLE `sessions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`,`device_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `user_security`
--
ALTER TABLE `user_security`
  ADD PRIMARY KEY (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `blocked_users`
--
ALTER TABLE `blocked_users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `bots`
--
ALTER TABLE `bots`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `bot_callbacks`
--
ALTER TABLE `bot_callbacks`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `bot_callback_answers`
--
ALTER TABLE `bot_callback_answers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT for table `bot_outgoing_events`
--
ALTER TABLE `bot_outgoing_events`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `bot_updates`
--
ALTER TABLE `bot_updates`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `chat_actions`
--
ALTER TABLE `chat_actions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=98;

--
-- AUTO_INCREMENT for table `device_tokens`
--
ALTER TABLE `device_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1459;

--
-- AUTO_INCREMENT for table `flasskdev_mobilestickerpacks`
--
ALTER TABLE `flasskdev_mobilestickerpacks`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12628;

--
-- AUTO_INCREMENT for table `pending_users`
--
ALTER TABLE `pending_users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=161;

--
-- AUTO_INCREMENT for table `pinned_messages`
--
ALTER TABLE `pinned_messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=48;

--
-- AUTO_INCREMENT for table `reports`
--
ALTER TABLE `reports`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `sessions`
--
ALTER TABLE `sessions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3468;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=114;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `auth_challenges`
--
ALTER TABLE `auth_challenges`
  ADD CONSTRAINT `auth_challenges_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `auth_sessions`
--
ALTER TABLE `auth_sessions`
  ADD CONSTRAINT `auth_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `blocked_users`
--
ALTER TABLE `blocked_users`
  ADD CONSTRAINT `blocked_users_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `blocked_users_ibfk_2` FOREIGN KEY (`blocked_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `flasskdev_mobileuserstickerpacks`
--
ALTER TABLE `flasskdev_mobileuserstickerpacks`
  ADD CONSTRAINT `fk_usp_pack` FOREIGN KEY (`pack_id`) REFERENCES `flasskdev_mobilestickerpacks` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `fk_message_reply` FOREIGN KEY (`reply_to_id`) REFERENCES `messages` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `notification_contacts`
--
ALTER TABLE `notification_contacts`
  ADD CONSTRAINT `notification_contacts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notification_settings`
--
ALTER TABLE `notification_settings`
  ADD CONSTRAINT `notification_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `pinned_messages`
--
ALTER TABLE `pinned_messages`
  ADD CONSTRAINT `pinned_messages_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `privacy_settings`
--
ALTER TABLE `privacy_settings`
  ADD CONSTRAINT `privacy_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `reports`
--
ALTER TABLE `reports`
  ADD CONSTRAINT `fk_reports_from_user` FOREIGN KEY (`from_user`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reports_to_user` FOREIGN KEY (`to_user`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `sessions`
--
ALTER TABLE `sessions`
  ADD CONSTRAINT `sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_security`
--
ALTER TABLE `user_security`
  ADD CONSTRAINT `user_security_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
