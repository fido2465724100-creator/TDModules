# 🤖 SPEC-TDS-AI — AI-Native Protocol & API Discovery Specification

## 1. Концепция AI-Native среды
Платформа TDscript спроектирована для прямого взаимодействия с LLM (как локальными через `Drlmstudio` / `LM Studio`, так и облачными из пула `core-ai/FreeModelPool`).

---

## 2. Протокол API Discovery (Машиночитаемый снимок хоста)

Хост или IDE по вызову или API-запросу формирует снимок `HostDiscoverySnapshot`:

```json
{
  "runtime": {
    "engine": "TDscript Core",
    "version": "1.1.0",
    "host_platform": "Linux x86_64",
    "device_specs": {
      "total_ram_mb": 16384,
      "cpu_cores": 8,
      "is_low_end": false
    }
  },
  "capabilities": {
    "granted_mask": "0x03FF",
    "active_capabilities": [
      "NET_READ", "PLC_READ", "PLC_WRITE", "I2C_READ", "I2C_WRITE"
    ],
    "scope": {
      "allowed_i2c_addresses": ["0x76", "0x77"],
      "allowed_modbus_ranges": ["100..200"]
    }
  },
  "connected_transports": [
    {
      "id": "i2c_bus_1",
      "type": "I2C",
      "available": true,
      "details": "/dev/i2c-1 (400 kHz)"
    },
    {
      "id": "modbus_tcp_gw",
      "type": "NETWORK_TCP",
      "available": true,
      "details": "192.168.1.50:502"
    }
  ],
  "installed_drivers": [
    {
      "name": "bosch.bme280",
      "version": "1.0.0",
      "vendor": "Bosch Sensortec"
    }
  ]
}
```

---

## 3. Формат машиночитаемых ошибок компилятора и рантайма

При обнаружении ошибки компилятор возвращает структурированный JSON:
```json
{
  "status": "ERROR",
  "compiler_version": "1.1.0",
  "error_code": "TDS_E104_I2C_SCOPE_VIOLATION",
  "file": "bme280_read.tds",
  "line": 18,
  "column": 9,
  "message": "Access to I2C slave address 0x68 is prohibited by active capability scope.",
  "expected": "Transaction to one of: [0x76, 0x77]",
  "actual": "0x68",
  "suggestion": "Check jumper SDO on BME280 sensor: pull SDO to GND (0x76) or VDDIO (0x77), or update manifest.json capability_scope."
}
```
Этот формат позволяет LLM автоматически исправить код пользователя или манифест пакета без участия человека.
