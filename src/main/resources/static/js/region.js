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
        siSel.innerHTML = '';
        if (!doSel.value) return;
        const sis = await fetchJSON('/api/regions/sis?do=' + encodeURIComponent(doSel.value));
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
