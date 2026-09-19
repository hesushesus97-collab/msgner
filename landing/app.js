/**
 * Vibe Messenger Landing Page Application Script
 * Full Bilingual Support (RU/EN), Interactive Mockup & Docs Tabs
 */

// ==========================================================================
// 1. DICTIONARY & LOCALIZATION (RU / EN)
// ==========================================================================
const translations = {
    ru: {
        // Navigation
        nav_features: "Возможности",
        nav_bots: "Боты & SDK",
        nav_docs: "Документация",
        nav_security: "Безопасность",
        nav_faq: "FAQ",
        nav_get_app: "Скачать",

        // Hero
        hero_badge: "Новое поколение связи & SDK ботов",
        hero_title_1: "Общение без границ. ",
        hero_title_2: "Свобода для ботов.",
        hero_subtitle: "Ультрабыстрый мессенджер с молниеносной передачей сообщений через WebSocket, современным дизайном Glassmorphism и открытой платформой для создания мощных ботов с кастомизируемыми инлайн-кнопками.",
        btn_download_sub: "Для Android",
        btn_download_apk: "Скачать APK",
        btn_sdk_sub: "Разработчикам",
        btn_sdk_title: "Python SDK",
        stat_latency: "Latency сообщений",
        stat_rate_limit: "Защита от флуда",
        stat_design: "Стеклянный дизайн",

        // Mockup
        mockup_bot_status: "бот • в сети",
        mockup_today: "Сегодня",
        mockup_bot_msg: "Привет! Добро пожаловать в Vibe Messenger. Попробуйте наши новые интерактивные инлайн-кнопки прямо сейчас:",
        mockup_btn_features: "Возможности",
        mockup_btn_docs: "Документация",
        mockup_btn_alert: "Показать Alert",
        mockup_alert_title: "Сообщение от бота",

        // Features
        feat_pill: "Технологии будущего",
        feat_title: "Почему выбирают Vibe",
        feat_subtitle: "Все, что нужно для комфортного, безопасного и технологичного общения.",
        f1_title: "WebSocket в реальном времени",
        f1_desc: "Мгновенная доставка сообщений, статусов прочтения и индикаторов набора текста без задержек.",
        f2_title: "Инновационный Glassmorphism",
        f2_desc: "Утонченный стеклянный интерфейс с физикой отклика, плавными градиентами и адаптивной темой.",
        f3_title: "Мощная платформа ботов",
        f3_desc: "Поддержка инлайн-кнопки (до 5 в строке, до 10 строк), кастомных HEX цветов фона и текста, callback-хэндлеров.",
        f4_title: "Защита от флуда и спама",
        f4_desc: "Встроенный rate limiter на уровне API (3 клика/сек) и гибкие настройки приватности аккаунта.",
        f5_title: "Встроенный плеер и аудио",
        f5_desc: "Глобальный мини-плеер, плейлисты чата, прослушивание голосовых и музыкальных треков в фоне.",
        f6_title: "Оффлайн кэш и Room DB",
        f6_desc: "Локальное хранилище данных и надежная синхронизация истории сообщений при подключении к сети.",

        // Bots Banner
        bots_pill: "Vibe Bot Platform",
        bots_title: "Создавайте ботов нового поколения",
        bots_desc: "Наша Python-библиотека vibebots предоставляет простой и мощный инструментарий для взаимодействия с пользователями: команды, inline-клавиатуры с индивидуальными цветами и моментальная обработка нажатий в реальном времени.",
        bots_btn_docs: "Открыть документацию SDK",

        // Documentation Tabs & Headers
        docs_pill: "Документация разработчика",
        docs_title: "Vibe Bots Python SDK",
        docs_subtitle: "Полное руководство по созданию и развертыванию ботов для Vibe Messenger.",
        doc_menu_getting_started: "Начало работы",
        doc_tab_install: "Установка и настройка",
        doc_tab_quickstart: "Быстрый старт",
        doc_menu_features: "Функционал SDK",
        doc_tab_messages: "Обработка сообщений",
        doc_tab_keyboards: "Инлайн-кнопки и цвета",
        doc_tab_callbacks: "Обработка нажатий (Callbacks)",
        doc_tab_ratelimit: "Защита от флуда (Rate Limits)",
        doc_menu_api: "API Справочник",
        doc_tab_api: "Справочник классов и методов",
        copy_btn: "Копировать",

        doc_install_title: "1. Установка и структура проекта",
        doc_install_desc: "Библиотека написана на чистом Python 3.8+ и использует стандартный модуль requests для обращения к Bot API.",
        doc_quickstart_title: "2. Быстрый старт: Первый бот",
        doc_quickstart_desc: "Инициализируйте объект Bot с полученным токеном и запустите опрос обновлений через метод start_polling().",
        doc_messages_title: "3. Обработка входящих сообщений",
        doc_messages_desc: "Декоратор @bot.message_handler(...) поддерживает фильтрацию по командам или пользовательским лямбда-функциям.",
        doc_kb_title: "4. Инлайн-кнопки и кастомные цвета (Glassmorphism)",
        doc_kb_desc: "Vibe Messenger поддерживает до 5 кнопок в одной строке и до 10 строк суммарно. Каждая кнопка может иметь свой индивидуальный цвет фона bg_color и цвет текста text_color (в формате HEX).",
        doc_callbacks_title: "5. Обработка нажатий на кнопки (Callback Query)",
        doc_callbacks_desc: "При нажатии на кнопку бот получает объект CallbackQuery. Вы можете отправить всплывающее уведомление или модальный диалог через call.answer().",
        doc_ratelimit_title: "6. Защита от спама и флуда",
        doc_ratelimit_desc: "На уровне WebSocket / API действует автоматическая защита: максимум 3 нажатия в секунду на одного пользователя. Все избыточные клики отсекаются сервером и не нагружают вашего бота.",
        doc_ratelimit_note_title: "Встроенная защита:",
        doc_ratelimit_note_desc: "Вам не нужно писать собственные ограничители кликов в боте — сервер Vibe гарантирует чистоту входящего потока данных.",
        doc_api_title: "7. Справочник API классов",

        // Security & Legal
        sec_pill: "Конфиденциальность на первом месте",
        sec_title: "Ваши данные принадлежат только вам",
        sec_desc: "Vibe разработан с учетом строжайших стандартов приватности. Мы не продаем ваши данные третьим лицам, не используем ваши сообщения для обучения рекламы и предоставляем полный контроль над черными списками, сессиями устройств и настройками видимости.",
        btn_privacy_policy: "Политика конфиденциальности",
        btn_terms: "Пользовательское соглашение",

        // Download
        dl_title: "Начните общаться в Vibe прямо сейчас",
        dl_subtitle: "Скачайте приложение для Android или подключитесь через Web-версию.",
        dl_direct: "Прямая загрузка",
        dl_source: "Open Source",

        // FAQ
        faq_pill: "Вопросы и ответы",
        faq_title: "Часто задаваемые вопросы",
        faq_q1: "Как создать своего бота в Vibe?",
        faq_a1: "Для создания бота зарегистрируйте его через сервисного бота в мессенджере, получите токен и используйте нашу Python-библиотеку `vibebots` для написания логики.",
        faq_q2: "Сколько инлайн-кнопок можно добавить в сообщение?",
        faq_a2: "Вы можете расположить до 5 кнопок в одном ряду и до 10 рядов суммарно (максимум 50 кнопок на одно сообщение). Каждой кнопке можно задать уникальный цвет фона и текста.",
        faq_q3: "Есть ли ограничение на количество кликов по кнопкам?",
        faq_a3: "Да, для защиты ботов от спама и перегрузки на уровне API действует ограничение: максимум 3 нажатия в секунду от одного пользователя.",
        faq_q4: "Бесплатен ли мессенджер Vibe?",
        faq_a4: "Да, Vibe Messenger и платформа ботов полностью бесплатны для всех пользователей и разработчиков.",

        // Footer & Modals
        footer_slogan: "Инновационный мессенджер с открытой платформой ботов.",
        footer_nav: "Навигация",
        footer_legal: "Правовая информация",
        footer_rights: "Все права защищены.",
        modal_close_btn: "Закрыть"
    },
    en: {
        // Navigation
        nav_features: "Features",
        nav_bots: "Bots & SDK",
        nav_docs: "Documentation",
        nav_security: "Security",
        nav_faq: "FAQ",
        nav_get_app: "Get App",

        // Hero
        hero_badge: "Next-Gen Messenger & Bot SDK",
        hero_title_1: "Limitless Messaging. ",
        hero_title_2: "Freedom for Bots.",
        hero_subtitle: "An ultra-fast messenger powered by real-time WebSockets, modern Glassmorphism aesthetics, and an open bot platform featuring customizable inline buttons.",
        btn_download_sub: "For Android",
        btn_download_apk: "Download APK",
        btn_sdk_sub: "For Developers",
        btn_sdk_title: "Python SDK",
        stat_latency: "Message Latency",
        stat_rate_limit: "Anti-flood Protection",
        stat_design: "Glassmorphism UI",

        // Mockup
        mockup_bot_status: "bot • online",
        mockup_today: "Today",
        mockup_bot_msg: "Hello! Welcome to Vibe Messenger. Test out our brand new interactive inline keyboard buttons right now:",
        mockup_btn_features: "Features",
        mockup_btn_docs: "Documentation",
        mockup_btn_alert: "Show Alert",
        mockup_alert_title: "Bot Message",

        // Features
        feat_pill: "Future Tech",
        feat_title: "Why Choose Vibe",
        feat_subtitle: "Everything you need for seamless, secure, and cutting-edge communication.",
        f1_title: "Real-time WebSockets",
        f1_desc: "Instant message delivery, read receipts, and live typing indicators with zero delay.",
        f2_title: "Innovative Glassmorphism",
        f2_desc: "Sophisticated frosted-glass interface with physics feedback, smooth gradients, and adaptive theme.",
        f3_title: "Powerful Bot Platform",
        f3_desc: "Support for inline buttons (up to 5 per row, 10 rows), custom HEX background and text colors, and callback handlers.",
        f4_title: "Anti-Flood & Spam Protection",
        f4_desc: "Built-in rate limiting at API level (3 clicks/sec) and comprehensive privacy controls.",
        f5_title: "Built-in Music Player",
        f5_desc: "Global mini-player, chat playlists, and background playback for voice notes and music.",
        f6_title: "Offline Cache & Room DB",
        f6_desc: "Robust local database storage and seamless message history synchronization.",

        // Bots Banner
        bots_pill: "Vibe Bot Platform",
        bots_title: "Build the Next Generation of Bots",
        bots_desc: "Our vibebots Python SDK provides an elegant and powerful toolkit: commands, inline keyboards with custom styling, and instant real-time callback processing.",
        bots_btn_docs: "Open SDK Documentation",

        // Documentation Tabs & Headers
        docs_pill: "Developer Docs",
        docs_title: "Vibe Bots Python SDK",
        docs_subtitle: "Complete guide to developing and deploying bots on Vibe Messenger.",
        doc_menu_getting_started: "Getting Started",
        doc_tab_install: "Installation & Setup",
        doc_tab_quickstart: "Quick Start",
        doc_menu_features: "SDK Features",
        doc_tab_messages: "Handling Messages",
        doc_tab_keyboards: "Inline Buttons & Colors",
        doc_tab_callbacks: "Callback Query Handlers",
        doc_tab_ratelimit: "Rate Limiting (Anti-Flood)",
        doc_menu_api: "API Reference",
        doc_tab_api: "Class & Method Reference",
        copy_btn: "Copy",

        doc_install_title: "1. Installation & Project Structure",
        doc_install_desc: "The library is written in pure Python 3.8+ and uses the standard requests library to communicate with the Bot API.",
        doc_quickstart_title: "2. Quick Start: Your First Bot",
        doc_quickstart_desc: "Initialize a Bot instance with your token and start polling updates with start_polling().",
        doc_messages_title: "3. Handling Incoming Messages",
        doc_messages_desc: "The @bot.message_handler(...) decorator supports command filtering and custom lambda predicates.",
        doc_kb_title: "4. Inline Keyboards & Custom Colors (Glassmorphism)",
        doc_kb_desc: "Vibe Messenger supports up to 5 buttons per row and up to 10 rows in total. Each button can specify custom bg_color and text_color HEX codes.",
        doc_callbacks_title: "5. Handling Button Clicks (Callback Query)",
        doc_callbacks_desc: "When a button is tapped, your bot receives a CallbackQuery object. Respond with a toast or modal dialog via call.answer().",
        doc_ratelimit_title: "6. Anti-Flood & Rate Limiting",
        doc_ratelimit_desc: "An automated rate limit of max 3 clicks/sec per user operates at WebSocket/API level. Flood traffic is automatically filtered out before reaching your bot.",
        doc_ratelimit_note_title: "Built-in Security:",
        doc_ratelimit_note_desc: "No need to implement custom debounce mechanisms in your bot — Vibe servers guarantee clean update streams.",
        doc_api_title: "7. Class & Method Reference",

        // Security & Legal
        sec_pill: "Privacy by Design",
        sec_title: "Your Data Belongs Only to You",
        sec_desc: "Vibe is crafted with strict privacy standards. We never sell your data to third parties, never train ad algorithms on your conversations, and give you complete control over blacklists, device sessions, and visibility preferences.",
        btn_privacy_policy: "Privacy Policy",
        btn_terms: "Terms of Service",

        // Download
        dl_title: "Start Chatting on Vibe Today",
        dl_subtitle: "Download the Android application or connect via Web.",
        dl_direct: "Direct Download",
        dl_source: "Open Source",

        // FAQ
        faq_pill: "Questions & Answers",
        faq_title: "Frequently Asked Questions",
        faq_q1: "How can I create a bot in Vibe?",
        faq_a1: "Create and register your bot via our official service bot inside the messenger, obtain an API token, and use the `vibebots` Python library to write your logic.",
        faq_q2: "How many inline buttons can I add to a message?",
        faq_a2: "You can place up to 5 buttons per row and up to 10 rows in total (up to 50 buttons per message). Each button supports custom background and text HEX colors.",
        faq_q3: "Is there a limit on button click frequency?",
        faq_a3: "Yes, to protect bot servers from spam and overloading, a built-in rate limit of maximum 3 clicks per second per user is enforced at API level.",
        faq_q4: "Is Vibe Messenger free to use?",
        faq_a4: "Yes, Vibe Messenger and the Bot Platform are completely free for all users and developers.",

        // Footer & Modals
        footer_slogan: "Next-generation messenger with an open bot ecosystem.",
        footer_nav: "Navigation",
        footer_legal: "Legal",
        footer_rights: "All rights reserved.",
        modal_close_btn: "Close"
    }
};

