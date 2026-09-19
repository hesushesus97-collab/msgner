/**
 * Vibe Bots SDK Documentation Script
 * Search filtering, copy-to-clipboard, bilingual RU/EN support, and theme sync
 */

const docsTranslations = {
    ru: {
        docs_back_home: "На главную",
        docs_breadcrumb_title: "Документация Python SDK",
        doc_nav_quickstart: "Быстрый старт",
        doc_link_first_bot: "Первый бот",
        doc_link_polling: "Polling & Обновления",
        doc_nav_messages: "Сообщения",
        doc_link_msg_handlers: "Хэндлеры сообщений",
        doc_link_commands: "Команды и фильтры",
        doc_link_sending: "Отправка сообщений",
        doc_nav_keyboards: "Инлайн-кнопки & Цвета",
        doc_link_grid: "Сетка 5x10 кнопок",
        doc_link_colors: "Кастомные HEX цвета",
        doc_link_url_btns: "URL-кнопки",
        doc_nav_callbacks: "Обработка нажатий",
        doc_link_cb_handlers: "Callback-хэндлеры",
        doc_link_alerts: "Алерты vs Тосты",
        doc_nav_security: "Безопасность",
        doc_link_rate_limits: "Rate Limit (3 клика/сек)",
        doc_nav_api: "Справочник API",
        doc_nav_new: "Новые методы",
        doc_link_new_overview: "Обзор новинок",
        doc_link_new_commands: "Роутинг команд",
        doc_link_new_edit: "Редактирование/удаление",
        doc_link_new_reactions: "Реакции и закрепления",
        doc_link_new_forward: "Пересылка и история",
        doc_link_new_single: "Один токен = один бот",
        doc_new_title: "7.5. Новые методы: редактирование, реакции, закрепления и команды",
        doc_new_intro: "Начиная с этой версии библиотека умеет не только отправлять сообщения, но и редактировать их, удалять, пересылать, ставить реакции, закреплять сообщения, читать историю переписки и удобно роутить команды. Все действия отображаются в мессенджере в реальном времени.",
        doc_single_instance: "По одному токену одновременно может работать только один экземпляр бота. Если запустить второй, первый автоматически получит ConflictError и корректно остановится. Отдельные файлы блокировок больше не создаются — синхронизация ведётся через базу данных.",
        doc_nav_examples: "Готовые примеры",
        doc_link_shop_bot: "Бот-Магазин",
        doc_link_quiz_bot: "Бот-Викторина",

        copy_btn: "Копировать",
        doc_sec1_title: "1. Быстрый старт: Создание первого бота",
        doc_sec1_p1: "Библиотека vibebots позволяет разрабатывать высокопроизводительных ботов на Python с поддержкой интерактивных инлайн-кнопок, кастомных цветов и мгновенных ответов.",
        doc_sec2_title: "2. Polling & Обработка обновлений",
        doc_sec2_p1: "Метод bot.start_polling() автоматически получает входящие события (сообщения и нажатия на кнопки) и передает их в соответствующие обработчики.",
        doc_sec3_title: "3. Обработка сообщений и фильтры",
        doc_sec3_p1: "Декоратор @bot.message_handler() позволяет перехватывать входящие сообщения с использованием различных фильтров.",
        doc_sec3_sub1: "Фильтрация по командам и тексту",
        doc_sec3_sub2: "Отправка действий в чате (Typing Indicator)",
        doc_sec3_p2: "Вы можете показать пользователю, что бот печатает ответ, перед длительной операцией:",
        doc_sec4_title: "4. Инлайн-кнопки, Сетка 5x10 и Кастомные цвета",
        doc_sec4_p1: "Vibe Messenger предоставляет богатейшие возможности кастомизации кнопок:",
        doc_palette_title: "Рекомендуемые HEX палитры для кнопок",
        doc_sec5_title: "5. Обработка нажатий и ответы (Callbacks)",
        doc_sec5_p1: "Когда пользователь нажимает на инлайн-кнопку, сервер Vibe передает событие callback_query в ваш бот. Бот может моментально ответить всплывающим тостом или модальным диалогом через метод call.answer().",
        doc_sec5_sub1: "Всплывающий тост vs Модальное окно (Alert)",
        doc_sec6_title: "6. Защита от спама (3 клика в секунду)",
        doc_sec6_p1: "На уровне WebSocket шлюза Vibe реализован автоматический Rate Limiter по скользящему окну времени (Sliding Window): максимум 3 клика в 1.0 секунду от одного пользователя.",
        doc_sec6_note_title: "Защита инфраструктуры:",
        doc_sec6_note_desc: "Если пользователь попытается спамить кнопку кликером, сервер автоматически отклонит лишние запросы и покажет предупреждение пользователю, не создавая нагрузку на ваш процесс бота.",
        doc_sec7_title: "7. Справочник API классов",
        doc_sec8_title: "8. Полные примеры ботов",
        doc_store_example_title: "Пример 1: Бот-Каталог товаров с корзиной",
        doc_quiz_example_title: "Пример 2: Интерактивная Викторина с модальными алертами",
        pager_prev: "Назад",
        pager_home: "Главная страница Vibe",
        pager_top: "Наверх",
        pager_to_top: "К началу документации ↑"
    },
    en: {
        docs_back_home: "Home",
        docs_breadcrumb_title: "Python SDK Documentation",
        doc_nav_quickstart: "Quick Start",
        doc_link_first_bot: "First Bot",
        doc_link_polling: "Polling & Updates",
        doc_nav_messages: "Messages",
        doc_link_msg_handlers: "Message Handlers",
        doc_link_commands: "Commands & Filters",
        doc_link_sending: "Sending Messages",
        doc_nav_keyboards: "Inline Buttons & Colors",
        doc_link_grid: "5x10 Button Grid",
        doc_link_colors: "Custom HEX Colors",
        doc_link_url_btns: "URL Buttons",
        doc_nav_callbacks: "Callback Handling",
        doc_link_cb_handlers: "Callback Handlers",
        doc_link_alerts: "Alerts vs Toasts",
        doc_nav_security: "Security",
        doc_link_rate_limits: "Rate Limiting (3 clicks/s)",
        doc_nav_api: "API Reference",
        doc_nav_new: "New Methods",
        doc_link_new_overview: "What's New",
        doc_link_new_commands: "Command Routing",
        doc_link_new_edit: "Edit / Delete",
        doc_link_new_reactions: "Reactions & Pins",
        doc_link_new_forward: "Forward & History",
        doc_link_new_single: "One token = one bot",
        doc_new_title: "7.5. New methods: editing, reactions, pinning and commands",
        doc_new_intro: "Starting with this version the library can not only send messages, but also edit, delete, forward and copy them, add reactions, pin messages, read chat history and route commands conveniently. All actions appear in the messenger in real time.",
        doc_single_instance: "Only one bot instance can run per token at a time. If a second one starts, the first automatically receives a ConflictError and stops cleanly. No lock files are created anymore — synchronization is done through the database.",
        doc_nav_examples: "Complete Examples",
        doc_link_shop_bot: "Store / Shop Bot",
        doc_link_quiz_bot: "Quiz / Survey Bot",

        copy_btn: "Copy",
        doc_sec1_title: "1. Quick Start: Your First Bot",
        doc_sec1_p1: "The vibebots SDK lets you build high-performance Python bots with interactive inline buttons, customizable colors, and real-time responses.",
        doc_sec2_title: "2. Polling & Updates Handling",
        doc_sec2_p1: "The bot.start_polling() method continuously listens for incoming events (messages and button clicks) and routes them to appropriate handlers.",
        doc_sec3_title: "3. Message Handlers & Filtering",
        doc_sec3_p1: "The @bot.message_handler() decorator allows intercepting incoming messages with various custom filters.",
        doc_sec3_sub1: "Command & Text Predicates",
        doc_sec3_sub2: "Chat Actions (Typing Indicator)",
        doc_sec3_p2: "Notify users that your bot is typing before performing lengthy tasks:",
        doc_sec4_title: "4. Inline Keyboards, 5x10 Grid & Custom Colors",
        doc_sec4_p1: "Vibe Messenger provides rich button styling and layout customization:",
        doc_palette_title: "Recommended Button HEX Color Palettes",
        doc_sec5_title: "5. Button Clicks & Responses (Callbacks)",
        doc_sec5_p1: "When a user taps an inline button, the Vibe server transmits a callback_query event to your bot. Respond with a toast or modal dialog using call.answer().",
        doc_sec5_sub1: "Toast Notification vs Modal Dialog (Alert)",
        doc_sec6_title: "6. Anti-Flood & Rate Limiting (3 clicks/sec)",
        doc_sec6_p1: "A sliding-window Rate Limiter operates at Vibe WebSocket gateway level: strictly max 3 button clicks per 1.0 second per user.",
        doc_sec6_note_title: "Infrastructure Protection:",
        doc_sec6_note_desc: "Spam clicks are filtered at edge level with immediate user warnings, shielding your bot process from traffic spikes.",
        doc_sec7_title: "7. API Class & Method Reference",
        doc_sec8_title: "8. Complete Working Bot Examples",
        doc_store_example_title: "Example 1: Interactive Product Store Bot with Cart",
        doc_quiz_example_title: "Example 2: Interactive Quiz Bot with Modal Alerts",
        pager_prev: "Back",
        pager_home: "Vibe Messenger Home",
        pager_top: "Top",
        pager_to_top: "Back to top ↑"
    }
};

