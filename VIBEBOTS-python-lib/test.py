from __future__ import annotations

import logging
import re
from collections import defaultdict, deque
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Deque

import config
from mistralai.client import Mistral
from python.main import Bot, InlineKeyboardButton, InlineKeyboardMarkup


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger("vibe_support_bot")

bot = Bot(token=config.token)
mistral = Mistral(api_key=config.mistralai_token)

MAX_USER_MESSAGE_LENGTH = 2_000
MAX_REPLY_LENGTH = 1_600
MAX_CONTEXT_MESSAGES = 6
SUPPORT_CHAT_URL = "https://t.me/vibemsgchat"

conversation_history: dict[int, Deque[dict[str, str]]] = defaultdict(
    lambda: deque(maxlen=MAX_CONTEXT_MESSAGES)
)


@dataclass(frozen=True)
class DiagnosticOption:
    label: str
    value: str


@dataclass(frozen=True)
class DiagnosticStep:
    field: str
    question: str
    options: tuple[DiagnosticOption, ...]


@dataclass
class DiagnosticSession:
    category: str
    step_index: int = 0
    answers: dict[str, str] = field(default_factory=dict)
    started_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))


CATEGORY_TITLES = {
    "connection": "Подключение и отправка",
    "notifications": "Уведомления",
    "media": "Фото, видео и файлы",
    "messages": "Сообщения и элементы чата",
    "account": "Аккаунт и безопасность",
}

FLOW_STEPS: dict[str, tuple[DiagnosticStep, ...]] = {
    "connection": (
        DiagnosticStep(
            field="network",
            question="Шаг 1 из 3. Через какую сеть сейчас работает Vibe?",
            options=(
                DiagnosticOption("Wi-Fi", "Wi-Fi"),
                DiagnosticOption("Мобильная сеть", "мобильная сеть"),
                DiagnosticOption("Пробовал обе", "обе сети"),
            ),
        ),
        DiagnosticStep(
            field="internet",
            question="Шаг 2 из 3. Открываются ли сайты или другие приложения в этой сети?",
            options=(
                DiagnosticOption("Да", "да"),
                DiagnosticOption("Нет", "нет"),
                DiagnosticOption("Только иногда", "нестабильно"),
            ),
        ),
        DiagnosticStep(
            field="delivery",
            question="Шаг 3 из 3. Что происходит с тестовым сообщением в этот чат?",
            options=(
                DiagnosticOption("Отправилось", "отправилось"),
                DiagnosticOption("Осталось с часами", "ожидание"),
                DiagnosticOption("Появилась ошибка", "ошибка"),
            ),
        ),
    ),
    "notifications": (
        DiagnosticStep(
            field="permission",
            question="Шаг 1 из 3. Разрешены ли уведомления для Vibe в настройках Android?",
            options=(
                DiagnosticOption("Разрешены", "разрешены"),
                DiagnosticOption("Запрещены", "запрещены"),
                DiagnosticOption("Не знаю", "не проверял"),
            ),
        ),
        DiagnosticStep(
            field="chat_muted",
            question="Шаг 2 из 3. Уведомления выключены именно у нужного чата?",
            options=(
                DiagnosticOption("Нет, чат не выключен", "нет"),
                DiagnosticOption("Да, чат выключен", "да"),
                DiagnosticOption("Не знаю", "не проверял"),
            ),
        ),
        DiagnosticStep(
            field="battery",
            question="Шаг 3 из 3. Есть ли для Vibe ограничение батареи или фоновой работы?",
            options=(
                DiagnosticOption("Нет ограничений", "нет"),
                DiagnosticOption("Есть ограничение", "есть"),
                DiagnosticOption("Не знаю", "не проверял"),
            ),
        ),
    ),
    "media": (
        DiagnosticStep(
            field="media_type",
            question="Шаг 1 из 3. Что не отправляется?",
            options=(
                DiagnosticOption("Фото", "фото"),
                DiagnosticOption("Видео", "видео"),
                DiagnosticOption("Файл или документ", "файл"),
                DiagnosticOption("Голосовое", "голосовое"),
            ),
        ),
        DiagnosticStep(
            field="permission",
            question="Шаг 2 из 3. Есть ли у Vibe доступ к выбранным фото или файлам?",
            options=(
                DiagnosticOption("Да", "да"),
                DiagnosticOption("Нет", "нет"),
                DiagnosticOption("Не знаю", "не проверял"),
            ),
        ),
        DiagnosticStep(
            field="size",
            question="Шаг 3 из 3. Как ведёт себя небольшое вложение до 10 МБ?",
            options=(
                DiagnosticOption("Отправляется", "отправляется"),
                DiagnosticOption("Тоже не отправляется", "не отправляется"),
                DiagnosticOption("Не проверял", "не проверял"),
            ),
        ),
    ),
    "messages": (
        DiagnosticStep(
            field="feature",
            question="Шаг 1 из 3. С чем возникла проблема?",
            options=(
                DiagnosticOption("Отправка текста", "отправка"),
                DiagnosticOption("Кнопка бота", "инлайн-кнопка"),
                DiagnosticOption("Ответ или закреп", "ответ или закреп"),
                DiagnosticOption("Реакция или пересылка", "реакция или пересылка"),
            ),
        ),
        DiagnosticStep(
            field="symptom",
            question="Шаг 2 из 3. Как именно проявляется проблема?",
            options=(
                DiagnosticOption("Нажатие не срабатывает", "нет реакции"),
                DiagnosticOption("Ответ приходит с задержкой", "задержка"),
                DiagnosticOption("Экран выглядит неправильно", "визуальная ошибка"),
                DiagnosticOption("Есть текст ошибки", "ошибка"),
            ),
        ),
        DiagnosticStep(
            field="repeatability",
            question="Шаг 3 из 3. Ошибка повторяется после закрытия и повторного открытия чата?",
            options=(
                DiagnosticOption("Да, всегда", "всегда"),
                DiagnosticOption("Иногда", "иногда"),
                DiagnosticOption("Нет, исчезла", "исчезла"),
            ),
        ),
    ),
    "account": (
        DiagnosticStep(
            field="account_case",
            question="Шаг 1 из 2. Что случилось с аккаунтом?",
            options=(
                DiagnosticOption("Не могу войти", "не удаётся войти"),
                DiagnosticOption("Подозрительный сеанс", "подозрительный сеанс"),
                DiagnosticOption("Блокировка или заморозка", "блокировка"),
            ),
        ),
        DiagnosticStep(
            field="access_state",
            question="Шаг 2 из 2. Есть ли доступ к Vibe хотя бы на одном устройстве?",
            options=(
                DiagnosticOption("Да", "есть доступ"),
                DiagnosticOption("Нет", "доступа нет"),
                DiagnosticOption("Не уверен", "не уверен"),
            ),
        ),
    ),
}

