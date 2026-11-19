# 🤖 Универсальный системный промпт для AI-агентов экосистемы (TDModules Agent Directive)

> **Инструкция для пользователя:**
> Скопируйте текст ниже и отправьте его AI-агенту в соседней вкладке (`Tdroid`, `TDRobotics`, `TDnetScript`, `Drlmstudio`, `UniTest`, `SuperDriver`), чтобы подключить его к единому центру модулей.

---

```markdown
Ты работаешь в экосистеме проектов Антона (TD Ecosystem).
Все общие модули, архитектурные решения, алгоритмы шифрования, сетевые туннели и образовательные движки вынесены в центральный репозиторий:
📂 `/home/anton/TDModules`

### Твои правила взаимодействия с TDModules:
1. **Используй готовое**:
   - Не пиши заново свои реализации шифрования, обратных туннелей SSH, парсеров тестов или ротации LLM.
   - Подключай готовые проверенные модули из `/home/anton/TDModules/modules/`:
     - `core-security`: AES-GCM-256 (IV на пакет), RSA-OAEP 2048, XOR String Masking, RateLimiter.
     - `core-network`: TunnelManager (6 провайдеров, Direct LAN priority, 20s Heartbeat, Backoff+Jitter), NetworkUtils.
     - `core-ai`: FreeModelPool (ротация бесплатных LLM, Retry-After, кулдауны), PromptSanitizer, DeviceProfiler.
     - `core-education`: ConceptTermModels (якорь концептов, мультиязычность, SM-2 SRS), PolymorphicQuizModels (смена языка тестов на лету), UnifiedEducationalParser, PositionalShuffle.

2. **Правило вклада (ОБЯЗАТЕЛЬНО К ИСПОЛНЕНИЮ)**:
   Если тебе требуется доработать или расширить существующий модуль:
   - Внеси изменения в код в `/home/anton/TDModules/modules/`.
   - ОБЯЗАТЕЛЬНО обнови соответствующий документ в `/home/anton/TDModules/docs/` (опиши новый метод, параметры и пример).
   - Повысь версию в `/home/anton/TDModules/VERSION` по SemVer.
   - Добавь описание в `/home/anton/TDModules/CHANGELOG.md` с указанием твоего проекта.
   - Если изменение ломает обратную совместимость — заархивируй старый срез в `/home/anton/TDModules/releases/vX.Y.Z/`.

3. **Справка и документация**:
   Перед началом работы ознакомься с:
   - `/home/anton/TDModules/docs/CONTRIBUTING_RULES.md`
   - `/home/anton/TDModules/docs/01_SECURITY_AND_PROTECTION.md`
   - `/home/anton/TDModules/docs/02_NETWORK_AND_TUNNELS.md`
   - `/home/anton/TDModules/docs/03_FREE_LLM_POOL.md`
   - `/home/anton/TDModules/docs/04_POLYGLOT_QUIZ_AND_DICTIONARY.md`
   - `/home/anton/TDModules/templates/gradle-include.kts` (инструкция по импорту в Gradle).
```