// ==========================================================================
// 2. LEGAL DOCUMENTS CONTENT (RU / EN)
// ==========================================================================
const legalDocs = {
    privacy: {
        ru: {
            title: "Политика конфиденциальности Vibe Messenger",
            content: `
                <h4>1. Общие положения</h4>
                <p>Настоящая Политика конфиденциальности определяет порядок обработки и защиты персональных данных пользователей приложения и сервисов Vibe Messenger (далее — «Сервис»). Мы уважаем вашу конфиденциальность и стремимся обеспечить максимальную безопасность ваших личных данных.</p>

                <h4>2. Собираемая информация</h4>
                <p>Для обеспечения работы Сервиса мы обрабатываем следующие категории данных:</p>
                <ul>
                    <li><strong>Учетные данные:</strong> имя пользователя, отображаемое имя, статус, дата регистрации и аватар профиля.</li>
                    <li><strong>Сообщения и медиа:</strong> текст сообщений, отправленные файлы, голосовые сообщения и реакции, передаваемые исключительно для доставки получателям.</li>
                    <li><strong>Технические данные:</strong> идентификаторы активных сессий устройств, IP-адреса для установления WebSocket-соединений.</li>
                </ul>

                <h4>3. Безопасность и защита данных</h4>
                <p>Все соединения между клиентом и сервером защищены шифрованием TLS/SSL и протоколами WSS. Сервер защищен от DDoS-атак и попыток несанкционированного доступа.</p>

                <h4>4. Платформа ботов и сторонние сервисы</h4>
                <p>При взаимодействии с ботами (через команды или нажатие на инлайн-кнопки) бот получает только общедоступную информацию вашего профиля (ID, имя, username) и данные нажатия (callback_data). Бот не имеет доступа к вашей переписке с другими пользователями.</p>

                <h4>5. Права пользователя</h4>
                <p>Вы имеете полное право удалять свои сообщения, блокировать нежелательных пользователей, управлять активными сессиями устройств или запросить полное удаление своего аккаунта.</p>
            `
        },
        en: {
            title: "Vibe Messenger Privacy Policy",
            content: `
                <h4>1. General Provisions</h4>
                <p>This Privacy Policy outlines how personal data is collected, used, and safeguarded when using Vibe Messenger applications and related services (the "Service"). We are committed to protecting your personal privacy.</p>

                <h4>2. Information We Collect</h4>
                <p>To provide communication services, we process the following categories of information:</p>
                <ul>
                    <li><strong>Account Information:</strong> username, display name, about bio, registration timestamp, and profile avatar.</li>
                    <li><strong>Messages and Media:</strong> message content, attachments, voice notes, and reactions transmitted solely to deliver them to intended recipients.</li>
                    <li><strong>Technical Data:</strong> active device session identifiers and network connection metadata for WebSocket sessions.</li>
                </ul>

                <h4>3. Data Security</h4>
                <p>All communication channels between client devices and servers are protected by industry-standard TLS/WSS encryption. Multi-layer defense systems prevent spam and unauthorized access.</p>

                <h4>4. Bot Ecosystem & Third Parties</h4>
                <p>When interacting with bots via commands or inline buttons, the bot receives only your public profile info (ID, name, username) and the button payload (callback_data). Bots cannot access private chats with other users.</p>

                <h4>5. User Rights</h4>
                <p>You retain full control over your data: you can delete messages, manage blocked contacts, revoke device sessions, or delete your account at any time.</p>
            `
        }
    },
    terms: {
        ru: {
            title: "Пользовательское соглашение Vibe Messenger",
            content: `
                <h4>1. Предмет соглашения</h4>
                <p>Используя Vibe Messenger, вы соглашаетесь соблюдать настоящее Пользовательское соглашение. Сервис предоставляется на условиях «как есть» (as is).</p>

                <h4>2. Правила использования</h4>
                <p>Пользователи обязуются не использовать Сервис для:</p>
                <ul>
                    <li>Распространения вредоносного ПО, спама и несанкционированной рекламы.</li>
                    <li>Совершения действий, нарушающих законодательство или права третьих лиц.</li>
                    <li>Попыток взлома, декомпиляции протоколов и нарушения работы WebSocket серверов.</li>
                </ul>

                <h4>3. Правила для разработчиков ботов</h4>
                <p>Разработчики обязуются соблюдать лимиты вызовов API (не более 3 кликов в секунду от одного пользователя), не создавать ботов для фишинга и своевременно отвечать на callback query запросы пользователей.</p>

                <h4>4. Ограничение ответственности</h4>
                <p>Администрация сервиса не несет ответственности за контент, передаваемый пользователями в личных сообщениях, и за действия сторонних ботов.</p>
            `
        },
        en: {
            title: "Vibe Messenger Terms of Service",
            content: `
                <h4>1. Subject of Agreement</h4>
                <p>By accessing or using Vibe Messenger, you agree to be bound by these Terms of Service. The service is provided on an "as is" and "as available" basis.</p>

                <h4>2. Acceptable Use</h4>
                <p>Users agree not to use the Service for:</p>
                <ul>
                    <li>Distributing malicious software, automated spam, or unauthorized advertising.</li>
                    <li>Conducting activities that violate local or international laws.</li>
                    <li>Attempting unauthorized access, reverse engineering, or disrupting WebSocket infrastructure.</li>
                </ul>

                <h4>3. Bot Developer Rules</h4>
                <p>Bot developers agree to respect rate limits (3 button clicks/sec per user), refrain from phishing or deceptive practices, and provide valid responses to user callback queries.</p>

                <h4>4. Limitation of Liability</h4>
                <p>Vibe Messenger shall not be liable for user-generated content or actions of third-party bots connected through the Bot API.</p>
            `
        }
    }
};