diagnostic_sessions: dict[int, DiagnosticSession] = {}

SUPPORT_SYSTEM_PROMPT = """
Ты — Vibe Support, помощник VibeMessenger. Отвечай практично и кратко.
Не повторяй универсальные советы, если пользователь уже прошёл диагностику: опирайся
на её результаты. Предлагай одно конкретное действие за раз и объясняй ожидаемый
результат. Не выдумывай статусы, доступ к аккаунту, сроки, логи или действия команды.
Никогда не проси пароль, код подтверждения, токен, API-ключ или платёжные данные.

Функции Vibe: личные сообщения, фото, видео, голосовые и видеосообщения, музыка,
документы, реакции, ответы, пересылка, редактирование, удаление, закрепления,
Markdown, приватность, устройства, push-уведомления, блокировки, жалобы на спам
и Bot API. Если нужны данные для поддержки, попроси только версию приложения,
модель Android, время ошибки и безопасные шаги воспроизведения.
""".strip()


def normalize_text(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip())


def contains_secret(value: str) -> bool:
    """Detect information that support must never accept from a user."""
    return bool(
        re.search(
            r"(?i)(?:token|api[ _-]?key|парол[ья]?|код\s+подтверждения|otp)\s*[:=]\s*\S{6,}",
            value,
        )
        or re.search(r"\b(?:\d[ -]?){8,}\b", value)
    )


def redact_sensitive_text(value: str) -> str:
    value = re.sub(
        r"(?i)((?:token|api[ _-]?key|парол[ья]?|код\s+подтверждения|otp)\s*[:=]\s*)\S+",
        r"\1[REDACTED]",
        value,
    )
    return re.sub(r"\b(?:\d[ -]?){8,}\b", "[REDACTED]", value)


