"""VibeBots — компактная Python-библиотека для Bot API VibeMessenger.

Модуль не требует отдельного фреймворка: создайте :class:`Bot`, зарегистрируйте
обработчики и запустите :meth:`Bot.start_polling`.

Основные возможности:

* Отправка, редактирование, удаление, пересылка и копирование сообщений.
* Инлайн-кнопки с кастомными цветами и обработка нажатий (callback query).
* Реакции, закрепление и открепление сообщений от имени бота.
* Информация о пользователях, история переписки и данные о самом боте.
* Роутинг команд (``@bot.command("start")``) и текстовых фильтров.
* Устойчивый long-polling с автоматическим повтором сетевых ошибок и
  гарантией, что по одному токену одновременно работает только один экземпляр.
"""

from __future__ import annotations

import time
from threading import Event
from typing import Any, Callable, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple, Union
from urllib.parse import urlparse

import requests


ReplyMarkup = Union["InlineKeyboardMarkup", Dict[str, Any], List[Any]]
MessageHandler = Callable[["Message"], Any]
CallbackHandler = Callable[["CallbackQuery"], Any]
MessagePredicate = Callable[["Message"], bool]
CallbackPredicate = Callable[["CallbackQuery"], bool]


class BotAPIError(Exception):
    """Raised when Vibe Bot API rejects a request or the request cannot be completed."""


class ConflictError(BotAPIError):
    """Raised when another bot instance takes over polling for the same token."""


class DotDict(dict):
    """Dictionary with safe dot notation and helpers for incoming Bot API objects."""

    def __getattr__(self, item: str) -> Any:
        try:
            return self[item]
        except KeyError as exc:
            raise AttributeError(f"'{type(self).__name__}' object has no attribute '{item}'") from exc

    def __setattr__(self, key: str, value: Any) -> None:
        if key.startswith("_"):
            object.__setattr__(self, key, value)
        else:
            self[key] = value

    def __delattr__(self, item: str) -> None:
        if item.startswith("_"):
            object.__delattr__(self, item)
            return
        try:
            del self[item]
        except KeyError as exc:
            raise AttributeError(f"'{type(self).__name__}' object has no attribute '{item}'") from exc

    @classmethod
    def from_dict(cls, data: Any) -> Any:
        """Recursively convert API dictionaries to :class:`DotDict` instances."""
        if isinstance(data, Mapping):
            result = cls()
            for key, value in data.items():
                result[key] = DotDict.from_dict(value)
            return result
        if isinstance(data, list):
            return [DotDict.from_dict(item) for item in data]
        if isinstance(data, tuple):
            return tuple(DotDict.from_dict(item) for item in data)
        return data

    def get_path(self, path: str, default: Any = None, separator: str = ".") -> Any:
        """Return a nested value without raising when one of its keys is absent.

        ``message.get_path("sender_info.username")`` is equivalent to a sequence
        of guarded ``dict.get`` calls.
        """
        value: Any = self
        for part in path.split(separator):
            if isinstance(value, Mapping) and part in value:
                value = value[part]
            else:
                return default
        return value

    def to_dict(self) -> Dict[str, Any]:
        """Return a plain recursive dictionary suitable for serialisation or logging."""
        def unpack(value: Any) -> Any:
            if isinstance(value, Mapping):
                return {key: unpack(item) for key, item in value.items()}
            if isinstance(value, list):
                return [unpack(item) for item in value]
            if isinstance(value, tuple):
                return tuple(unpack(item) for item in value)
            return value

        return unpack(self)


