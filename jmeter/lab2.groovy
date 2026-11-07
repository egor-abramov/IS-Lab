import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

def json = new JsonSlurper()
def base = props.getProperty('lab2.base', 'http://localhost:8080/lab1/api')

def request = { String method, String path, String body = null, String token = null, String contentType = 'application/json' ->
    def connection = (HttpURLConnection) new URL(base + path).openConnection()
    connection.requestMethod = method
    connection.connectTimeout = 10000
    connection.readTimeout = 20000
    if (token != null) connection.setRequestProperty('Authorization', 'Bearer ' + token)
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

SampleResult.sampleStart()
try {
    String phase = Parameters
    if (phase == 'setup') {
        String run = 'lab2_' + System.currentTimeMillis()
        String password = 'lab2password123'
        props.put('lab2.run', run)
        props.put('lab2.y', ((int) (System.currentTimeMillis() % 1000000000)).toString())
        props.put('lab2.race.barrier', new CyclicBarrier(2))
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
        if (!(response.status in [201, 409])) throw new AssertionError("HTTP ${response.status}: ${response.body}")
        props.put('lab2.unique.' + thread, response.status.toString())
    } else if (phase == 'edit_delete') {
        int thread = ctx.getThreadNum()
        props.get('lab2.edit.barrier').await(15, TimeUnit.SECONDS)
        def response = thread == 0
                ? sendJson('PUT', '/organizations/' + props.get('lab2.common'),
                    organization(props.get('lab2.run') + '_updated', props.get('lab2.coordinates.2') as long,
                            props.get('lab2.address.0') as long), props.get('lab2.token.0'))
                : request('DELETE', '/organizations/' + props.get('lab2.common'), null, props.get('lab2.token.0'))
        if (thread == 0 && !(response.status in [200, 404])) throw new AssertionError("HTTP ${response.status}: ${response.body}")
        if (thread == 1 && response.status != 204) throw new AssertionError("HTTP ${response.status}: ${response.body}")
        props.put('lab2.edit.' + thread, response.status.toString())
    } else if (phase == 'import') {
        int thread = ctx.getThreadNum()
        String run = props.get('lab2.run')
        int y = props.get('lab2.y') as int
        String body = thread == 0 ? """
- name: ${run}_import_a
  coordinates: {x: 401, y: ${y + 1}}
  employeesCount: 10
  postalAddress: {street: JMeter A}
- name: ${run}_import_b
  coordinates: {x: 402, y: ${y + 2}}
  employeesCount: 11
  postalAddress: {street: JMeter B}
""" : """
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
        def response = request('POST', '/organizations/list', body, props.get('lab2.token.' + thread), 'application/yaml')
        requireStatus(response, thread == 0 ? 201 : 409)
        props.put('lab2.import.' + thread, response.status.toString())
    } else if (phase == 'verify') {
        String run = props.get('lab2.run')
        String token = props.get('lab2.token.0')
        def statuses = [props.get('lab2.unique.0'), props.get('lab2.unique.1')].sort()
        if (statuses != ['201', '409']) throw new AssertionError('Уникальность: ' + statuses)
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
        SampleResult.setResponseMessage('create=201/409; update=' + props.get('lab2.edit.0') +
                '; delete=204; import=201/409; итог: 1 уникальная организация, 2 импортированы, 0 из ошибочного импорта')
    }
    SampleResult.setResponseCode('200')
    SampleResult.setSuccessful(true)
} catch (Throwable error) {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseCode('500')
    SampleResult.setResponseMessage(error.toString())
    SampleResult.setResponseData(error.stackTrace.join('\n'), 'UTF-8')
} finally {
    SampleResult.sampleEnd()
}
