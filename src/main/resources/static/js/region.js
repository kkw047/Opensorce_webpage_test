// /api/regions/dos , /api/regions/sis?do=경기도
(function () {
    async function fetchJSON(url) {
        const res = await fetch(url);
        if (!res.ok) throw new Error('네트워크 오류');
        return await res.json();
    }

    async function fillDo(doSel) {
        const dos = await fetchJSON('/api/regions/dos');
        doSel.innerHTML = '';
        dos.forEach(d => {
            const o = document.createElement('option');
            o.value = d; o.textContent = d;
            doSel.appendChild(o);
        });
    }

    async function fillSi(doSel, siSel) {
        const val = doSel.value;
        const sis = val ? await fetchJSON('/api/regions/sis?do=' + encodeURIComponent(val)) : [];
        siSel.innerHTML = '';
        const all = document.createElement('option');
        all.value = ''; all.textContent = '전체';
        siSel.appendChild(all);

        sis.forEach(s => {
            const o = document.createElement('option');
            o.value = o.textContent = s;
            siSel.appendChild(o);
        });
    }

    window.__initRegions = async function (doId, siId) {
        const doSel = document.getElementById(doId);
        const siSel = document.getElementById(siId);
        await fillDo(doSel);
        await fillSi(doSel, siSel);
        doSel.addEventListener('change', () => fillSi(doSel, siSel));
    }
})();
