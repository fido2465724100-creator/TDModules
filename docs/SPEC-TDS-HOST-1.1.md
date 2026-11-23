# 🔌 SPEC-TDS-HOST-1.1 — Спецификация Transport API, CapabilityScope и Fail-Safe

## 1. Архитектурная модель
Слой хоста (Host Runtime) изолирует виртуальную машину от конкретной операционной системы (Android, Linux x86_64/ARM, Windows) и аппаратуры.

---

## 2. Гранулярный контроль доступа: `CapabilityScope`

Для предотвращения атак типа "захват шины" или случайной порчи регистров соседних устройств битовые маски `Capabilities` дополняются объектом **`CapabilityScope`**:

```kotlin
data class CapabilityScope(
    // Разрешенные 7-битные адреса на шине I2C (например, setOf(0x76, 0x77))
    val allowedI2cAddresses: Set<Int> = emptySet(),

    // Разрешенные пары VendorID:ProductID для USB-Serial адаптеров
    val allowedUsbVidPid: Set<Pair<Int, Int>> = emptySet(),

    // Разрешенные UUID сервисов BLE GATT
    val allowedBleServiceUuids: Set<String> = emptySet(),

    // Разрешенные диапазоны регистров Modbus [startRegister..endRegister]
    val allowedModbusRegisterRanges: List<IntRange> = emptyList(),

    // Разрешенные порты TCP/UDP и диапазоны IP-адресов
    val allowedNetworkHosts: Set<String> = emptySet()
)
```

### 2.1 Правило проверки области действия в `HostSPI`
Перед вызовом любой транзакции низкого уровня хост обязан проверить **два условия**:
1. Наличие соответствующего битового флага в маске `capabilities`.
2. Попадание целевого адреса или идентификатора в `CapabilityScope`.
При несоблюдении хотя бы одного условия транзакция немедленно отклоняется с записью в `diagnosticLines`:
`SecurityException: SCOPE_VIOLATION: Address 0x68 is not allowed in I2C scope [0x76, 0x77]`.

---

## 3. Транспортные интерфейсы и обнаружение (`probe()`)

### 3.1 Метод `HostSPI.probe()`
Позволяет среде исполнения и AI-ассистенту получить достоверный снимок фактически подключённых аппаратных шин:
```kotlin
fun probe(): List<TransportAdapterInfo>
```
Возвращает список адаптеров со статусом доступности:
* `TransportType.I2C`: например, `/dev/i2c-1` на Raspberry Pi или Android I2C-bridge через USB.
* `TransportType.SERIAL_USB`: обнаруженные USB-UART чипы (FTDI, CH340, CP2102) с их VID:PID.
* `TransportType.BLE`: состояние адаптера Bluetooth Low Energy.
* `TransportType.NETWORK_TCP`: сетевые интерфейсы (WLAN, LAN).

---

## 4. Механизм отказоустойчивости (Fail-Safe Watchdog)

В робототехнике зависание управляющего сценария может привести к физической аварии.
Хост предоставляет контракт сторожевого таймера:
1. `fun watchdogFeed(): Boolean` — регулярный сброс таймера из цикла VM.
2. `fun emergencyStop(): Unit` — безусловный аварийный останов. Если в течение заданного интервала (по умолчанию 500 мс) сторожевой таймер не сброшен или в VM произошла критическая ошибка:
   - Все ШИМ-каналы и выходы моторов принудительно сбрасываются в `0`.
   - Реле и нагреватели отключаются.
   - В журнал диагностики пишется `EMERGENCY_STOP_TRIGGERED`.
