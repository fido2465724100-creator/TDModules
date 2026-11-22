# 📚 Полиглот-тестирование и словари терминов (core-education)

## Назначение
В проектах `UniTest`, `Tdroid`, `Drlmstudio` и `SuperDriver` постоянно возникали две фундаментальные проблемы:
1. **Проблема жестких пар слов в словарях**: Традиционные приложения хранят пары «Язык А ↔ Язык Б» (например, английский ↔ русский). Если специалист (например, врач) годами формировал базу медицинской латыни и терминов на английском, а затем решил учить немецкий или испанский язык — ему приходилось заново вручную создавать отдельную базу, при этом полностью терялся накопленный интервальный прогресс повторений (SRS).
2. **Проблема жестко зашитого языка в тестах**: Тест по физике или медицине, составленный на немецком языке, невозможно было переключить на английский или русский: требовалось создавать отдельные дубликаты тестов, сбрасывалась статистика попыток, а ключи вариантов расходились.

Модуль `core-education` полностью решает обе проблемы через две инновационные концепции:
- **`Concept-Pivot Terminology Model`** (семантический якорь терминов).
- **`Polymorphic Multilingual Quiz Engine`** (полиморфное тестирование с инвариантными ключами).

---

## 1. Модель семантического якоря (Concept-Pivot Architecture)

Каждый термин в базе — это не пара строк, а независимый концептуальный объект `ConceptTerm`:

```
                       [Concept ID: "med.cardio.infarction"]
                        English Anchor: "myocardial infarction"
                                    |
          +-------------------------+-------------------------+
          |                         |                         |
        [ru]                      [de]                      [es]
 "инфаркт миокарда"        "Myokardinfarkt"          "infarto de miocardio"
  def: некроз участка...    def: Gewebsnekrose...     def: necrosis tisular...
```

### Преимущества для врача / студента:
- Если пользователь учил термины на английском, а теперь выбирает немецкий: система **мгновенно проецирует** всю терминологическую базу на немецкий язык.
- Прогресс запоминания (SRS) хранится в привязке к концепту и целевому языку в структуре `SpacedRepetitionData`.
- **Счетчики успеваемости**: поля `correctCount` и `wrongCount` хранятся раздельно для каждого целевого языка, что позволяет отображать точную цветовую индикацию (зеленый/красный) в интерфейсе для каждого языка без взаимного затирания. Метод `recordResult(isCorrect)` синхронно обновляет счетчики и интервалы SM-2.
- **Алгоритм SuperMemo SM-2**:
  - Фактор легкости `easeFactor` (начинается с 2.5, не падает ниже 1.3).
  - Интервалы: 1 день -> 6 дней -> `(interval * easeFactor)`.
  - При ошибке (`quality < 3`) цикл сбрасывается на 1 день, обеспечивая надежное закрепление сложных терминов.

---

## 2. Полиморфные мультиязычные тесты (Polymorphic Quiz)

В тестах применяется **Key-Invariant Architecture**:

```kotlin
val question = PolymorphicQuizQuestion(
    id = "phys_01",
    correctOptionKey = "opt_correct", // Инвариантный ключ правильного ответа
    localizations = mutableMapOf(
        "de" to QuestionLocalization(
            prompt = "Was besagt der zweite Hauptsatz der Thermodynamik?",
            options = mapOf(
                "opt_correct" to "Die Entropie eines isolierten Systems nimmt niemals ab.",
                "opt_distractor_1" to "Energie kann weder erzeugt noch vernichtet werden.",
                "opt_distractor_2" to "Der absolute Nullpunkt kann erreicht werden."
            ),
            explanation = "Entropiesatz nach Clausius."
        ),
        "en" to QuestionLocalization(
            prompt = "What does the second law of thermodynamics state?",
            options = mapOf(
                "opt_correct" to "The entropy of an isolated system never decreases.",
                "opt_distractor_1" to "Energy can neither be created nor destroyed.",
                "opt_distractor_2" to "Absolute zero can be fully reached."
            ),
            explanation = "Entropy principle by Clausius."
        ),
        "ru" to QuestionLocalization(
            prompt = "Что гласит второй закон термодинамики?",
            options = mapOf(
                "opt_correct" to "Энтропия изолированной системы никогда не убывает.",
                "opt_distractor_1" to "Энергия не может быть создана или уничтожена.",
                "opt_distractor_2" to "Абсолютный ноль может быть полностью достигнут."
            ),
            explanation = "Принцип возрастания энтропии Клаузиуса."
        )
    ),
    quizType = EducationalQuizType.SCIENCE_3
)
```

### Переключение языка прямо во время теста:
```kotlin
// Ученик начал тест на немецком:
val deQ = question.render(lang = "de")
println(deQ.prompt) // "Was besagt der zweite Hauptsatz...?"

// Решил переключиться на английский:
val enQ = question.render(lang = "en")
println(enQ.prompt) // "What does the second law...?"

// Проверка ответа:
val isCorrect = question.isCorrectAnswer("opt_correct") // true независимо от языка!
```

---

## 3. Всеядный парсер (`UnifiedEducationalParser`)

Автоматически распознает и преобразует любые входные форматы без ручной разметки:
1. **Двухколоночные данные**: TSV (Anki), CSV, строки вида `Термин :: Определение`, `Слово - Перевод`, `Вопрос | Ответ`.
2. **Опросники с маркерами**: блоки с ответами `*Верный`, `+Правильный`, `[x] Отмеченный`, `A)`, `1)`.
3. **Автоматическая генерация тестов из словарей**: при парсинге двух колонок парсер берет случайные уникальные значения из других строк того же набора и формирует 3 правдоподобных дистрактора (неправильных варианта) для формата `STANDARD_4` или 2 дистрактора для формата `SCIENCE_3`.
4. **Защита от позиционной предвзятости (`PositionalShuffle`)**: если автор теста всегда ставил верный ответ на первое место (`>65%` совпадений), алгоритм детектирует это и рандомизирует порядок вариантов.