def detect_category(text: str) -> str | None:
    normalized = text.casefold()
    if any(value in normalized for value in ("взлом", "украли аккаунт", "чужое устройство", "не мой сеанс", "войти", "логин", "заблокир", "замороз")):
        return "account"
    if any(value in normalized for value in ("уведомлен", "push", "не приходит", "не приходят")):
        return "notifications"
    if any(value in normalized for value in ("фото", "видео", "файл", "голосов", "медиа", "вложени", "загруз")):
        return "media"
    if any(value in normalized for value in ("кнопк", "инлайн", "закреп", "ответ", "пересыл", "реакци")):
        return "messages"
    if any(value in normalized for value in ("не подключ", "нет сети", "соединен", "соединён", "интернет", "задержк", "не отправля", "сообщение висит")):
        return "connection"
    return None


def main_menu() -> InlineKeyboardMarkup:
    keyboard = InlineKeyboardMarkup(row_width=2)
    keyboard.add(
        InlineKeyboardButton("Проверить отправку", callback_data="diag:start:connection"),
        InlineKeyboardButton("Нет уведомлений", callback_data="diag:start:notifications"),
        InlineKeyboardButton("Не отправляются медиа", callback_data="diag:start:media"),
        InlineKeyboardButton("Ошибка в чате", callback_data="diag:start:messages"),
        InlineKeyboardButton("Аккаунт и безопасность", callback_data="diag:start:account"),
        InlineKeyboardButton("Подготовить обращение", callback_data="diag:report"),
    )
    keyboard.row(
        InlineKeyboardButton(
            "Написать в поддержку",
            url=SUPPORT_CHAT_URL,
            bg_color="#4F46E5",
            text_color="#FFFFFF",
        )
    )
    return keyboard


def step_keyboard(category: str, step: DiagnosticStep) -> InlineKeyboardMarkup:
    keyboard = InlineKeyboardMarkup(row_width=1)
    for option in step.options:
        keyboard.row(
            InlineKeyboardButton(
                option.label,
                callback_data=f"diag:answer:{category}:{step.field}:{option.value}",
            )
        )
    keyboard.row(InlineKeyboardButton("Завершить и создать обращение", callback_data="diag:report"))
    return keyboard


def completion_keyboard() -> InlineKeyboardMarkup:
    keyboard = InlineKeyboardMarkup(row_width=1)
    keyboard.row(InlineKeyboardButton("Создать черновик обращения", callback_data="diag:report"))
    keyboard.row(InlineKeyboardButton("Начать другую проверку", callback_data="diag:menu"))
    keyboard.row(
        InlineKeyboardButton(
            "Открыть чат поддержки",
            url=SUPPORT_CHAT_URL,
            bg_color="#4F46E5",
            text_color="#FFFFFF",
        )
    )
    return keyboard


def send_message(user_id: int, text: str, reply_markup: InlineKeyboardMarkup | None = None) -> None:
    bot.send_message(
        user_id=user_id,
        text=text[:MAX_REPLY_LENGTH],
        reply_markup=reply_markup if reply_markup is not None else main_menu(),
    )


def start_diagnostic(user_id: int, category: str, preface: str | None = None) -> None:
    session = DiagnosticSession(category=category)
    diagnostic_sessions[user_id] = session
    title = CATEGORY_TITLES[category]
    first_step = FLOW_STEPS[category][0]
    text = f"Диагностика: {title}.\n\n{preface or 'Я задам несколько коротких вопросов и затем подготовлю конкретные действия.'}\n\n{first_step.question}"
    send_message(user_id, text, step_keyboard(category, first_step))