let currentLang = localStorage.getItem('vibe_lang') || 'ru';
let currentTheme = localStorage.getItem('vibe_theme') || 'dark';

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initLanguage();
    initMobileMenu();
    initActiveNavScroll();
    if (window.Prism) {
        Prism.highlightAll();
    }
});

// Theme Management
function initTheme() {
    document.documentElement.setAttribute('data-theme', currentTheme);
    const themeBtn = document.getElementById('themeToggleBtn');
    if (themeBtn) {
        themeBtn.addEventListener('click', () => {
            currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', currentTheme);
            localStorage.setItem('vibe_theme', currentTheme);
        });
    }
}

// Language Management
function initLanguage() {
    const langBtn = document.getElementById('langSwitchBtn');
    if (langBtn) {
        langBtn.addEventListener('click', () => {
            currentLang = currentLang === 'ru' ? 'en' : 'ru';
            localStorage.setItem('vibe_lang', currentLang);
            applyTranslations(currentLang);
        });
    }
    applyTranslations(currentLang);
}

function applyTranslations(lang) {
    document.documentElement.setAttribute('lang', lang);
    const label = document.getElementById('currentLangLabel');
    if (label) label.textContent = lang.toUpperCase();

    const dict = docsTranslations[lang];
    if (!dict) return;

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) {
            el.textContent = dict[key];
        }
    });
}