// ==========================================================================
// 3. CORE APPLICATION STATE & INITIALIZATION
// ==========================================================================
let currentLang = localStorage.getItem('vibe_lang') || 'ru';
let currentTheme = localStorage.getItem('vibe_theme') || 'dark';

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initLanguage();
    initDocsTabs();
    initFAQ();
    initMobileMenu();
    if (window.Prism) {
        Prism.highlightAll();
    }
});

// ==========================================================================
// 4. THEME MANAGEMENT
// ==========================================================================
function initTheme() {
    document.documentElement.setAttribute('data-theme', currentTheme);
    const themeBtn = document.getElementById('themeToggleBtn');
    if (themeBtn) {
        themeBtn.addEventListener('click', toggleTheme);
    }
}

function toggleTheme() {
    currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', currentTheme);
    localStorage.setItem('vibe_theme', currentTheme);
}

// ==========================================================================
// 5. LANGUAGE MANAGEMENT
// ==========================================================================
function initLanguage() {
    const langBtn = document.getElementById('langSwitchBtn');
    if (langBtn) {
        langBtn.addEventListener('click', toggleLanguage);
    }
    applyTranslations(currentLang);
}

function toggleLanguage() {
    currentLang = currentLang === 'ru' ? 'en' : 'ru';
    localStorage.setItem('vibe_lang', currentLang);
    applyTranslations(currentLang);
}

