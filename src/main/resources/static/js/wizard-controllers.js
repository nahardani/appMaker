/* ========== Helpers ========== */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
const show = el => {
    if (el) el.style.display = '';
};
const hide = el => {
    if (el) el.style.display = 'none';
};

/* ========== HTTP Method sync & Body visibility ========== */
function syncHttpMethod() {
    const top = document.querySelector('#httpMethodTop');        // سلکت در باکس ۱
    const hidden = document.querySelector('#httpMethodHidden');  // hidden در باکس ۲
    if (top && hidden) hidden.value = top.value;
    refreshBodyVisibility(); // اگر POST باشد، باکس Body را نشان بده
}

function wireHttpSync() {
    try {
        const top = $('#httpMethodTop');
        if (top) top.addEventListener('change', syncHttpMethod);
        // بار اول
        syncHttpMethod();
    } catch (e) {
        console.warn('wireHttpSync err', e);
    }
}

function refreshBodyVisibility() {
    const top = document.querySelector('#httpMethodTop');
    const bodyInner = document.querySelector('#bodyCardInner');
    if (!top || !bodyInner) return;
    if ((top.value || '').toUpperCase() === 'POST') {
        bodyInner.style.display = '';
    } else {
        bodyInner.style.display = 'none';
    }
}

/* ========== URL/HEADER Params ========== */
function addParamRow() {
    const tbody = $('#paramsBody');
    const tpl = $('#paramRowTemplate');
    if (!tbody || !tpl) return;
    const row = tpl.cloneNode(true);
    row.id = '';

    const idx = $$('tr', tbody).length;
    $$('input,select', row).forEach(el => {
        const n = el.getAttribute('name');
        if (!n) return;
        if (n === '__NAME__') el.name = `params[${idx}].name`;
        else if (n === '__IN__') el.name = `params[${idx}].in`;
        else if (n === '__TYPE__') el.name = `params[${idx}].javaType`;
        else if (n === '__REQ__') el.name = `params[${idx}].required`;
    });
    tbody.appendChild(row);
}

function removeParamRow(btn) {
    const tr = btn && btn.closest ? btn.closest('tr') : null;
    const tbody = tr && tr.parentElement;
    if (!tbody) return;
    tr.remove();
    // Reindex
    const rows = $$('tr', tbody);
    rows.forEach((row, i) => {
        const name = row.querySelector('input[name^="params["][name$=".name"]');
        const loc = row.querySelector('select[name^="params["][name$=".in"]');
        const type = row.querySelector('select[name^="params["][name$=".javaType"]');
        const req = row.querySelector('input[name^="params["][name$=".required"]');
        if (name) name.name = `params[${i}].name`;
        if (loc) loc.name = `params[${i}].in`;
        if (type) type.name = `params[${i}].javaType`;
        if (req) req.name = `params[${i}].required`;
    });
}

