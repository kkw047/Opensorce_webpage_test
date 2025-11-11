/* ====== Modal helpers (Login / Register) ====== */
function openModal(id){ document.getElementById(id)?.classList.add('open'); }
function closeModal(id){ document.getElementById(id)?.classList.remove('open'); }
document.addEventListener('click', (e)=>{
    if(e.target.classList.contains('modal')) e.target.classList.remove('open');
});

/* ====== Common fetch helpers ====== */
async function jget(url){
    const r = await fetch(url, {headers:{'Accept':'application/json'}});
    if(!r.ok) throw new Error(await r.text());
    return r.json();
}
async function jpost(url, body){
    const r = await fetch(url, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
    if(!r.ok) throw new Error(await r.text());
    return r.json();
}

/* ====== Regions & Categories ====== */
async function loadDos(){ return jget('/api/regions/do'); }
async function loadSis(rdo){ return jget('/api/regions/si?rdo='+encodeURIComponent(rdo)); }
async function loadCategories(){ return jget('/api/categories'); } // GET 목록 (서버에서 제공)

/* ====== Clubs search & render ====== */
async function searchClubs(params){
    const qs = new URLSearchParams(params);
    return jget('/api/clubs?'+qs.toString()); // GET 검색 (Page JSON 가정: {content:[], totalElements...})
}

function clubCard(c){
    const cats = (c.categories||[]).map(k=>`<span class="badge"># ${k.name||k}</span>`).join('');
    const cover = c.imageUrl || '';
    return `
    <div class="club">
      <img class="cover" src="${cover}" alt="">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:8px">
        <div style="font-weight:700">${c.name}</div>
        <div class="badge">${c.regionDo||''} ${c.regionSi||''}</div>
      </div>
      <div style="color:#475569;font-size:14px">${(c.description||'').substring(0,120)}</div>
      <div class="badges">${cats}</div>
    </div>
  `;
}

function renderClubs(list){
    const root = document.getElementById('clubList');
    root.innerHTML = list.map(clubCard).join('') || '<div class="card">검색 결과가 없습니다.</div>';
}

/* ====== Sidebar categories (filter only) ====== */
async function mountSidebarCategories(){
    const cats = await loadCategories();
    const holder = document.getElementById('sideCats');
    holder.innerHTML = cats.map(c=>`
    <label class="cat-row">
      <input type="checkbox" name="sideCat" value="${c.id}">
      <span># ${c.name}</span>
    </label>
  `).join('');
}

/* ====== Search row wiring ====== */
async function mountSearchBar(){
    const selDo = document.getElementById('srchDo');
    const selSi = document.getElementById('srchSi');
    const kw = document.getElementById('srchKw');
    const btn = document.getElementById('btnSearch');

    const dos = await loadDos();
    selDo.innerHTML = '<option value="">도(전체)</option>' + dos.map(d=>`<option value="${d}">${d}</option>`).join('');
    selDo.addEventListener('change', async ()=>{
        const v = selDo.value;
        selSi.innerHTML = '<option value="">시/군/구(전체)</option>';
        if(v){
            const sis = await loadSis(v);
            selSi.innerHTML = '<option value="">시/군/구(전체)</option>' + sis.map(s=>`<option value="${s}">${s}</option>`).join('');
        }
    });

    btn.addEventListener('click', runSearch);
    kw.addEventListener('keydown', (e)=>{ if(e.key==='Enter'){ e.preventDefault(); runSearch(); }});
}

/* run search uses both search-row & sidebar checked categories */
async function runSearch(){
    const rdo = document.getElementById('srchDo').value || '';
    const rsi = document.getElementById('srchSi').value || '';
    const kw  = document.getElementById('srchKw').value || '';
    const catChecked = Array.from(document.querySelectorAll('input[name="sideCat"]:checked')).map(i=>i.value);

    const params = new URLSearchParams();
    if(rdo) params.set('rdo', rdo);
    if(rsi) params.set('rsi', rsi);
    if(kw)  params.set('kw', kw);
    catChecked.forEach(v=>params.append('cats', v));
    params.set('page','0'); params.set('size','12');

    const page = await searchClubs(params);
    const list = page.content || page; // fallback
    renderClubs(list);
}

/* ====== Create Club panel (2) ====== */
function showCreatePanel(){
    const panel = document.getElementById('content');
    panel.innerHTML = `
    <div class="card">
      <h2 style="margin:0 0 10px 0;">모임 만들기</h2>
      <div class="form-grid">
        <div class="form-row">
          <label>모임 이름</label>
          <input id="c_name" class="input" type="text" placeholder="예) 주말 러닝 메이트">
        </div>
        <div class="form-row">
          <label>이미지 URL</label>
          <input id="c_img" class="input" type="url" placeholder="https://...">
        </div>
        <div class="form-row" style="grid-column:1/-1">
          <label>홍보 문구</label>
          <textarea id="c_desc" class="input textarea" placeholder="간단한 소개"></textarea>
        </div>

        <div class="form-row">
          <label>지역(도)</label>
          <select id="c_do" class="input"></select>
        </div>
        <div class="form-row">
          <label>지역(시/군/구)</label>
          <select id="c_si" class="input"><option value="">도 먼저 선택</option></select>
        </div>

        <div class="form-row" style="grid-column:1/-1">
          <label>카테고리</label>
          <div id="c_pills" class="pills"></div>
          <div style="display:grid;grid-template-columns:1fr auto;gap:8px;margin-top:8px">
            <input id="c_newcat" class="input" type="text" placeholder="+ 새 카테고리 이름 (모임 만들기에서만 추가 가능)">
            <button id="c_addcat" class="btn" type="button">+ 카테고리 추가</button>
          </div>
        </div>
      </div>

      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
        <div><img id="c_preview" class="preview" alt=""></div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-ghost" id="c_cancel">취소</button>
          <button class="btn btn-primary" id="c_submit">만들기</button>
        </div>
      </div>
    </div>
  `;

    // wire image preview
    const imgInput = document.getElementById('c_img');
    const preview  = document.getElementById('c_preview');
    imgInput.addEventListener('input', ()=>{ preview.src = imgInput.value.trim(); });

    // load dos & sis
    (async()=>{
        const dos = await loadDos();
        const selDo = document.getElementById('c_do');
        const selSi = document.getElementById('c_si');
        selDo.innerHTML = '<option value="">선택</option>' + dos.map(d=>`<option value="${d}">${d}</option>`).join('');
        selDo.addEventListener('change', async ()=>{
            const v = selDo.value;
            selSi.innerHTML = '<option value="">선택</option>';
            if(v){
                const sis = await loadSis(v);
                selSi.innerHTML = '<option value="">선택</option>' + sis.map(s=>`<option value="${s}">${s}</option>`).join('');
            }
        });
    })();

    // load categories as chips
    (async()=>{
        const cats = await loadCategories();
        const holder = document.getElementById('c_pills');
        holder.innerHTML = cats.map(c=>`
      <label class="pill"><input type="checkbox" value="${c.id}"><span># ${c.name}</span></label>
    `).join('');
        // chip toggle
        holder.querySelectorAll('.pill').forEach(p=>{
            const input = p.querySelector('input');
            const apply = ()=>p.classList.toggle('active', input.checked);
            p.addEventListener('click', (e)=>{ if(e.target.tagName!=='INPUT'){ input.checked = !input.checked; apply(); }});
            apply();
        });
    })();

    // add new category (ONLY here)
    document.getElementById('c_addcat').addEventListener('click', async ()=>{
        const v = document.getElementById('c_newcat').value.trim();
        if(!v) return;
        try{
            const cat = await jpost('/api/categories', {name:v});
            const holder = document.getElementById('c_pills');
            // avoid dup
            if(holder.querySelector(`input[value="${cat.id}"]`)) return;
            const el = document.createElement('label');
            el.className='pill active';
            el.innerHTML = `<input type="checkbox" value="${cat.id}" checked><span># ${cat.name}</span>`;
            holder.prepend(el);
            document.getElementById('c_newcat').value='';
        }catch(e){ alert('카테고리 추가 실패'); }
    });

    // cancel -> back to list
    document.getElementById('c_cancel').addEventListener('click', async (e)=>{ e.preventDefault(); await initialList(); });

    // submit
    document.getElementById('c_submit').addEventListener('click', async (e)=>{
        e.preventDefault();
        const name = document.getElementById('c_name').value.trim();
        if(!name){ alert('모임 이름을 입력하세요'); return; }

        const body = {
            name,
            description: document.getElementById('c_desc').value.trim(),
            regionDo: document.getElementById('c_do').value || null,
            regionSi: document.getElementById('c_si').value || null,
            imageUrl: document.getElementById('c_img').value.trim() || null,
            categoryIds: Array.from(document.querySelectorAll('#c_pills input:checked')).map(i=>Number(i.value)),
            newCategoryNames: [] // 만들기 화면에서만 +버튼으로 생성함
        };

        try{
            await jpost('/api/clubs', body); // 서버의 ClubService.createClub 시그니처 가정
            alert('모임이 생성되었습니다.');
            await initialList();
        }catch(e){
            alert('모임 생성 실패: ' + (e.message||''));
        }
    });
}

/* ====== Left menu actions ====== */
function mountMenu(){
    const main = document.getElementById('menuMain');
    const rec  = document.getElementById('menuRec');
    const cal  = document.getElementById('menuCal');
    const mine = document.getElementById('menuMine');
    const mk   = document.getElementById('menuMake');

    function setActive(el){
        document.querySelectorAll('.menu .item').forEach(i=>i.classList.remove('active'));
        el.classList.add('active');
    }

    main.addEventListener('click', async()=>{ setActive(main); await initialList(); });
    rec.addEventListener('click', ()=>{ setActive(rec); alert('추천은 다음 단계에서 붙일게'); });
    cal.addEventListener('click', ()=>{ setActive(cal); alert('캘린더는 다음 단계에서 붙일게'); });
    mine.addEventListener('click', ()=>{ setActive(mine); alert('내 모임은 로그인 연동 후 제공'); });
    mk.addEventListener('click', ()=>{ setActive(mk); showCreatePanel(); });
}

/* ====== Login/Register Modals ====== */
function mountAuth(){
    document.getElementById('openLogin').addEventListener('click', ()=>openModal('loginModal'));
    document.getElementById('openRegister').addEventListener('click', ()=>openModal('registerModal'));
    document.querySelectorAll('[data-close]').forEach(b=>b.addEventListener('click', (e)=>{ e.preventDefault(); closeModal(b.dataset.close); }));

    // Register: categories in modal
    (async()=>{
        const cats = await loadCategories();
        const holder = document.getElementById('regCats');
        holder.innerHTML = cats.map(c=>`
      <label class="cat-row">
        <input type="checkbox" name="categoryIds" value="${c.id}">
        <span># ${c.name}</span>
      </label>
    `).join('');
    })();
}

/* ====== Initial list ====== */
async function initialList(){
    const page = await searchClubs(new URLSearchParams([['page','0'],['size','12']]));
    renderClubs(page.content || page);
}

/* ====== Boot ====== */
window.addEventListener('DOMContentLoaded', async ()=>{
    mountAuth();
    mountMenu();
    await mountSidebarCategories();
    await mountSearchBar();
    await initialList();
});
