# 🤖 Универсальная директива для AI-агентов экосистемы (TDModules Reference Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в любой проект (`Tdroid`, `TDnetScript`, `TDRobotics`, `UniTest`, `SuperDriver`, `Drlmstudio`). Агент сам определит, какие модули ему нужны, подключит их и будет следовать общим правилам версионирования.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие архитектурные модули, шифрование, сетевые туннели, образовательные движки и ядро языка TDscript вынесены в централизованный репозиторий:
📂 Локальный путь: `/home/anton/TDModules`
🔒 Защищенный приватный GitHub: `https://github.com/fido2465724100-creator/TDModules.git`
🏷️ Текущий стабильный релиз: v1.1.1

---

### 📦 Каталог доступных модулей (выбирай сам под задачи своего проекта):

Изучи исходники и подключай те модули, которые посчитаешь нужными для своего проекта:
- **`core-tdscript`** (`com.tdscript.core.*`): ядро детерминированного языка TDscript (SPEC-TDS-CORE-1.0.1: Lexer, Parser, AST, Interpreter, Capabilities, HostSPI). 0 KB VRAM / Headless, подходит для любых платформ.
- **`core-security`** (`com.tdmodules.security.*`): сессионный AES-GCM-256 (IV на пакет), RSA-OAEP 2048, динамическое маскирование строк SecureStringMask (XOR), RateLimiter против DoS, генерация защищенных сессионных токенов.
- **`core-network`** (`com.tdmodules.network.*`): TunnelManager (6 бесплатных обратных SSH-шлюзов: localhost.run, pinggy.io, serveo.net и др. + Direct LAN авто-приоритет + 20s Heartbeat), NetworkUtils.
- **`core-ai`** (`com.tdmodules.ai.*`): FreeModelPool (ротация бесплатных LLM-нод gpt-oss-120b, Llama-3.3-70B, openai-fast, обработка Retry-After и ошибок 429/503), PromptSanitizer, DeviceProfiler (подбор GGUF-моделей под RAM/CPU устройства).
- **`core-education`** (`com.tdmodules.education.*`): ConceptTermModels (Concept-Pivot термины с английским якорем и сохранением SM-2 SRS для полиглотов и врачей), PolymorphicQuizModels (полиморфные тесты со сменой языка на лету без потери ответов), UnifiedEducationalParser, PositionalShuffle.

---

### 🛠️ Подключение через Gradle:
Подключай через `includeBuild("../TDModules")` или `sourceSets`. Выбирай сам, какие именно зависимости объявлять в `dependencies { ... }` своего приложения:
```kotlin
dependencies {
    // Подключай то, что нужно твоему приложению:
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
Если проект пока не готов к обновлениям, можно брать стабильный срез напрямую из `releases/vX.Y.Z/`.

---

### 🔒 Регламент внесения изменений (CONTRIBUTING_RULES):
Если тебе потребуется доработать существующий модуль или расширить его API:
1. Внеси код в нужный модуль в `/home/anton/TDModules/modules/`.
2. Обнови документацию в `/home/anton/TDModules/docs/`.
3. Подними версию в `/home/anton/TDModules/VERSION` по SemVer.
4. Запиши изменения в `/home/anton/TDModules/CHANGELOG.md` с указанием своего проекта.
5. Создай архивный срез в `/home/anton/TDModules/releases/vX.Y.Z/`.
6. Обязательно выполни тест-гейт: `./gradlew test` в корне `TDModules` (все тесты должны пройти).
7. Закоммить по Conventional Commits (`git commit -m "feat/fix: ..."`), поставь тег `git tag vX.Y.Z`.
8. Отправь в репозиторий: `git push origin master && git push origin --tags`.
```
