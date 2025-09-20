/* ===== Helpers ===== */
function $(sel, root=document){ return root.querySelector(sel); }
function $all(sel, root=document){ return Array.from(root.querySelectorAll(sel)); }
function addHidden(container, name, value){
    const i=document.createElement('input'); i.type='hidden'; i.name=name; i.value=value; container.appendChild(i);
}

/* انواع اسکالر آماده برای combo */
const JAVA_TYPES = ["String","Long","Integer","Double","Boolean","UUID","LocalDate","LocalDateTime"];

/* ===== Endpoint toggles ===== */
const methodEl = document.getElementById('httpMethod');
const usePath  = document.getElementById('usePathChk');
const endpointPath = document.getElementById('endpointPath');

function toggleBodyCard(){
    const bodyCard = document.getElementById('bodyCard');
    const hiddenReq = document.getElementById('hiddenRequest');
    const isPost = (methodEl?.value || 'GET').toUpperCase() === 'POST';
    if (bodyCard){
        bodyCard.style.display = isPost ? '' : 'none';
        if (!isPost && hiddenReq) hiddenReq.innerHTML = '';
    }
}
function toggleEndpointPath(){
    const on = !!(usePath && usePath.checked);
    if (endpointPath){ endpointPath.disabled = !on; if(!on) endpointPath.value=''; }
}

/* ===== URL/HEADER params ===== */
function addParamRow(){
    const tbody = document.getElementById('paramsBody');
    const idx   = tbody.querySelectorAll('tr').length;
    const tmpl  = document.getElementById('paramRowTemplate').cloneNode(true);
    tmpl.id='';
    tmpl.querySelector('input[name="__NAME__"]').name = `params[${idx}].name`;
    tmpl.querySelector('select[name="__IN__"]').name   = `params[${idx}].in`;
    tmpl.querySelector('select[name="__TYPE__"]').name = `params[${idx}].javaType`;
    tmpl.querySelector('input[name="__REQ__"]').name   = `params[${idx}].required`;
    tbody.appendChild(tmpl);
}
function removeRow(btn){ btn.closest('tr').remove(); }

/* ===== Modals ===== */
function openModal(id){ document.getElementById(id).style.display='flex'; }
function closeModal(id){ document.getElementById(id).style.display='none'; }

/* Request Body fields */
function addFieldRow(tableId){
    const tbody = document.querySelector(`#${tableId} tbody`);
    const tr = document.getElementById('fieldRowTemplate').cloneNode(true); tr.id='';
    tbody.appendChild(tr);
}
function saveRequestFields(){
    const rows = $all('#reqTable tbody tr');
    const hidden = document.getElementById('hiddenRequest');
    hidden.innerHTML='';
    rows.forEach((tr,i)=>{
        const name = tr.querySelector('input[type=text]').value.trim();
        if(!name) return;
        const type = tr.querySelector('select').value;
        const req  = tr.querySelector('input[type=checkbox]').checked;
        addHidden(hidden, `requestFields[${i}].name`, name);
        addHidden(hidden, `requestFields[${i}].javaType`, type);
        addHidden(hidden, `requestFields[${i}].required`, req?'true':'false');
    });
    closeModal('reqModal');
}

/* ===== Response Parts ===== */
const respTableBody = document.querySelector("#respPartsTable tbody");

function addResponsePart(){
    const tr = document.getElementById('respPartRowTemplate').cloneNode(true); tr.id='';
    respTableBody.appendChild(tr);
    wireResponsePartRow(tr);
    syncResponsePartsHidden();
}

/* فیلد چهارم (نوع اسکالر/نام مدل) را بسته به Container/Kind بین input و select سوئیچ می‌کنیم */
function wireResponsePartRow(tr){
    let inputs = tr.querySelectorAll('input,select');
    const nameEl = inputs[0];
    const containerEl = inputs[1];
    const kindEl = inputs[2];
    let typeOrNameEl = inputs[3]; // ممکن است بعداً به select تبدیل شود
    const fieldsBtn = tr.querySelector('button.btn');

    function ensureScalarEditor(container, kind){
        const cell = typeOrNameEl.parentElement;
        if (kind==='SCALAR' && container==='SINGLE'){
            // به combo تبدیل شود
            if (typeOrNameEl.tagName.toLowerCase() !== 'select'){
                const current = typeOrNameEl.value?.trim() || '';
                const sel = document.createElement('select');
                JAVA_TYPES.forEach(t=>{ const o=document.createElement('option'); o.value=t; o.textContent=t; sel.appendChild(o); });
                sel.value = JAVA_TYPES.includes(current) ? current : 'String';
                cell.replaceChild(sel, typeOrNameEl);
                typeOrNameEl = sel;
            }
            fieldsBtn.disabled = true;
        } else {
            // به textbox تبدیل شود
            if (typeOrNameEl.tagName.toLowerCase() !== 'input'){
                const sel = typeOrNameEl;
                const inp = document.createElement('input');
                inp.type='text';
                inp.placeholder = (kind==='SCALAR') ? 'String (برای Scalar)' : 'OrderItemDto (نام مدل)';
                inp.value = sel.value || '';
                cell.replaceChild(inp, sel);
                typeOrNameEl = inp;
            }
            fieldsBtn.disabled = (kind!=='OBJECT') ? true : false;
        }
    }

    function updateEditors(){
        const cont = (containerEl.value || 'SINGLE').toUpperCase();
        const kind = (kindEl.value || 'SCALAR').toUpperCase();
        ensureScalarEditor(cont, kind);
        syncResponsePartsHidden();
    }

    [nameEl, containerEl, kindEl].forEach(el => el.addEventListener('change', updateEditors));
    [nameEl].forEach(el => el.addEventListener('input', syncResponsePartsHidden));
    // مقدار اولیه
    updateEditors();
}