/* ========== Request Body (fields in modal) ========== */
function openReqModal() {
    const tbody = $('#reqTable tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    const groups = {};
    $$('#hiddenRequest input').forEach(inp => {
        const m = inp.name && inp.name.match(/^requestFields\[(\d+)]\.(name|javaType|required)$/);
        if (!m) return;
        const idx = +m[1], key = m[2];
        groups[idx] = groups[idx] || {};
        groups[idx][key] = inp.value;
    });

    const keys = Object.keys(groups).sort((a, b) => +a - +b);
    if (keys.length === 0) addBodyFieldRow();
    else {
        keys.forEach(k => {
            const tr = $('#fieldRowTemplate').cloneNode(true);
            tr.id = '';
            tr.querySelector('input[type="text"]').value = groups[k].name || '';
            tr.querySelector('select').value = groups[k].javaType || 'String';
            tr.querySelector('input[type="checkbox"]').checked = (groups[k].required === 'true');
            tbody.appendChild(tr);
        });
    }
    openModal('reqModal');
}

function addBodyFieldRow() {
    const tbody = $('#reqTable tbody');
    if (!tbody) return;
    const tr = $('#fieldRowTemplate').cloneNode(true);
    tr.id = '';
    tbody.appendChild(tr);
}

function addBodyFieldAndOpen() {
    openReqModal();
    addBodyFieldRow();
}

function removeRow(btn) {
    const tr = btn && btn.closest ? btn.closest('tr') : null;
    if (tr) tr.remove();
}

function saveRequestFields() {
    const tbody = $('#reqTable tbody');
    const rows = tbody ? $$('tr', tbody) : [];
    const hidden = $('#hiddenRequest');
    if (!hidden) return;
    hidden.innerHTML = '';

    rows.forEach((tr, i) => {
        const name = tr.querySelector('input[type="text"]')?.value?.trim();
        if (!name) return;
        const type = tr.querySelector('select')?.value || 'String';
        const req = !!tr.querySelector('input[type="checkbox"]')?.checked;
        hidden.insertAdjacentHTML('beforeend',
            `<input type="hidden" name="requestFields[${i}].name" value="${name}">` +
            `<input type="hidden" name="requestFields[${i}].javaType" value="${type}">` +
            `<input type="hidden" name="requestFields[${i}].required" value="${req ? 'true' : 'false'}">`
        );
    });

    const preview = $('#reqPreview');
    if (preview) {
        const items = $$('#hiddenRequest input[name$=".name"]').map(inp => inp.value);
        preview.textContent = items.length ? ('فیلدها: ' + items.join(', ')) : 'بدنه‌ای تعریف نشده است.';
    }
    closeModal('reqModal');
}

/* ========== Response Parts (table + modal) ========== */
function addResponsePart() {
    const tbody = $('#respPartsTable tbody');
    const tpl = $('#respPartRowTemplate');
    if (!tbody || !tpl) return;
    const tr = tpl.cloneNode(true);
    tr.id = '';
    tbody.appendChild(tr);
    bindRespRow(tr);
    syncResponsePartsHidden();
}

function removeRespPartRow(btn) {
    const tr = btn && btn.closest ? btn.closest('tr') : null;
    if (tr) tr.remove();
    syncResponsePartsHidden();
}

function bindRespRow(tr) {
    if (!tr) return;
    const kindSel = tr.querySelector('.rp-kind');
    const typeCell = tr.querySelector('.rp-type-cell');
    const fieldsBtn = tr.querySelector('button');

    function renderTypeCell() {
        if (!typeCell) return;
        if (kindSel && kindSel.value === 'SCALAR') {
            typeCell.innerHTML =
                `<select class="scalar-type">
           <option>String</option><option>Long</option><option>Integer</option><option>Double</option>
           <option>Boolean</option><option>UUID</option><option>LocalDate</option><option>LocalDateTime</option>
         </select>`;
            if (fieldsBtn) fieldsBtn.disabled = true;
        } else {
            typeCell.innerHTML = `<input type="text" class="object-name" placeholder="OrderItemDto"/>`;
            if (fieldsBtn) fieldsBtn.disabled = false;
        }
        syncResponsePartsHidden();
    }

    if (kindSel) kindSel.addEventListener('change', renderTypeCell);
    tr.addEventListener('input', syncResponsePartsHidden);
    tr.addEventListener('change', syncResponsePartsHidden);
    renderTypeCell();
}

function openResPartFields(btn) {
    const tr = btn && btn.closest ? btn.closest('tr') : null;
    if (!tr) return;
    const rows = $$('#respPartsTable tbody tr');
    const idx = rows.indexOf(tr);
    const idxInp = $('#resPartIndex');
    if (idxInp) idxInp.value = idx;

    const tableBody = $('#resPartFieldsTable tbody');
    if (!tableBody) return;
    tableBody.innerHTML = '';

    const data = tr.dataset.fields ? JSON.parse(tr.dataset.fields) : [];
    if (data.length === 0) addResPartFieldRow();
    else {
        data.forEach(f => {
            const row = $('#resPartFieldTemplate').cloneNode(true);
            row.id = '';
            row.querySelector('input[type="text"]').value = f.name || '';
            row.querySelector('select').value = f.javaType || 'String';
            row.querySelector('input[type="checkbox"]').checked = !!f.required;
            tableBody.appendChild(row);
        });
    }
    openModal('resPartModal');
}

function addResPartFieldRow() {
    const tbody = $('#resPartFieldsTable tbody');
    if (!tbody) return;
    const tr = $('#resPartFieldTemplate').cloneNode(true);
    tr.id = '';
    tbody.appendChild(tr);
}

function saveResPartFields() {
    const idx = +($('#resPartIndex')?.value ?? -1);
    const rows = $$('#resPartFieldsTable tbody tr');
    const data = rows.map(tr => ({
        name: tr.querySelector('input[type="text"]')?.value?.trim(),
        javaType: tr.querySelector('select')?.value || 'String',
        required: !!tr.querySelector('input[type="checkbox"]')?.checked
    })).filter(f => f.name);

    const tr = $$('#respPartsTable tbody tr')[idx];
    if (tr) tr.dataset.fields = JSON.stringify(data);
    closeModal('resPartModal');
    syncResponsePartsHidden();
}

function syncResponsePartsHidden() {
    const wrap = $('#hiddenResponseParts');
    if (!wrap) return;
    wrap.innerHTML = '';
    const rows = $$('#respPartsTable tbody tr');
    rows.forEach((tr, i) => {
        const name = tr.querySelector('td:first-child input')?.value?.trim();
        if (!name) return;
        const container = tr.querySelector('.rp-container')?.value || 'SINGLE';
        const kind = tr.querySelector('.rp-kind')?.value || 'SCALAR';
        wrap.insertAdjacentHTML('beforeend',
            `<input type="hidden" name="responseParts[${i}].name" value="${name}">` +
            `<input type="hidden" name="responseParts[${i}].container" value="${container}">` +
            `<input type="hidden" name="responseParts[${i}].kind" value="${kind}">`
        );
        if (kind === 'SCALAR') {
            const scalar = tr.querySelector('.scalar-type')?.value || 'String';
            wrap.insertAdjacentHTML('beforeend',
                `<input type="hidden" name="responseParts[${i}].scalarType" value="${scalar}">`
            );
        } else {
            const obj = tr.querySelector('.object-name')?.value?.trim() || '';
            if (obj) wrap.insertAdjacentHTML('beforeend',
                `<input type="hidden" name="responseParts[${i}].objectName" value="${obj}">`
            );
            const fields = tr.dataset.fields ? JSON.parse(tr.dataset.fields) : [];
            fields.forEach((f, j) => {
                wrap.insertAdjacentHTML('beforeend',
                    `<input type="hidden" name="responseParts[${i}].fields[${j}].name" value="${f.name}">` +
                    `<input type="hidden" name="responseParts[${i}].fields[${j}].javaType" value="${f.javaType}">` +
                    `<input type="hidden" name="responseParts[${i}].fields[${j}].required" value="${f.required ? 'true' : 'false'}">`
                );
            });
        }
    });
}

/* ========== Modal helpers ========== */
function openModal(id) {
    const el = $('#' + id);
    if (el) el.style.display = 'flex';
}

function closeModal(id) {
    const el = $('#' + id);
    if (el) el.style.display = 'none';
}

/* ========== Endpoint delete helper ========== */
function deleteEndpoint(projectId, ctrlName, epIndex) {
    if (!confirm('این اندپوینت حذف شود؟')) return;
    const f = $('#epDeleteForm');
    if (!f) return;
    f.action = `/wizard/${projectId}/controllers/${encodeURIComponent(ctrlName)}/endpoints/${epIndex}/delete`;
    f.submit();
}

/* ========== Hydration on load ========== */
function hydrateResponseFromHidden() {
    const wrap = document.querySelector('#hiddenResponseParts');
    if (!wrap) return;
    const inputs = Array.from(wrap.querySelectorAll('input'));
    if (inputs.length === 0) return;

    const groups = {};
    inputs.forEach(inp => {
        const m = inp.name.match(/^responseParts\[(\d+)]\.(name|container|kind|scalarType|objectName|fields\[(\d+)]\.(name|javaType|required))$/);
        if (!m) return;
        const i = +m[1];
        groups[i] = groups[i] || {fields: []};
        if (m[3] !== undefined) {
            const j = +m[3];
            groups[i].fields[j] = groups[i].fields[j] || {};
            groups[i].fields[j][m[4]] = inp.value;
        } else {
            groups[i][m[2]] = inp.value;
        }
    });

    const tbody = document.querySelector('#respPartsTable tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    Object.keys(groups).sort((a, b) => +a - +b).forEach(k => {
        const g = groups[k];
        const tr = document.querySelector('#respPartRowTemplate').cloneNode(true);
        tr.id = '';

        // نام
        tr.querySelector('td:first-child input').value = g.name || '';

        // ظرف و نوع
        tr.querySelector('.rp-container').value = g.container || 'SINGLE';
        tr.querySelector('.rp-kind').value = g.kind || 'SCALAR';

        // سلول نوع
        const typeCell = tr.querySelector('.rp-type-cell');
        if ((g.kind || 'SCALAR') === 'SCALAR') {
            typeCell.innerHTML =
                `<select class="scalar-type">
           <option>String</option><option>Long</option><option>Integer</option><option>Double</option>
           <option>Boolean</option><option>UUID</option><option>LocalDate</option><option>LocalDateTime</option>
         </select>`;
            tr.querySelector('button').disabled = true;
            tr.querySelector('.scalar-type').value = g.scalarType || 'String';
        } else {
            typeCell.innerHTML = `<input type="text" class="object-name" placeholder="OrderItemDto"/>`;
            tr.querySelector('button').disabled = false;
            tr.querySelector('.object-name').value = g.objectName || '';
        }
        const fields = (g.fields || []).filter(Boolean).map(f => ({
            name: f.name || '',
            javaType: f.javaType || 'String',
            required: (f.required === 'true' || f.required === true)
        })).filter(f => f.name);
        if (fields.length) tr.dataset.fields = JSON.stringify(fields);

        // بایند رویدادها
        bindRespRow(tr);

        tbody.appendChild(tr);
    });
}

