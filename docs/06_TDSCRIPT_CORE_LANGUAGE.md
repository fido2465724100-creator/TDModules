# ⚡ TDscript Core — Универсальный детерминированный язык сценариев и Host SPI

Модуль **`core-tdscript`** (`com.tdscript.core.*`) — это единое эталонное ядро легковесного языка **TDscript**, используемое во всей экосистеме проектов:
- **`Tdroid`**: терминальный лаунчер и текстовая ОС (0 KB VRAM, чистый TUI-вывод через `HostSPI.print`).
- **`TDnetScript`**: промышленная автоматизация, SCADA, Modbus, опрос оборудования и сетевые сценарии.
- **`TDRobotics`**: управление роботами, кинематика, реакция на сенсоры и схемотехника.
- **`SuperDriver`**: оркестрация фоновых демонов и автоматизация сервисов.

---

## 🏛️ Ключевые архитектурные принципы

1. **0 KB VRAM / Headless**:
   Ядро не содержит зависимостей от графических подсистем (Android View, Compose, Swing). Вывод и взаимодействие с пользователем делегируются абстракции `HostSPI`.
2. **Детерминизм вычислений**:
   Одинаковый исходный код при равном начальном состоянии символьной таблицы гарантирует идентичные результаты на Android-смартфоне, Linux-сервере или контроллере робота.
3. **Эшелонированная безопасность (Capabilities)**:
   Любые операции ввода-вывода (сеть, Modbus, вибрация, фонарик, доступ к системным службам) проверяются через битовую маску мандатов безопасности. При отсутствии права вызов блокируется, а в диагностический лог хоста пишется `PERMISSION_DENIED`.

---

## 🔒 Битовые маски прав (Capabilities)

```kotlin
object Capabilities {
    const val NET_READ   = 0x0001 // Разрешено чтение по сети (HTTP GET, Ping)
    const val NET_SCAN   = 0x0002 // Сканирование портов и Wake-on-LAN
    const val PLC_READ   = 0x0004 // Опрос регистров Modbus PLC
    const val PLC_WRITE  = 0x0008 // Запись в регистры Modbus PLC
    const val BT_ACCESS  = 0x0010 // Доступ к Bluetooth SPP / BLE UART
    const val HARDWARE   = 0x0020 // Аппаратные функции (фонарик, вибрация, сирена)
    const val UI_CORE    = 0x0040 // Вывод уведомлений (Toast, Log)
    const val BACKGROUND = 0x0080 // Фоновое исполнение в сервисе
}
```

---

## 🔌 Контракт Host SPI

Любой проект-хост реализует интерфейс `HostSPI`:

```kotlin
interface HostSPI {
    val stdoutLines: MutableList<String>
    val diagnosticLines: MutableList<String>
    var capabilities: Int

    fun checkCapability(capBit: Int, capName: String): Boolean
    fun print(msg: String)
    
    // Сетевые вызовы
    fun netHttpGet(url: String): String?
    fun netPing(host: String): Float
    fun netScanPort(host: String, port: Int): Boolean
    fun netWol(mac: String): Boolean

    // Промышленная автоматика (PLC / Modbus)
    fun plcModbusRead(host: String, reg: Int): Int
    fun plcModbusWrite(host: String, reg: Int, value: Int): Boolean

    // Аппаратные датчики и приводы
    fun phoneAlarm(volume: Int): Boolean
    fun phoneVibrate(ms: Long): Boolean
    fun phoneTorch(state: Boolean): Boolean
    fun phoneBeep(ms: Int): Boolean

    // Интерфейс пользователя
    fun uiToast(msg: String): Boolean
    fun uiLog(msg: String): Boolean
}
```

---

## 💻 Пример использования ядра в проекте

```kotlin
import com.tdscript.core.*

// 1. Создаем хост с нужными правами
val host = object : StandardHostSPI(
    capabilities = Capabilities.NET_READ or Capabilities.UI_CORE
) {
    override fun print(msg: String) {
        // В Tdroid: вывод в символьный экран TUI
        // В терминале: println(msg)
        super.print(msg)
    }
}

// 2. Исполняем сценарий
val code = """
    let target = "192.168.1.1"
    let latency = NET.ping(target)
    if (latency >= 0) {
        print("Хост доступен! RTT: " + latency + " ms")
    } else {
        print("Хост недоступен")
    }
""".trimIndent()

val lexer = Lexer(code)
val parser = Parser(lexer.tokenize())
val program = parser.parse()

val interpreter = Interpreter(host)
interpreter.execute(program)
```

---

## 📖 Стандарты синтаксиса TDscript (SPEC-TDS-CORE-1.0.1)

- **Переменные**: `let a = 10` (константа), `var b = 20` (изменяемая).
- **Типы**: `Int`, `Double`, `String`, `Boolean`, `List` (`[1, 2, 3]`), `Map` (`{"key": "value"}`).
- **Операторы**: `+`, `-`, `*`, `/`, `%`, `==`, `!=`, `<`, `<=`, `>`, `>=`, `&&`, `||`, `!`.
- **Ветвления и циклы**: `if (cond) { ... } else { ... }`, `while (cond) { ... }`.
- **Комментарии**: `//` или `#`.
- **Автоматический перенос строк**: при незавершенном операторе в конце строки (`+`, `,`, `&&`).