class Message(DotDict):
    """Represents an incoming message and provides convenient shortcuts.

    A :class:`Message` behaves like a normal ``dict``/``DotDict`` (so
    ``message.content`` and ``message.sender_id`` keep working), and additionally
    exposes helpers such as :meth:`reply`, :meth:`edit`, :meth:`delete`,
    :meth:`react` and command parsing via :attr:`command` and :attr:`args`.
    """

    def __init__(self, *args: Any, **kwargs: Any) -> None:
        super().__init__(*args, **kwargs)
        self._bot: Optional[Bot] = None

    # --- Identity helpers -------------------------------------------------
    @property
    def message_id(self) -> Optional[int]:
        value = self.get("message_id", self.get("id"))
        return int(value) if value is not None else None

    @property
    def user_id(self) -> Optional[int]:
        value = self.get("sender_id", self.get("user_id"))
        return int(value) if value is not None else None

    @property
    def text(self) -> str:
        return str(self.get("content", ""))

    @property
    def reply_to_id(self) -> Optional[int]:
        value = self.get("reply_to_id")
        return int(value) if value is not None else None

    @property
    def is_bot(self) -> bool:
        return bool(self.get("is_bot"))

    # --- Command parsing --------------------------------------------------
    @property
    def command(self) -> Optional[str]:
        """Return the command name (without leading slash) or ``None``."""
        text = self.text.strip()
        if not text.startswith("/"):
            return None
        first = text.split(maxsplit=1)[0]
        return first[1:].split("@", 1)[0].casefold() or None

    @property
    def args(self) -> List[str]:
        """Return command arguments as a list of whitespace-separated tokens."""
        text = self.text.strip()
        parts = text.split()
        return parts[1:] if parts else []

    @property
    def args_text(self) -> str:
        """Return everything after the command as a single string."""
        text = self.text.strip()
        parts = text.split(maxsplit=1)
        return parts[1] if len(parts) > 1 else ""

    # --- Actions ----------------------------------------------------------
    def _require_bot(self) -> "Bot":
        if self._bot is None:
            raise BotAPIError("Bot instance is not attached to this Message")
        return self._bot

    def reply(self, text: str, reply_markup: Optional[ReplyMarkup] = None, quote: bool = False) -> Dict[str, Any]:
        """Send a message back to the sender, optionally as a reply to this message."""
        bot = self._require_bot()
        if self.user_id is None:
            raise BotAPIError("Message does not contain a sender_id")
        if quote:
            if self.message_id is None:
                raise BotAPIError("Message does not contain a message_id")
            return bot.reply(self.user_id, self.message_id, text, reply_markup=reply_markup)
        return bot.send_message(self.user_id, text, reply_markup=reply_markup)

    def answer(self, text: str, reply_markup: Optional[ReplyMarkup] = None) -> Dict[str, Any]:
        """Alias for :meth:`reply` without quoting."""
        return self.reply(text, reply_markup=reply_markup)

    def edit(self, text: str, reply_markup: Optional[ReplyMarkup] = None) -> Dict[str, Any]:
        """Edit this message (only messages sent by the bot can be edited)."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Message does not contain a message_id")
        return bot.edit_message(self.message_id, text, reply_markup=reply_markup)

    def delete(self) -> Dict[str, Any]:
        """Delete this message (only messages sent by the bot can be deleted)."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Message does not contain a message_id")
        return bot.delete_message(self.message_id)

    def react(self, emoji: str) -> Dict[str, Any]:
        """Toggle a reaction from the bot on this message."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Message does not contain a message_id")
        return bot.set_reaction(self.message_id, emoji)

    def pin(self) -> Dict[str, Any]:
        """Pin this message in the chat."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Message does not contain a message_id")
        return bot.pin_message(self.message_id)

    def forward_to(self, user_id: int) -> Dict[str, Any]:
        """Forward this message to another user."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Message does not contain a message_id")
        return bot.forward_message(user_id, self.message_id)


class CallbackQuery(DotDict):
    """Represents an incoming callback from an inline keyboard button."""

    def __init__(self, *args: Any, **kwargs: Any) -> None:
        super().__init__(*args, **kwargs)
        self._bot: Optional[Bot] = None

    @property
    def callback_query_id(self) -> Optional[Union[int, str]]:
        return self.get("callback_query_id", self.get("id"))

    @property
    def user_id(self) -> Optional[int]:
        value = self.get("sender_id", self.get("user_id"))
        return int(value) if value is not None else None

    @property
    def data(self) -> str:
        return str(self.get("data", ""))

    @property
    def message_id(self) -> Optional[int]:
        value = self.get("message_id")
        return int(value) if value is not None else None

    def _require_bot(self) -> "Bot":
        if self._bot is None:
            raise BotAPIError("Bot instance is not attached to this CallbackQuery")
        return self._bot

    def answer(self, text: Optional[str] = None, show_alert: bool = False) -> Dict[str, Any]:
        """Acknowledge this button press and release its waiting state in the client."""
        bot = self._require_bot()
        callback_query_id = self.callback_query_id
        if callback_query_id is None:
            raise BotAPIError("Callback query does not contain callback_query_id")
        return bot.answer_callback_query(
            callback_query_id=callback_query_id,
            text=text,
            show_alert=show_alert,
            user_id=self.user_id,
        )

    def reply(self, text: str, reply_markup: Optional[ReplyMarkup] = None) -> Dict[str, Any]:
        """Send a new message to the user who pressed this button."""
        bot = self._require_bot()
        if self.user_id is None:
            raise BotAPIError("Callback query does not contain sender_id")
        return bot.send_message(self.user_id, text, reply_markup=reply_markup)

    def edit_message(self, text: str, reply_markup: Optional[ReplyMarkup] = None) -> Dict[str, Any]:
        """Edit the message the button belongs to."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Callback query does not contain message_id")
        return bot.edit_message(self.message_id, text, reply_markup=reply_markup)

    def delete_message(self) -> Dict[str, Any]:
        """Delete the message the button belongs to."""
        bot = self._require_bot()
        if self.message_id is None:
            raise BotAPIError("Callback query does not contain message_id")
        return bot.delete_message(self.message_id)


