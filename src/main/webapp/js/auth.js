let currentUser = null;

function canEdit(entity) {
    return currentUser && entity.ownerId === currentUser.id;
}

async function submitAuth(event) {
    event.preventDefault();
    const form = event.target;
    const action = event.submitter.value;
    const buttons = form.querySelectorAll('button');
    buttons.forEach(button => button.disabled = true);
    document.getElementById('auth-error').textContent = '';
    try {
        const data = await api('POST', '/auth/' + action, {
            login: document.getElementById('auth-login').value,
            password: document.getElementById('auth-password').value
        });
        sessionStorage.setItem('token', data.token);
        location.reload();
    } catch (e) {
        document.getElementById('auth-error').textContent = e.message;
        buttons.forEach(button => button.disabled = false);
    }
}

function logout() {
    sessionStorage.removeItem('token');
    location.reload();
}

async function loadUsers() {
    const data = await api('GET', '/users');
    document.getElementById('user-rows').innerHTML = data.items.map(user =>
        '<tr><td>' + user.id + '</td><td>' + esc(user.login) + '</td><td>' + esc(user.role) + '</td></tr>'
    ).join('');
}

async function initializeAuth() {
    if (!sessionStorage.getItem('token')) return;
    try {
        currentUser = await api('GET', '/auth/me');
        document.getElementById('auth-panel').hidden = true;
        document.getElementById('app-header').hidden = false;
        document.getElementById('app-main').hidden = false;
        document.getElementById('current-user').textContent = currentUser.login;
        document.getElementById('users-nav').hidden = currentUser.role !== 'ADMIN';
        await loadAll();
        connectUpdates(false);
    } catch (e) {
        document.getElementById('auth-error').textContent = e.message;
    }
}