def recommendation(session: DiagnosticSession) -> str:
    answers = session.answers
    if session.category == "connection":
        if answers.get("internet") == "нет":
            return "Другие сервисы тоже не работают, поэтому сначала восстановите сеть или отключите VPN/частный DNS. Затем повторите отправку одного короткого сообщения."
        if answers.get("delivery") == "ожидание":
            return "Сеть доступна, но сообщение остаётся в ожидании. Переключите сеть, полностью закройте Vibe и откройте чат снова. Если часы остаются, приложите этот черновик обращения."
        if answers.get("delivery") == "ошибка":
            return "Ошибка отправки при рабочей сети требует проверки на стороне приложения или сервера. Не повторяйте отправку десятки раз: подготовьте черновик и добавьте время появления ошибки."
        return "Тестовое сообщение доставляется. Вероятнее всего, сбой был связан с временным состоянием сети или соединения."
    if session.category == "notifications":
        if answers.get("permission") != "разрешены":
            return "Включите уведомления для Vibe в настройках Android, затем отправьте тестовое сообщение из другого диалога."
        if answers.get("chat_muted") == "да":
            return "Включите уведомления именно для нужного чата — глобальное разрешение Android этого не заменяет."
        if answers.get("battery") == "есть":
            return "Снимите для Vibe ограничение батареи и фоновой работы. После этого перезапустите приложение и выполните тест сообщения."
        return "Базовые настройки включены. Если уведомления всё ещё отсутствуют, подготовьте обращение с версией Android и временем тестового сообщения."
    if session.category == "media":
        if answers.get("permission") != "да":
            return "Дайте Vibe доступ к выбранным фото и файлам в настройках Android, затем попробуйте вложение до 10 МБ."
        if answers.get("size") == "отправляется":
            return "Небольшое вложение отправляется: вероятная причина — размер, формат или нестабильная сеть для исходного файла."
        return "Проблема повторяется и с небольшим вложением. Нужен черновик обращения с типом файла, примерным размером и временем попытки."
    if session.category == "messages":
        if answers.get("repeatability") == "исчезла":
            return "Ошибка не повторилась после повторного открытия чата. Скорее всего, это было временное состояние интерфейса; если вернётся, создайте обращение сразу после сбоя."
        if answers.get("symptom") == "задержка":
            return "Для задержки важны сеть и время события. Выполните один тест без VPN, затем сохраните черновик обращения."
        return "Ошибка воспроизводится. Черновик обращения ниже содержит структуру, по которой разработчик сможет повторить проблему."
    if session.category == "account":
        if answers.get("account_case") == "подозрительный сеанс":
            return "Откройте «Настройки → Устройства», завершите все незнакомые сеансы. Не передавайте коды и пароли в сообщениях."
        if answers.get("access_state") == "есть доступ":
            return "Сохраните доступное устройство: проверьте «Настройки → Устройства» и завершите незнакомые сеансы."
        return "Не передавайте коды или пароли. Для восстановления используйте официальный чат поддержки и приложите безопасный черновик обращения."
    return "Подготовьте черновик обращения и опишите ожидаемый и фактический результат."


def format_report(user_id: int) -> str:
    session = diagnostic_sessions.get(user_id)
    if session is None:
        return (
            "Черновик пока пуст. Сначала выберите тип проблемы и пройдите короткую диагностику — "
            "тогда я соберу обращение без лишних персональных данных."
        )

    title = CATEGORY_TITLES[session.category]
    answer_lines = "\n".join(f"• {field}: {value}" for field, value in session.answers.items())
    if not answer_lines:
        answer_lines = "• Диагностика ещё не пройдена"
    timestamp = session.started_at.strftime("%Y-%m-%d %H:%M UTC")
    return (
        "ЧЕРНОВИК ОБРАЩЕНИЯ В ПОДДЕРЖКУ\n"
        f"Тип: {title}\n"
        f"Начато: {timestamp}\n"
        "\nПроверено:\n"
        f"{answer_lines}\n"
        "\nОписание проблемы: [добавьте, что ожидали и что произошло]\n"
        "Время последней ошибки: [добавьте время]\n"
        "Версия Vibe / Android: [добавьте без личных данных]\n"
        "\nНе добавляйте в обращение пароль, код входа, токены и платёжные данные."
    )


def finish_diagnostic(user_id: int) -> None:
    session = diagnostic_sessions[user_id]
    send_message(
        user_id,
        f"Проверка завершена.\n\nРезультат: {recommendation(session)}\n\n"
        "Я могу сразу сформировать безопасный черновик обращения в поддержку.",
        completion_keyboard(),
    )


def answer_diagnostic(user_id: int, category: str, field: str, value: str) -> None:
    session = diagnostic_sessions.get(user_id)
    if session is None or session.category != category:
        start_diagnostic(user_id, category, "Начнём проверку заново, чтобы не потерять контекст.")
        return

    steps = FLOW_STEPS[category]
    if session.step_index >= len(steps):
        finish_diagnostic(user_id)
        return

    expected_step = steps[session.step_index]
    if field != expected_step.field:
        send_message(user_id, "Этот шаг уже неактуален. Продолжим с текущего вопроса.", step_keyboard(category, expected_step))
        return

    allowed_values = {option.value for option in expected_step.options}
    if value not in allowed_values:
        send_message(user_id, "Не удалось распознать ответ. Выберите один из вариантов ниже.", step_keyboard(category, expected_step))
        return

    session.answers[field] = value
    session.step_index += 1
    if session.step_index >= len(steps):
        finish_diagnostic(user_id)
        return

    next_step = steps[session.step_index]
    title = CATEGORY_TITLES[category]
    send_message(
        user_id,
        f"Диагностика: {title}.\n\n{next_step.question}",
        step_keyboard(category, next_step),
    )