const OLLAMA_BASE = '/wizard'; // ما از سرور خودمان پروکسی می‌زنیم




// نمایش طرح و فایل‌ها + آماده‌سازی payload برای apply
function renderAiResult(data) {
    const planEl = document.getElementById('aiPlan');
    const filesEl = document.getElementById('aiFiles');
    const wrap = document.getElementById('aiResult');
    const apply = document.getElementById('aiApplyPayload');

    planEl.textContent = data.plan || '—';
    filesEl.innerHTML = '';
    (data.files || []).forEach((f, idx) => {
        const card = document.createElement('div');
        card.className = 'card';
        card.style.marginBottom = '8px';
        card.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <strong style="direction:ltr">${escapeHtml(f.path)}</strong>
        <span class="muted">${f.lang || ''}</span>
      </div>
      <pre style="white-space:pre-wrap;direction:ltr;">${escapeHtml(f.content || '')}</pre>
    `;
        filesEl.appendChild(card);
    });

    apply.value = JSON.stringify(data);
    wrap.style.display = 'block';
}

function escapeHtml(s) {
    return (s || '').replace(/[&<>"']/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[c]))
}


function getCsrf() {
    const token  = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    return { token, header };
}








/* ========== Init ========== */
document.addEventListener('DOMContentLoaded', () => {
    hydrateResponseFromHidden();
    const top = document.querySelector('#httpMethodTop');
    if (top) top.addEventListener('change', syncHttpMethod);
    syncHttpMethod(); // مقدار اولیه را هم بنویس
});


/* ========== Export to window so inline onclick handlers work ========== */
window.addParamRow = addParamRow;
window.removeParamRow = removeParamRow;
window.openReqModal = openReqModal;
window.addBodyFieldRow = addBodyFieldRow;
window.addBodyFieldAndOpen = addBodyFieldAndOpen;
window.saveRequestFields = saveRequestFields;
window.addResponsePart = addResponsePart;
window.removeRespPartRow = removeRespPartRow;
window.openResPartFields = openResPartFields;
window.addResPartFieldRow = addResPartFieldRow;
window.saveResPartFields = saveResPartFields;
window.deleteEndpoint = deleteEndpoint;


// ========= Security box helpers =========
(function(){
    function show(el, on){ if(el){ el.classList[on?'remove':'add']('hidden'); } }

    function refreshSecurityVisibility(valOpt){
        var sel = document.getElementById('authTypeSelect');
        var v = (valOpt || (sel ? sel.value : 'NONE') || 'NONE').toUpperCase();
        document.querySelectorAll('.auth-block').forEach(function(block){
            var kind = (block.getAttribute('data-auth') || '').toUpperCase();
            show(block, kind === v);
        });
    }

    // global handlers
    window.onAuthTypeChange = function(val){ refreshSecurityVisibility(val); };

    window.addRoleRow = function(){
        var tbody = document.querySelector('#rolesTable tbody');
        if(!tbody) return;
        var idx = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td><input type="text" name="roles['+idx+'].name" placeholder="ROLE_USER"/></td>' +
            '<td><input type="text" name="roles['+idx+'].desc" placeholder="کاربر"/></td>' +
            '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeRow(this)">حذف</button></td>';
        tbody.appendChild(tr);
    };

    window.addRuleRow = function(){
        var tbody = document.querySelector('#rulesTable tbody');
        if(!tbody) return;
        var idx = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td><input type="text" name="rules['+idx+'].pathPattern" placeholder="/api/**"/></td>' +
            '<td><select name="rules['+idx+'].httpMethod">' +
            '<option>ANY</option><option>GET</option><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option>' +
            '</select></td>' +
            '<td><input type="text" name="rules['+idx+'].requirement" placeholder="authenticated"/></td>' +
            '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeRow(this)">حذف</button></td>';
        tbody.appendChild(tr);
    };

    window.removeRow = function(btn){
        var tr = btn && btn.closest('tr');
        if(!tr) return;
        tr.remove();
        // اختیاری: Reindex کردن name ها اگر لازم شد
    };

    // init on page load
    document.addEventListener('DOMContentLoaded', function(){
        refreshSecurityVisibility(); // اعمال وضعیت اولیه
    });


})();




