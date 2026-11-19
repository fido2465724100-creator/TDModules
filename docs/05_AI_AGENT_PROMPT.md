# 🤖 Универсальная директива для AI-агентов экосистемы (TDModules Migration & Sync Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в соседней вкладке (`Tdroid`, `TDnetScript`, `TDRobotics`, `UniTest`, `SuperDriver`, `Drlmstudio`). Агент изучит централизованные модули `TDModules`, спланирует миграцию проекта на единую кодовую базу и будет поддерживать автоматическую синхронизацию с локальным архивом и защищенным GitHub.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие архитектурные модули, шифрование, сетевые туннели, образовательные движки и единое ядро языка TDscript вынесены в централизованный репозиторий:
📂 Локальный путь: `/home/anton/TDModules`
🔒 Защищенный приватный GitHub: `https://github.com/fido2465724100-creator/TDModules.git`
🏷️ Текущий стабильный релиз: v1.1.0

### 📦 Архивные копии предыдущих версий (Zero-Breakage):
Все прошлые релизы неизменяемо сохранены локально и в облаке в каталоге `releases/`:
- `releases/v1.0.0/` — базовый срез v1.0.0 (core-security, core-network, core-ai, core-education).
- `releases/v1.1.0/` — срез v1.1.0 (+ модуль core-tdscript).
Если твой проект пока не готов переходить на новейший API, подключай гарантированно стабильную архивную версию напрямую из `/home/anton/TDModules/releases/v1.0.0/`.

---

### 🎯 Твои задачи в текущем проекте:

#### 1. Провести аудит дублирующегося кода и спланировать миграцию:
Проверь свой проект на наличие локальных копий или самописных реализаций, которые уже стандартизированы и протестированы в `TDModules`:
- `core-tdscript` (`com.tdscript.core.*`): единое ядро TDscript (SPEC-TDS-CORE-1.0.1: Lexer, Parser, AST, Interpreter, Capabilities, HostSPI). 0 KB VRAM / Headless — работает одинаково в TUI текстовом режиме Tdroid, промышленных сетях TDnetScript и роботах TDRobotics.
- `core-security` (`com.tdmodules.security.*`): сессионный AES-GCM-256 (IV на пакет), RSA-OAEP 2048, динамическое маскирование строк SecureStringMask (XOR), RateLimiter против DoS.
- `core-network` (`com.tdmodules.network.*`): TunnelManager (6 бесплатных обратных SSH-шлюзов: localhost.run, pinggy.io, serveo.net и др. + Direct LAN авто-приоритет + 20s Heartbeat), NetworkUtils.
- `core-ai` (`com.tdmodules.ai.*`): FreeModelPool (ротация бесплатных LLM-нод gpt-oss-120b, Llama-3.3-70B, openai-fast, обработка Retry-After и ошибок 429/503), PromptSanitizer, DeviceProfiler.
- `core-education` (`com.tdmodules.education.*`): ConceptTermModels (Concept-Pivot термины с английским якорем и сохранением SM-2 SRS для полиглотов и врачей), PolymorphicQuizModels (полиморфные тесты со сменой языка на лету без потери ответов), UnifiedEducationalParser, PositionalShuffle.

#### 2. Подключение через Gradle (без дублирования):
- Вариант 1 (рекомендуемый): через `includeBuild("../TDModules")` в `settings.gradle.kts`.
- Вариант 2 (для прямого включения): через `sourceSets` в `build.gradle.kts` (указав пути к `../TDModules/modules/.../src/main/kotlin` или архивным `../TDModules/releases/vX.Y.Z/...`).
- Шаблоны подключения смотри в `/home/anton/TDModules/templates/gradle-include.kts`.
- После переключения удали локальные дубликаты классов.

---

### 🔒 Регламент внесения изменений и обязательной синхронизации с GitHub (CONTRIBUTING_RULES):

Если твоему проекту требуется доработать или расширить функционал общего модуля, строго соблюдай порядок:

1. **Внеси код** в нужный подкаталог `/home/anton/TDModules/modules/`.
2. **Синхронизируй документацию**: обнови описание в `/home/anton/TDModules/docs/` (сигнатуры, параметры, назначение).
3. **Повысь версию** в `/home/anton/TDModules/VERSION` по правилам SemVer (MAJOR / MINOR / PATCH).
4. **Зафиксируй в CHANGELOG.md**: добавь дату, версию, список изменений и имя твоего проекта-инициатора:
   `## [vX.Y.Z] - ГГГГ-ММ-ДД (Инициатор: [ИмяТвоегоПроекта])`
5. **Создай архивный срез**: скопируй текущее состояние в `/home/anton/TDModules/releases/vX.Y.Z/`.
6. **Пройди шлюз тестов (Pre-commit Testing Gate)**:
   Выполни в `/home/anton/TDModules`:
   `./gradlew test`
   Если упал хотя бы один тест — коммит и пуш **СТРОГО ЗАПРЕЩЕНЫ** до исправления!
7. **Зафиксируй коммит (Conventional Commits)**:
   `git commit -m "feat(scope): описание"  # или fix(scope): ...`
   `git tag vX.Y.Z`
8. **ОБЯЗАТЕЛЬНО отправь в защищенный GitHub-архив**:
   Выполни в `/home/anton/TDModules`:
   `git push origin master && git push origin --tags`
   *(Это обеспечивает сохранность резервной копии и доступность обновлений для всей команды).*

---

### 📚 Файлы для изучения:
- `/home/anton/TDModules/README.md`
- `/home/anton/TDModules/docs/CONTRIBUTING_RULES.md`
- `/home/anton/TDModules/docs/06_TDSCRIPT_CORE_LANGUAGE.md`
- `/home/anton/TDModules/templates/gradle-include.kts`
- `/home/anton/TDModules/releases/`
```
