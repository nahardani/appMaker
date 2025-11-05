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
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    return {token, header};
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
(function () {
    function show(el, on) {
        if (el) {
            el.classList[on ? 'remove' : 'add']('hidden');
        }
    }

    function refreshSecurityVisibility(valOpt) {
        var sel = document.getElementById('authTypeSelect');
        var v = (valOpt || (sel ? sel.value : 'NONE') || 'NONE').toUpperCase();
        document.querySelectorAll('.auth-block').forEach(function (block) {
            var kind = (block.getAttribute('data-auth') || '').toUpperCase();
            show(block, kind === v);
        });
    }

    // global handlers
    window.onAuthTypeChange = function (val) {
        refreshSecurityVisibility(val);
    };

    window.addRoleRow = function () {
        var tbody = document.querySelector('#rolesTable tbody');
        if (!tbody) return;
        var idx = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td><input type="text" name="roles[' + idx + '].name" placeholder="ROLE_USER"/></td>' +
            '<td><input type="text" name="roles[' + idx + '].desc" placeholder="کاربر"/></td>' +
            '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeRow(this)">حذف</button></td>';
        tbody.appendChild(tr);
    };

    window.addRuleRow = function () {
        var tbody = document.querySelector('#rulesTable tbody');
        if (!tbody) return;
        var idx = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td><input type="text" name="rules[' + idx + '].pathPattern" placeholder="/api/**"/></td>' +
            '<td><select name="rules[' + idx + '].httpMethod">' +
            '<option>ANY</option><option>GET</option><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option>' +
            '</select></td>' +
            '<td><input type="text" name="rules[' + idx + '].requirement" placeholder="authenticated"/></td>' +
            '<td><button type="button" class="btn btn-danger btn-sm" onclick="removeRow(this)">حذف</button></td>';
        tbody.appendChild(tr);
    };


    let EP_CTX = {projectId: null, ctrlName: null};

    function qs(sel) {
        return document.querySelector(sel);
    }

    function openEpModal(projectId, ctrlName) {
        if (!projectId || !ctrlName) {
            alert('ابتدا یک کنترلر از سایدبار انتخاب یا ایجاد کنید.');
            return;
        }
        EP_CTX = {projectId, ctrlName};
        qs('#epModalCtrlLabel').textContent = `کنترلر: ${ctrlName}`;
        resetEpForm();
        loadEndpoints();
        qs('#epModal').style.display = 'block';
    }

    function closeEpModal() {
        qs('#epModal').style.display = 'none';
    }

    function resetEpForm() {
        qs('#epIndex').value = '';
        qs('#epNameNew').value = '';
        qs('#epMethod').value = 'GET';
        qs('#epPath').value = '';
    }

    async function loadEndpoints() {
        const {projectId, ctrlName} = EP_CTX;
        try {
            const r = await fetch(`/wizard/${encodeURIComponent(projectId)}/controllers/${encodeURIComponent(ctrlName)}/endpoints/json`);
            const data = await r.json();
            if (!r.ok) {
                throw new Error(data.message || 'load failed');
            }
            renderEndpointList(data || []);
            // کمبوی اصلی صفحه را هم ریفرش کن
            refreshEndpointSelect(data || []);
        } catch (e) {
            qs('#epList').innerHTML = `<div class="muted">خطا در بارگذاری: ${e.message}</div>`;
        }
    }

    function renderEndpointList(list) {
        const c = qs('#epList');
        if (!list || !list.length) {
            c.innerHTML = `<div class="muted">هنوز اندپوینتی ثبت نشده است.</div>`;
            return;
        }
        c.innerHTML = '';
        list.forEach((ep, idx) => {
            const row = document.createElement('div');
            row.className = 'row';
            row.innerHTML = `
        <div class="muted">${idx + 1}</div>
        <div><code>${ep.name || ''}</code></div>
        <div><span class="chip">${ep.httpMethod || ''}</span></div>
        <div><code>/${(ep.path || '')}</code></div>
        <div style="display:flex;gap:8px;justify-content:flex-end">
          <button class="btn" onclick="editEp(${idx})">ویرایش</button>
          <button class="btn btn-secondary" onclick="deleteEp(${idx})">حذف</button>
        </div>
      `;
            c.appendChild(row);
        });
    }

    function editEp(idx) {
        const row = qs(`#epList .row:nth-child(${idx + 1})`);
        // بهتر: از سرویس بخوانیم؛ ساده‌تر: یک بار دیگر loadEndpoints و ذخیره کنیم.
        // چون loadEndpoints صدا خورده، می‌توانیم داده را از سرور بخوانیم:
        fetch(`/wizard/${encodeURIComponent(EP_CTX.projectId)}/controllers/${encodeURIComponent(EP_CTX.ctrlName)}/endpoints/json`)
            .then(r => r.json())
            .then(list => {
                const ep = list[idx];
                if (!ep) return;
                qs('#epIndex').value = idx;
                qs('#epNameNew').value = ep.name || '';
                qs('#epMethod').value = ep.httpMethod || 'GET';
                qs('#epPath').value = ep.path || '';
            });
    }

    async function saveEndpoint(e) {
        e.preventDefault();
        const {projectId, ctrlName} = EP_CTX;
        const idx = qs('#epIndex').value;
        const body = {
            index: (idx === '' ? null : Number(idx)),
            name: qs('#epNameNew').value?.trim(),
            httpMethod: qs('#epMethod').value,
            path: qs('#epPath').value?.trim()
        };
        if (!body.name || !body.httpMethod || !body.path) {
            alert('نام/متد/مسیر الزامی است.');
            return false;
        }

        // CSRF
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {'Content-Type': 'application/json'};
        if (token && header) headers[header] = token;

        const r = await fetch(`/wizard/${encodeURIComponent(projectId)}/controllers/${encodeURIComponent(ctrlName)}/endpoints`, {
            method: 'POST', headers, body: JSON.stringify(body)
        });
        const data = await r.json().catch(() => ({}));
        if (!r.ok || !data.ok) {
            alert(data.message || 'ذخیره ناموفق بود.');
            return false;
        }
        resetEpForm();
        await loadEndpoints();
        return false;
    }

    async function deleteEp(idx) {
        if (!confirm('حذف این اندپوینت؟')) return;
        const {projectId, ctrlName} = EP_CTX;

        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {};
        if (token && header) headers[header] = token;

        const r = await fetch(`/wizard/${encodeURIComponent(projectId)}/controllers/${encodeURIComponent(ctrlName)}/endpoints/${idx}`, {
            method: 'DELETE', headers
        });
        const data = await r.json().catch(() => ({}));
        if (!r.ok || !data.ok) {
            alert(data.message || 'حذف ناموفق بود.');
            return;
        }
        await loadEndpoints();
    }

    function refreshEndpointSelect(list) {
        const sel = qs('#endpointSelect');
        if (!sel) return;
        sel.innerHTML = '';
        (list || []).forEach(ep => {
            const opt = document.createElement('option');
            opt.value = ep.name;
            opt.textContent = `${ep.name}  —  [${ep.httpMethod}] /${ep.path}`;
            sel.appendChild(opt);
        });
    }


    window.removeRow = function (btn) {
        var tr = btn && btn.closest('tr');
        if (!tr) return;
        tr.remove();
        // اختیاری: Reindex کردن name ها اگر لازم شد
    };

    // init on page load
    document.addEventListener('DOMContentLoaded', function () {
        refreshSecurityVisibility(); // اعمال وضعیت اولیه
    });

    function currentCtx() {
        const pid  = document.getElementById('projectId')?.value;
        const ctrl = document.getElementById('ctrlName')?.value || document.querySelector('.tag')?.textContent?.trim();
        const ep   = document.getElementById('endpointSelect')?.value;
        return {pid, ctrl, ep};
    }

// ----- Prompt Modal -----
    async function openPromptModal(){
        const {pid, ctrl, ep} = currentCtx();
        if(!pid || !ctrl || !ep){ alert('ابتدا اندپوینت را انتخاب کنید.'); return; }
        try{
            const r = await fetch(`/wizard/${encodeURIComponent(pid)}/controllers/${encodeURIComponent(ctrl)}/endpoints/${encodeURIComponent(ep)}/prompt`);
            const data = await r.json();
            if(!data.ok){ throw new Error('load failed'); }
            document.getElementById('promptTextarea').value = data.prompt || '';
            document.getElementById('promptMeta').textContent = data.updatedAt ? ('آخرین ویرایش: ' + new Date(data.updatedAt).toLocaleString()) : '';
            document.getElementById('promptModal').style.display = 'block';
        }catch(e){ alert('خطا در بارگذاری پرامپت'); }
    }

    function closePromptModal() {
        document.getElementById('promptModal').style.display = 'none';
    }

    async function savePrompt(){
        const {pid, ctrl, ep} = currentCtx();
        const prompt = document.getElementById('promptTextarea').value || '';
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {'Content-Type':'application/json'}; if (token && header) headers[header]=token;

        const r = await fetch(`/wizard/${encodeURIComponent(pid)}/controllers/${encodeURIComponent(ctrl)}/endpoints/${encodeURIComponent(ep)}/prompt`, {
            method:'POST', headers, body: JSON.stringify({prompt})
        });
        const data = await r.json().catch(()=>({}));
        if(!r.ok || !data.ok){ alert('ذخیره ناموفق بود'); return; }
        closePromptModal();
    }

// ----- Raw Modal -----
    async function openRawModal(){
        const {pid, ctrl, ep} = currentCtx();
        if(!pid || !ctrl || !ep){ alert('ابتدا اندپوینت را انتخاب کنید.'); return; }
        try{
            const r = await fetch(`/wizard/${encodeURIComponent(pid)}/controllers/${encodeURIComponent(ctrl)}/endpoints/${encodeURIComponent(ep)}/ai/raw`);
            const data = await r.json();
            if(!data.ok){ throw new Error('load failed'); }
            document.getElementById('rawViewer').textContent = data.raw || '(خالی)';
            document.getElementById('rawMeta').textContent = data.updatedAt ? ('آخرین به‌روزرسانی: ' + new Date(data.updatedAt).toLocaleString()) : '';
            document.getElementById('rawModal').style.display = 'block';
        }catch(e){ alert('خطا در بارگذاری خروجی AI'); }
    }

    function closeRawModal() {
        document.getElementById('rawModal').style.display = 'none';
    }


    function getIdsForAI() {
        const pid  = document.getElementById('projectId')?.value || '';
        const ctrl = document.getElementById('ctrlName')?.value   || '';
        // اندپوینت را همیشه از کمبوی بالای صفحه بگیر
        const ep   = document.getElementById('endpointSelect')?.value || '';
        return { pid, ctrl, ep };
    }

    async function hydrateAiBoxFromSaved() {
        const { pid, ctrl, ep } = getIdsForAI();
        if (!pid || !ctrl || !ep) return;

        // پرامپت ذخیره‌شده → textarea#prompt
        try {
            const r = await fetch(`/wizard/${encodeURIComponent(pid)}/controllers/${encodeURIComponent(ctrl)}/endpoints/${encodeURIComponent(ep)}/prompt`);
            const data = await r.json().catch(()=> ({}));
            if (r.ok && data?.ok && typeof data.prompt === 'string') {
                const ta = document.getElementById('prompt');
                if (ta) ta.value = data.prompt || '';
            }
        } catch (_) {}

        // RAW ذخیره‌شده → pre#raw
        try {
            const r2 = await fetch(`/wizard/${encodeURIComponent(pid)}/controllers/${encodeURIComponent(ctrl)}/endpoints/${encodeURIComponent(ep)}/ai/raw`);
            const d2 = await r2.json().catch(()=> ({}));
            if (r2.ok && d2?.ok && typeof d2.raw === 'string') {
                const pre = document.getElementById('raw');
                if (pre) pre.textContent = d2.raw || '';
                // اگر parse-file‌ها هم داشتی، همون‌جا می‌تونی renderFiles(...) رو صدا بزنی
            }
        } catch (_) {}
    }

// ترایگرهای مینیمال: روی لود و روی تغییر اندپوینت
    document.addEventListener('DOMContentLoaded', () => {
        hydrateAiBoxFromSaved(); // دفعه‌ی اول که صفحه بالا می‌آد

        const sel = document.getElementById('endpointSelect');
        if (sel) {
            sel.addEventListener('change', () => {
                hydrateAiBoxFromSaved(); // هر بار اندپوینت عوض شد
            });
        }

        // اگر aiBox داخل <details> است و “باز/بسته” می‌شود:
        const det = document.getElementById('aiBoxToggler');
        if (det) {
            det.addEventListener('toggle', () => {
                if (det.open) hydrateAiBoxFromSaved();
            });
        }
    });


    window.openEpModal = openEpModal;
    window.closeEpModal = closeEpModal;
    window.saveEndpoint = saveEndpoint;
    window.deleteEp = deleteEp;
    window.editEp = editEp;
    window.openRawModal = openRawModal;
    window.openPromptModal = openPromptModal;
    window.closePromptModal = closePromptModal;
    window.closeRawModal = closeRawModal;
    window.savePrompt = savePrompt;
    window.getIdsForAI = getIdsForAI;



})();




