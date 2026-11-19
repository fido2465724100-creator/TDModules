# 📦 TDModules — Единый центр универсальных модулей и инструкций

Централизованный репозиторий общих архитектурных решений, алгоритмов, модулей безопасности, сетевого стека и образовательных движков для экосистемы проектов:
- **`SuperDriver`** (драйверный стек, даемон, веб-панель)
- **`UniTest`** (тестирование, мобильный сервер, анти-чит)
- **`Tdroid`** (Android-платформа, утилиты)
- **`TDRobotics`** (робототехника, IoT, контроллеры)
- **`TDnetScript`** (сетевые сценарии, автоматизация)
- **`Drlmstudio`** (локальные LLM-агенты)

---

## 🏗️ Архитектура репозитория

```
TDModules/
├── VERSION                         # Текущая версия библиотеки (SemVer: 1.0.0)
├── CHANGELOG.md                    # История изменений с фиксацией проектов-инициаторов
├── README.md                       # Главная страница и навигация
├── docs/                           # Исчерпывающая документация
│   ├── CONTRIBUTING_RULES.md       # Главный регламент разработки и работы AI-агентов
│   ├── 01_SECURITY_AND_PROTECTION.md # Шифрование AES-GCM, RSA-OAEP, маскирование строк
│   ├── 02_NETWORK_AND_TUNNELS.md     # 6 бесплатных туннелей, Direct LAN, Heartbeat
│   ├── 03_FREE_LLM_POOL.md           # Ротация бесплатных LLM, PromptSanitizer, DeviceProfiler
│   ├── 04_POLYGLOT_QUIZ_AND_DICTIONARY.md # Concept-Pivot термины, полиморфные тесты, SM-2 SRS
│   ├── 04_POLYGLOT_QUIZ_AND_DICTIONARY.md # Concept-Pivot термины, полиморфные тесты, SM-2 SRS
│   ├── 05_AI_AGENT_PROMPT.md         # Промпт для агентов в соседних вкладках
│   └── 06_TDSCRIPT_CORE_LANGUAGE.md  # Детерминированный скриптинг, Host SPI, Capabilities, 0 KB VRAM
├── modules/                        # Исходный код модулей (Kotlin/Java)
│   ├── core-security/              # Безопасность, шифрование и обфускация
│   │   └── SessionCrypto.kt, SecureStringMask.kt, RateLimiter.kt
│   ├── core-network/               # Отказоустойчивое сетевое взаимодействие
│   │   └── TunnelManager.kt, NetworkUtils.kt
│   ├── core-ai/                    # Нейросетевая инфраструктура
│   │   └── FreeModelPool.kt, PromptSanitizer.kt, DeviceProfiler.kt
│   ├── core-education/             # Образовательный движок и полиглот-словари
│   │   ├── model/ConceptTermModels.kt
│   │   ├── model/PolymorphicQuizModels.kt
│   │   ├── engine/PositionalShuffle.kt
│   │   └── parser/UnifiedEducationalParser.kt
│   └── core-tdscript/              # Единый детерминированный язык TDscript Core 1.0.1
│       └── Lexer.kt, Parser.kt, AST.kt, Interpreter.kt
├── templates/                      # Шаблоны интеграции
│   ├── proguard-rules.pro          # Правила защиты и обфускации R8/ProGuard
│   └── gradle-include.kts          # Инструкция и шаблоны подключения в Gradle
└── releases/                       # Архив неизменяемых срезов старых версий
    ├── v1.0.0/                     # Срез релиза 1.0.0
    └── v1.1.0/                     # Срез релиза 1.1.0 (включая core-tdscript)
```

---

## 🧩 Описание ключевых модулей

| Модуль | Ключевые возможности | Основные классы |
|---|---|---|
| **`core-security`** | Сессионное AES-GCM-256 (IV на каждый пакет), RSA-OAEP 2048, HMAC-SHA256 постоянного времени против тайминг-атак, XOR-обфускация строк от декомпиляторов, RateLimiter. | `SessionCrypto`<br>`SecureStringMask`<br>`RateLimiter` |
| **`core-network`** | Пул из 6 обратных SSH-шлюзов с $0 затратами (`localhost.run`, `pinggy.io`, `serveo.net`, `telebit.cloud`, `bore.pub`, Cloudflare Quick Tunnel), Direct LAN приоритет, активный Heartbeat каждые 20с. | `TunnelManager`<br>`NetworkUtils` |
| **`core-ai`** | Ротация бесплатных моделей (`gpt-oss-120b`, `Meta-Llama-3_3-70B`, `openai-fast`), мониторинг здоровья эндпоинтов, обработка Retry-After, фильтр инъекций, профилировщик аппаратных ресурсов (RAM/CPU). | `FreeModelPool`<br>`PromptSanitizer`<br>`DeviceProfiler` |
| **`core-education`** | **Concept-Pivot Model**: словари привязаны к семантическим концептам, позволяя врачам и специалистам добавлять новые языки (DE, ES, RU, EN) с сохранением интервального повторения (SM-2 SRS). **Polymorphic Quiz**: смена языка теста на лету (например, физика с немецкого на английский) без потери правильных ответов и прогресса. Всеядный парсер. | `ConceptTermModels`<br>`PolymorphicQuizModels`<br>`PositionalShuffle`<br>`UnifiedEducationalParser` |
| **`core-tdscript`** | **0 KB VRAM / Headless**: унифицированное ядро сценарного языка TDscript (SPEC-TDS-CORE-1.0.1). Эшелонированная безопасность `Capabilities`, нормативный контракт `HostSPI` для текстового режима TUI (Tdroid), сетей (TDnetScript) и робототехники (TDRobotics). | `Lexer`<br>`Parser`<br>`AST`<br>`Interpreter`<br>`Capabilities`<br>`HostSPI` |


---

## 🔒 Главный регламент разработки (CONTRIBUTING_RULES)

Любой разработчик и AI-агент, работающий в экосистеме, **обязан** следовать правилам:
1. **«Добавил код — обнови документацию»**: каждый новый метод или изменение должно быть описано в `docs/`.
2. **SemVer**: любое изменение поднимает версию в `VERSION`.
3. **Архивация**: при выпуске нового релиза предыдущий срез сохраняется в `releases/vX.Y.Z/`.
4. **Обратная совместимость**: не удалять старые методы без переходного периода (`@Deprecated`).

Полный текст регламента: [docs/CONTRIBUTING_RULES.md](docs/CONTRIBUTING_RULES.md).
Инструкция для AI-агентов: [docs/05_AI_AGENT_PROMPT.md](docs/05_AI_AGENT_PROMPT.md).