class InlineKeyboardButton:
    """A button used by :class:`InlineKeyboardMarkup`.

    A button can either contain ``callback_data`` for the bot or an HTTP(S) ``url``.
    Custom colours are sent to the Vibe client unchanged.
    """

    def __init__(
        self,
        text: str,
        callback_data: Optional[Union[str, int]] = None,
        url: Optional[str] = None,
        bg_color: Optional[str] = None,
        text_color: Optional[str] = None,
    ) -> None:
        text = str(text).strip()
        if not text:
            raise ValueError("Inline keyboard button text must not be empty")
        if callback_data is not None and url is not None:
            raise ValueError("A button can have callback_data or url, but not both")
        if url is not None and not str(url).startswith(("https://", "http://")):
            raise ValueError("Inline keyboard URL must start with http:// or https://")

        self.text = text
        self.callback_data = callback_data
        self.url = url
        self.bg_color = bg_color
        self.text_color = text_color

    @classmethod
    def callback(
        cls,
        text: str,
        data: Union[str, int],
        **style: Optional[str],
    ) -> "InlineKeyboardButton":
        """Create a callback button with concise syntax."""
        return cls(text=text, callback_data=data, **style)

    @classmethod
    def link(cls, text: str, url: str, **style: Optional[str]) -> "InlineKeyboardButton":
        """Create a URL button with concise syntax."""
        return cls(text=text, url=url, **style)

    @property
    def is_callback(self) -> bool:
        return self.callback_data is not None

    @property
    def is_url(self) -> bool:
        return self.url is not None

    def copy(self, **changes: Any) -> "InlineKeyboardButton":
        """Return a button copy with selected fields replaced."""
        values = {
            "text": self.text,
            "callback_data": self.callback_data,
            "url": self.url,
            "bg_color": self.bg_color,
            "text_color": self.text_color,
        }
        values.update(changes)
        return InlineKeyboardButton(**values)

    def to_dict(self) -> Dict[str, Any]:
        result: Dict[str, Any] = {"text": self.text}
        if self.callback_data is not None:
            result["callback_data"] = str(self.callback_data)
        if self.url is not None:
            result["url"] = str(self.url)
        if self.bg_color is not None:
            result["bg_color"] = str(self.bg_color)
        if self.text_color is not None:
            result["text_color"] = str(self.text_color)
        return result


class InlineKeyboardMarkup:
    """Builder for inline keyboards accepted by the Vibe client.

    A keyboard supports no more than five buttons in a row and ten rows in total.
    Additional buttons are intentionally ignored to keep messages valid on every
    supported client.
    """

    MAX_BUTTONS_PER_ROW = 5
    MAX_ROWS = 10

    def __init__(
        self,
        inline_keyboard: Optional[Sequence[Sequence[Union[InlineKeyboardButton, Mapping[str, Any]]]]] = None,
        row_width: int = 5,
    ) -> None:
        self.row_width = min(max(1, int(row_width)), self.MAX_BUTTONS_PER_ROW)
        self.keyboard: List[List[Union[InlineKeyboardButton, Mapping[str, Any]]]] = []
        if inline_keyboard:
            for row in inline_keyboard[: self.MAX_ROWS]:
                self.row(*row)

    @classmethod
    def from_rows(
        cls,
        *rows: Sequence[Union[InlineKeyboardButton, Mapping[str, Any]]],
    ) -> "InlineKeyboardMarkup":
        """Create a keyboard from explicitly defined rows."""
        return cls(inline_keyboard=rows)

    def add(self, *buttons: Union[InlineKeyboardButton, Mapping[str, Any]]) -> "InlineKeyboardMarkup":
        """Add buttons, splitting them into rows of the configured width."""
        for index in range(0, len(buttons), self.row_width):
            if len(self.keyboard) >= self.MAX_ROWS:
                break
            self.row(*buttons[index : index + self.row_width])
        return self

    def row(self, *buttons: Union[InlineKeyboardButton, Mapping[str, Any]]) -> "InlineKeyboardMarkup":
        """Add one row of at most five buttons."""
        if buttons and len(self.keyboard) < self.MAX_ROWS:
            self.keyboard.append(list(buttons[: self.MAX_BUTTONS_PER_ROW]))
        return self

    def clear(self) -> "InlineKeyboardMarkup":
        """Remove all rows and return this keyboard for fluent construction."""
        self.keyboard.clear()
        return self

    def remove_last_row(self) -> "InlineKeyboardMarkup":
        """Remove the last row when it exists."""
        if self.keyboard:
            self.keyboard.pop()
        return self

    def copy(self) -> "InlineKeyboardMarkup":
        """Create an independent keyboard with the same serialised buttons."""
        return InlineKeyboardMarkup(
            inline_keyboard=[[dict(button) for button in row] for row in self.to_dict()["inline_keyboard"]],
            row_width=self.row_width,
        )

    @property
    def button_count(self) -> int:
        return sum(len(row) for row in self.keyboard)

    @property
    def is_empty(self) -> bool:
        return not self.keyboard

    def __len__(self) -> int:
        return len(self.keyboard)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "inline_keyboard": [
                [button.to_dict() if isinstance(button, InlineKeyboardButton) else dict(button) for button in row]
                for row in self.keyboard
            ]
        }