def get_ai_reply(sender_id: int, text: str) -> str:
    history = conversation_history[sender_id]
    session = diagnostic_sessions.get(sender_id)
    diagnostic_context = ""
    if session and session.answers:
        diagnostic_context = f"\nТекущая диагностика: {CATEGORY_TITLES[session.category]}; данные: {session.answers}."

    messages = [{"role": "system", "content": SUPPORT_SYSTEM_PROMPT + diagnostic_context}, *history]
    messages.append({"role": "user", "content": text})

    try:
        response = mistral.chat.complete(model=config.mistral_model, messages=messages)
        reply = normalize_text(str(response.choices[0].message.content or ""))
        if not reply:
            raise ValueError("empty model response")
    except Exception:
        logger.exception("Support model request failed for user_id=%s", sender_id)
        return (
            "Не удалось подготовить автоматический ответ. Выберите тип проблемы кнопкой ниже, "
            "пройдите короткую диагностику и получите готовый черновик обращения."
        )

    history.append({"role": "user", "content": text})
    history.append({"role": "assistant", "content": reply})
    return reply[:MAX_REPLY_LENGTH]


@bot.message_handler()
def handle_message(message) -> None:
    sender_id = message.sender_id
    raw_text = str(getattr(message, "content", "") or "")
    text = normalize_text(raw_text)

    # Content is intentionally never logged. User ID and length are enough for operations.
    logger.info("Received support request from user_id=%s, length=%s", sender_id, len(text))

    if not text or text.casefold() in {"/start", "старт", "помощь", "меню"}:
        send_message(
            sender_id,
            "Я помогу проверить проблему по шагам, а затем подготовлю аккуратный черновик обращения. Выберите сценарий ниже.",
            main_menu(),
        )
        return

    if text.casefold() in {"/report", "отчёт", "отчет", "обращение"}:
        send_message(sender_id, format_report(sender_id), completion_keyboard())
        return

    if len(text) > MAX_USER_MESSAGE_LENGTH:
        text = text[:MAX_USER_MESSAGE_LENGTH]
        logger.info("Truncated oversized support request from user_id=%s", sender_id)

    if contains_secret(text):
        send_message(
            sender_id,
            "Для безопасности не отправляйте сюда пароли, коды подтверждения, токены или ключи API. "
            "Удалите эти данные и опишите проблему без секретов.",
            main_menu(),
        )
        return

    category = detect_category(text)
    if category is not None:
        start_diagnostic(sender_id, category, "Я распознал тип проблемы по вашему сообщению.")
        return

    try:
        bot.send_chat_action(user_id=sender_id, action="typing")
    except Exception:
        logger.warning("Could not send typing action for user_id=%s", sender_id)

    send_message(sender_id, get_ai_reply(sender_id, redact_sensitive_text(text)), main_menu())


@bot.callback_query_handler()
def handle_callback(call) -> None:
    data = str(getattr(call, "data", "") or "")
    user_id = call.sender_id
    logger.info("Received support callback=%s from user_id=%s", data, user_id)

    try:
        # Always acknowledge first; the client immediately clears its pending callback state.
        call.answer(text="Принято", show_alert=False)
    except Exception:
        logger.warning("Could not acknowledge callback for user_id=%s", user_id)

    if data == "diag:menu":
        send_message(user_id, "Выберите проблему, которую нужно проверить.", main_menu())
        return

    if data == "diag:report":
        send_message(user_id, format_report(user_id), completion_keyboard())
        return

    parts = data.split(":", maxsplit=4)
    if len(parts) == 3 and parts[0] == "diag" and parts[1] == "start":
        category = parts[2]
        if category in FLOW_STEPS:
            start_diagnostic(user_id, category)
        else:
            send_message(user_id, "Этот сценарий пока недоступен. Выберите другой тип проблемы.", main_menu())
        return

    if len(parts) == 5 and parts[0] == "diag" and parts[1] == "answer":
        _, _, category, field, value = parts
        if category in FLOW_STEPS:
            answer_diagnostic(user_id, category, field, value)
        else:
            send_message(user_id, "Сценарий устарел. Выберите проблему заново.", main_menu())
        return

    send_message(user_id, "Команда больше недоступна. Выберите проблему заново.", main_menu())


if __name__ == "__main__":
    logger.info("Starting Vibe support bot with model=%s", config.mistral_model)
    bot.start_polling(skip_updates=True)
