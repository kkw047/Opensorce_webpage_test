// region.js — 도/시군구 공통 바인딩 (검색/회원가입/모임만들기 전부 지원)

async function getJSON(url) {
    const res = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!res.ok) throw new Error("HTTP " + res.status);
    return await res.json();
}

function fillSelect(selectEl, items, placeholderText) {
    if (!selectEl) return;
    selectEl.innerHTML = "";
    // placeholder
    if (placeholderText !== undefined) {
        const ph = document.createElement("option");
        ph.value = "";
        ph.textContent = placeholderText;
        selectEl.appendChild(ph);
    }
    // options
    (items || []).forEach(v => {
        const opt = document.createElement("option");
        opt.value = v;
        opt.textContent = v;
        selectEl.appendChild(opt);
    });
}

async function wireDoSi(doId, siId, opts = {}) {
    const doSel = document.getElementById(doId);
    const siSel = document.getElementById(siId);
    if (!doSel || !siSel) return;

    const phDo = doSel.dataset.placeholder || "전체";
    const phSi = siSel.dataset.placeholder || "전체";

    const initialDo = opts.initialDo || doSel.dataset.selectedDo || doSel.value || "";
    const initialSi = opts.initialSi || siSel.dataset.selectedSi || siSel.value || "";

    try {
        // 도 목록
        let dos = null;
        if (!opts.skipLoadDos) {
            dos = await getJSON("/api/regions/dos");
            fillSelect(doSel, dos, phDo);

            if (initialDo && dos.includes(initialDo)) {
                doSel.value = initialDo;
            }
        }

        const refreshSi = async () => {
            const selDo = doSel.value || initialDo;
            if (!selDo) {
                fillSelect(siSel, [], phSi);
                return;
            }
            const sis = await getJSON("/api/regions/si?do=" + encodeURIComponent(selDo));
            fillSelect(siSel, sis, phSi);

            if (initialSi && sis.includes(initialSi)) {
                siSel.value = initialSi;
            }
        };

        doSel.addEventListener("change", refreshSi);
        await refreshSi();
    } catch (e) {
        console.error("Region wiring failed", e);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    // 메인 검색바
    wireDoSi("searchDo", "searchSi", { skipLoadDos: true });
    // 회원가입 모달
    wireDoSi("regDo", "regSi");
    // 우측 '모임 만들기' 패널
    wireDoSi("createDo", "createSi", { phDo: "도 선택", phSi: "시/군/구" });
});
