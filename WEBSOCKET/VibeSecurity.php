<?php
/**
 * Token-based authentication. Requires migrations/20260907_security_settings.sql;
 * резервная почта и сброс второго фактора — migrations/20260908_two_factor_recovery.sql.
 */
final class VibeSecurity {
    private $db;
    /** callable(string $to, string $subject, string $body): bool — очередь писем (Chat::sendEmailToRabbitMQ). */
    private $mailer;
    public function __construct(PDO $db, $mailer = null) { $this->db = $db; $this->mailer = $mailer; }
    private function row($sql, array $args) {
        $q = $this->db->prepare($sql); $q->execute($args); return $q->fetch(PDO::FETCH_ASSOC);
    }
    private function run($sql, array $args = []) {
        $q = $this->db->prepare($sql); $q->execute($args); return $q;
    }
    public function validate($token, $userId = null, $deviceId = null) {
        if (!is_string($token) || !preg_match('/^[a-f0-9]{64}$/D', $token)) return false;
        $row = $this->row('SELECT a.*, u.is_banned, u.is_freezed FROM auth_sessions a JOIN users u ON u.id=a.user_id WHERE token_hash=? AND expires_at>NOW()', [hash('sha256', $token)]);
        if (!$row || $row['is_banned'] || $row['is_freezed']) return false;
        if ($userId !== null && (int)$row['user_id'] !== (int)$userId) return false;
        if ($deviceId !== null && !hash_equals($row['device_id'], (string)$deviceId)) return false;
        return $row;
    }
    /**
     * @param bool $notifyLogin login_notified=0 — первый auth_connect этой сессии отправит
     *                          «В ваш аккаунт совершён вход» через Vibe cat. Для новой
     *                          регистрации уведомление бессмысленно, там передаём false.
     */
    public function issue($userId, $deviceId, $notifyLogin = true) {
        if (!is_string($deviceId) || strlen($deviceId)<8 || strlen($deviceId)>255) throw new InvalidArgumentException('Invalid device');
        $user = $this->row('SELECT id FROM users WHERE id=? AND is_banned=0 AND is_freezed=0', [$userId]);
        if (!$user) throw new RuntimeException('Account unavailable');
        $token = bin2hex(random_bytes(32));
        $this->run('DELETE FROM auth_sessions WHERE user_id=? AND device_id=?', [$userId, $deviceId]);
        try {
            $this->run('INSERT INTO auth_sessions (token_hash,user_id,device_id,expires_at,login_notified) VALUES (?,?,?,DATE_ADD(NOW(), INTERVAL 30 DAY),?)', [hash('sha256',$token),$userId,$deviceId,$notifyLogin?0:1]);
        } catch (PDOException $e) {
            // Миграция 20260908_login_notifications ещё не применена: сессия важнее уведомления.
            $this->run('INSERT INTO auth_sessions (token_hash,user_id,device_id,expires_at) VALUES (?,?,?,DATE_ADD(NOW(), INTERVAL 30 DAY))', [hash('sha256',$token),$userId,$deviceId]);
        }
        return $token;
    }
    public function afterEmail($userId, $deviceId, $isNew) {
        if (!is_string($deviceId) || strlen($deviceId)<8 || strlen($deviceId)>255) throw new InvalidArgumentException('Update the app');
        $s = $this->row('SELECT password_hash,hint FROM user_security WHERE user_id=?', [$userId]);
        if ($s && $s['password_hash']) {
            $token = bin2hex(random_bytes(32));
            $this->run('DELETE FROM auth_challenges WHERE expires_at<NOW() OR (user_id=? AND device_id=?)', [$userId,$deviceId]);
            $this->run('INSERT INTO auth_challenges (token_hash,user_id,device_id,expires_at) VALUES (?,?,?,DATE_ADD(NOW(), INTERVAL 5 MINUTE))', [hash('sha256',$token),$userId,$deviceId]);
            return ['type'=>'verify_code_result','success'=>false,'requires_two_factor'=>true,'challenge_token'=>$token,'hint'=>$s['hint']];
        }
        return ['type'=>'verify_code_result','success'=>true,'user_id'=>$userId,'is_new_user'=>$isNew,'session_token'=>$this->issue($userId,$deviceId, !$isNew)];
    }
    private function verifyPassword($userId, $password, $s) {
        if ($s['locked_until'] && strtotime($s['locked_until'])>time()) return false;
        if (!is_string($password) || strlen($password)>256 || !password_verify($password, $s['password_hash'])) {
            $this->run('UPDATE user_security SET failed_attempts=IF(locked_until IS NOT NULL AND locked_until<=NOW(),1,failed_attempts+1), locked_until=IF(failed_attempts>=5,DATE_ADD(NOW(),INTERVAL 5 MINUTE),NULL) WHERE user_id=?', [$userId]);
            return false;
        }
        $this->run('UPDATE user_security SET failed_attempts=0,locked_until=NULL WHERE user_id=?', [$userId]);
        return true;
    }
    public function challenge(array $data) {
        $this->db->beginTransaction();
        try {
            $c = $this->row('SELECT * FROM auth_challenges WHERE token_hash=? AND expires_at>NOW() FOR UPDATE', [hash('sha256', (string)($data['challenge_token']??''))]);
            if (!$c) { $this->db->commit(); return ['type'=>'verify_code_result','success'=>false,'challenge_expired'=>true,'message'=>'Срок проверки истёк. Войдите заново.']; }
            $s = $this->row('SELECT * FROM user_security WHERE user_id=? FOR UPDATE', [$c['user_id']]);
            if (!$s || !$s['password_hash'] || !$this->verifyPassword($c['user_id'],$data['password']??'', $s)) {
                $this->db->commit(); return ['type'=>'verify_code_result','success'=>false,'message'=>'Неверный пароль или временная блокировка. После 5 ошибок подождите 5 минут.'];
            }
            $this->run('DELETE FROM auth_challenges WHERE token_hash=?', [$c['token_hash']]);
            $token=$this->issue((int)$c['user_id'],$c['device_id']);
            $this->db->commit();
            return ['type'=>'verify_code_result','success'=>true,'user_id'=>(int)$c['user_id'],'is_new_user'=>false,'session_token'=>$token];
        } catch (Throwable $e) { if ($this->db->inTransaction()) $this->db->rollBack(); throw $e; }
    }
    public function settings($userId, array $data, $currentToken) {
        $this->db->beginTransaction();
        try {
            $this->run('INSERT IGNORE INTO user_security (user_id) VALUES (?)',[$userId]);
            $s=$this->row('SELECT * FROM user_security WHERE user_id=? FOR UPDATE',[$userId]);
            // Резервная почта не зависит от операции ниже; null, если миграция ещё не применена.
            $recovery = $s['recovery_email'] ?? null;
            if ($data['type']==='set_two_factor') {
                if ($s['password_hash'] && !$this->verifyPassword($userId,$data['current_password']??'', $s)) {
                    $this->db->commit(); return ['type'=>'two_factor_result','success'=>false,'message'=>'Неверный пароль или временная блокировка.'];
                }
                $op=$data['operation']??'set'; $hash=$s['password_hash'];
                if ($op==='disable') { $hash=null; $hint=null; }
                elseif ($op==='set') {
                    $p=$data['password']??'';
                    if (!is_string($p) || strlen($p)<6 || strlen($p)>72) {
                        $this->db->rollBack(); return ['type'=>'two_factor_result','success'=>false,'message'=>'Пароль должен занимать от 6 до 72 байт UTF-8.'];
                    }
                    $hash=password_hash($p,PASSWORD_DEFAULT); $hint=mb_substr((string)($data['hint']??''),0,128);
                    if ($hint!=='' && mb_stripos($hint,$p)!==false) { $this->db->rollBack(); return ['type'=>'two_factor_result','success'=>false,'message'=>'Не включайте пароль в подсказку.']; }
                } elseif ($op==='hint' && $hash) { $hint=mb_substr((string)($data['hint']??''),0,128); }
                else { $this->db->rollBack(); return ['type'=>'two_factor_result','success'=>false,'message'=>'Недопустимая операция.']; }
                $this->run('UPDATE user_security SET password_hash=?,hint=?,failed_attempts=0,locked_until=NULL WHERE user_id=?',[$hash,$hint,$userId]);
                // Changing protection revokes all OTHER sessions and pending challenges.
                if ($op!=='hint') {
                    $this->run('DELETE FROM auth_sessions WHERE user_id=? AND token_hash<>?',[$userId,hash('sha256',$currentToken)]);
                    $this->run('DELETE FROM sessions WHERE user_id=? AND device_id NOT IN (SELECT device_id FROM auth_sessions WHERE user_id=?)',[$userId,$userId]);
                    $this->run('DELETE FROM auth_challenges WHERE user_id=?',[$userId]);
                }
                $s=['password_hash'=>$hash,'hint'=>$hint];
            }
            $this->db->commit();
            return ['type'=>'two_factor_result','success'=>true,'enabled'=>!empty($s['password_hash']),'hint'=>$s['hint'],'recovery_email'=>$recovery];
        } catch (Throwable $e) { if ($this->db->inTransaction()) $this->db->rollBack(); throw $e; }
    }