function applyTranslations(lang) {
    document.documentElement.setAttribute('lang', lang);
    const label = document.getElementById('currentLangLabel');
    if (label) label.textContent = lang.toUpperCase();

    const dict = translations[lang];
    if (!dict) return;

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) {
            el.textContent = dict[key];
        }
    });
}

// ==========================================================================
// 6. INTERACTIVE PHONE MOCKUP LOGIC
// ==========================================================================
function handleDemoClick(type, responseText) {
    showMockupToast(type === 'features' ? '✨ Загрузка возможностей...' : '📚 Открытие SDK...');
    
    // Simulate user message reply
    setTimeout(() => {
        const replyBubble = document.getElementById('userDemoReply');
        const replyText = document.getElementById('userDemoReplyText');
        if (replyBubble && replyText) {
            replyText.textContent = type === 'features' ? 'Клик: Возможности' : 'Клик: Документация';
            replyBubble.style.display = 'flex';
            
            const chatBody = document.getElementById('mockupChatBody');
            if (chatBody) {
                chatBody.scrollTop = chatBody.scrollHeight;
            }
        }
        showMockupToast(responseText);
    }, 400);
}

function handleDemoClickAlert(alertMessage) {
    const overlay = document.getElementById('mockupAlertOverlay');
    const text = document.getElementById('mockupAlertText');
    if (overlay && text) {
        text.textContent = alertMessage;
        overlay.style.display = 'flex';
    }
}

