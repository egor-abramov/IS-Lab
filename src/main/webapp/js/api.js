async function api(method, url, body) {
    const options = {method: method, headers: {}};
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }
    let resp;
    try {
        resp = await fetch('api' + url, options);
    } catch (e) {
        throw apiError(0, ['сервер недоступен']);
    }
    if (resp.status === 204) {
        return null;
    }
    const text = await resp.text();
    let data = null;
    try {
        data = JSON.parse(text);
    } catch (e) {
    }
    if (!resp.ok) {
        throw apiError(resp.status, data && data.errors ? data.errors : ['ошибка ' + resp.status]);
    }
    return data;
}

function apiError(status, errors) {
    const err = new Error(errors.join('; '));
    err.status = status;
    err.errors = errors;
    return err;
}

function esc(value) {
    if (value === null || value === undefined) {
        return '';
    }
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showMessage(text) {
    document.getElementById('message').textContent = text;
}

function showError(e) {
    showMessage(e.errors ? e.errors.join('; ') : e.message);
}
