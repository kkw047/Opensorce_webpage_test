/* ============== 유틸 ============== */
async function fetchJSON(url){
    const res = await fetch(url,{headers:{"Accept":"application/json"}});
    if(!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
}
function fillSelect(sel, items, firstLabel){
    const keep = sel.value;
    sel.innerHTML = "";
    const o0 = document.createElement("option");
    o0.value = ""; o0.textContent = firstLabel ?? "전체";
    sel.appendChild(o0);
    items.forEach(s=>{
        const o = document.createElement("option");
        o.value = s; o.textContent = s; sel.appendChild(o);
    });
    if([...sel.options].some(o=>o.value===keep)) sel.value = keep;
}

/* ============== 검색 영역 ============== */
async function initSearchRegion(){
    const d = document.getElementById("searchDo");
    const s = document.getElementById("searchSi");
    if(!d || !s) return;
    if(d.value){
        const sis = await fetchJSON(`/api/regions/si?do=${encodeURIComponent(d.value)}`);
        fillSelect(sis ? s : s, sis, "전체");
    }
    d.addEventListener("change", async ()=>{
        const sis = d.value ? await fetchJSON(`/api/regions/si?do=${encodeURIComponent(d.value)}`) : [];
        fillSelect(s, sis, "전체");
    });
}

/* ============== 모임 만들기 오버레이 ============== */
const overlay = {
    el: null, panel: null, closeBtn: null, backdrop: null,
    open(){ if(this.el){ this.el.hidden = false; } },
    close(){ if(this.el){ this.el.hidden = true; } }
};

function initCreatePanel(){
    overlay.el = document.getElementById("createOverlay");
    overlay.panel = overlay.el?.querySelector(".create-panel");
    overlay.closeBtn = document.getElementById("closeCreatePanelBtn");
    overlay.backdrop = document.getElementById("createBackdrop");

    // 기본은 반드시 닫혀있게
    if(overlay.el) overlay.el.hidden = true;

    // 열기/닫기 버튼
    const openBtn = document.getElementById("openCreatePanelBtn");
    if(openBtn) openBtn.addEventListener("click", ()=>overlay.open());
    if(overlay.closeBtn) overlay.closeBtn.addEventListener("click", ()=>overlay.close());
    if(overlay.backdrop) overlay.backdrop.addEventListener("click", ()=>overlay.close());
    document.addEventListener("keydown", (e)=>{ if(e.key==="Escape") overlay.close(); });

    // 지역 연동
    const doSel = document.getElementById("createDo");
    const siSel = document.getElementById("createSi");
    if(doSel && siSel){
        doSel.addEventListener("change", async ()=>{
            const sis = doSel.value ? await fetchJSON(`/api/regions/si?do=${encodeURIComponent(doSel.value)}`) : [];
            fillSelect(siSel, sis, "시/군/구");
        });
    }

    // 이미지 미리보기
    const file = document.getElementById("imageFile");
    const preview = document.getElementById("imagePreview");
    if(file && preview){
        file.addEventListener("change", ()=>{
            const f = file.files?.[0];
            if(!f){ preview.innerHTML = "이미지 미리보기"; return; }
            const img = document.createElement("img");
            img.src = URL.createObjectURL(f);
            preview.innerHTML = ""; preview.appendChild(img);
        });
    }

    // 새 카테고리(쉼표 다중) → 칩 + hidden
    const addBtn = document.getElementById("addCategoryBtn");
    const input  = document.getElementById("newCategoryInput");
    const chips  = document.getElementById("newCategoryChips");
    const hidden = document.getElementById("newCategoryHidden");

    const addChip = (name)=>{
        const n = (name||"").trim(); if(!n) return;
        if([...chips.querySelectorAll(".chip")].some(c=>c.dataset.name===n)) return;
        const chip = document.createElement("span");
        chip.className="chip"; chip.dataset.name=n; chip.textContent = `#${n}`;
        chip.title = "클릭하면 제거";
        chip.addEventListener("click", ()=>{
            chip.remove();
            hidden.querySelectorAll(`input[name="newCategoryNames"]`).forEach(h=>{
                if(h.value===n) h.remove();
            });
        });
        chips.appendChild(chip);

        const h=document.createElement("input");
        h.type="hidden"; h.name="newCategoryNames"; h.value=n;
        hidden.appendChild(h);
    };

    const addFromInput=()=>{
        (input.value||"")
            .split(",")
            .map(s=>s.trim())
            .filter(Boolean)
            .forEach(addChip);
        input.value="";
    };

    if(addBtn) addBtn.addEventListener("click", addFromInput);
    if(input)  input.addEventListener("keydown",(e)=>{ if(e.key==="Enter"){e.preventDefault(); addFromInput();}});

    // 중복 제출 방지 + 성공 후 닫힘 (PRG 리다이렉트로 기본 닫힘이지만 방어)
    const form = document.getElementById("createForm");
    if(form){
        form.addEventListener("submit", ()=>{
            const btn = form.querySelector('button[type="submit"]');
            if(btn){ btn.disabled = true; btn.textContent="생성 중..."; }
            // 혹시 SPA처럼 보일 상황 대비 → 즉시 닫기
            overlay.close();
        });
    }
}

/* ============== 모달 ============== */
function modal(id, open){ const m=document.getElementById(id); if(m) m.hidden=!open; }
function initAuthModals(){
    const loginBtn=document.getElementById("openLoginBtn");
    const regBtn=document.getElementById("openRegisterBtn");
    if(loginBtn) loginBtn.addEventListener("click",()=>modal("loginModal",true));
    if(regBtn)   regBtn.addEventListener("click",()=>modal("registerModal",true));
    document.querySelectorAll("[data-close]").forEach(el=>{
        el.addEventListener("click",()=>modal(el.dataset.close.slice(1),false));
    });
}

/* ============== 부트스트랩 ============== */
window.addEventListener("DOMContentLoaded", ()=>{
    initSearchRegion();
    initCreatePanel();
    initAuthModals();

    // 서버 플래시 토스트
    const t = document.getElementById("toast");
    if(t){ setTimeout(()=>t.classList.remove("show"), 2000); }
});