    /* ------------------------------------------------------------------ */
    /*  Резервная почта и сброс второго фактора                          */
    /* ------------------------------------------------------------------ */

    /** a***@example.com — для экрана входа, где владение аккаунтом ещё не доказано. */
    public static function maskEmail($email) {
        $email = (string)$email;
        $at = strrpos($email, '@');
        if ($at === false || $at === 0) return '***';
        return mb_substr(substr($email, 0, $at), 0, 1) . '***' . substr($email, $at);
    }
    private static function validEmail($email) {
        return is_string($email) && strlen($email) <= 255 && filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
    }
    private function mail($to, $subject, $body) {
        return $this->mailer ? (bool)call_user_func($this->mailer, $to, $subject, $body) : false;
    }
    private function newCode() { return sprintf('%06d', random_int(0, 999999)); }
    /** Повтор не чаще раза в минуту: код живёт 10 минут, значит «только что отправлен», если осталось > 9. */
    private static function codeJustSent($expiresAt) {
        return $expiresAt && strtotime($expiresAt) - time() > 9 * 60;
    }
    private function finish(array $resp) { if ($this->db->inTransaction()) $this->db->commit(); return $resp; }
    private function challengeRow($token, $forUpdate = false) {
        return $this->row('SELECT * FROM auth_challenges WHERE token_hash=? AND expires_at>NOW()' . ($forUpdate ? ' FOR UPDATE' : ''), [hash('sha256', (string)$token)]);
    }
    private static function challengeExpired() {
        return ['type'=>'two_factor_reset_result','success'=>false,'challenge_expired'=>true,'message'=>'Срок проверки истёк. Войдите заново.'];
    }