// Search Filter
function filterDocs() {
    const query = document.getElementById('docsSearchInput').value.toLowerCase();
    const links = document.querySelectorAll('.sidebar-link');
    links.forEach(link => {
        const text = link.textContent.toLowerCase();
        const listItem = link.parentElement;
        if (text.includes(query)) {
            listItem.style.display = 'block';
        } else {
            listItem.style.display = 'none';
        }
    });
}

// Copy to Clipboard
function copyCodeBlock(button) {
    const codeEl = button.closest('.code-container').querySelector('pre code');
    if (!codeEl) return;

    const textToCopy = codeEl.innerText;
    navigator.clipboard.writeText(textToCopy).then(() => {
        const originalHtml = button.innerHTML;
        button.innerHTML = `<i class="fa-solid fa-check" style="color: var(--accent-emerald)"></i> ${currentLang === 'ru' ? 'Скопировано!' : 'Copied!'}`;
        setTimeout(() => {
            button.innerHTML = originalHtml;
        }, 2000);
    });
}

// Mobile Sidebar
function initMobileMenu() {
    const btn = document.getElementById('docsMobileMenuBtn');
    const sidebar = document.getElementById('docsSidebar');
    if (btn && sidebar) {
        btn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });

        sidebar.querySelectorAll('a').forEach(a => {
            a.addEventListener('click', () => {
                sidebar.classList.remove('open');
            });
        });
    }
}

// Active Nav Link On Scroll
function initActiveNavScroll() {
    const sections = document.querySelectorAll('.doc-section-block');
    const navLinks = document.querySelectorAll('.sidebar-link');

    window.addEventListener('scroll', () => {
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop - 120;
            if (window.pageYOffset >= sectionTop) {
                current = section.getAttribute('id');
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    });
}