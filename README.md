# Лабораторная работа 1

* Студент: Абрамов Егор Денисович
* Группа: З3319

## Настройка

```
cp config/example.properties config/local.properties
```

Вписать данные БД в `config/local.properties`, путь к WildFly -- в `gradle.properties`.

## Запуск

```
./gradlew up          # таблицы в БД, деплой, запуск WildFly
./gradlew deploy      # передеплоить после изменений
./gradlew stopServer  # остановить сервер
./gradlew seedDb      # добавить 10 тестовых организаций
```

## helios

```
psql -h pg -d studs -f sql/schema.sql -f sql/functions.sql
psql -h pg -d studs -f sql/seed.sql   # тестовые данные, по желанию
./gradlew war jdbcDriver -Pprofile=helios
```

`build/libs/lab1.war` и `build/libs/postgresql.jar` положить в `standalone/deployments` WildFly.
Datasource `java:jboss/datasources/lab1` описан в `WEB-INF/lab1-ds.xml` внутри war, драйвер берется из `postgresql.jar`.