function openResPartFields(btn){
    const tr = btn.closest('tr');
    const idx = Array.from(respTableBody.children).indexOf(tr);
    if (idx < 0) return;

    document.getElementById('resPartIndex').value = idx;

    const tbody = document.querySelector('#resPartFieldsTable tbody');
    tbody.innerHTML='';

    const hidden = document.getElementById('hiddenResponseParts');
    const existing = $all(`input[name^="responseParts[${idx}].fields"]`, hidden);
    const grouped = {};
    existing.forEach(inp=>{
        const m = inp.name.match(/responseParts\[(\d+)]\.fields\[(\d+)]\.(name|javaType|required)/);
        if(!m) return;
        const j=parseInt(m[2],10), key=m[3];
        grouped[j] = grouped[j] || {};
        grouped[j][key] = inp.value;
    });
    Object.keys(grouped).sort((a,b)=>a-b).forEach(j=>{
        const row = document.getElementById('resPartFieldTemplate').cloneNode(true); row.id='';
        row.querySelector('input[type=text]').value = grouped[j].name || '';
        row.querySelector('select').value = grouped[j].javaType || 'String';
        row.querySelector('input[type=checkbox]').checked = (grouped[j].required === 'true');
        tbody.appendChild(row);
    });

    openModal('resPartModal');
}
function addResPartFieldRow(){
    const row = document.getElementById('resPartFieldTemplate').cloneNode(true); row.id='';
    document.querySelector('#resPartFieldsTable tbody').appendChild(row);
}
function saveResPartFields(){
    const idx = parseInt(document.getElementById('resPartIndex').value || '-1', 10);
    if (idx < 0) return;

    const hidden = document.getElementById('hiddenResponseParts');
    $all(`input[name^="responseParts[${idx}].fields"]`, hidden).forEach(e=>e.remove());

    const rows = $all('#resPartFieldsTable tbody tr');
    rows.forEach((tr,j)=>{
        const name = tr.querySelector('input[type=text]').value.trim();
        if(!name) return;
        const type = tr.querySelector('select').value;
        const req  = tr.querySelector('input[type=checkbox]').checked;
        addHidden(hidden, `responseParts[${idx}].fields[${j}].name`, name);
        addHidden(hidden, `responseParts[${idx}].fields[${j}].javaType`, type);
        addHidden(hidden, `responseParts[${idx}].fields[${j}].required`, req?'true':'false');
    });

    closeModal('resPartModal');
}

function syncResponsePartsHidden(){
    const hidden = document.getElementById('hiddenResponseParts');
    $all(`input[name^="responseParts["]:not([name*=".fields["])`, hidden).forEach(e=>e.remove());

    $all("#respPartsTable tbody tr").forEach((tr,i)=>{
        const [nameEl, containerEl, kindEl, typeOrNameEl] = tr.querySelectorAll('input,select');
        const name = (nameEl.value || '').trim(); if(!name) return;
        const container = (containerEl.value || 'SINGLE').toUpperCase();
        const kind = (kindEl.value || 'SCALAR').toUpperCase();
        const v = (typeOrNameEl.value || '').trim();

        addHidden(hidden, `responseParts[${i}].name`, name);
        addHidden(hidden, `responseParts[${i}].container`, container);
        addHidden(hidden, `responseParts[${i}].kind`, kind);
        if (kind === 'SCALAR') {
            addHidden(hidden, `responseParts[${i}].scalarType`, v || 'String');
        } else if (v) {
            addHidden(hidden, `responseParts[${i}].objectName`, v);
        }
    });
}

/* ===== Init ===== */
document.addEventListener('DOMContentLoaded', ()=>{
    toggleBodyCard();
    toggleEndpointPath();
    methodEl?.addEventListener('change', toggleBodyCard);
    usePath?.addEventListener('change', toggleEndpointPath);

    // اگر هیچ بخشی تعریف نشده، یک ردیف بساز
    if (document.querySelector("#respPartsTable tbody").children.length === 0) addResponsePart();
});
