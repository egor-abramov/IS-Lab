import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.Driver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

def json = new JsonSlurper()
def base = props.getProperty('lab2.base', 'http://localhost:8080/lab1/api')

def request = { String method, String path, String body = null, String token = null, String contentType = 'application/json' ->
    def connection = (HttpURLConnection) new URL(base + path).openConnection()
    connection.requestMethod = method
    connection.connectTimeout = 10000
    connection.readTimeout = 20000

    if (token != null)
        connection.setRequestProperty('Authorization', 'Bearer ' + token)

    if (body != null) {
        connection.doOutput = true
        connection.setRequestProperty('Content-Type', contentType + '; charset=UTF-8')
        connection.outputStream.withCloseable { it.write(body.getBytes('UTF-8')) }
    }

    int status = connection.responseCode
    String response = (status < 400 ? connection.inputStream : connection.errorStream)?.getText('UTF-8') ?: ''
    connection.disconnect()
    [status: status, body: response]
}

def sendJson = { String method, String path, Object data, String token ->
    request(method, path, JsonOutput.toJson(data), token)
}

def organization = { String name, long coordinatesId, long addressId ->
    [name: name, coordinatesId: coordinatesId, postalAddressId: addressId, employeesCount: 10]
}

def requireStatus = { response, int expected ->
    if (response.status != expected) throw new AssertionError("HTTP ${response.status} вместо ${expected}: ${response.body}")
    response
}

