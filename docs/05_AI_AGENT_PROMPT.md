# 🤖 Универсальная директива для AI-агентов экосистемы (TDModules Migration & Rules Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в любой проект (`Tdroid`, `TDnetScript`, `TDRobotics`, `UniTest`, `SuperDriver`, `Drlmstudio`, `AIDream`). Здесь содержится полный каталог модулей, последние обновления v1.3.2, способы подключения и исчерпывающий регламент внесения изменений.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие архитектурные модули, шифрование, сетевые туннели, образовательные движки, локализация и ядро языка TDscript вынесены в централизованный репозиторий:
📂 Локальный путь: `/home/anton/TDModules`
🔒 Защищенный приватный GitHub: `https://github.com/fido2465724100-creator/TDModules.git`
🏷️ Текущий стабильный релиз: v1.3.2

---

### 🚀 ЧТО НОВОГО В TDModules (Релизы v1.3.0 - v1.3.2):
Обязательно изучи эти обновления — они позволяют кардинально упростить и оптимизировать код твоего проекта:
1. **НОВЫЙ МОДУЛЬ `core-localization` (v1.3.0)**:
   - `LocaleRegistry`: нормативный реестр 109 мировых языков (ISO 639-1) с самоназваниями (Endonyms) и поддержкой RTL.
   - `LocalizationEngine`: отказоустойчивая мультиязычность с ленивой загрузкой JSON, фоллбеками (`Target -> Fallback EN -> Key`), интерполяцией строк (`%s`, `{}`) и слушателями смены языка.
   - `CommonKeys` и `BuiltinTranslations`: общие системные ключи для кнопок и статусов. Больше не нужно изобретать свои парсеры строк и хардкодить UI!
2. **ОБНОВЛЕНИЯ `core-education` (v1.3.1 - v1.3.2)**:
   - `DynamicContentTranslator`: динамический перевод формулировок тестов и терминов на лету под язык интерфейса с кэшированием.
   - `MatchingPairItem`, `MatchingRoundState`, `MatchingSessionConstants`: унифицированный движок игрового сопоставления пар (Matching Pairs) и единые константы анимации (180ms/320ms, 0.82 scale).
   - `TerminologyDeck`: кросс-языковая проекция (`toMatchingPairs`, `toMatchingRound`) и раздельный учет статистики успеха/ошибок по каждому языку (`recordResult`, `recordMatchResult`) без затирания прогресса SM-2.
3. **ОБНОВЛЕНИЯ `core-ai`**:
   - `DeviceProfiler`: автоматический fallback определения модели чипсета (`socModel`) для ARM/x86 устройств.

---

### 📦 Полный каталог доступных модулей (выбирай сам под задачи своего проекта):
- **`core-localization`** (`com.tdmodules.localization.*`): реестр 109 языков, LocalizationEngine, CommonKeys, BuiltinTranslations.
- **`core-tdscript`** (`com.tdscript.core.*`): ядро детерминированного языка TDscript (SPEC-TDS-CORE-1.0.1: Lexer, Parser, AST, Interpreter, Capabilities, HostSPI с поддержкой Bluetooth и AI). 0 KB VRAM / Headless.
- **`core-security`** (`com.tdmodules.security.*`): сессионный AES-GCM-256 (IV на пакет), RSA-OAEP 2048, динамическое маскирование строк SecureStringMask (XOR), RateLimiter против DoS, генерация уникальных токенов.
- **`core-network`** (`com.tdmodules.network.*`): TunnelManager (6 бесплатных обратных SSH-шлюзов: localhost.run, pinggy.io, serveo.net и др. + Direct LAN авто-приоритет + 20s Heartbeat), NetworkUtils.
- **`core-ai`** (`com.tdmodules.ai.*`): FreeModelPool (ротация бесплатных LLM-нод gpt-oss-120b, Llama-3.3-70B, openai-fast, обработка Retry-After и ошибок 429/503), PromptSanitizer, DeviceProfiler (подбор GGUF-моделей под RAM/CPU).
- **`core-education`** (`com.tdmodules.education.*`): ConceptTermModels, DynamicContentTranslator, Matching Pairs Engine, PolymorphicQuizModels, UnifiedEducationalParser, PositionalShuffle.

---

### 🛠️ Подключение через Gradle:
Подключай через `includeBuild("../TDModules")` или `sourceSets`. Выбирай сам, какие именно зависимости объявлять в `dependencies { ... }` своего приложения:
```kotlin
dependencies {
    // Подключай только то, что нужно твоему приложению:
    implementation("com.tdmodules:core-localization")
    implementation("com.tdmodules:core-security")
    implementation("com.tdmodules:core-network")
    implementation("com.tdmodules:core-ai")
    implementation("com.tdmodules:core-education")
    implementation("com.tdmodules:core-tdscript")
}
```
Справка по подключению: `/home/anton/TDModules/templates/gradle-include.kts`.

