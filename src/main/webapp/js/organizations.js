let page = 0;
let total = 0;
const size = 5;
let editingId = null;
let viewedId = null;

function formatDate(value) {
    if (!value) return '';
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        const twoDigits = number => String(number).padStart(2, '0');
        return year + '-' + twoDigits(month) + '-' + twoDigits(day) + ' '
            + twoDigits(hour) + ':' + twoDigits(minute) + ':' + twoDigits(second);
    }
    return String(value).replace('T', ' ').substring(0, 19);
}

function orgRow(o) {
    return '<tr>'
        + '<td>' + o.id + '</td>'
        + '<td>' + esc(o.name) + '</td>'
        + '<td>' + o.coordinates.x + '</td>'
        + '<td>' + o.coordinates.y + '</td>'
        + '<td>' + formatDate(o.creationDate) + '</td>'
        + '<td>' + esc(addressText(o.officialAddress)) + '</td>'
        + '<td>' + esc(o.annualTurnover) + '</td>'
        + '<td>' + o.employeesCount + '</td>'
        + '<td>' + esc(o.rating) + '</td>'
        + '<td>' + esc(o.fullName) + '</td>'
        + '<td>' + esc(o.type) + '</td>'
        + '<td>' + esc(addressText(o.postalAddress)) + '</td>'
        + '<td>' + esc(o.ownerId) + '</td>'
        + '<td><button onclick="viewOrg(' + o.id + ')">Просмотр</button>'
        + (canEdit(o) ? '<button onclick="openOrgDialog(' + o.id + ')">Изменить</button>'
        + '<button class="danger" onclick="deleteOrg(' + o.id + ')">Удалить</button>' : '') + '</td>'
        + '</tr>';
}

function organizationsUrl() {
    const filterValue = document.getElementById('filter-value').value;
    let url = '/organizations?page=' + page + '&size=' + size
        + '&sortField=' + document.getElementById('sort-field').value
        + '&sortDir=' + document.getElementById('sort-dir').value;
    if (filterValue !== '') {
        url += '&filterField=' + document.getElementById('filter-field').value
            + '&filterValue=' + encodeURIComponent(filterValue);
    }
    return url;
}

async function loadOrganizations() {
    const data = await api('GET', organizationsUrl());
    total = data.total;
    const pages = Math.max(1, Math.ceil(total / size));
    if (page >= pages) {
        page = pages - 1;
        return loadOrganizations();
    }
    document.getElementById('org-rows').innerHTML = data.items.map(orgRow).join('');
    document.getElementById('page-info').textContent = 'страница ' + (page + 1) + ' из ' + pages;
}

function applyFilter() {
    page = 0;
    reload(loadOrganizations);
}

function prevPage() {
    if (page > 0) {
        page--;
        reload(loadOrganizations);
    }
}

function nextPage() {
    if ((page + 1) * size < total) {
        page++;
        reload(loadOrganizations);
    }
}

function setField(id, value) {
    document.getElementById(id).value = value === null || value === undefined ? '' : value;
}

async function openOrgDialog(id) {
    let o = {};
    if (id) {
        try {
            o = await api('GET', '/organizations/' + id);
        } catch (e) {
            showError(e);
            return;
        }
    }
    editingId = id;
    document.getElementById('org-errors').innerHTML = '';
    document.getElementById('org-dialog-title').textContent = id ? 'Изменение организации #' + id : 'Новая организация';
    setField('f-name', o.name);
    setField('f-coordinates', o.coordinates ? o.coordinates.id : '');
    setField('f-official', o.officialAddress ? o.officialAddress.id : '');
    setField('f-turnover', o.annualTurnover);
    setField('f-employees', o.employeesCount);
    setField('f-rating', o.rating);
    setField('f-fullname', o.fullName);
    setField('f-type', o.type);
    setField('f-postal', o.postalAddress ? o.postalAddress.id : '');
    document.getElementById('org-dialog').showModal();
}

const INVALID_NUMBER = -1;

function readNumber(id, name, integer, errors) {
    const input = document.getElementById(id);
    if (input.validity.badInput) {
        errors.push(name + ' должно быть числом');
        return INVALID_NUMBER;
    }
    if (input.value === '') {
        return null;
    }
    const n = Number(input.value);
    if (integer && !Number.isInteger(n)) {
        errors.push(name + ' должно быть целым числом');
        return INVALID_NUMBER;
    }
    return n;
}

async function saveOrg() {
    const errors = [];
    const body = {
        name: document.getElementById('f-name').value,
        coordinatesId: valueOrNull('f-coordinates'),
        officialAddressId: valueOrNull('f-official'),
        annualTurnover: readNumber('f-turnover', 'annualTurnover', false, errors),
        employeesCount: readNumber('f-employees', 'employeesCount', true, errors),
        rating: readNumber('f-rating', 'rating', true, errors),
        fullName: document.getElementById('f-fullname').value,
        type: document.getElementById('f-type').value || null,
        postalAddressId: valueOrNull('f-postal')
    };
    const errorsList = document.getElementById('org-errors');
    const badFields = errors.map(x => x.split(' ')[0]);
    try {
        if (editingId) {
            await api('PUT', '/organizations/' + editingId, body);
        } else {
            await api('POST', '/organizations', body);
        }
        document.getElementById('org-dialog').close();
    } catch (e) {
        const serverErrors = (e.errors || [e.message]).filter(x => !badFields.includes(x.split(' ')[0]));
        errorsList.innerHTML = errors.concat(serverErrors).map(x => '<li>' + esc(x) + '</li>').join('');
    }
}

async function deleteOrg(id) {
    if (!confirm('Удалить организацию #' + id + '?')) return;
    try {
        await api('DELETE', '/organizations/' + id);
        showMessage('');
    } catch (e) {
        showError(e);
    }
}

async function viewOrg(id) {
    if (!id) return;
    try {
        const o = await api('GET', '/organizations/' + id);
        viewedId = o.id;
        document.getElementById('edit-viewed').hidden = !canEdit(o);
        const rows = [
            ['id', o.id],
            ['ownerId', o.ownerId],
            ['name', o.name],
            ['coordinates', coordText(o.coordinates)],
            ['creationDate', formatDate(o.creationDate)],
            ['officialAddress', addressText(o.officialAddress)],
            ['annualTurnover', o.annualTurnover],
            ['employeesCount', o.employeesCount],
            ['rating', o.rating],
            ['fullName', o.fullName],
            ['type', o.type],
            ['postalAddress', addressText(o.postalAddress)]
        ];
        let html = '';
        for (const r of rows) {
            html += '<tr><td>' + r[0] + '</td><td>' + esc(r[1]) + '</td></tr>';
        }
        document.getElementById('view-table').innerHTML = html;
        document.getElementById('view-dialog').showModal();
    } catch (e) {
        showError(e);
    }
}

function editViewed() {
    document.getElementById('view-dialog').close();
    openOrgDialog(viewedId);
}