try {
    String phase = Parameters
    if (phase == 'setup') {
        String run = 'lab2_' + System.currentTimeMillis()
        String password = 'lab2password123'
        props.put('lab2.run', run)
        props.put('lab2.y', ((int) (System.currentTimeMillis() % 1000000000)).toString())
        props.put('lab2.race.barrier', new CyclicBarrier(2))
        props.put('lab2.foreign.barrier', new CyclicBarrier(2))
        props.put('lab2.edit.barrier', new CyclicBarrier(2))
        props.put('lab2.import.barrier', new CyclicBarrier(2))
        (0..1).each { i ->
            String login = (run + '_' + i).toLowerCase()
            requireStatus(sendJson('POST', '/auth/register', [login: login, password: password], null), 201)
            String token = json.parseText(requireStatus(sendJson('POST', '/auth/login',
                    [login: login, password: password], null), 200).body).token
            props.put('lab2.token.' + i, token)

            long addressId = json.parseText(requireStatus(sendJson('POST', '/addresses',
                    [street: 'JMeter ' + run + ' ' + i], token), 201).body).id
            props.put('lab2.address.' + i, addressId.toString())

            long coordinatesId = json.parseText(requireStatus(sendJson('POST', '/coordinates',
                    [x: 300 + i, y: (int) (System.currentTimeMillis() % 1000000) + i], token), 201).body).id
            props.put('lab2.coordinates.' + i, coordinatesId.toString())
        }

        String token = props.get('lab2.token.0')
        long commonCoordinates = json.parseText(requireStatus(sendJson('POST', '/coordinates',
                [x: 302, y: (int) (System.currentTimeMillis() % 1000000) + 2], token), 201).body).id
        props.put('lab2.coordinates.2', commonCoordinates.toString())

        long commonId = json.parseText(requireStatus(sendJson('POST', '/organizations',
                organization(run + '_common', commonCoordinates, props.get('lab2.address.0') as long), token), 201).body).id
        props.put('lab2.common', commonId.toString())
    } else if (phase == 'unique') {
        int thread = ctx.getThreadNum()
        props.get('lab2.race.barrier').await(15, TimeUnit.SECONDS)

        def response = sendJson('POST', '/organizations', organization(props.get('lab2.run') + '_unique',
                props.get('lab2.coordinates.' + thread) as long, props.get('lab2.address.' + thread) as long),
                props.get('lab2.token.' + thread))
        if (!(response.status in [201, 409])) {
            throw new AssertionError('Создание с одинаковым name: HTTP ' + response.status + ': ' + response.body)
        }
        props.put('lab2.unique.' + thread, response.status.toString())
    } else if (phase == 'foreign_edit_delete') {
        int thread = ctx.getThreadNum()
        props.get('lab2.foreign.barrier').await(15, TimeUnit.SECONDS)
        def response
        if (thread == 0) {
            response = sendJson('PUT', '/organizations/' + props.get('lab2.common'),
                    organization(props.get('lab2.run') + '_access_updated',
                            props.get('lab2.coordinates.2') as long, props.get('lab2.address.0') as long),
                    props.get('lab2.token.0'))
            requireStatus(response, 200)
        } else {
            response = request('DELETE', '/organizations/' + props.get('lab2.common'),
                    null, props.get('lab2.token.1'))
            requireStatus(response, 404)
        }
        props.put('lab2.foreign.' + thread, response.status.toString())
    } else if (phase == 'edit_delete') {
        int thread = ctx.getThreadNum()
        props.get('lab2.edit.barrier').await(15, TimeUnit.SECONDS)
        def response
        if (thread == 0) {
            response = sendJson('PUT', '/organizations/' + props.get('lab2.common'),
                    organization(props.get('lab2.run') + '_updated',
                            props.get('lab2.coordinates.2') as long, props.get('lab2.address.0') as long),
                    props.get('lab2.token.0'))
            if (!(response.status in [200, 404])) {
                throw new AssertionError('Обновление: HTTP ' + response.status + ': ' + response.body)
            }
        } else {
            response = request('DELETE', '/organizations/' + props.get('lab2.common'),
                    null, props.get('lab2.token.0'))
            requireStatus(response, 204)
        }
        props.put('lab2.edit.' + thread, response.status.toString())
    } else if (phase == 'import') {
        int thread = ctx.getThreadNum()
        String run = props.get('lab2.run')
        int y = props.get('lab2.y') as int
        String validImport = """
            - name: ${run}_import_a
              coordinates: {x: 401, y: ${y + 1}}
              employeesCount: 10
              postalAddress: {street: JMeter A}
            - name: ${run}_import_b
              coordinates: {x: 402, y: ${y + 2}}
              employeesCount: 11
              postalAddress: {street: JMeter B}
            """
                    String invalidImport = """
            - name: ${run}_failed_a
              fullName: ${run}_same_full_name
              coordinates: {x: 403, y: ${y + 3}}
              employeesCount: 10
              postalAddress: {street: JMeter C}
            - name: ${run}_failed_b
              fullName: ${run}_same_full_name
              coordinates: {x: 404, y: ${y + 4}}
              employeesCount: 11
              postalAddress: {street: JMeter D}
            """
        props.get('lab2.import.barrier').await(15, TimeUnit.SECONDS)
        String body = thread == 0 ? validImport : invalidImport
        def response = request('POST', '/organizations/list', body,
                props.get('lab2.token.' + thread), 'application/yaml')
        if (thread == 0) {
            requireStatus(response, 201)
        } else {
            requireStatus(response, 409)
        }
        props.put('lab2.import.' + thread, response.status.toString())
    } else if (phase == 'race') {
        String run = 'race_' + System.currentTimeMillis() + '_' + UUID.randomUUID().toString().substring(0, 8)
        String duplicateName = run + '_same_name'
        String password = 'racepassword123'
        def tokens = []
        def addressIds = []
        def coordinatesIds = []

        for (int i = 0; i < 2; i++) {
            String login = run + '_' + i
            requireStatus(sendJson('POST', '/auth/register', [login: login, password: password], null), 201)
            String token = json.parseText(requireStatus(sendJson('POST', '/auth/login',
                    [login: login, password: password], null), 200).body).token
            tokens.add(token)
            addressIds.add(json.parseText(requireStatus(sendJson('POST', '/addresses',
                    [street: run + '_' + i], token), 201).body).id)
            coordinatesIds.add(json.parseText(requireStatus(sendJson('POST', '/coordinates',
                    [x: 510 + i, y: (int) (System.currentTimeMillis() % 1000000000) + i],
                    token), 201).body).id)
        }

        def db = new Properties()
        Files.newBufferedReader(Paths.get(props.getProperty('race.dbConfig', 'config/local.properties')))
                .withCloseable { db.load(it) }
        def jar = Paths.get(props.getProperty('race.jdbcJar', 'build/libs/postgresql.jar')).toFile()
        URLClassLoader driverLoader = new URLClassLoader([jar.toURI().toURL()] as URL[])
        Connection lockConnection = null
        def workers = null
        try {
            Driver driver = (Driver) driverLoader.loadClass('org.postgresql.Driver')
                    .getDeclaredConstructor().newInstance()
            def credentials = new Properties()
            credentials.setProperty('user', db.getProperty('db.user'))
            credentials.setProperty('password', db.getProperty('db.password'))
            lockConnection = driver.connect(db.getProperty('db.url'), credentials)
            if (lockConnection == null) throw new AssertionError('Не удалось подключиться к PostgreSQL')

            lockConnection.autoCommit = false
            lockConnection.createStatement().withCloseable {
                it.execute('LOCK TABLE organization IN SHARE MODE')
            }

            workers = Executors.newFixedThreadPool(2)
            def start = new CountDownLatch(1)
            def requests = []
            for (int i = 0; i < 2; i++) {
                int user = i
                requests.add(workers.submit({ ->
                    start.await()
                    sendJson('POST', '/organizations',
                            organization(duplicateName, coordinatesIds[user] as long, addressIds[user] as long),
                            tokens[user])
                } as java.util.concurrent.Callable))
            }
            start.countDown()

            int waitingInserts = 0
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            while (System.nanoTime() < deadline) {
                lockConnection.createStatement().withCloseable { statement ->
                    statement.executeQuery("select count(*) from pg_locks where relation = 'organization'::regclass " +
                            "and mode = 'RowExclusiveLock' and not granted").withCloseable { rows ->
                        rows.next()
                        waitingInserts = rows.getInt(1)
                    }
                }
                if (waitingInserts >= 2) break
                Thread.sleep(50)
            }
            if (waitingInserts < 2) {
                throw new AssertionError('До INSERT дошли только ' + waitingInserts + ' из 2 запросов')
            }

            lockConnection.commit()
            def responses = requests.collect { it.get(35, TimeUnit.SECONDS) }
            def statuses = responses.collect { it.status }.sort()
            int saved = 0
            for (int i = 0; i < 2; i++) {
                if (responses[i].status == 201) {
                    long id = json.parseText(responses[i].body).id
                    if (request('GET', '/organizations/' + id, null, tokens[i]).status == 200) saved++
                }
            }
            if (statuses != [201, 409] || saved != 1) {
                throw new AssertionError('Нарушена уникальность ' + duplicateName +
                        ': HTTP ' + statuses + ', сохранено ' + saved + ' организаций')
            }
            SampleResult.setResponseMessage('HTTP ' + statuses + ', сохранена 1 организация')
        } finally {
            if (lockConnection != null) {
                try { lockConnection.rollback() } catch (Exception ignored) {}
                lockConnection.close()
            }
            if (workers != null) workers.shutdownNow()
            driverLoader.close()
        }
    } else if (phase == 'verify') {
        String run = props.get('lab2.run')
        String token = props.get('lab2.token.0')

        def statuses = [props.get('lab2.unique.0'), props.get('lab2.unique.1')].sort()
        if (statuses != ['201', '409'])
            throw new AssertionError('Уникальность: ' + statuses)

        if (props.get('lab2.token.0') == props.get('lab2.token.1')
                || props.get('lab2.foreign.0') != '200' || props.get('lab2.foreign.1') != '404')
            throw new AssertionError('Два пользователя: ' + [props.get('lab2.foreign.0'), props.get('lab2.foreign.1')])

        if (props.get('lab2.edit.1') != '204' || !(props.get('lab2.edit.0') in ['200', '404']))
            throw new AssertionError('Обновление/удаление: ' + [props.get('lab2.edit.0'), props.get('lab2.edit.1')])

        requireStatus(request('GET', '/organizations/' + props.get('lab2.common'), null, token), 404)
        def names = (0..1).collectMany { i ->
            def page = json.parseText(requireStatus(request('GET', '/organizations?size=100&filterField=name&filterValue=' + run,
                    null, props.get('lab2.token.' + i)), 200).body)
            page.items.collect { it.name }
        }
        if (names.count(run + '_unique') != 1 || !names.containsAll([run + '_import_a', run + '_import_b'])
                || names.any { it.startsWith(run + '_failed_') }) throw new AssertionError('Список организаций: ' + names)

        def successfulHistory = json.parseText(requireStatus(request('GET', '/imports', null, token), 200).body).items

        def failedHistory = json.parseText(requireStatus(request('GET', '/imports', null,
                props.get('lab2.token.1')), 200).body).items

        if (successfulHistory.count { it.status == 'SUCCESS' && it.addedCount == 2 } != 1
                || failedHistory.count { it.status == 'FAILED' && it.addedCount == null } != 1)
            throw new AssertionError('История импорта: ' + [successfulHistory, failedHistory])
        SampleResult.setResponseMessage('create=201/409; owner/foreign=200/404; update=' + props.get('lab2.edit.0') +
                '; delete=204; import=201/409; итог: 1 уникальная организация, 2 импортированы, 0 из ошибочного импорта')
    }
    SampleResult.setResponseCode('200')
    SampleResult.setSuccessful(true)
} catch (Throwable error) {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseCode('500')
    SampleResult.setResponseMessage(error.toString())
    SampleResult.setResponseData(error.stackTrace.join('\n'), 'UTF-8')
}
