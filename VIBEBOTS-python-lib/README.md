# VibeBots Python Library

`python/main.py` — синхронная библиотека для создания ботов VibeMessenger. Она сохраняет прежний способ подключения и добавляет безопасные помощники для клавиатур, ответов, обработчиков и polling-цикла.

```python
from python.main import Bot, InlineKeyboardButton, InlineKeyboardMarkup

bot = Bot("VIBE_BOT_TOKEN")

@bot.message_handler(commands=["start", "help"])
def start(message):
    menu = InlineKeyboardMarkup(row_width=2).add(
        InlineKeyboardButton.callback("Поддержка", "support:menu"),
        InlineKeyboardButton.link("Сайт", "https://example.com"),
    )
    bot.reply(message.sender_id, message.message_id, "Чем помочь?", reply_markup=menu)

@bot.callback_query_handler(data_startswith="support:")
def support(callback):
    callback.answer("Принято")
    callback.reply("Опишите проблему одним сообщением.")

bot.start_polling()
```

> `callback.answer()` нужно вызывать сразу после обработки нажатия. Это снимает состояние ожидания у конкретной инлайн-кнопки в клиенте Vibe.

## Добавленные методы

| Компонент | Методы | Назначение |
| --- | --- | --- |
| `DotDict` | `get_path()`, `to_dict()` | Безопасно читать вложенные поля update-объекта и получать обычный словарь. |
| `CallbackQuery` | `answer()`, `reply()`; свойства `callback_query_id`, `user_id`, `data`, `message_id` | Подтвердить нажатие и ответить пользователю без ручного извлечения ID. |
| `InlineKeyboardButton` | `callback()`, `link()`, `copy()`; свойства `is_callback`, `is_url` | Быстро создавать, копировать и проверять кнопки. |
| `InlineKeyboardMarkup` | `from_rows()`, `add()`, `row()`, `clear()`, `remove_last_row()`, `copy()`; свойства `button_count`, `is_empty` | Удобно строить и изменять клавиатуры в рамках лимитов клиента. |
| `Bot` — сообщения | `reply(chat, message, text)`, `send_messages()`, `broadcast()` | Ответить в указанном чате именно на указанное сообщение, отправить последовательность сообщений или рассылку с результатом по каждому пользователю. |
| `Bot` — статусы | `send_typing()`, `send_upload_photo()`, `send_upload_video()`, `send_upload_document()`, `send_record_voice()`, `send_record_video()` | Показывать пользователю текущий статус работы бота. |
| `Bot` — обновления | `poll_once()`, `dispatch_update()`, `stop_polling()`, `poll_forever()` | Точно контролировать один цикл polling, обработку update и штатную остановку бота. |
| `Bot` — обработчики | `message_handler(commands=…, predicate=…)`, `callback_query_handler(data=…, data_startswith=…, predicate=…)` | Отбирать команды, callback-данные, префиксы и произвольные условия до запуска обработчика. |

## Дополнительные улучшения

| Возможность | Поведение |
| --- | --- |
| Повторы временных сетевых ошибок | `Bot` повторяет ошибки соединения и тайм-ауты с экспоненциальной паузой. Параметры задаются в конструкторе: `retry_attempts`, `retry_backoff`, `timeout`. |
| Проверка входных данных | Библиотека отклоняет пустой текст, некорректный токен, неизвестный chat action, пустой текст кнопки и кнопку, у которой одновременно задано действие и URL. |
| Совместимость | Существующие вызовы `send_message`, `get_me`, `get_updates`, `send_chat_action`, `message_handler()`, `callback_query_handler()`, `callback_handler()` и `start_polling()` продолжают работать. |
| Ошибки обработчиков | По умолчанию ошибка одного обработчика не останавливает бот. Для отладки передайте `raise_handler_errors=True` в `poll_once()` или `start_polling()`. |

## Примеры

### Фильтр команд

```python
@bot.message_handler(commands="status")
def status(message):
    bot.reply(message.sender_id, message.message_id, "Бот работает.")
```

### Ответ на конкретное сообщение

Первым аргументом передаётся чат, вторым — сообщение, на которое нужно ответить, третьим — текст. Для личных чатов идентификатор чата — это ID собеседника.

```python
bot.reply(chat=42, message=105, text="Ответ будет привязан к сообщению #105")
```

### Фильтр callback по префиксу

```python
@bot.callback_query_handler(data_startswith="order:")
def order_action(callback):
    callback.answer("Заказ обновлён")
    order_id = callback.data.removeprefix("order:")
    callback.reply(f"Заказ {order_id} принят в обработку.")
```

### Ручной polling

Это полезно, когда приложение само хранит offset или объединяет VibeBots с другим циклом обработки.

```python
offset = 0
while True:
    updates, offset = bot.poll_once(offset)
    print(f"Получено обновлений: {len(updates)}")
```

### Рассылка с контролем ошибок

```python
results = bot.broadcast([101, 202, 303], "Вышло обновление приложения.")
for user_id, result in results.items():
    if isinstance(result, Exception):
        print(f"Не удалось отправить {user_id}: {result}")
```

## Проверка

Автономные тесты не используют токен и не выполняют сетевых запросов:

```bat
cd VIBEBOTS-python-lib\python
py -3 test_main.py
```

Также можно проверить только синтаксис библиотеки и действующего бота поддержки:

```bat
cd VIBEBOTS-python-lib
py -3 -m py_compile python\main.py python\test_main.py test.py
```