class Bot:
    """Synchronous Vibe Bot API client with handler registration and polling support."""

    DEFAULT_API_URL = "https://flasskdev.alwaysdata.net/api/v1/"
    VALID_CHAT_ACTIONS = frozenset({
        "typing",
        "upload_photo",
        "upload_video",
        "upload_document",
        "record_voice",
        "record_video",
    })

    def __init__(
        self,
        token: str,
        api_url: str = DEFAULT_API_URL,
        *,
        timeout: Union[int, float] = 35,
        retry_attempts: int = 2,
        retry_backoff: Union[int, float] = 0.75,
        session: Optional[requests.Session] = None,
    ) -> None:
        token = str(token).strip()
        if len(token) < 10:
            raise ValueError("Bot token is invalid or too short")
        if timeout <= 0:
            raise ValueError("timeout must be greater than zero")
        if retry_attempts < 0:
            raise ValueError("retry_attempts cannot be negative")

        self.token = token
        self.api_url = str(api_url).rstrip("/")
        parsed_api_url = urlparse(self.api_url)
        if parsed_api_url.scheme != "https" and parsed_api_url.hostname not in ("localhost", "127.0.0.1", "::1"):
            raise ValueError(
                "api_url must use https:// — the bot token is sent as part of the URL and would be "
                "exposed in clear text over http (plain http is allowed only for localhost)"
            )
        self.timeout = float(timeout)
        self.retry_attempts = int(retry_attempts)
        self.retry_backoff = float(retry_backoff)
        self._session = session or requests.Session()
        self._owns_session = session is None
        self._message_handlers: List[Tuple[MessageHandler, Optional[MessagePredicate]]] = []
        self._callback_handlers: List[Tuple[CallbackHandler, Optional[CallbackPredicate]]] = []
        self._polling_stopped = Event()

    # ------------------------------------------------------------------
    # Lifecycle / resource management
    # ------------------------------------------------------------------
    def close(self) -> None:
        """Close the underlying HTTP session if it is owned by this bot."""
        if self._owns_session:
            self._session.close()

    def __enter__(self) -> "Bot":
        return self

    def __exit__(self, *exc: Any) -> None:
        self.close()

    def _redact(self, text: str) -> str:
        """Replace the bot token with a placeholder so it never reaches logs or error messages."""
        return text.replace(self.token, "***TOKEN***") if self.token else text

    def _request(self, method: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Execute a Bot API request with bounded retries for temporary network errors."""
        method = str(method).strip().strip("/")
        if not method:
            raise ValueError("API method must not be empty")

        url = f"{self.api_url}/{self.token}/{method}"
        last_error: Optional[Exception] = None
        for attempt in range(self.retry_attempts + 1):
            try:
                response = self._session.post(url, json=params or {}, timeout=self.timeout)
                response.raise_for_status()
                data = response.json()
                if not isinstance(data, Mapping):
                    raise BotAPIError("API returned an invalid JSON response")
                if not data.get("ok"):
                    description = str(data.get("description", "Unknown API error"))
                    if "Conflict" in description:
                        raise ConflictError(description)
                    raise BotAPIError(description)
                return dict(data)
            except BotAPIError:
                raise
            except (requests.ConnectionError, requests.Timeout) as exc:
                last_error = exc
                if attempt < self.retry_attempts:
                    time.sleep(self.retry_backoff * (2 ** attempt))
                    continue
                break
            except requests.RequestException as exc:
                # ``requests`` exceptions embed the full request URL, which contains the bot
                # token. Never propagate them (or their message) unredacted — ``from None``
                # also keeps the raw exception out of the chained traceback.
                raise BotAPIError(
                    f"Network error ({type(exc).__name__}): {self._redact(str(exc))}"
                ) from None
            except ValueError as exc:
                raise BotAPIError("API returned malformed JSON") from exc

        raise BotAPIError(
            f"Network error after {self.retry_attempts + 1} attempt(s) "
            f"({type(last_error).__name__}): {self._redact(str(last_error))}"
        )

    @staticmethod
    def _serialize_reply_markup(reply_markup: Optional[ReplyMarkup]) -> Optional[Union[Dict[str, Any], List[Any]]]:
        if reply_markup is None:
            return None
        if hasattr(reply_markup, "to_dict"):
            return reply_markup.to_dict()  # type: ignore[no-any-return]
        if isinstance(reply_markup, (dict, list)):
            return reply_markup
        raise TypeError("reply_markup must be InlineKeyboardMarkup, dict, list, or None")

    @staticmethod
    def _extract_chat_id(chat: Union[int, Mapping[str, Any], DotDict]) -> int:
        """Extract the direct-chat recipient ID from an ID or a chat-like object."""
        if isinstance(chat, int):
            return chat
        for key in ("chat_id", "id", "user_id", "receiver_id", "sender_id"):
            if chat.get(key) is not None:
                return int(chat[key])
        raise ValueError("Chat object does not contain chat_id, id, user_id, receiver_id, or sender_id")

    @staticmethod
    def _extract_message_id(message: Union[int, Mapping[str, Any], DotDict]) -> int:
        if isinstance(message, int):
            return message
        for key in ("message_id", "id"):
            if message.get(key) is not None:
                return int(message[key])
        raise ValueError("Message object does not contain message_id or id")

    # ------------------------------------------------------------------
    # Sending messages
    # ------------------------------------------------------------------
    def send_message(
        self,
        user_id: int,
        text: str,
        reply_markup: Optional[ReplyMarkup] = None,
        *,
        reply_to_id: Optional[int] = None,
    ) -> DotDict:
        """Send one text message, optionally with an inline keyboard or as a reply."""
        text = str(text)
        if not text.strip():
            raise ValueError("Message text must not be empty")
        params: Dict[str, Any] = {"user_id": int(user_id), "text": text}
        markup = self._serialize_reply_markup(reply_markup)
        if markup is not None:
            params["reply_markup"] = markup
        if reply_to_id is not None:
            params["reply_to_id"] = int(reply_to_id)
        response = self._request("send_message", params)
        return DotDict.from_dict(response.get("result", response))

    def reply(
        self,
        chat: Union[int, Mapping[str, Any], DotDict],
        message: Union[int, Mapping[str, Any], DotDict],
        text: str,
        reply_markup: Optional[ReplyMarkup] = None,
    ) -> DotDict:
        """Reply to ``message`` in ``chat``.

        The first argument identifies the direct chat (normally the other user's
        ID), and the second identifies the original message. Both arguments may
        be integer IDs or API objects containing the corresponding identifiers.
        """
        return self.send_message(
            self._extract_chat_id(chat),
            text,
            reply_markup=reply_markup,
            reply_to_id=self._extract_message_id(message),
        )

    def send_messages(
        self,
        user_id: int,
        texts: Iterable[str],
        reply_markup: Optional[ReplyMarkup] = None,
    ) -> List[DotDict]:
        """Send several messages in order and return every API response."""
        return [self.send_message(user_id, text, reply_markup=reply_markup) for text in texts]

    def broadcast(
        self,
        user_ids: Iterable[int],
        text: str,
        reply_markup: Optional[ReplyMarkup] = None,
        *,
        continue_on_error: bool = True,
    ) -> Dict[int, Union[Dict[str, Any], BotAPIError]]:
        """Send a message to many users, preserving per-user success or failure results."""
        results: Dict[int, Union[Dict[str, Any], BotAPIError]] = {}
        for raw_user_id in user_ids:
            user_id = int(raw_user_id)
            try:
                results[user_id] = self.send_message(user_id, text, reply_markup=reply_markup)
            except BotAPIError as exc:
                results[user_id] = exc
                if not continue_on_error:
                    raise
        return results

    def edit_message(
        self,
        message_id: int,
        text: str,
        reply_markup: Optional[ReplyMarkup] = None,
    ) -> DotDict:
        """Edit the text and/or keyboard of a message previously sent by the bot."""
        text = str(text)
        if not text.strip():
            raise ValueError("Message text must not be empty")
        params: Dict[str, Any] = {"message_id": int(message_id), "text": text}
        markup = self._serialize_reply_markup(reply_markup)
        if markup is not None:
            params["reply_markup"] = markup
        response = self._request("edit_message", params)
        return DotDict.from_dict(response.get("result", response))

    def edit_message_reply_markup(
        self,
        message_id: int,
        reply_markup: Optional[ReplyMarkup],
    ) -> DotDict:
        """Replace only the inline keyboard of a message sent by the bot."""
        params: Dict[str, Any] = {"message_id": int(message_id)}
        params["reply_markup"] = self._serialize_reply_markup(reply_markup) or {"inline_keyboard": []}
        response = self._request("edit_message_reply_markup", params)
        return DotDict.from_dict(response.get("result", response))

    def delete_message(self, message_id: int) -> Dict[str, Any]:
        """Delete a single message sent by the bot."""
        return self.delete_messages([int(message_id)])

    def delete_messages(self, message_ids: Sequence[int]) -> Dict[str, Any]:
        """Delete multiple messages sent by the bot."""
        ids = [int(mid) for mid in message_ids]
        if not ids:
            raise ValueError("message_ids must not be empty")
        return self._request("delete_message", {"message_ids": ids})

    def forward_message(self, user_id: int, from_message_id: int) -> DotDict:
        """Forward an existing message to a user, keeping the forwarded marker."""
        response = self._request(
            "forward_message",
            {"user_id": int(user_id), "from_message_id": int(from_message_id)},
        )
        return DotDict.from_dict(response.get("result", response))

    def copy_message(
        self,
        user_id: int,
        from_message_id: int,
        reply_markup: Optional[ReplyMarkup] = None,
    ) -> DotDict:
        """Copy the content of a message to a user without the forwarded marker."""
        params: Dict[str, Any] = {"user_id": int(user_id), "from_message_id": int(from_message_id)}
        markup = self._serialize_reply_markup(reply_markup)
        if markup is not None:
            params["reply_markup"] = markup
        response = self._request("copy_message", params)
        return DotDict.from_dict(response.get("result", response))

    # ------------------------------------------------------------------
    # Reactions & pins
    # ------------------------------------------------------------------
    def set_reaction(self, message_id: int, emoji: str) -> Dict[str, Any]:
        """Toggle a reaction from the bot on a message."""
        emoji = str(emoji).strip()
        if not emoji:
            raise ValueError("emoji must not be empty")
        return self._request("set_reaction", {"message_id": int(message_id), "emoji": emoji})

    def remove_reaction(self, message_id: int) -> Dict[str, Any]:
        """Remove the bot's reaction from a message."""
        return self._request("set_reaction", {"message_id": int(message_id), "emoji": ""})

    def pin_message(self, message_id: int, *, both: bool = True) -> Dict[str, Any]:
        """Pin a message in the chat. ``both`` pins it for the bot and the user."""
        return self._request("pin_message", {"message_id": int(message_id), "for_both": bool(both)})

    def unpin_message(self, message_id: int, *, both: bool = True) -> Dict[str, Any]:
        """Unpin a previously pinned message."""
        return self._request("unpin_message", {"message_id": int(message_id), "for_both": bool(both)})

    def unpin_all_messages(self, user_id: int) -> Dict[str, Any]:
        """Unpin every message in the chat with the given user."""
        return self._request("unpin_all_messages", {"user_id": int(user_id)})

    # ------------------------------------------------------------------
    # Chat actions
    # ------------------------------------------------------------------
    def answer_callback_query(
        self,
        callback_query_id: Union[int, str],
        text: Optional[str] = None,
        show_alert: bool = False,
        user_id: Optional[int] = None,
    ) -> Dict[str, Any]:
        """Acknowledge a callback query and release the client-side waiting state."""
        params: Dict[str, Any] = {
            "callback_query_id": callback_query_id,
            "show_alert": bool(show_alert),
        }
        if text is not None:
            params["text"] = str(text)
        if user_id is not None:
            params["user_id"] = int(user_id)
        return self._request("answer_callback_query", params)

    def send_chat_action(self, user_id: int, action: str = "typing") -> Dict[str, Any]:
        """Send a transient chat action such as ``typing`` or ``upload_video``."""
        action = str(action).strip()
        if action not in self.VALID_CHAT_ACTIONS:
            allowed = ", ".join(sorted(self.VALID_CHAT_ACTIONS))
            raise ValueError(f"Unsupported chat action '{action}'. Allowed actions: {allowed}")
        return self._request("send_chat_action", {"user_id": int(user_id), "action": action})

    def send_typing(self, user_id: int) -> Dict[str, Any]:
        """Shortcut for :meth:`send_chat_action` with the ``typing`` action."""
        return self.send_chat_action(user_id, "typing")

    def send_upload_photo(self, user_id: int) -> Dict[str, Any]:
        """Tell the user that the bot is preparing a photo."""
        return self.send_chat_action(user_id, "upload_photo")

    def send_upload_video(self, user_id: int) -> Dict[str, Any]:
        """Tell the user that the bot is preparing a video."""
        return self.send_chat_action(user_id, "upload_video")

    def send_upload_document(self, user_id: int) -> Dict[str, Any]:
        """Tell the user that the bot is preparing a document."""
        return self.send_chat_action(user_id, "upload_document")

    def send_record_voice(self, user_id: int) -> Dict[str, Any]:
        """Tell the user that the bot is recording a voice message."""
        return self.send_chat_action(user_id, "record_voice")

    def send_record_video(self, user_id: int) -> Dict[str, Any]:
        """Tell the user that the bot is recording a video message."""
        return self.send_chat_action(user_id, "record_video")

    # ------------------------------------------------------------------
    # Getters & bot profile
    # ------------------------------------------------------------------
    def get_me(self) -> DotDict:
        """Return basic information about the authorised bot."""
        response = self._request("get_me")
        return DotDict.from_dict(response.get("result", response))

    def get_user(self, user_id: int) -> DotDict:
        """Return public profile information for a user."""
        response = self._request("get_user", {"user_id": int(user_id)})
        return DotDict.from_dict(response.get("result", response))

    def get_message(self, message_id: int) -> DotDict:
        """Return a single message by id (must involve the bot's chat)."""
        response = self._request("get_message", {"message_id": int(message_id)})
        return DotDict.from_dict(response.get("result", response))

    def get_chat_history(
        self,
        user_id: int,
        *,
        limit: int = 50,
        offset: int = 0,
    ) -> List[DotDict]:
        """Return recent messages exchanged between the bot and a user."""
        response = self._request(
            "get_chat_history",
            {"user_id": int(user_id), "limit": int(limit), "offset": int(offset)},
        )
        return [DotDict.from_dict(item) for item in response.get("result", [])]

    def set_my_name(self, name: str) -> Dict[str, Any]:
        """Update the bot's display name."""
        return self._request("set_my_name", {"name": str(name)})

    def set_my_description(self, description: str) -> Dict[str, Any]:
        """Update the bot's short description (``about``)."""
        return self._request("set_my_description", {"description": str(description)})

    # ------------------------------------------------------------------
    # Updates & polling
    # ------------------------------------------------------------------
    def get_updates(self, offset: Optional[int] = None, skip_updates: bool = False) -> Dict[str, Any]:
        """Receive pending message and callback updates from the Bot API."""
        params: Dict[str, Any] = {}
        if offset is not None:
            params["offset"] = int(offset)
        if skip_updates:
            params["skip_updates"] = True
        return self._request("get_updates", params)

    def poll_once(
        self,
        offset: Optional[int] = None,
        *,
        skip_updates: bool = False,
        dispatch: bool = True,
        raise_handler_errors: bool = False,
    ) -> Tuple[List[DotDict], int]:
        """Perform one long-poll request and optionally dispatch its updates.

        The returned offset can be saved by an application that wants to own the
        polling loop instead of using :meth:`start_polling`.
        """
        response = self.get_updates(offset=offset, skip_updates=skip_updates)
        updates = [DotDict.from_dict(update) for update in response.get("result", [])]
        # The API's offset is the ID of the last successfully consumed update,
        # not the ID after it. Advancing to ``id + 1`` skipped the next update
        # whenever the server inserted a new row between two polling requests.
        next_offset = int(response.get("new_offset", offset or 0))
        for update in updates:
            if dispatch:
                self.dispatch_update(update, raise_handler_errors=raise_handler_errors)
            update_id = update.get("id")
            if update_id is not None:
                next_offset = max(next_offset, int(update_id))
        return updates, next_offset

    def message_handler(
        self,
        func: Optional[MessageHandler] = None,
        *,
        commands: Optional[Union[str, Sequence[str]]] = None,
        command: Optional[Union[str, Sequence[str]]] = None,
        predicate: Optional[MessagePredicate] = None,
    ) -> Union[MessageHandler, Callable[[MessageHandler], MessageHandler]]:
        """Register a message handler, optionally limited to commands or a predicate.

        Both ``command`` and ``commands`` are accepted (they are merged), so you can
        write ``@bot.message_handler(command="start")`` or
        ``@bot.message_handler(commands=["start", "help"])``.

        Examples::

            @bot.message_handler(commands=["start", "help"])
            def show_menu(message):
                ...

            @bot.message_handler(predicate=lambda message: message.sender_id == 42)
            def trusted_message(message):
                ...
        """
        raw_commands: List[str] = []
        for source in (commands, command):
            if source is None:
                continue
            if isinstance(source, str):
                raw_commands.append(source)
            else:
                raw_commands.extend(str(item) for item in source)
        command_set = {c.lstrip("/").casefold() for c in raw_commands} if raw_commands else None

        def decorator(handler: MessageHandler) -> MessageHandler:
            def matches(message: Message) -> bool:
                if command_set is not None:
                    text = str(message.get("content", "")).strip()
                    cmd = text.split(maxsplit=1)[0].lstrip("/").split("@", 1)[0].casefold() if text else ""
                    if cmd not in command_set:
                        return False
                return predicate(message) if predicate is not None else True

            self._message_handlers.append((handler, matches))
            return handler

        return decorator(func) if func is not None else decorator

    def command(
        self,
        name: Union[str, Sequence[str]],
        *,
        predicate: Optional[MessagePredicate] = None,
    ) -> Callable[[MessageHandler], MessageHandler]:
        """Concise decorator to register a command handler: ``@bot.command("start")``."""
        return self.message_handler(commands=name, predicate=predicate)

    def callback_query_handler(
        self,
        func: Optional[CallbackHandler] = None,
        *,
        data: Optional[Union[str, Sequence[str]]] = None,
        data_startswith: Optional[str] = None,
        predicate: Optional[CallbackPredicate] = None,
    ) -> Union[CallbackHandler, Callable[[CallbackHandler], CallbackHandler]]:
        """Register a callback handler with optional data, prefix, or custom filters."""
        if isinstance(data, str):
            accepted_data = {data}
        elif data is None:
            accepted_data = None
        else:
            accepted_data = {str(item) for item in data}

        def decorator(handler: CallbackHandler) -> CallbackHandler:
            def matches(callback: CallbackQuery) -> bool:
                if accepted_data is not None and callback.data not in accepted_data:
                    return False
                if data_startswith is not None and not callback.data.startswith(data_startswith):
                    return False
                return predicate(callback) if predicate is not None else True

            self._callback_handlers.append((handler, matches))
            return handler

        return decorator(func) if func is not None else decorator

    def callback_handler(
        self,
        func: Optional[CallbackHandler] = None,
        **kwargs: Any,
    ) -> Union[CallbackHandler, Callable[[CallbackHandler], CallbackHandler]]:
        """Backward-compatible alias for :meth:`callback_query_handler`."""
        return self.callback_query_handler(func, **kwargs)

    @staticmethod
    def _is_callback_update(update: Mapping[str, Any]) -> bool:
        update_type = update.get("type") or update.get("update_type")
        return bool(
            update_type == "callback_query"
            or "callback_query_id" in update
            or ("data" in update and "content" not in update)
        )

    def dispatch_update(self, update: Union[Mapping[str, Any], DotDict], *, raise_handler_errors: bool = False) -> int:
        """Dispatch one raw update to matching handlers and return handler count."""
        raw_update = DotDict.from_dict(update)
        handled = 0
        if self._is_callback_update(raw_update):
            callback = CallbackQuery(raw_update)
            callback._bot = self
            for handler, predicate in self._callback_handlers:
                if predicate is not None and not predicate(callback):
                    continue
                try:
                    handler(callback)
                    handled += 1
                except Exception as exc:
                    if raise_handler_errors:
                        raise
                    print(f"Callback handler error: {exc}", flush=True)
            return handled

        message = Message(raw_update)
        message._bot = self
        for handler, predicate in self._message_handlers:
            if predicate is not None and not predicate(message):
                continue
            try:
                handler(message)
                handled += 1
            except Exception as exc:
                if raise_handler_errors:
                    raise
                print(f"Message handler error: {exc}", flush=True)
        return handled

    def stop_polling(self) -> None:
        """Request a clean stop after the currently active long-poll finishes."""
        self._polling_stopped.set()

    def start_polling(
        self,
        skip_updates: bool = True,
        *,
        poll_interval: Union[int, float] = 0.0,
        error_backoff: Union[int, float] = 2.0,
        raise_handler_errors: bool = False,
        stop_on_conflict: bool = True,
    ) -> None:
        """Run the long-poll loop until :meth:`stop_polling` is called or a conflict occurs.

        Only one bot instance can poll a given token at a time. When another
        instance starts, the current one receives a :class:`ConflictError` and,
        by default, stops cleanly.
        """
        if poll_interval < 0 or error_backoff < 0:
            raise ValueError("poll_interval and error_backoff cannot be negative")

        offset = 0
        first_request = skip_updates
        self._polling_stopped.clear()
        print("Bot started polling...")
        while not self._polling_stopped.is_set():
            try:
                _, offset = self.poll_once(
                    offset=offset,
                    skip_updates=first_request,
                    dispatch=True,
                    raise_handler_errors=raise_handler_errors,
                )
                first_request = False
                if poll_interval:
                    self._polling_stopped.wait(float(poll_interval))
            except ConflictError as exc:
                print(f"Polling stopped: {exc}")
                if stop_on_conflict:
                    break
                self._polling_stopped.wait(float(error_backoff))
            except BotAPIError as exc:
                print(f"API error: {exc}")
                self._polling_stopped.wait(float(error_backoff))
            except Exception as exc:
                if raise_handler_errors:
                    raise
                print(f"Polling error: {exc}")
                self._polling_stopped.wait(float(error_backoff))

    def poll_forever(self, *args: Any, **kwargs: Any) -> None:
        """Alias for :meth:`start_polling`."""
        self.start_polling(*args, **kwargs)