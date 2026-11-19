# 🤖 Универсальная директива для AI-агентов экосистемы (TDModules Migration & Sync Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в соседней вкладке (`Tdroid`, `TDnetScript`, `TDRobotics`, `UniTest`, `SuperDriver`, `Drlmstudio`). Агент изучит централизованные модули `TDModules`, спланирует избирательную миграцию проекта на нужные компоненты и будет поддерживать автоматическую синхронизацию с локальным архивом и защищенным GitHub.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие архитектурные модули, шифрование, сетевые туннели, образовательные движки и ядро языка TDscript вынесены в централизованный репозиторий:
📂 Локальный путь: `/home/anton/TDModules`
🔒 Защищенный приватный GitHub: `https://github.com/fido2465724100-creator/TDModules.git`
🏷️ Текущий стабильный релиз: v1.1.1

---

### 💡 ГЛАВНЫЙ ПРИНЦИП: ИЗБИРАТЕЛЬНОСТЬ (ZERO-BLOAT)
**Никогда не подключай все модули подряд!** 
Каждый модуль TDModules абсолютно автономен. Проект обязан подключать **ТОЛЬКО ТЕ** библиотеки, которые реально решают его задачи:
- `Tdroid` (терминальная ОС) -> `core-tdscript`, `core-network`, `core-security` (без core-education).
- `TDnetScript` (промышленная автоматика) -> `core-tdscript`, `core-network`, `core-security` (без core-education).
- `TDRobotics` (роботы и схемотехника) -> `core-tdscript`, `core-network` (без core-ai/education).
- `UniTest` (экзамены и тесты) -> `core-education`, `core-security`, `core-network`, `core-ai` (без core-tdscript!).
- `SuperDriver` (сервер и драйверы) -> `core-security`, `core-network`, `core-ai`.

---

### 🎯 Твои задачи в текущем проекте:

#### 1. Провести аудит и выбрать ТОЛЬКО нужные модули:
Изучи кодовую базу своего проекта и подключи лишь те компоненты из `/home/anton/TDModules/modules/`, функции которых действительно требуются:
- `core-tdscript` (`com.tdscript.core.*`): подключай, ТОЛЬКО если в приложении есть запуск сценариев TDscript (SPEC-TDS-CORE-1.0.1: Lexer, Parser, AST, Interpreter, Capabilities, HostSPI).
- `core-security` (`com.tdmodules.security.*`): сессионный AES-GCM-256 (IV на пакет), RSA-OAEP 2048, маскирование строк SecureStringMask (XOR), RateLimiter против DoS.
- `core-network` (`com.tdmodules.network.*`): TunnelManager (6 бесплатных обратных SSH-шлюзов: localhost.run, pinggy.io, serveo.net и др. + Direct LAN авто-приоритет + 20s Heartbeat), NetworkUtils.
- `core-ai` (`com.tdmodules.ai.*`): FreeModelPool (ротация бесплатных LLM-нод gpt-oss-120b, Llama-3.3-70B, openai-fast, обработка Retry-After и ошибок 429/503), PromptSanitizer, DeviceProfiler.
- `core-education` (`com.tdmodules.education.*`): ConceptTermModels (Concept-Pivot термины с английским якорем и сохранением SM-2 SRS для полиглотов и врачей), PolymorphicQuizModels (полиморфные тесты со сменой языка на лету без потери ответов), UnifiedEducationalParser, PositionalShuffle.

#### 2. Выборочное подключение через Gradle:
- В `settings.gradle.kts` через `includeBuild("../TDModules")` заменяй **ТОЛЬКО** нужные библиотеки.
- В `build.gradle.kts` в блоке `dependencies { ... }` объявляй **ТОЛЬКО** используемые модули:
  ```kotlin
  dependencies {
      // Пример: подключаем только безопасность и туннели
      implementation("com.tdmodules:core-security")
      implementation("com.tdmodules:core-network")
  }
  ```
- Удали локальные дубликаты классов в своем проекте после переключения на TDModules.
- Подробные примеры смотри в `/home/anton/TDModules/templates/gradle-include.kts`.

---

### 📦 Архивные копии предыдущих версий (Zero-Breakage):
Все прошлые релизы неизменяемо сохранены локально и на GitHub в каталоге `releases/`:
- `releases/v1.0.0/` — срез v1.0.0 (core-security, core-network, core-ai, core-education).
- `releases/v1.1.0/` — срез v1.1.0 (+ core-tdscript).
- `releases/v1.1.1/` — срез v1.1.1 (методы сессий UniTest и совместимость).
Если твой проект пока не готов переходить на новейший API, подключай зафиксированную версию напрямую из `releases/vX.Y.Z/`.

---

### 🔒 Регламент внесения изменений и обязательной синхронизации с GitHub (CONTRIBUTING_RULES):
Если твоему проекту требуется доработать или расширить функционал общего модуля:
1. Внеси код в нужный подкаталог `/home/anton/TDModules/modules/`.
2. Синхронизируй документацию в `/home/anton/TDModules/docs/`.
3. Повысь версию в `/home/anton/TDModules/VERSION` по правилам SemVer (MAJOR / MINOR / PATCH).
4. Зафиксируй изменения в `CHANGELOG.md` с указанием инициатора:
   `## [vX.Y.Z] - ГГГГ-ММ-ДД (Инициатор: [ИмяТвоегоПроекта])`
5. Создай архивный срез в `/home/anton/TDModules/releases/vX.Y.Z/`.
6. Выполни тест-гейт в `/home/anton/TDModules`: `./gradlew test` (все тесты обязаны пройти).
7. Зафиксируй коммит по стандарту Conventional Commits: `git commit -m "feat(scope): ..."` и поставь тег `git tag vX.Y.Z`.
8. Обязательно отправь в защищенный GitHub-архив:
   `git push origin master && git push origin --tags`

---

### 📚 Файлы для изучения:
- `/home/anton/TDModules/README.md`
- `/home/anton/TDModules/docs/CONTRIBUTING_RULES.md`
- `/home/anton/TDModules/templates/gradle-include.kts`
- `/home/anton/TDModules/releases/`
```