    /**
     * set_recovery_email. operation = request (email, current_password) → письмо с кодом;
     * confirm (code) → адрес сохранён; remove (current_password) → адрес удалён.
     * При включённой защите request/remove требуют её пароль: иначе любой, кто взял
     * разблокированный телефон, подменил бы адрес и снял защиту через «сброс».
     */
    public function recoveryEmail($userId, array $data) {
        $op = $data['operation'] ?? 'request';
        $fail = function ($msg, array $extra = []) { return array_merge(['type'=>'recovery_email_result','success'=>false,'message'=>$msg], $extra); };
        $this->db->beginTransaction();
        try {
            $this->run('INSERT IGNORE INTO user_security (user_id) VALUES (?)', [$userId]);
            $s = $this->row('SELECT * FROM user_security WHERE user_id=? FOR UPDATE', [$userId]);
            if (!$s || !array_key_exists('recovery_email', $s)) return $this->finish($fail('Сервер ещё не обновлён: миграция резервной почты не применена.'));
            if ($op === 'request') {
                $email = mb_strtolower(trim((string)($data['email'] ?? '')));
                if (!self::validEmail($email)) return $this->finish($fail('Некорректный адрес почты.', ['error'=>'invalid_email']));
                $u = $this->row('SELECT email FROM users WHERE id=?', [$userId]);
                if ($u && mb_strtolower(trim((string)$u['email'])) === $email) return $this->finish($fail('Основная почта аккаунта не может быть резервной: укажите другой адрес.', ['error'=>'same_as_primary']));
                if ($s['password_hash'] && !$this->verifyPassword($userId, $data['current_password'] ?? '', $s)) return $this->finish($fail('Неверный пароль или временная блокировка.', ['error'=>'wrong_password']));
                if ($s['recovery_pending_email'] === $email && self::codeJustSent($s['recovery_code_expires'])) return $this->finish($fail('Код уже отправлен. Повторить можно через минуту.', ['error'=>'too_soon']));
                $code = $this->newCode();
                $this->run('UPDATE user_security SET recovery_pending_email=?, recovery_code_hash=?, recovery_code_expires=DATE_ADD(NOW(), INTERVAL 10 MINUTE), recovery_attempts=0 WHERE user_id=?', [$email, hash('sha256', $code), $userId]);
                $this->db->commit(); // письмо ходит долго — блокировки строк держать не нужно
                if (!$this->mail($email, 'Подтверждение резервной почты Vibe', "Код подтверждения резервной почты: {$code}\nКод действует 10 минут. Если вы не запрашивали его, просто проигнорируйте письмо.")) {
                    return $fail('Не удалось отправить письмо. Попробуйте позже.');
                }
                return ['type'=>'recovery_email_result','success'=>true,'step'=>'code_sent','email'=>$email,'recovery_email'=>$s['recovery_email']];
            }
            if ($op === 'confirm') {
                if (!$s['recovery_pending_email'] || !$s['recovery_code_hash'] || strtotime($s['recovery_code_expires']) <= time()) return $this->finish($fail('Код устарел. Запросите новый.'));
                $attempts = (int)$s['recovery_attempts'] + 1;
                if (!hash_equals($s['recovery_code_hash'], hash('sha256', trim((string)($data['code'] ?? ''))))) {
                    if ($attempts >= 5) {
                        $this->run('UPDATE user_security SET recovery_pending_email=NULL, recovery_code_hash=NULL, recovery_code_expires=NULL, recovery_attempts=0 WHERE user_id=?', [$userId]);
                        return $this->finish($fail('Превышено число попыток. Запросите новый код.'));
                    }
                    $this->run('UPDATE user_security SET recovery_attempts=? WHERE user_id=?', [$attempts, $userId]);
                    return $this->finish($fail('Неверный код. Осталось попыток: ' . (5 - $attempts)));
                }
                $this->run('UPDATE user_security SET recovery_email=recovery_pending_email, recovery_pending_email=NULL, recovery_code_hash=NULL, recovery_code_expires=NULL, recovery_attempts=0 WHERE user_id=?', [$userId]);
                return $this->finish(['type'=>'recovery_email_result','success'=>true,'step'=>'confirmed','recovery_email'=>$s['recovery_pending_email']]);
            }
            if ($op === 'remove') {
                if ($s['password_hash'] && !$this->verifyPassword($userId, $data['current_password'] ?? '', $s)) return $this->finish($fail('Неверный пароль или временная блокировка.', ['error'=>'wrong_password']));
                $this->run('UPDATE user_security SET recovery_email=NULL, recovery_pending_email=NULL, recovery_code_hash=NULL, recovery_code_expires=NULL, recovery_attempts=0, reset_code_hash=NULL, reset_code_expires=NULL, reset_attempts=0 WHERE user_id=?', [$userId]);
                return $this->finish(['type'=>'recovery_email_result','success'=>true,'step'=>'removed','recovery_email'=>null]);
            }
            return $this->finish($fail('Недопустимая операция.'));
        } catch (Throwable $e) { if ($this->db->inTransaction()) $this->db->rollBack(); throw $e; }
    }

