# 📋 История изменений TDModules (CHANGELOG)

Все заметные изменения в данном репозитории фиксируются в соответствии с правилами [CONTRIBUTING_RULES](docs/CONTRIBUTING_RULES.md).
Формат версионирования основан на [Semantic Versioning (SemVer)](https://semver.org/).

---

## [v1.1.1] - 2026-11-19 (Инициатор: UniTest)

### 🛡️ Обратная совместимость и поддержка мобильных сессий в core-security и core-ai
- **core-security**:
  - `SessionCrypto`: Добавлен метод `generateStudentToken(studentName, clientSalt)` для криптографической генерации токенов сессий с защитой от тайминг-атак.
  - Добавлены псевдонимы `computeHmac(data)` и `verifyHmac(data, expectedHmac)` для бесшовной миграции проектов со старыми сигнатурами HMAC.
- **core-ai**:
  - `FreeModelPool`: Добавлены методы совместимости `reportNodeThrottled` и `reportNodeSuccess` с псевдонимами свойств `ENDPOINTS` и `MODELS`.
  - `DeviceProfiler`: Добавлен `typealias DeviceSpecs = DeviceHardwareSpecs` и перегрузка `getSpecs(context: Any)` для бесшовной интеграции с Android `Context`.
- **Релизы и архивы**:
  - Сформирован архивный срез `releases/v1.1.1/`.

---

## [v1.1.0] - 2026-11-19 (Инициаторы: Tdroid, TDnetScript, TDRobotics)

### ⚡ Добавлен унифицированный модуль core-tdscript
- **core-tdscript**:
  - `Lexer`: Нормативный токенизатор языка TDscript (SPEC-TDS-CORE-1.0.1) с поддержкой UTF-8, CRLF/LF, автоматического переноса строк на бинарных операторах и запятых, строковых литералов с escape-последовательностями и комментариев (`//`, `#`).
  - `Parser`: Синтаксический анализатор рекурсивного спуска, строящий AST с корректным приоритетом арифметических и логических операций, скобок и правоассоциативного присваивания.
  - `AST`: Полная типизированная иерархия узлов выражений, операторов, вызовов функций и структур управления.
  - `Interpreter`: Детерминированное ядро исполнения со строгой моделью типов, single symbol table, циклами `while`, ветвлениями `if/else`, контролем зацикливаний и нулевым оверхедом видеопамяти (**0 KB VRAM / Headless**).
  - `HostSPI` & `Capabilities`: Стандартизированный интерфейс абстракции хост-системы с проверкой битовых масок безопасности (`NET_READ`, `NET_SCAN`, `PLC_READ`, `PLC_WRITE`, `BT_ACCESS`, `HARDWARE`, `UI_CORE`, `BACKGROUND`).
- **Синхронизация между проектами**:
  - Обеспечено полное совпадение синтаксиса и контрактов в `Tdroid` (текстовый режим TUI), `TDnetScript` (промышленная автоматика, SCADA, Modbus) и `TDRobotics` (оркестрация и схемотехника).
- **Документация и шаблоны**:
  - Добавлено исчерпывающее руководство `docs/06_TDSCRIPT_CORE_LANGUAGE.md`.
  - Обновлены правила `templates/proguard-rules.pro` для защиты классов `com.tdscript.core.*`.
  - Обновлен шаблон `templates/gradle-include.kts` для интеграции `core-tdscript`.

---

## [v1.0.0] - 2026-11-19 (Инициаторы: SuperDriver, UniTest, TD Ecosystem)


### 🚀 Первый релиз единой библиотеки универсальных модулей
- **core-security**:
  - `SessionCrypto`: AES-GCM-256 с уникальным 12-байтным IV на пакет + RSA-OAEP 2048 (SHA-256) + HMAC-SHA256 постоянного времени против тайминг-атак.
  - `SecureStringMask`: Динамическое XOR-маскирование секретов и URL для защиты от статического анализа (`strings`, `jadx`, `ghidra`).
  - `RateLimiter`: Потокобезопасный ограничитель частоты запросов с защитой от DoS и автоматической очисткой устаревших записей.
- **core-network**:
  - `TunnelManager`: Пул из 6 обратных SSH-шлюзов (`localhost.run`, `pinggy.io`, `serveo.net`, `telebit.cloud`, `bore.pub`, Cloudflare Quick Tunnel) с Exponential Backoff + Jitter, 20-секундным сокетным Heartbeat и автоматическим Direct LAN приоритетом.
  - `NetworkUtils`: Приоритетное определение локального IPv4 устройства (Wi-Fi AP, Wi-Fi client, USB/Ethernet Tethering).
- **core-ai**:
  - `FreeModelPool`: Ротация бесплатных нейросетевых нод (`gpt-oss-120b`, `gpt-oss-20b`, `Meta-Llama-3_3-70B`, `openai-fast`) с кулдауном, отслеживанием кодов ошибок (429, 502, 503) и dynamic health monitoring.
  - `PromptSanitizer`: Фильтрация промпт-инъекций, jailbreak и утечек системных промптов.
  - `DeviceProfiler`: Определение RAM, ядер CPU и архитектуры с формированием рекомендаций локальных моделей GGUF (1.5B, 3B, 7B).
- **core-education**:
  - `ConceptTermModels`: Архитектура Concept-Pivot для мультиязычных словарей и терминов с английским якорем, бесконечным добавлением языков и сохранением истории интервального повторения по алгоритму SuperMemo SM-2.
  - `PolymorphicQuizModels`: Инвариантные полиморфные тесты с переключением языков на лету (например, с немецкого на английский или русский) без потери прогресса и соответствия ответов. Поддержка `MATCHING_PAIRS`, `SCIENCE_3`, `STANDARD_4`, `TRUE_FALSE`.
  - `PositionalShuffle`: Детекция позиционного смещения ответов автора и рандомизация вариантов.
  - `UnifiedEducationalParser`: Всеядный парсер двухколоночных данных, таблиц, опросников с маркерами (`*`, `+`, `[x]`, `1)`, `A)`), Anki TSV/CSV и генератор дистракторов.
- **Инфраструктура и документация**:
  - `CONTRIBUTING_RULES.md`: Обязательный регламент синхронизации кода, документации, архивации и повышения версий.
  - Подробные руководства в каталоге `docs/` (`01` - `05`).
  - Шаблоны `templates/proguard-rules.pro` и `templates/gradle-include.kts`.
