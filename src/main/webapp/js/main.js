function showSection(name) {
    for (const id of ['organizations', 'coordinates', 'addresses', 'locations', 'special']) {
        document.getElementById(id).hidden = id !== name;
    }
    for (const button of document.querySelectorAll('nav button')) {
        button.classList.toggle('active', button.dataset.section === name);
    }
}

async function loadAll() {
    const results = await Promise.allSettled([
        loadCoordinates(), loadLocations(), loadAddresses(), loadOrganizations()
    ]);
    const failed = results.find(r => r.status === 'rejected');
    if (failed) {
        showError(failed.reason);
    }
}

async function reload(what) {
    try {
        await what();
    } catch (e) {
        showError(e);
    }
}

function connectUpdates(reconnect) {
    const path = location.pathname.replace(/[^/]*$/, '');
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
    const ws = new WebSocket(protocol + location.host + path + 'updates');
    ws.onopen = function () {
        if (reconnect) {
            loadAll();
        }
    };
    ws.onmessage = function (e) {
        if (e.data === 'organizations') reload(loadOrganizations);
        if (e.data === 'coordinates') reload(loadCoordinates);
        if (e.data === 'addresses') reload(loadAddresses);
        if (e.data === 'locations') reload(loadLocations);
    };
    ws.onclose = function () {
        setTimeout(() => connectUpdates(true), 3000);
    };
}

loadAll();
connectUpdates(false);
