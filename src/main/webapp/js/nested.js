let coordinatesList = [];
let addressList = [];
let locationList = [];

function coordText(c) {
    return '#' + c.id + ' (' + c.x + '; ' + c.y + ')';
}

function locationText(l) {
    if (!l) return '';
    return '#' + l.id + ' ' + l.name + ' (' + l.x + '; ' + l.y + ')';
}

function addressText(a) {
    if (!a) return '';
    return '#' + a.id + ' ' + a.street + (a.town ? ', ' + a.town.name : '');
}

const nestedKinds = {
    coordinates: {title: 'координаты', list: () => coordinatesList, text: coordText},
    addresses: {title: 'адрес', list: () => addressList, text: addressText},
    locations: {title: 'локация', list: () => locationList, text: locationText}
};

function fillSelect(id, list, textFn, withEmpty) {
    const select = document.getElementById(id);
    const old = select.value;
    let html = withEmpty ? '<option value=""></option>' : '';
    for (const x of list) {
        html += '<option value="' + x.id + '">' + esc(textFn(x)) + '</option>';
    }
    select.innerHTML = html;
    select.value = old;
}

function deleteButton(kind, id) {
    const entity = nestedKinds[kind].list().find(item => item.id === id);
    if (!canEdit(entity)) return '';
    return '<button class="danger" onclick="deleteNested(\'' + kind + '\', ' + id + ')">Удалить</button>';
}

async function loadCoordinates() {
    coordinatesList = (await api('GET', '/coordinates')).items;
    document.getElementById('coord-rows').innerHTML = coordinatesList.map(c =>
        '<tr><td>' + c.id + '</td><td>' + c.x + '</td><td>' + c.y + '</td><td>'
        + deleteButton('coordinates', c.id) + '</td></tr>').join('');
    fillSelect('f-coordinates', coordinatesList.filter(canEdit), coordText, false);
}

async function loadAddresses() {
    addressList = (await api('GET', '/addresses')).items;
    document.getElementById('addr-rows').innerHTML = addressList.map(a =>
        '<tr><td>' + a.id + '</td><td>' + esc(a.street) + '</td><td>' + esc(locationText(a.town))
        + '</td><td>' + deleteButton('addresses', a.id) + '</td></tr>').join('');
    fillSelect('f-official', addressList.filter(canEdit), addressText, true);
    fillSelect('f-postal', addressList.filter(canEdit), addressText, false);
}

async function loadLocations() {
    locationList = (await api('GET', '/locations')).items;
    document.getElementById('loc-rows').innerHTML = locationList.map(l =>
        '<tr><td>' + l.id + '</td><td>' + l.x + '</td><td>' + l.y + '</td><td>' + esc(l.name)
        + '</td><td>' + deleteButton('locations', l.id) + '</td></tr>').join('');
    fillSelect('addr-town', locationList.filter(canEdit), locationText, true);
}

function valueOrNull(id) {
    const v = document.getElementById(id).value;
    return v === '' ? null : Number(v);
}

async function createNested(kind, body) {
    try {
        await api('POST', '/' + kind, body);
        showMessage('');
    } catch (e) {
        showError(e);
    }
}

function createCoordinates() {
    createNested('coordinates', {x: valueOrNull('coord-x'), y: valueOrNull('coord-y')});
}

function createAddress() {
    createNested('addresses', {
        street: document.getElementById('addr-street').value,
        townId: valueOrNull('addr-town')
    });
}

function createLocation() {
    createNested('locations', {
        x: valueOrNull('loc-x'),
        y: valueOrNull('loc-y'),
        name: document.getElementById('loc-name').value
    });
}

let replacing = null;

async function deleteNested(kind, id) {
    if (!confirm('Удалить ' + nestedKinds[kind].title + ' #' + id + '?')) return;
    try {
        await api('DELETE', '/' + kind + '/' + id);
        showMessage('');
    } catch (e) {
        if (e.status === 409) {
            openReplaceDialog(kind, id, e.errors.join('; '));
        } else {
            showError(e);
        }
    }
}

function openReplaceDialog(kind, id, reason) {
    const candidates = nestedKinds[kind].list().filter(x => x.id !== id && canEdit(x));
    if (candidates.length === 0) {
        showMessage(reason + '. Нет других объектов для замены — сначала создайте новый.');
        return;
    }
    replacing = {kind: kind, id: id};
    document.getElementById('replace-title').textContent = 'Удаление: ' + nestedKinds[kind].title + ' #' + id;
    document.getElementById('replace-reason').textContent = reason;
    document.getElementById('replace-errors').innerHTML = '';
    fillSelect('replace-select', candidates, nestedKinds[kind].text, false);
    document.getElementById('replace-dialog').showModal();
}

async function confirmReplace() {
    const replacement = document.getElementById('replace-select').value;
    try {
        await api('DELETE', '/' + replacing.kind + '/' + replacing.id + '?replaceWith=' + encodeURIComponent(replacement));
        document.getElementById('replace-dialog').close();
        showMessage('');
    } catch (e) {
        document.getElementById('replace-errors').innerHTML =
            (e.errors || [e.message]).map(x => '<li>' + esc(x) + '</li>').join('');
    }
}
