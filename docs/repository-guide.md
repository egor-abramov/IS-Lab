# Гайд по пакету `core.repository`

Разбор всего кода в `src/main/java/core/repository`: откуда берется `EntityManager`, кто управляет транзакциями и что делает каждая строчка Criteria API.

## Содержание

1. [Общая картина](#1-общая-картина)
2. [Шпаргалка по JPA](#2-шпаргалка-по-jpa)
3. [Шпаргалка по Criteria API](#3-шпаргалка-по-criteria-api)
4. [Что делает контейнер](#4-что-делает-контейнер)
5. [BaseRepository](#5-baserepository)
6. [Coordinates, Location, Address](#6-coordinatesrepository-locationrepository-addressrepository)
7. [OrganizationRepository](#7-organizationrepository)
8. [SpecialOpsRepository](#8-specialopsrepository)
9. [Подводные камни](#9-подводные-камни)

---

## 1. Общая картина

```
Handler (REST)  ->  Service (@Transactional)  ->  Repository  ->  EntityManager (EclipseLink)  ->  PostgreSQL
```

- **Service** проверяет данные, кидает понятные исключения и рассылает уведомления по WebSocket. Каждый его публичный метод выполняется в одной транзакции.
- **Repository** только ходит в БД: найти, сохранить, посчитать, удалить. Про транзакции он ничего не знает.
- **Контейнер (WildFly)** создает `EntityManager`, держит пул соединений к БД и открывает/закрывает транзакции.

Файлы пакета:

| Файл | Что делает |
|---|---|
| `BaseRepository` | Получает `EntityManager` от контейнера, содержит общие методы: `save`, `delete`, `findAll`, `countReferences`, `replaceReferences` |
| `CoordinatesRepository`, `LocationRepository`, `AddressRepository` | Поиск вложенных объектов и удаление с заменой ссылок |
| `OrganizationRepository` | Поиск и удаление организаций, страница с фильтром и сортировкой |
| `SpecialOpsRepository` | Вызов функций БД для специальных операций |

---

## 2. Шпаргалка по JPA

| Понятие | Что это |
|---|---|
| **Entity** | Класс с `@Entity`, одна строка таблицы = один объект (`Organization`, `Address`, ...) |
| **Persistence unit** | Настройки из `META-INF/persistence.xml`: провайдер, datasource, список сущностей, свойства. У нас он называется `lab1` |
| **`EntityManager` (EM)** | Через него читаем и пишем сущности |
| **Persistence context** | Внутренний "кэш" EM: все сущности, которые он загрузил или сохранил в текущей транзакции. Такие сущности называются **managed**: их изменения сами уйдут в БД при коммите |
| **Detached** | Сущность после конца транзакции. Это просто Java-объект, его изменения в БД сами не попадут |
| **Транзакция** | Группа операций, которая выполняется целиком или откатывается целиком |
| **Flush** | Момент, когда EM отправляет накопленные `insert`/`update`/`delete` в БД. По умолчанию перед коммитом, вручную через `em.flush()` |

Основные методы `EntityManager`, которые встречаются в коде:

| Метод | SQL примерно | Комментарий |
|---|---|---|
| `em.find(Cls, id)` | `select ... where id = ?` | Вернет `null`, если строки нет |
| `em.merge(obj)` | `insert` или `update` | Если `id == null`, будет insert, иначе update. **Возвращает managed-объект**, который может быть не тем, что передали |
| `em.remove(obj)` | `delete ... where id = ?` | Объект должен быть managed, поэтому перед этим делаем `find` |
| `em.flush()` | выполнить накопленные изменения | Транзакция при этом не коммитится |
| `em.createQuery(...)` | любой запрос | Создает запрос из JPQL-строки или из Criteria-объекта |
| `em.createStoredProcedureQuery(name)` | вызов функции БД | Используется в `SpecialOpsRepository` |

---

## 3. Шпаргалка по Criteria API

### Зачем он нужен

Запрос можно написать строкой на JPQL:

```java
em.createQuery("select o from Organization o where o.name = :name order by o.id", Organization.class)
```

Можно тот же запрос **собрать из Java-объектов**. Это и есть Criteria API:

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Organization> query = cb.createQuery(Organization.class);
Root<Organization> o = query.from(Organization.class);
query.where(cb.equal(o.get("name"), name))
     .orderBy(cb.asc(o.get("id")));
em.createQuery(query).getResultList();
```

Чем Criteria удобнее строк:

- запрос можно собирать по кусочкам и по условию (`if` фильтр задан, добавить `where`), не склеивая строки;
- значения передаются как значения, а не как кусок текста запроса, поэтому SQL-инъекции не бывает;
- один и тот же кусок (например, условие фильтра) можно переиспользовать в двух запросах.

Минус: код длиннее и сначала читается тяжелее.

### Главные объекты

| Объект | Аналог в JPQL / SQL | Как получить |
|---|---|---|
| `CriteriaBuilder cb` | "фабрика" всего: условий, функций, сортировок | `em.getCriteriaBuilder()` |
| `CriteriaQuery<R> query` | весь `select ... from ... where ... order by` | `cb.createQuery(R.class)`, где `R` это тип **результата** |
| `CriteriaUpdate<T> update` | `update T set ... where ...` | `cb.createCriteriaUpdate(T.class)` |
| `Root<T> root` | `from T root` | `query.from(T.class)` |
| `Path` | `root.field`, `root.field.id` | `root.get("field")`, `root.get("field").get("id")` |
| `Predicate` | условие в `where` | `cb.equal(...)`, `cb.or(...)`, `cb.greaterThan(...)` |
| `Order` | элемент `order by` | `cb.asc(path)`, `cb.desc(path)` |

### Как читать Criteria-код

Переводи каждую строку в кусок JPQL:

| Java | JPQL |
|---|---|
| `cb.createQuery(Long.class)` | результат запроса будет `Long` |
| `Root<Organization> o = query.from(Organization.class)` | `from Organization o` |
| `query.select(cb.count(o))` | `select count(o)` |
| `o.get("coordinates").get("id")` | `o.coordinates.id` |
| `cb.equal(a, b)` | `a = b` |
| `cb.or(p1, p2)` | `p1 or p2` |
| `cb.lower(x)` | `lower(x)` |
| `cb.locate(x, "abc")` | `locate('abc', x)`: позиция подстроки, `0` если не нашлась |
| `cb.greaterThan(x, 0)` | `x > 0` |
| `path.in(list)` | `path in (...)` |
| `cb.conjunction()` | `1 = 1` (всегда истина) |
| `cb.disjunction()` | `1 = 0` (всегда ложь) |
| `query.where(p)` | `where p` |
| `query.orderBy(o1, o2)` | `order by o1, o2` |
| `update.set(path, value)` | `set path = value` |

И отдельно настройки самого запроса, это уже не Criteria, а `TypedQuery`:

| Java | SQL |
|---|---|
| `.setFirstResult(n)` | `offset n` |
| `.setMaxResults(n)` | `limit n` |
| `.getResultList()` | выполнить, вернуть список |
| `.getSingleResult()` | выполнить, вернуть ровно одно значение |
| `.executeUpdate()` | выполнить `update`/`delete`, вернуть число затронутых строк |

### Про строки `"id"`, `"coordinates"`

`root.get("coordinates")` принимает **имя поля Java-класса**, а не имя колонки. Поле `coordinates` в `Organization` лежит в колонке `coordinates_id`, но в Criteria пишем `"coordinates"`. Если ошибиться в имени, получишь `IllegalArgumentException` при выполнении, компилятор не подскажет.

Можно сделать проверку на этапе компиляции через статическую метамодель (`Organization_.coordinates`), но для этого нужен генератор метамодели. В проекте его нет, поэтому используются строки.

---

## 4. Что делает контейнер

Своего кода для подключения к БД и транзакций в проекте нет: все делает WildFly по конфигурации.

### Datasource

`config/lab1-ds.xml` это шаблон. При сборке (`./gradlew war`) gradle подставляет в него url, логин и пароль из `config/<profile>.properties` и кладет в war как `WEB-INF/lab1-ds.xml`. При деплое WildFly читает этот файл и создает пул соединений с JNDI-именем `java:jboss/datasources/lab1`.

Драйвер PostgreSQL лежит не в war, а рядом отдельным деплойментом `postgresql.jar`. WildFly регистрирует его как JDBC-драйвер с именем `postgresql.jar`, и `lab1-ds.xml` на него ссылается. Если положить драйвер внутрь того же war, WildFly не успевает зарегистрировать его до создания datasource, и деплой падает.

### Persistence unit

```xml
<persistence-unit name="lab1" transaction-type="JTA">
    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
    <jta-data-source>java:jboss/datasources/lab1</jta-data-source>
    ...
    <property name="jboss.as.jpa.providerModule" value="application"/>
    <property name="eclipselink.target-server" value="JBoss"/>
```

| Настройка | Смысл |
|---|---|
| `transaction-type="JTA"` | транзакциями управляет сервер (Jakarta Transactions), а не код через `em.getTransaction()` |
| `<provider>` | ORM-провайдер: EclipseLink |
| `<jta-data-source>` | откуда брать соединения: datasource из `lab1-ds.xml` |
| `jboss.as.jpa.providerModule=application` | WildFly по умолчанию использует Hibernate. Это свойство говорит ему: провайдер (EclipseLink) лежит в самом приложении, в `WEB-INF/lib` |
| `eclipselink.target-server=JBoss` | EclipseLink должен присоединяться к транзакциям сервера. Без этого он не узнает о `begin`/`commit` контейнера |

### `@PersistenceContext`

```java
@PersistenceContext(unitName = "lab1")
EntityManager em;
```

Контейнер подставляет сюда не настоящий `EntityManager`, а **прокси**. При каждом вызове `em.find(...)`, `em.merge(...)` и т.д. прокси смотрит, какая транзакция сейчас идет, и передает вызов в `EntityManager`, привязанный к этой транзакции. Поэтому:

- одно поле `em` безопасно использовать из многих потоков: у каждого запроса своя транзакция и свой настоящий EM;
- все вызовы репозиториев внутри одного метода сервиса работают с **одним** persistence context: объект, найденный в `findById`, будет тем же объектом, что потом попадет в `merge`;
- закрывать EM не нужно, контейнер закроет его в конце транзакции.

### `@Transactional`

На классах `OrganizationService`, `CoordinatesService`, `AddressService`, `LocationService` стоит `@Transactional`. Это CDI-интерцептор: контейнер оборачивает каждый публичный метод сервиса в код, который делает примерно то же, что раньше делал наш `inTransaction`:

```
begin
    вызвать метод сервиса
commit                        (если метод завершился нормально)
rollback и пробросить ошибку  (если вылетело RuntimeException)
```

Пример: `CoordinatesService.delete(id, replacementId)` вызывает `findById`, `countUsages`, `findById(replacementId)` и `deleteWithReplace`. Все это одна транзакция. Если удаление упадет, откатится и замена ссылок.

Еще пример: при обновлении организации сервис меняет поля найденного (managed) объекта, а потом выясняется, что адреса с таким id нет, и вылетает `InvalidDataException`. Транзакция откатывается, и измененные поля в БД не попадают.

`SpecialOpsService` без `@Transactional`: он только читает. Вне транзакции контейнер на каждый вызов `em` создает временный EM и сразу его закрывает.

### Уведомления после коммита

Сервисы вызывают `notifier.broadcast("organizations")` еще **внутри** транзакции. Если сразу отправить сообщение в WebSocket, клиент может успеть запросить данные до коммита и получить старые. Поэтому `UpdateNotifier` не отправляет сообщение сразу, а бросает CDI-событие:

```java
void send(@Observes(during = TransactionPhase.AFTER_SUCCESS) Update update)
```

`AFTER_SUCCESS` значит: контейнер вызовет этот метод только после успешного коммита. При откате сообщение не уйдет вовсе.

---

## 5. BaseRepository

Абстрактный родитель всех репозиториев. Сам он не бин, бинами являются наследники с `@ApplicationScoped`.

```java
@PersistenceContext(unitName = "lab1")
EntityManager em;
```

См. [раздел 4](#persistencecontext). Поле без модификатора доступа (package-private), поэтому наследники из того же пакета пишут просто `em.find(...)`.

### `findAll`: все объекты типа, отсортированные по id

```java
protected <T> List<T> findAll(Class<T> type) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<T> query = cb.createQuery(type);   // результат: T
    Root<T> root = query.from(type);                 // from T root
    query.orderBy(cb.asc(root.get("id")));           // order by root.id asc
    return em.createQuery(query).getResultList();
}
```

`select` не указан: если его нет, выбирается сам `root`, то есть вся сущность.

Итоговый JPQL для `findAll(Address.class)`:

```sql
select root from Address root order by root.id asc
```

Метод обобщенный (`<T>`), поэтому один и тот же код работает для `Address`, `Location` и `Coordinates`.

### `save`: сохранить объект

```java
public <T> T save(T entity) {
    T saved = em.merge(entity);
    em.flush();
    return saved;
}
```

- `merge` делает insert (если `id == null`) или update и возвращает managed-объект.
- `flush` сразу отправляет SQL в БД, не дожидаясь коммита. Это нужно по двум причинам:
  1. `id` генерирует база (`GenerationType.IDENTITY`), и он появится у объекта только после реального `insert`. Сервис превращает объект в DTO еще до коммита, и без `flush` в ответе был бы `"id": null`.
  2. Если БД отклонит данные (нарушение ограничения), ошибка вылетит здесь как обычное `PersistenceException`, и ее поймает `PersistenceExceptionMapper`. Без `flush` она вылетела бы при коммите внутри интерцептора уже завернутой в `TransactionalException`, и клиент получил бы 500.

Метод `public`, поэтому сервисы вызывают `repository.save(c)` у любого репозитория.

### `delete`: удалить объект по id

```java
protected boolean delete(Class<?> type, long id) {
    Object entity = em.find(type, id);
    if (entity == null) {
        return false;
    }
    em.remove(entity);
    em.flush();
    return true;
}
```

Возвращает `false`, если удалять нечего. `flush` здесь по второй причине из `save`: если на объект еще есть ссылки, БД не даст удалить строку из-за внешнего ключа, и ошибка должна вылететь сразу.

### `countReferences`: сколько объектов ссылаются на данный id

```java
protected long countReferences(Class<?> type, long id, String... references) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);       // результат: Long
    Root<?> root = query.from(type);                              // from type root
    Predicate[] conditions = Arrays.stream(references)
            .map(reference -> cb.equal(root.get(reference).get("id"), id))
            .toArray(Predicate[]::new);                           // [root.ref1.id = id, root.ref2.id = id, ...]
    query.select(cb.count(root))                                  // select count(root)
            .where(cb.or(conditions));                            // where cond1 or cond2 ...
    return em.createQuery(query).getSingleResult();
}
```

`String... references` это varargs: можно передать одно имя поля или несколько.

Пример из `AddressRepository`:

```java
countReferences(Organization.class, id, "officialAddress", "postalAddress");
```

Получится:

```sql
select count(root) from Organization root
where root.officialAddress.id = :id or root.postalAddress.id = :id
```

Stream превращает каждое имя поля в условие `root.<поле>.id = id`, а `cb.or(...)` объединяет их через `or`.

`root.get(reference).get("id")`: сначала переходим по связи `@ManyToOne` (`root.officialAddress`), потом берем у связанного объекта поле `id`. EclipseLink поймет, что это просто колонка `official_address_id`, и join делать не будет.

### `replaceReferences`: перевесить ссылки на другой объект

```java
protected <T> void replaceReferences(Class<T> type, String reference, long oldId, Object replacement) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaUpdate<T> update = cb.createCriteriaUpdate(type);       // update T
    Root<T> root = update.from(type);
    update.set(root.get(reference), replacement)                    // set root.reference = replacement
            .where(cb.equal(root.get(reference).get("id"), oldId)); // where root.reference.id = oldId
    em.createQuery(update).executeUpdate();
}
```

Это **массовый** `update`: одним SQL-запросом меняет все подходящие строки, не загружая их в память.

Пример:

```java
replaceReferences(Organization.class, "coordinates", 5, newCoords);
```

```sql
update Organization root set root.coordinates = :newCoords where root.coordinates.id = 5
-- в SQL примерно:
update organization set coordinates_id = <id newCoords> where coordinates_id = 5
```

`executeUpdate` работает только внутри транзакции. Транзакцию открыл `@Transactional` на сервисе, поэтому замена ссылок и удаление старого объекта выполняются атомарно: либо все, либо ничего.

---

## 6. CoordinatesRepository, LocationRepository, AddressRepository

Три репозитория устроены одинаково. Разберу на `CoordinatesRepository`.

```java
@ApplicationScoped
public class CoordinatesRepository extends BaseRepository {
```

`@ApplicationScoped` делает класс CDI-бином в одном экземпляре. Сервис получает его через `@Inject CoordinatesRepository repository`.

| Метод | Что внутри | Пояснение |
|---|---|---|
| `findAll()` | `findAll(Coordinates.class)` | см. [`findAll`](#findall-все-объекты-типа-отсортированные-по-id) |
| `findById(id)` | `em.find(Coordinates.class, id)` | вернет `null`, если нет |
| `save(c)` | унаследован из `BaseRepository` | insert или update, возвращает сохраненный объект с заполненным `id` |
| `countUsages(id)` | `countReferences(Organization.class, id, "coordinates")` | сколько организаций используют эти координаты |
| `deleteWithReplace(id, replacementId)` | см. ниже | удалить, при необходимости перевесив ссылки |

### `deleteWithReplace`

```java
public void deleteWithReplace(long id, Long replacementId) {
    if (replacementId != null) {
        replaceReferences(Organization.class, "coordinates", id, findById(replacementId));
    }
    delete(Coordinates.class, id);
}
```

По шагам, все в транзакции сервиса:

1. Если передан `replacementId`, загружаем объект-замену и массово перевешиваем на него все организации со старых координат.
2. Удаляем старые координаты.

Если на шаге 2 что-то упадет, контейнер откатит и шаг 1.

Проверки "объект существует", "замена не совпадает с удаляемым", "замена нужна, раз есть ссылки" делает **сервис** до вызова. Репозиторий считает, что данные уже корректны.

### Чем отличаются три репозитория

| Репозиторий | Кто ссылается | Поля-ссылки |
|---|---|---|
| `CoordinatesRepository` | `Organization` | `coordinates` |
| `LocationRepository` | `Address` | `town` |
| `AddressRepository` | `Organization` | `officialAddress`, `postalAddress` |

У адреса две ссылки, поэтому в `countUsages` передаются два поля, а в `deleteWithReplace` `replaceReferences` вызывается дважды.

---

## 7. OrganizationRepository

`findById` такой же, как выше, `save` унаследован. `delete(id)` просто вызывает `delete(Organization.class, id)` из `BaseRepository`. По возвращенному `false` сервис бросает `NotFoundException`.

### `findPage`: одна страница таблицы

```java
public List<Organization> findPage(String filterField, String filterValue,
                                   String sortField, boolean asc, int page, int size) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Organization> query = cb.createQuery(Organization.class);
    Root<Organization> o = query.from(Organization.class);                    // from Organization o

    Order order = asc ? cb.asc(o.get(sortField)) : cb.desc(o.get(sortField)); // o.<sortField> asc|desc
    query.where(filter(cb, o, filterField, filterValue))                      // where <фильтр>
            .orderBy(order, cb.asc(o.get("id")));                             // order by <sort>, o.id asc

    return em.createQuery(query)
            .setFirstResult(page * size)                                      // offset page*size
            .setMaxResults(size)                                              // limit size
            .getResultList();
}
```

Пример: `findPage("name", "Abc", "rating", false, 2, 10)` даст примерно:

```sql
select o from Organization o
where locate('abc', lower(o.name)) > 0
order by o.rating desc, o.id asc
-- offset 20 limit 10
```

Детали:

- **Сортировка по полю из переменной.** `o.get(sortField)` берет поле по имени из параметра. Поэтому сервис заранее проверяет `sortField` по белому списку `SORT_FIELDS`. Иначе неизвестное имя бросило бы `IllegalArgumentException`.
- **Вторая сортировка по `id`.** Если у нескольких строк одинаковый `rating`, их порядок в PostgreSQL не определен, и при переходе между страницами строки могли бы повторяться или пропадать. `id` уникален, и с ним порядок всегда однозначный.
- **Пагинация.** Страницы нумеруются с 0. Страница `page` начинается со строки `page * size`.

### `count`: сколько всего строк под фильтром

```java
public long count(String filterField, String filterValue) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<Organization> o = query.from(Organization.class);

    query.select(cb.count(o))                                   // select count(o)
            .where(filter(cb, o, filterField, filterValue));    // тот же фильтр

    return em.createQuery(query).getSingleResult();
}
```

Нужен фронтенду, чтобы посчитать количество страниц. Условие строит тот же метод `filter`, что и в `findPage`, поэтому "страница" и "всего" всегда считаются по одному правилу.

Почему `filter` вызывается дважды, а не один раз: `Predicate` привязан к своему `Root`, а `Root` к своему `CriteriaQuery`. У каждого запроса свой `Root`, значит и условие для него строится заново.

### `filter`: условие "поле содержит подстроку"

```java
private Predicate filter(CriteriaBuilder cb, Root<Organization> o, String field, String value) {
    if (field == null || value == null || value.isEmpty()) {
        return cb.conjunction();                                   // 1 = 1, фильтра нет
    }
    String substring = value.toLowerCase();

    if (field.equals("type")) {
        List<OrganizationType> types = Arrays.stream(OrganizationType.values())
                .filter(t -> t.name().toLowerCase().contains(substring))
                .toList();
        return types.isEmpty() ? cb.disjunction() : o.get("type").in(types);
    }

    return cb.greaterThan(cb.locate(cb.lower(o.<String>get(field)), substring), 0);
}
```

Три ветки:

**1. Фильтр не задан.** Возвращаем `cb.conjunction()`. Это "пустое `and`", которое всегда истинно, то есть `where 1 = 1`. Благодаря этому `findPage` и `count` всегда могут вызывать `query.where(...)`, без `if` "есть фильтр или нет".

**2. Фильтр по `type`.** `type` это enum, в БД он хранится строкой (`@Enumerated(EnumType.STRING)`). Но в JPQL поле enum нельзя передать в `lower()` или `locate()`: для JPA это не строка. Поэтому подстроку ищем **в Java**:

- пробегаем все значения `OrganizationType`;
- оставляем те, в чьем имени есть подстрока (`"com"` даст `[COMMERCIAL]`, `"t"` даст почти все);
- строим `o.type in (COMMERCIAL, ...)`.

Если ни одно значение не подошло, возвращаем `cb.disjunction()`. Это "пустое `or`", всегда ложное (`1 = 0`), и запрос вернет 0 строк. Пустой список в `in ()` передавать нельзя, это ошибка SQL.

**3. Фильтр по строковому полю** (`name`, `fullName`):

```java
cb.greaterThan(cb.locate(cb.lower(o.<String>get(field)), substring), 0)
```

Читаем изнутри наружу:

| Кусок | JPQL |
|---|---|
| `o.<String>get(field)` | `o.name` |
| `cb.lower(...)` | `lower(o.name)` |
| `cb.locate(..., substring)` | `locate('abc', lower(o.name))`: позиция подстроки (с 1), или 0 |
| `cb.greaterThan(..., 0)` | `... > 0`, то есть "подстрока найдена" |

Неполное совпадение без учета регистра: значение приводится к нижнему регистру в Java (`value.toLowerCase()`), поле в SQL (`lower(...)`).

`o.<String>get(field)` это явное указание дженерика. `get` возвращает `Path<Y>`, и Java не знает, что `Y` это `String`. А `cb.lower` принимает только `Expression<String>`. Запись `<String>` говорит компилятору: "это поле строковое".

Почему `locate`, а не `like '%abc%'`: если пользователь введет `%` или `_`, в `like` это спецсимволы, и их пришлось бы экранировать. `locate` ищет подстроку буквально, экранировать ничего не надо.

Если поле `fullName` равно `null`, `lower(null)` и `locate` вернут `null`, а `null > 0` не истинно. Такие строки под фильтр просто не попадут, что и нужно.

---

## 8. SpecialOpsRepository

По заданию спецоперации реализованы как **функции PostgreSQL** (`sql/functions.sql`). Репозиторий их только вызывает.

### Как EclipseLink вызывает функцию

```java
em.createStoredProcedureQuery("count_full_name_greater")
        .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
        .setParameter(1, fullName)
        .getSingleResult()
```

| Строка | Смысл |
|---|---|
| `createStoredProcedureQuery("имя")` | запрос-вызов процедуры/функции БД с таким именем |
| `createStoredProcedureQuery("имя", Organization.class)` | то же, но строки результата превратить в сущности `Organization` |
| `registerStoredProcedureParameter(1, String.class, ParameterMode.IN)` | у функции есть параметр №1 типа `String`, это входной параметр |
| `setParameter(1, value)` | значение для параметра №1 |
| `getSingleResult()` / `getResultList()` | выполнить и забрать одну строку / все строки |

`ParameterMode.IN` значит "входной". Есть еще `OUT` и `INOUT`, они для процедур, которые возвращают значения через параметры. Нам не нужны: наши функции возвращают результат обычным `select`.

Для PostgreSQL EclipseLink превращает это в запрос вида

```sql
SELECT * FROM count_full_name_greater(?)
```

Отсюда главная особенность: результат всегда приходит как **строки таблицы**. Даже если функция возвращает одно число, это таблица из одной строки и одной колонки, и EclipseLink отдает такую строку как `Object[]`.

### `firstColumn`

```java
private Number firstColumn(Object row) {
    return (Number) ((Object[]) row)[0];
}
```

Берет из строки-массива первую (единственную) колонку и приводит к `Number`. `Number` это общий родитель `Long`, `Integer`, `Double`, `BigDecimal`. Так не важно, каким именно числовым типом JDBC-драйвер вернул значение: дальше вызываем `.longValue()` или `.doubleValue()`.

### Методы

| Метод | Функция БД | Что возвращает функция | Как обрабатываем |
|---|---|---|---|
| `countFullNameGreater(s)` | `count_full_name_greater(text)` | `bigint` | одна строка -> `firstColumn(...).longValue()` |
| `findNameContains(s)` | `find_name_contains(text)` | `setof organization` | `Organization.class` в запросе, EclipseLink сам соберет сущности |
| `uniqueRatings()` | `unique_ratings()` | `setof bigint` | много строк по одной колонке -> `firstColumn` у каждой |
| `top5ByTurnover()` | `top5_by_turnover()` | `setof organization` | как `findNameContains` |
| `avgEmployeesTop10()` | `avg_employees_top10()` | `double precision` | одна строка -> `firstColumn(...).doubleValue()` |

Разбор `uniqueRatings`, он самый запутанный:

```java
@SuppressWarnings("unchecked")
public List<Long> uniqueRatings() {
    return ((List<Object>) em.createStoredProcedureQuery("unique_ratings")
            .getResultList())                                   // List<Object[]>, каждая строка [rating]
            .stream().map(row -> firstColumn(row).longValue())  // [rating] -> long
            .toList();
}
```

- `getResultList()` у `StoredProcedureQuery` возвращает сырой `List` без дженерика. Приводим его к `List<Object>`, компилятор об этом предупреждает, и `@SuppressWarnings("unchecked")` это предупреждение глушит.
- Каждую строку `Object[]{rating}` превращаем в `long`.

В `findNameContains` и `top5ByTurnover` тоже стоит `@SuppressWarnings("unchecked")`, по той же причине: сырой `List` неявно превращается в `List<Organization>`.

Как `Organization.class` превращает строки в сущности: функция возвращает `setof organization`, то есть строки с теми же колонками, что у таблицы `organization`. EclipseLink сопоставляет колонки с маппингом сущности (`full_name` -> `fullName`, `coordinates_id` -> `coordinates` и так далее) и собирает объекты. Связанные `Coordinates` и `Address` он догружает отдельными запросами по их `id`.

---

## 9. Подводные камни

**Репозиторий вызывать только из транзакционного сервиса, если он пишет.** `save`, `delete`, `replaceReferences` вне транзакции упадут с `TransactionRequiredException`. Читать (`find`, запросы) можно и без транзакции, так работает `SpecialOpsService`.

**Managed-объекты меняются в БД сами.** Внутри транзакции объект из `findById` managed: если поменять ему поле и ничего не вызывать, изменение все равно уйдет в БД при коммите. Поэтому в `OrganizationService` проверки идут до коммита, и любая ошибка откатывает уже сделанные изменения.

**Связи загружаются сразу.** У `@ManyToOne` по умолчанию `FetchType.EAGER`: координаты и адреса подгружаются вместе с организацией. Поэтому после конца транзакции DTO можно собирать без ошибок ленивой загрузки.

**Кэш EclipseLink выключен** (`eclipselink.cache.shared.default=false`). Иначе после массового `update` в `replaceReferences` общий кэш EclipseLink мог бы отдавать старые ссылки: bulk update идет напрямую в БД, мимо кэша.

**Проверки в сервисе, а не в репозитории.** Например, `deleteWithReplace` не проверяет, существует ли замена. Если вызвать его в обход сервиса с плохим `replacementId`, `findById` вернет `null`, и все ссылки станут `null`. Для `coordinates` и `postalAddress` это нарушит `NOT NULL`, и транзакция откатится.

**Имена полей строками.** `"coordinates"`, `"officialAddress"`, `"town"` нигде не проверяются компилятором. Переименовал поле в сущности, значит надо поискать его имя по репозиториям.

**`-ds.xml` в war помечен в WildFly как deprecated.** Сейчас работает (в логе будет `WFLYJCA0091`). Если поддержку уберут, datasource придется создавать в самом сервере через `jboss-cli` (`data-source add ...`).
