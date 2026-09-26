function showResult(html) {
    document.getElementById('sp-result').innerHTML = html;
}

function orgsTable(list) {
    let html = '<table><tr><th>id</th><th>name</th><th>fullName</th><th>annualTurnover</th>'
        + '<th>employeesCount</th><th>rating</th></tr>';
    for (const o of list) {
        html += '<tr><td>' + o.id + '</td><td>' + esc(o.name) + '</td><td>' + esc(o.fullName) + '</td><td>'
            + esc(o.annualTurnover) + '</td><td>' + o.employeesCount + '</td><td>' + esc(o.rating) + '</td></tr>';
    }
    return html + '</table>';
}

async function runSpecial(url, render) {
    try {
        render(await api('GET', url));
    } catch (e) {
        showResult(esc(e.errors ? e.errors.join('; ') : e.message));
    }
}

function countFullNameGreater() {
    const value = document.getElementById('sp-fullname').value;
    runSpecial('/special/count-full-name-greater?value=' + encodeURIComponent(value),
        d => showResult('Количество: ' + d.count));
}

function nameContains() {
    const value = document.getElementById('sp-name').value;
    runSpecial('/special/name-contains?value=' + encodeURIComponent(value), d => showResult(orgsTable(d.items)));
}

function uniqueRatings() {
    runSpecial('/special/unique-ratings', d => showResult('rating: ' + d.ratings.join(', ')));
}

function top5() {
    runSpecial('/special/top5-turnover', d => showResult(orgsTable(d.items)));
}

function avgEmployees() {
    runSpecial('/special/avg-employees-top10', d => showResult('Среднее: ' + d.avg));
}
