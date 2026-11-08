async function loadImportHistory() {
    const data = await api('GET', '/imports');
    const statuses = {IN_PROGRESS: 'Выполняется', SUCCESS: 'Успешно', FAILED: 'Ошибка'};
    document.getElementById('import-rows').innerHTML = data.items.map(item =>
        '<tr><td>' + item.id + '</td><td>' + esc(statuses[item.status] || item.status)
        + '</td><td>' + esc(item.user) + '</td><td>'
        + (item.status === 'SUCCESS' ? item.addedCount : '') + '</td></tr>'
    ).join('');
}

let importedPage = 0;
let importedTotal = 0;
const importedSize = 5;

async function loadImportedOrganizations() {
    const data = await api('GET', '/organizations/list?page=' + importedPage + '&size=' + importedSize);
    importedTotal = data.total;
    const pages = Math.max(1, Math.ceil(importedTotal / importedSize));
    if (importedPage >= pages) {
        importedPage = pages - 1;
        return loadImportedOrganizations();
    }
    document.getElementById('imported-rows').innerHTML = data.items.map(o =>
        '<tr><td>' + o.id + '</td><td>' + esc(o.name) + '</td><td>' + esc(o.ownerId)
        + '</td><td><button onclick="viewOrg(' + o.id + ')">Просмотр</button></td></tr>'
    ).join('');
    document.getElementById('imported-page-info').textContent = 'страница ' + (importedPage + 1) + ' из ' + pages;
}

function prevImportedPage() {
    if (importedPage > 0) {
        importedPage--;
        reload(loadImportedOrganizations);
    }
}

function nextImportedPage() {
    if ((importedPage + 1) * importedSize < importedTotal) {
        importedPage++;
        reload(loadImportedOrganizations);
    }
}

async function uploadOrganizations(event) {
    event.preventDefault();
    const input = document.getElementById('import-file');
    const button = document.getElementById('import-submit');
    const message = document.getElementById('import-message');
    if (!input.files.length) return;
    button.disabled = true;
    message.classList.remove('error');
    message.textContent = '';
    try {
        await api('POST', '/organizations/list', await input.files[0].text(), 'application/yaml');
        message.textContent = 'Файл загружен';
        input.value = '';
        importedPage = 0;
    } catch (e) {
        message.classList.add('error');
        message.textContent = e.errors ? e.errors.join('; ') : e.message;
        await reload(loadImportHistory);
        return;
    } finally {
        button.disabled = false;
    }
    await Promise.all([reload(loadOrganizations), reload(loadImportedOrganizations), reload(loadImportHistory),
        reload(loadCoordinates), reload(loadAddresses), reload(loadLocations)]);
}
