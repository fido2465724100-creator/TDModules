# 🤖 Универсальная директива для AI-агентов экосистемы (TDModules Migration Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в соседней вкладке (`Tdroid`, `TDnetScript`, `TDRobotics`, `UniTest`, `SuperDriver`, `Drlmstudio`). Агент изучит централизованные модули `TDModules` и спланирует миграцию проекта на единую кодовую базу.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие архитектурные модули, шифрование, сетевые туннели, образовательные движки и ядро языка TDscript вынесены в централизованный репозиторий:
📂 `/home/anton/TDModules` (текущий стабильный релиз: v1.1.0)

### 🎯 Твоя задача в текущем проекте:
1. **Провести аудит дублирующегося кода**:
   Проверь свой проект на наличие локальных копий или самописных реализаций, которые уже стандартизированы и протестированы в `TDModules`:
   - `core-tdscript` (`com.tdscript.core.*`): единое ядро интерпретатора TDscript (SPEC-TDS-CORE-1.0.1: Lexer, Parser, AST, Interpreter, Capabilities, HostSPI). 0 KB VRAM / Headless, одинаково работает в TUI текстовом режиме Tdroid, промышленных сетях TDnetScript и роботах TDRobotics.
   - `core-security` (`com.tdmodules.security.*`): AES-GCM-256 (IV на пакет), RSA-OAEP 2048, маскирование строк SecureStringMask (XOR), RateLimiter против DoS.
   - `core-network` (`com.tdmodules.network.*`): TunnelManager (6 бесплатных обратных SSH-шлюзов: localhost.run, pinggy.io, serveo.net и др. + Direct LAN авто-приоритет + 20s Heartbeat), NetworkUtils.
   - `core-ai` (`com.tdmodules.ai.*`): FreeModelPool (ротация бесплатных LLM-нод gpt-oss-120b, Llama-3.3-70B, openai-fast, обработка Retry-After и ошибок 429/503), PromptSanitizer, DeviceProfiler.
   - `core-education` (`com.tdmodules.education.*`): ConceptTermModels (Concept-Pivot термины с английским якорем и сохранением SM-2 SRS для полиглотов и врачей), PolymorphicQuizModels (полиморфные тесты со сменой языка на лету без потери ответов), UnifiedEducationalParser, PositionalShuffle.

2. **Спланировать и выполнить миграцию (подключение через Gradle)**:
   - Переключи проект на использование готовых модулей вместо дублирования кода.
   - Способы подключения подробно описаны в `/home/anton/TDModules/templates/gradle-include.kts`:
     - Рекомендуемый: через `includeBuild("../TDModules")` в `settings.gradle.kts`.
     - Альтернативный: через `sourceSets` в `build.gradle.kts` (прямое включение каталогов `../TDModules/modules/.../src/main/kotlin`).
   - Удали локальные устаревшие копии классов (например, удали локальные `com.tdscript.core.*`, если они дублируются в исходниках твоего проекта).

3. **Регламент обратного вклада (CONTRIBUTING_RULES)**:
   Если в процессе работы твоему проекту потребуется расширить общий модуль:
   - Внеси изменения напрямую в `/home/anton/TDModules/modules/`.
   - ОБЯЗАТЕЛЬНО обнови документацию в `/home/anton/TDModules/docs/` (сигнатуры, параметры, назначение).
   - Повысь версию в `/home/anton/TDModules/VERSION` по SemVer.
   - Запиши изменения в `/home/anton/TDModules/CHANGELOG.md` с указанием твоего проекта (например, `[Tdroid]` или `[TDnetScript]`).
   - Убедись, что все тесты проходят: выполни `./gradlew test` в `/home/anton/TDModules`.

4. **Документация для изучения**:
   - `/home/anton/TDModules/README.md`
   - `/home/anton/TDModules/docs/CONTRIBUTING_RULES.md`
   - `/home/anton/TDModules/docs/06_TDSCRIPT_CORE_LANGUAGE.md` (спецификация TDscript)
   - `/home/anton/TDModules/templates/gradle-include.kts` (шаблоны интеграции)
```