    /**
     * request_two_factor_reset. На экране входа клиент предъявляет challenge_token (значит,
     * код с основной почты / из Vibe cat уже введён); из настроек передаётся $authedUserId
     * уже проверенной сессии. Код сброса уходит только на резервную почту.
     */
    public function resetRequest(array $data, $authedUserId = null) {
        $fail = function ($msg, array $extra = []) { return array_merge(['type'=>'two_factor_reset_result','success'=>false,'message'=>$msg], $extra); };
        $challenge = null; $userId = $authedUserId;
        if ($userId === null) {
            $challenge = $this->challengeRow($data['challenge_token'] ?? '');
            if (!$challenge) return self::challengeExpired();
            $userId = (int)$challenge['user_id'];
        }
        $s = $this->row('SELECT * FROM user_security WHERE user_id=?', [$userId]);
        if (!$s || !$s['password_hash']) return $fail('Двухфакторная защита не включена.', ['error'=>'not_enabled']);
        if (empty($s['recovery_email'])) return $fail('Резервная почта не задана, сбросить пароль через неё нельзя. Обратитесь в поддержку.', ['error'=>'no_recovery_email']);
        if (self::codeJustSent($s['reset_code_expires'] ?? null)) return $fail('Код уже отправлен. Повторить можно через минуту.', ['error'=>'too_soon']);
        $code = $this->newCode();
        $this->run('UPDATE user_security SET reset_code_hash=?, reset_code_expires=DATE_ADD(NOW(), INTERVAL 10 MINUTE), reset_attempts=0 WHERE user_id=?', [hash('sha256', $code), $userId]);
        // Пока пользователь ходит за письмом, challenge входа не должен протухнуть.
        if ($challenge) $this->run('UPDATE auth_challenges SET expires_at=DATE_ADD(NOW(), INTERVAL 15 MINUTE) WHERE token_hash=?', [$challenge['token_hash']]);
        if (!$this->mail($s['recovery_email'], 'Сброс двухфакторной защиты Vibe', "Код для сброса двухфакторной защиты: {$code}\nКод действует 10 минут. Если это не вы, не вводите его и смените пароль защиты в настройках Vibe.")) {
            return $fail('Не удалось отправить письмо. Попробуйте позже.');
        }
        return ['type'=>'two_factor_reset_result','success'=>true,'step'=>'code_sent','email_masked'=>self::maskEmail($s['recovery_email'])];
    }

