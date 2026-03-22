# Roadmap разработки: кастомный инвентарь и крафт

## 1. Цель
Довести подсистему инвентаря и крафта до релиза с предсказуемой серверной логикой, без дюпов/десинка, с ванильной семантикой слотов и совместимостью с другими модами.

## 2. Ключевые принципы
- Сервер - единственный источник правды.
- Любая операция инвентаря выполняется атомарно и проходит серверную валидацию.
- Используются стандартные Forge/vanilla `Slot` и `AbstractContainerMenu`.
- Ванильные индексы слотов сохраняются фиксированными (canonical mapping).
- `schemaVersion` и миграции вводятся с раннего этапа, а не в конце.
- `death -> corpse` реализуется только в финальной фазе.

## 3. Фаза A: Transactional Core
### Цель
Собрать безопасное серверное ядро инвентаря с защитой от гонок и дюпов.

### Задачи
- Реализовать `PlayerLoadout`, `EquipmentSlotType`, `StorageProfile`, `slotActive`.
- Зафиксировать независимые слои `CHEST` и `VEST`.
- Ввести `FACE`/`GLOVES` без storage-емкости.
- Реализовать capability + NBT + `schemaVersion` + мигратор v1.
- Реализовать canonical mapping индексов (vanilla фиксированные, dynamic отдельный диапазон).
- Реализовать per-player lock/queue для операций инвентаря.
- Реализовать серверный API: `beginLoadoutOp/endLoadoutOp`, `validateAndApplyClick`, `applyOverflow`.
- Реализовать overflow-транзакцию: `begin -> plan -> commit -> sync -> recover`.
- Зафиксировать partial stack правила: перенос максимально возможного, остаток в `dropToWorld`.
- Добавить базовый контейнер на `AbstractContainerMenu` со стандартными `Slot`.

### Риски
- Race condition между кликами и сменой экипировки.
- Потеря предметов при disconnect во время overflow.
- Ложное ощущение консистентности на клиенте до подтверждения сервера.

### Definition of Done
- Нет критичных дюпов в тестах `equip + shift-click + overflow`.
- Overflow детерминирован и восстанавлиется после relog/disconnect.
- Ванильные диапазоны индексов слотов стабильны.
- Capability имеет рабочее версионирование и миграцию.

## 4. Фаза B: Network + UI Stability
### Цель
Стабилизировать сетевое поведение и интерфейс без рассинхронизации.

### Задачи
- Добавить idempotency для `C2S_ClickCustomSlot` и `C2S_RequestCraft` (`requestId`, dedup TTL + GC).
- Добавить проверку `loadoutVersion` на каждую операцию.
- Реализовать Saga-оркестратор (минимум: overflow + craft) с компенсациями.
- Реализовать `requestCraft(player, craftId, requestId)` с резервированием стеков и компенсацией.
- Реализовать `InventoryScreen` (survival/adventure) и vanilla fallback для creative.
- Реализовать pending/rollback UX для отклоненных действий.
- Ввести debounce/aggregation для `S2C_LoadoutSync` (конфиг `50-200ms`).
- Реализовать `CraftScreen` с категориями и карточками.
- Добавить базовые метрики и логи отклоненных запросов.

### Риски
- Перегрузка сети при частых sync.
- UI-мигание из-за частых откатов клиентских действий.
- Ошибки порядка обработки запросов.

### Definition of Done
- Повторные C2S запросы не вызывают повторных списаний/выдач.
- Массовые быстрые клики не приводят к дюпам и десинку.
- В creative всегда ванильный UI, в survival/adventure кастомный.
- Sync-трафик снижен за счет debounce без потери консистентности.

## 5. Фаза C: Data Contracts + Content Scaling
### Цель
Сделать систему контента масштабируемой и устойчивой к ошибкам данных.
добавить тестовые крафты и предметы для экипировки

