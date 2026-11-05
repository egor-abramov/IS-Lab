async function loadImportHistory() {
    const data = await api('GET', '/imports');
    const statuses = {IN_PROGRESS: 'Выполняется', SUCCESS: 'Успешно', FAILED: 'Ошибка'};
    document.getElementById('import-rows').innerHTML = data.items.map(item =>
        '<tr><td>' + item.id + '</td><td>' + esc(statuses[item.status] || item.status)
        + '</td><td>' + esc(item.user) + '</td><td>'
        + (item.status === 'SUCCESS' ? item.addedCount : '') + '</td></tr>'
    ).join('');
}