---

### 📦 Архивные срезы (Zero-Breakage):
Все прошлые релизы сохранены в каталоге `releases/`:
- `releases/v1.0.0/` — базовый срез v1.0.0
- `releases/v1.1.0/` — срез v1.1.0 (+ core-tdscript)
- `releases/v1.1.1/` — срез v1.1.1 (+ методы сессий UniTest)
- `releases/v1.2.0/` — срез v1.2.0 (+ Bluetooth и AI в HostSPI)
- `releases/v1.3.2/` — актуальный срез v1.3.2 (+ core-localization, Dynamic Translation, Matching Pairs)
Если проект пока не готов к обновлениям, можно брать стабильный срез напрямую из `releases/vX.Y.Z/`.

---

## 📜 ОБЯЗАТЕЛЬНЫЙ РЕГЛАМЕНТ РАЗРАБОТКИ (CONTRIBUTING_RULES)

Если твоему проекту требуется доработать существующий модуль или расширить его API, строго следуй 9 правилам:

### Правило 1. «Добавил код — обнови документацию»
Любое изменение в коде (`modules/`) ОБЯЗАНО сопровождаться обновлением соответствующего файла в папке `docs/`:
- Добавил метод или класс? Опиши его назначение, сигнатуру, параметры и пример вызова.
- Изменил поведение или исправил баг? Укажи это в описании модуля и в истории изменений.
- Запрещено коммитить код без синхронного обновления документации.

### Правило 2. Семантическое версионирование (SemVer)
Каждое изменение повышает номер релиза в файле `/home/anton/TDModules/VERSION`:
- **MAJOR (X.0.0)** — ломающие изменения архитектуры или API, несовместимые со старыми проектами.
- **MINOR (1.X.0)** — добавление нового функционала, нового туннеля, нового метода без поломки старого кода.
- **PATCH (1.0.X)** — исправление багов, оптимизация, обновление документации.

### Правило 3. Фиксация в CHANGELOG.md с указанием проекта-инициатора
В файле `/home/anton/TDModules/CHANGELOG.md` создается запись с точной датой, версией, списком изменений и именем проекта-инициатора:
```markdown
## [vX.Y.Z] - ГГГГ-ММ-ДД (Инициатор: [ИмяТвоегоПроекта])
- Добавлено: Описание нового функционала
- Изменено: Что было оптимизировано
```

### Правило 4. «Старый релиз уходит в архив» (Архивирование)
При выпуске нового минорного или мажорного релиза:
Текущий срез кода библиотеки архивируется в папку `/home/anton/TDModules/releases/vX.Y.Z/`.
Старые проекты всегда могут продолжить использовать зафиксированную архивную версию из `releases/`.

### Правило 5. Обратная совместимость (Zero-Breakage)
Все модули проектируются с сохранением обратной совместимости:
- Не удаляй старые методы сразу. Пометь их аннотацией `@Deprecated(message = "...", replaceWith = ...)` и оставь работать минимум на один релизный цикл.
- Значения параметров по умолчанию (`default values`) обязательны для всех новых полей в моделях данных, чтобы старые JSON-базы не ломались при десериализации.

### Правило 6. Защита от декомпиляции по умолчанию
Любой новый публичный интерфейс или модуль должен иметь соответствующую секцию правил в `templates/proguard-rules.pro`. Внешние эндпоинты, ключи и токены никогда не пишутся открытым текстом — только через `SecureStringMask` (XOR/AES).

### Правило 7. Стандарт сообщений коммитов (Conventional Commits)
Все коммиты должны следовать международному стандарту `type(scope): description`:
- `feat`: добавление нового функционала или модуля (например, `feat(tdscript): add while loop`).
- `fix`: исправление ошибки или бага (например, `fix(network): resolve heartbeat reconnect timeout`).
- `docs`: изменение или добавление документации.
- `test`: добавление или исправление тестов.
- `refactor`: оптимизация без изменения внешнего API.
- `chore`: рутинные задачи сборки, версионирования или зависимостей.

### Правило 8. Обязательный шлюз верификации перед коммитом (Pre-commit Testing Gate)
Ни один коммит или релиз не может быть зафиксирован без предварительного успешного выполнения тестов:
- Перед любым коммитом выполни команду `./gradlew test` в корне `/home/anton/TDModules`.
- Если падает хотя бы один тест в любом из модулей — фиксация изменений **СТРОГО ЗАПРЕЩЕНА** до устранения проблемы.

### Правило 9. Обязательная отправка в защищенный GitHub-архив
После успешного прохождения тестов, создания тега и локального коммита:
ОБЯЗАТЕЛЬНО отправь изменения в удаленный репозиторий:
`git push origin master && git push origin --tags`
Репозиторий: `https://github.com/fido2465724100-creator/TDModules.git`.
```