function closeMockupAlert() {
    const overlay = document.getElementById('mockupAlertOverlay');
    if (overlay) {
        overlay.style.display = 'none';
    }
}

function showMockupToast(message) {
    const toast = document.getElementById('mockupToast');
    const text = document.getElementById('mockupToastText');
    if (toast && text) {
        text.textContent = message;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }
}

function triggerDownloadAlert() {
    const msg = currentLang === 'ru' 
        ? '📦 Загрузка установочного пакета Vibe v1.0.0 APK начнется прямо сейчас!' 
        : '📦 Downloading Vibe v1.0.0 APK package!';
    alert(msg);
}

// ==========================================================================
// 7. DOCUMENTATION TABS SWITCHER
// ==========================================================================
function initDocsTabs() {
    const tabButtons = document.querySelectorAll('.doc-tab-btn');
    const tabPanes = document.querySelectorAll('.doc-pane');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');

            tabButtons.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const targetPane = document.getElementById(targetId);
            if (targetPane) {
                targetPane.classList.add('active');
            }
        });
    });
}

// Copy to Clipboard
function copyCode(button) {
    const pre = button.closest('.code-box').querySelector('pre code');
    if (!pre) return;

    const textToCopy = pre.innerText;
    navigator.clipboard.writeText(textToCopy).then(() => {
        const originalHtml = button.innerHTML;
        button.innerHTML = `<i class="fa-solid fa-check" style="color: var(--accent-emerald)"></i> ${currentLang === 'ru' ? 'Скопировано!' : 'Copied!'}`;
        setTimeout(() => {
            button.innerHTML = originalHtml;
        }, 2000);
    });
}

