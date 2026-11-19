# 📋 История изменений TDModules (CHANGELOG)

Все заметные изменения в данном репозитории фиксируются в соответствии с правилами [CONTRIBUTING_RULES](docs/CONTRIBUTING_RULES.md).
Формат версионирования основан на [Semantic Versioning (SemVer)](https://semver.org/).

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
