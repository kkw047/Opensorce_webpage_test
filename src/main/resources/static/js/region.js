export async function loadDos(selectEl) {
    selectEl.innerHTML = `<option value="">도(전체)</option>`;
    const res = await fetch('/api/regions/dos');
    const list = await res.json();
    list.forEach(d => {
        const opt = document.createElement('option');
        opt.value = d; opt.textContent = d;
        selectEl.appendChild(opt);
    });
}

export async function loadSis(doValue, selectEl, includeAll = true) {
    selectEl.innerHTML = includeAll ? `<option value="">시/군/구(전체)</option>` : `<option value="">선택</option>`;
    if (!doValue) return;
    const res = await fetch('/api/regions/sis?do=' + encodeURIComponent(doValue));
    const list = await res.json();
    list.forEach(si => {
        const opt = document.createElement('option');
        opt.value = si; opt.textContent = si;
        selectEl.appendChild(opt);
    });
}