// ==========================================================================
// 8. FAQ ACCORDION
// ==========================================================================
function initFAQ() {
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(item => {
        const questionBtn = item.querySelector('.faq-question');
        if (questionBtn) {
            questionBtn.addEventListener('click', () => {
                const isOpen = item.classList.contains('open');
                faqItems.forEach(i => i.classList.remove('open'));
                if (!isOpen) {
                    item.classList.add('open');
                }
            });
        }
    });
}

// ==========================================================================
// 9. LEGAL MODALS (PRIVACY POLICY & TERMS)
// ==========================================================================
function openLegalModal(type) {
    const docData = legalDocs[type] ? legalDocs[type][currentLang] : null;
    if (!docData) return;

    const modal = document.getElementById('legalModal');
    const title = document.getElementById('modalTitle');
    const content = document.getElementById('modalContent');

    if (modal && title && content) {
        title.textContent = docData.title;
        content.innerHTML = docData.content;
        modal.classList.add('show');
        document.body.style.overflow = 'hidden';
    }
}

function closeLegalModal() {
    const modal = document.getElementById('legalModal');
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// Close modal when clicking backdrop
window.addEventListener('click', (e) => {
    const modal = document.getElementById('legalModal');
    if (e.target === modal) {
        closeLegalModal();
    }
});

// ==========================================================================
// 10. MOBILE MENU TOGGLE
// ==========================================================================
function initMobileMenu() {
    const mobileBtn = document.getElementById('mobileMenuBtn');
    const navLinks = document.getElementById('navLinks');

    if (mobileBtn && navLinks) {
        mobileBtn.addEventListener('click', () => {
            navLinks.classList.toggle('open');
        });

        // Close when clicking nav items
        navLinks.querySelectorAll('a').forEach(a => {
            a.addEventListener('click', () => {
                navLinks.classList.remove('open');
            });
        });
    }
}
