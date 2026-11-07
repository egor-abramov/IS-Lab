# Лабораторная работа № 2. Информационные системы

**Студент**: Абрамов Егор Денисович

**Группа**: P3319

**Вариант**: 3171.
## 1. Задание

Доработать систему из ЛР1: добавить атомарный импорт объектов из файла и историю импортов с разграничением доступа, новые ограничения уникальности в бизнес-логике и сценарий Apache JMeter для проверки параллельного создания, изменения, удаления и импорта.

## 2. UML-диаграмма изменений

Архитектура приложения:

![Диаграмма пакетов и сервисов ЛР2](img/diagram.png)

В ветке `lab-2` добавлены YAML-импорт списка организаций, история импортов. Добавлены регистрация, вход по JWT, роли, владельцы данных и ограничения доступа. Сервисный слой теперь проверяет уникальность `name`, непустого `fullName` и пары координат `(x, y)` у организаций. 
## 3. Исходный код и проверка JMeter

**Исходный код**: [ветка `lab-2`](https://github.com/egor-abramov/IS-Lab/tree/lab-2). 

### Где смотреть реализацию

| Что проверять | Классы и методы |
| --- | --- |
| Загрузка файла и атомарный импорт | [`OrganizationHandler.load`](../src/main/java/core/handler/OrganizationHandler.java), [`OrganizationImportService.load`, `parse`](../src/main/java/core/service/OrganizationImportService.java), [`OrganizationService.load`](../src/main/java/core/service/OrganizationService.java) с `@Transactional` |
| История успешных и ошибочных загрузок | [`ImportHistoryService.start`, `finish`, `history`](../src/main/java/core/service/ImportHistoryService.java), [`ImportOperationRepository.history`](../src/main/java/core/repository/ImportOperationRepository.java), [`ImportHistoryHandler.list`](../src/main/java/core/handler/ImportHistoryHandler.java) |
| Новые правила уникальности | [`OrganizationService.checkUniqueness`](../src/main/java/core/service/OrganizationService.java), [`OrganizationRepository.lockUniquenessCheck`, `findAllForUniqueness`](../src/main/java/core/repository/OrganizationRepository.java) |
| Конкурентные изменения одного объекта | [`OrganizationService.update`, `delete`](../src/main/java/core/service/OrganizationService.java), [`OrganizationRepository.findByIdForUpdate`](../src/main/java/core/repository/OrganizationRepository.java), [`CurrentUser.requireWrite`](../src/main/java/core/security/CurrentUser.java) |
| Пользователи и авторизация | [`AuthService.register`, `login`](../src/main/java/core/service/AuthService.java), [`AuthFilter.filter`](../src/main/java/core/security/AuthFilter.java), Gradle-задача `createAdmin` в [`build.gradle`](../build.gradle) |
| Интерфейс импорта | Функции `uploadOrganizations`, `loadImportHistory` в [`imports.js`](../src/main/webapp/js/imports.js) |
| Параллельный сценарий | Фазы `unique`, `foreign_edit_delete`, `edit_delete`, `import`, `verify` в [`jmeter/lab2.groovy`](../jmeter/lab2.groovy) |

**Пример импорта**: [`examples/organizations.yml`](../examples/organizations.yml) содержит три организации с вложенными координатами и адресами. Файл загружается через форму импорта. Повторная загрузка вызовет `409` из-за ограничений уникальности.

**Сценарий**: [`jmeter/lab2.jmx`](../jmeter/lab2.jmx), код запросов: [`jmeter/lab2.groovy`](../jmeter/lab2.groovy), результаты: [`jmeter/results.jtl`](../jmeter/results.jtl).

Сценарий создает двух пользователей. Они одновременно пытаются создать организации с одинаковым именем.
Затем владелец обновляет организацию, а другой пользователь одновременно пытается ее удалить и получает отказ. Отдельно две сессии владельца одновременно изменяют и удаляют ту же организацию. После этого пользователи одновременно запускают корректный импорт двух организаций и импорт с повторяющимся `fullName` внутри файла.
Завершающая проверка читает организации и историю через API.
#### Результаты запросов:

Прогон 07.11.2026: 10 выборок JMeter, 0 ошибок.

| Проверка | Результат |
| --- | --- |
| Два одновременных создания с одним `name` | `201` и `409`; сохранена одна организация |
| Обновление и удаление одного объекта двумя пользователями | владелец получил `200`, чужое удаление отклонено с `404` |
| Изменение и удаление одного объекта | удаление `204`, изменение после удаления `404`; объект отсутствует |
| Два одновременных импорта | корректный `201`, ошибочный `409`; добавлены 2 и 0 организаций соответственно |
| История | `SUCCESS` с `addedCount = 2` и `FAILED` с `addedCount = null` у соответствующих пользователей |

PostgreSQL работает с уровнем изоляции `READ COMMITTED`. 
Сервис берет общую транзакционную блокировку перед проверкой и записью. 
Для изменения и удаления одного объекта применяется блокировка строки `PESSIMISTIC_WRITE`. 
Проверка после прогона не выявила нарушения, поэтому повышать уровень изоляции всех транзакций не потребовалось.