    /**
     * confirm_two_factor_reset. Сброс = отключение защиты; как и при обычном отключении,
     * остальные сессии отзываются. С экрана входа сразу выдаём сессию (verify_code_result,
     * клиент обрабатывает его как обычный успешный вход); из настроек — two_factor_result.
     */
    public function resetConfirm(array $data, $authedUserId = null, $currentToken = null) {
        $fail = function ($msg, array $extra = []) { return array_merge(['type'=>'two_factor_reset_result','success'=>false,'message'=>$msg], $extra); };
        $this->db->beginTransaction();
        try {
            $challenge = null; $userId = $authedUserId;
            if ($userId === null) {
                $challenge = $this->challengeRow($data['challenge_token'] ?? '', true);
                if (!$challenge) return $this->finish(self::challengeExpired());
                $userId = (int)$challenge['user_id'];
            }
            $s = $this->row('SELECT * FROM user_security WHERE user_id=? FOR UPDATE', [$userId]);
            if (!$s || empty($s['reset_code_hash']) || strtotime($s['reset_code_expires']) <= time()) return $this->finish($fail('Код устарел. Запросите новый.'));
            $attempts = (int)$s['reset_attempts'] + 1;
            if (!hash_equals($s['reset_code_hash'], hash('sha256', trim((string)($data['code'] ?? ''))))) {
                if ($attempts >= 5) {
                    $this->run('UPDATE user_security SET reset_code_hash=NULL, reset_code_expires=NULL, reset_attempts=0 WHERE user_id=?', [$userId]);
                    return $this->finish($fail('Превышено число попыток. Запросите новый код.'));
                }
                $this->run('UPDATE user_security SET reset_attempts=? WHERE user_id=?', [$attempts, $userId]);
                return $this->finish($fail('Неверный код. Осталось попыток: ' . (5 - $attempts)));
            }
            $this->run('UPDATE user_security SET password_hash=NULL, hint=NULL, failed_attempts=0, locked_until=NULL, reset_code_hash=NULL, reset_code_expires=NULL, reset_attempts=0 WHERE user_id=?', [$userId]);
            $this->run('DELETE FROM auth_challenges WHERE user_id=?', [$userId]);
            // При входе своей сессии ещё нет — отзываем все; из настроек — все, кроме текущей.
            $keep = $challenge ? '' : hash('sha256', (string)$currentToken);
            $this->run('DELETE FROM auth_sessions WHERE user_id=? AND token_hash<>?', [$userId, $keep]);
            $this->run('DELETE FROM sessions WHERE user_id=? AND device_id NOT IN (SELECT device_id FROM auth_sessions WHERE user_id=?)', [$userId, $userId]);
            if ($challenge) {
                $token = $this->issue($userId, $challenge['device_id']);
                $this->db->commit();
                return ['type'=>'verify_code_result','success'=>true,'user_id'=>$userId,'is_new_user'=>false,'session_token'=>$token,'two_factor_reset'=>true];
            }
            return $this->finish(['type'=>'two_factor_result','success'=>true,'enabled'=>false,'hint'=>null,'recovery_email'=>$s['recovery_email'],'reset'=>true]);
        } catch (Throwable $e) { if ($this->db->inTransaction()) $this->db->rollBack(); throw $e; }
    }

    public function notifications($userId, array $data) {
        if ($data['type']==='set_notification_settings') {
            $this->run('INSERT INTO notification_settings (user_id,mute_all,auto_mute_new) VALUES (?,?,?) ON DUPLICATE KEY UPDATE mute_all=VALUES(mute_all),auto_mute_new=VALUES(auto_mute_new)',[$userId,!empty($data['mute_all'])?1:0,!empty($data['auto_mute_new'])?1:0]);
        }
        $s=$this->row('SELECT mute_all,auto_mute_new FROM notification_settings WHERE user_id=?',[$userId]);
        return ['type'=>'notification_settings_result','success'=>true,'mute_all'=>!empty($s['mute_all']),'auto_mute_new'=>!empty($s['auto_mute_new'])];
    }
    /** Must run before delivering a message, also for bot messages. */
    public function registerContact($userId,$peerId) {
        $q=$this->run('INSERT IGNORE INTO notification_contacts (user_id,peer_id) SELECT id,? FROM users WHERE id=?',[$peerId,$userId]);
        if ($q->rowCount()>0) {
            $s=$this->row('SELECT auto_mute_new FROM notification_settings WHERE user_id=?',[$userId]);
            if (!empty($s['auto_mute_new'])) $this->run('INSERT IGNORE INTO muted_users (user_id,muted_id) VALUES (?,?)',[$userId,$peerId]);
        }
    }
}
