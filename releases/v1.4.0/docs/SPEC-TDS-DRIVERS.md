# 🚗 SPEC-TDS-DRIVERS — Спецификация пакетов драйверов микросхем и устройств

## 1. Архитектура драйвера (Стек Driver -> Protocol -> Transport)

Драйверы конкретных датчиков, контроллеров и микросхем пишутся на самом **TDscript** и распространяются в виде защищённого пакета:
```
+--------------------------------------------------------------+
| Driver: Bosch BME280 (Логика расчёта компенсации температуры) |
+--------------------------------------------------------------+
| Protocol / Bus API: I2C Register Read/Write                  |
+--------------------------------------------------------------+
| Transport API: HostSPI (I2C Bus Controller / USB-I2C Bridge) |
+--------------------------------------------------------------+
```

---

## 2. Формат манифеста (`manifest.json`)

Каждый драйвер обязан содержать манифест `manifest.json`:

```json
{
  "$schema": "https://tdscript.org/schemas/v1/driver-manifest.json",
  "name": "bosch.bme280",
  "version": "1.0.0",
  "min_core_version": "1.1.0",
  "min_host_api": "1.1.0",
  "title": "Bosch BME280 Environmental Sensor",
  "vendor": "Bosch Sensortec",
  "target_chip": "BME280",
  "transport": {
    "type": "I2C",
    "default_address": "0x76",
    "alternate_addresses": ["0x77"],
    "max_bus_frequency_khz": 3400
  },
  "required_capabilities": [
    "I2C_READ",
    "I2C_WRITE"
  ],
  "capability_scope": {
    "allowed_i2c_addresses": ["0x76", "0x77"]
  },
  "entrypoint": "bme280.tds",
  "checksum_sha256": "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
  "signature": {
    "algorithm": "ED25519",
    "signer": "tdscript-official-registry",
    "public_key": "3d4f...9a12",
    "signature_hex": "8f1a...c4b2"
  }
}
```

---

## 3. Требования безопасности при установке пакетов
1. **Проверка совместимости**: `min_core_version` проверяется средой исполнения. Если версия ядра ниже требуемой, установка отклоняется с ошибкой `TDS_E201_INCOMPATIBLE_CORE_VERSION`.
2. **Криптографическая верификация**: Сигнатура драйвера проверяется через `core-security` (`SessionCrypto`). Неподписанные или скомпрометированные драйверы не допускаются к запуску с правами прямого доступа к шинам.
3. **Изоляция области действия**: Перед запуском среда ограничивает `HostSPI` строго списком `capability_scope` из манифеста.