### Задачи
- Реализовать data-driven крафт (`craft_categories`, `craft_cards`).
- Ввести JSON schema validation + soft-disable fallback для некорректных объектов.
- Добавить error codes в логи загрузчика контента.
- Реализовать `ProtectionProfile` и `protection_profiles`.
- Зафиксировать `tags` с namespace (`modid:tag`) и `priority` для разрешения конфликтов.
- Добавить тесты на разные `armorValue` для разных предметов.

### Риски
- Некорректный JSON-контент ломает баланс или UX.
- Конфликтующие `tags` без четких приоритетов.

### Definition of Done
- Мир загружается даже при частично битом контенте (soft-disable с логом).
- Разные предметы дают разные значения защиты по профилям.
- Добавление нового предмета защиты не требует правок ядра.

## 6. Фаза D: QA Hardening + Release
### Цель
Финализировать релиз, миграции, совместимость и поздние механики.

### Задачи
- Провести полный regression SP/MP на длительных сессиях.
- Добавить CI pipeline: unit + concurrency + fuzz + smoke.
- Ввести мониторинг метрик (overflow, rollback, reject-rate, sync-size).
- Реализовать механику `death -> corpse with items` как финальную фичу.
- Протестировать corpse-сценарии: смерть, лутание, relog, очистка мира.
- Проверить совместимость с модами, читающими инвентарь как vanilla.
- Финализировать документацию и релизные заметки.

### Риски
- Регрессии при миграциях между версиями данных.
- Редкие MP-ошибки при высокой нагрузке.
- Ошибки целостности лута в corpse-механике.

### Definition of Done
- Стабильный релиз без критичных дюпов и потерь данных.
- Миграции между поддерживаемыми версиями проходят корректно.
- Corpse-механика стабильна в SP/MP.
- Сторонние моды читают инвентарь как ожидаемый стандартный инвентарь игрока.

## 7. Обязательный тестовый контур
- Unit: overflow (полный/частичный/нулевой), `schemaVersion` миграции, idempotency.
- Concurrency: `equip + shift-click + craft` при искусственных задержках.
- Property-based: инварианты сохранения предметов после произвольных последовательностей операций.
- Fuzz: случайные/битые JSON для `craft_cards` и `protection_profiles`.
- MP smoke: relog в середине overflow, массовые смены экипировки, repeated requests.
- Compatibility smoke: внешний мод читает inventory диапазоны без ошибок.

## 8. Порядок prompt-задач для Copilot Pro
1. `schemaVersion` + мигратор capability.
2. `PlayerLoadout`/`StorageProfile`/`slotActive` + canonical mapping.
3. Per-player lock/queue + транзакции overflow.
4. Partial stack правила + recovery при relog/disconnect.
5. Idempotency C2S (`requestId`, dedup TTL + GC) + проверка `loadoutVersion`.
6. Серверные API-контракты (`beginLoadoutOp`, `validateAndApplyClick`, `applyOverflow`, `requestCraft`).
7. Saga orchestration + компенсации для overflow/craft.
8. `AbstractContainerMenu` и slot validation на стандартных `Slot`.
9. `InventoryScreen` + pending/rollback + creative fallback.
10. Data-driven крафт + JSON schema validation + soft-disable fallback.
11. `ProtectionProfile` (`tags` namespace, `priority`) + тесты баланса.
12. CI (unit/concurrency/property-based/fuzz/smoke), метрики и релизный regression.
13. `death -> corpse with items` как финальная задача.

## 9. Milestones
- M1: Transactional server core + schemaVersion + canonical mapping.
- M2: Network/UI stability (idempotency, version checks, debounce sync).
- M3: Data contracts + scaling armor profiles.
- M4: QA hardening, corpse и релиз.

## 10. Статус решений
- Overflow-policy: утверждена (`moveToAvailable -> dropToWorld`).
- `CHEST`/`VEST`: утверждены как независимые слои.
- `FACE`/`GLOVES`: без storage-емкости.
- Сервер: единственный источник правды.
- Armor-параметры: разные по предметам, data-driven и масштабируемые.
- Corpse: зафиксирован как финальная фича конца разработки.
