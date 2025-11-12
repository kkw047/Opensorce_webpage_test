// 공용: 도 선택 시 시/군/구 채우기
document.addEventListener('DOMContentLoaded', () => {
    const pairs = [
        {doSel: '#searchDo', siSel: '#searchSi'},
        {doSel: '#createDo', siSel: '#createSi'},
        {doSel: '#regDo', siSel: '#regSi'}
    ];

    async function fillSi(doValue, siSelect) {
        siSelect.innerHTML = '<option value="">전체</option>';
        if (!doValue) return;
        const res = await fetch('/api/regions/sis?do=' + encodeURIComponent(doValue));
        const arr = await res.json();
        siSelect.innerHTML = '';
        arr.forEach(s => {
            const op = document.createElement('option');
            op.value = s; op.textContent = s; siSelect.appendChild(op);
        });
    }

    pairs.forEach(p => {
        const doEl = document.querySelector(p.doSel);
        const siEl = document.querySelector(p.siSel);
        if (!doEl || !siEl) return;
        // 초기값 적용
        if (doEl.value) fillSi(doEl.value, siEl);
        doEl.addEventListener('change', () => fillSi(doEl.value, siEl));
    });
});
