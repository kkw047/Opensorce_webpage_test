import { loadDos, loadSis } from './region.js';

const $ = (s, r=document)=>r.querySelector(s);
const $$ = (s, r=document)=>Array.from(r.querySelectorAll(s));

const state = {
    page: 0,
    size: 12,
    selectedCats: new Set(),
    rdo: '',
    rsi: '',
    kw: '',
    me: null
};

function openModal(id){ $(id).classList.add('show'); }
function closeModal(id){ $(id).classList.remove('show'); }

function toastErr(el, msg){ el.textContent = msg; setTimeout(()=>el.textContent='', 3000); }

async function refreshMe(){
    const res = await fetch('/api/auth/me');
    state.me = await res.json();
    const welcome = $('#welcomeArea');
    const btnLogin = $('#btnLogin'), btnReg = $('#btnRegister'), btnLogout = $('#btnLogout');
    if(state.me.authenticated){
        welcome.textContent = `안녕하세요, ${state.me.nickname}님`;
        btnLogin.style.display='none'; btnReg.style.display='none'; btnLogout.style.display='';
    }else{
        welcome.textContent = '';
        btnLogin.style.display=''; btnReg.style.display=''; btnLogout.style.display='none';
    }
}

async function initTopBar(){
    $('#btnLogin').addEventListener('click', ()=>openModal('#loginModal'));
    $('#btnRegister').addEventListener('click', async ()=>{
        await ensureRegCats(); openModal('#registerModal');
    });
    $('#btnLogout').addEventListener('click', async ()=>{
        await fetch('/api/auth/logout', {method:'POST'});
        await refreshMe();
        await fetchAndRender();
    });
    $$('#loginModal [data-close]').forEach(b=>b.addEventListener('click',e=>closeModal('#loginModal')));
    $$('#registerModal [data-close]').forEach(b=>b.addEventListener('click',e=>closeModal('#registerModal')));

    $('#doLogin').addEventListener('click', doLogin);
    $('#btnCheckId').addEventListener('click', checkId);
    $('#btnCheckEmail').addEventListener('click', checkEmail);
    $('#doRegister').addEventListener('click', doRegister);
}

async function doLogin(){
    const loginId = $('#loginId').value.trim();
    const password = $('#loginPw').value;
    const err = $('#loginErr');
    try{
        const res = await fetch('/api/auth/login', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({loginId, password})});
        if(!res.ok){ const j = await res.json(); throw new Error(j.error||'로그인 실패'); }
        closeModal('#loginModal'); await refreshMe();
    }catch(e){ toastErr(err, e.message); }
}

async function checkId(){
    const v = $('#regId').value.trim();
    const r = await fetch('/api/auth/check-id?loginId='+encodeURIComponent(v));
    const j = await r.json();
    $('#regMsg').textContent = j.duplicated ? '이미 사용중인 아이디입니다.' : '사용 가능한 아이디입니다.';
    setTimeout(()=>$('#regMsg').textContent='',2000);
}
async function checkEmail(){
    const v = $('#regEmail').value.trim();
    const r = await fetch('/api/auth/check-email?email='+encodeURIComponent(v));
    const j = await r.json();
    $('#regMsg').textContent = j.duplicated ? '이미 사용중인 이메일입니다.' : '사용 가능한 이메일입니다.';
    setTimeout(()=>$('#regMsg').textContent='',2000);
}

async function ensureRegCats(){
    const wrap = $('#regCats');
    if(wrap.childElementCount>0) return;
    const res = await fetch('/api/categories');
    const cats = await res.json();
    wrap.innerHTML='';
    cats.forEach(c=>{
        const lab = document.createElement('label');
        lab.className='chk';
        lab.innerHTML = `<input type="checkbox" value="${c.id}"> <span>${c.name}</span>`;
        wrap.appendChild(lab);
    });
}

async function doRegister(){
    const loginId = $('#regId').value.trim();
    const password = $('#regPw').value;
    const email = $('#regEmail').value.trim();
    const nickname = $('#regNick').value.trim();
    const categoryIds = $$('#regCats input[type=checkbox]:checked').map(i=>Number(i.value));
    const err = $('#regMsg');
    try{
        const res = await fetch('/api/auth/register', {method:'POST', headers:{'Content-Type':'application/json'},
            body:JSON.stringify({loginId,password,email,nickname,categoryIds})});
        if(!res.ok){ const j = await res.json().catch(()=>({})); throw new Error(j.error||'회원가입 실패'); }
        closeModal('#registerModal'); await refreshMe();
    }catch(e){ toastErr(err, e.message); }
}

async function initSearchBar(){
    const selDo = $('#selDo'), selSi = $('#selSi');
    await loadDos(selDo);
    selDo.addEventListener('change', ()=>loadSis(selDo.value, selSi,true));
    $('#btnSearch').addEventListener('click', ()=>{
        state.rdo = selDo.value; state.rsi = selSi.value; state.kw = $('#kw').value.trim(); state.page=0;
        fetchAndRender();
    });
}

async function initSidebar(){
    // 카테고리
    const res = await fetch('/api/categories');
    const cats = await res.json();
    const box = $('#catList');
    box.innerHTML = '';
    cats.forEach(c=>{
        const id = 'cat_'+c.id;
        const lab = document.createElement('label');
        lab.className='chk';
        lab.innerHTML = `<input id="${id}" type="checkbox" value="${c.id}"> <span>${c.name}</span>`;
        const i = lab.querySelector('input');
        i.addEventListener('change', ()=>{
            if(i.checked) state.selectedCats.add(Number(c.id));
            else state.selectedCats.delete(Number(c.id));
            state.page=0; fetchAndRender();
        });
        box.appendChild(lab);
    });

    // 모임 만들기 → 우측 패널로 폼 로드
    $('#btnNewClub').addEventListener('click', async ()=>{
        const main = $('#cards'); const pager = $('#pager');
        const html = await (await fetch('/clubs/create')).text();
        main.innerHTML = html; pager.innerHTML='';
        wireCreateForm(); // 폼 스크립트 바인딩
    });
}

function renderPager(totalPages){
    const pager = $('#pager'); pager.innerHTML='';
    const makeBtn = (p, lab)=> {
        const b = document.createElement('button'); b.textContent = lab;
        b.addEventListener('click', ()=>{ state.page = p; fetchAndRender(); });
        return b;
    };
    if(state.page>0) pager.appendChild(makeBtn(state.page-1, '이전'));
    pager.appendChild(document.createTextNode(` ${state.page+1} / ${Math.max(1,totalPages)} `));
    if(state.page+1<totalPages) pager.appendChild(makeBtn(state.page+1, '다음'));
}

function renderCards(page){
    const box = $('#cards'); box.innerHTML='';
    page.content.forEach(c=>{
        const div = document.createElement('div');
        div.className='card';
        div.innerHTML = `
      <img src="${c.imageUrl || 'https://picsum.photos/seed/'+c.id+'/400/240'}" alt="">
      <div class="card-body">
        <div style="font-weight:600">${c.name}</div>
        <div style="color:#6b7280;font-size:13px">${c.regionDo} ${c.regionSi}</div>
        <div class="badges">${(c.categories||[]).map(n=>`<span class="badge">${n}</span>`).join('')}</div>
      </div>`;
        box.appendChild(div);
    });
    renderPager(page.totalPages);
}

async function fetchAndRender(){
    const params = new URLSearchParams();
    if(state.rdo) params.set('rdo', state.rdo);
    if(state.rsi) params.set('rsi', state.rsi);
    if(state.kw) params.set('kw', state.kw);
    if(state.selectedCats.size>0) Array.from(state.selectedCats).forEach(id=>params.append('cats', String(id)));
    params.set('page', String(state.page));
    params.set('size', String(state.size));

    const res = await fetch('/api/clubs?'+params.toString());
    const page = await res.json();
    renderCards(page);
}

/* ----- 모임 만들기 폼 바인딩 (우측 패널) ----- */
function wireCreateForm(){
    const form = $('#clubForm'); if(!form) return;

    const formDo = $('#formDo'), formSi = $('#formSi');
    // 기존 템플릿 렌더에 도 리스트가 있음. 도 변경 시 시/군/구 채우기
    formDo.addEventListener('change', ()=>loadSis(formDo.value, formSi, false));

    // 신규 카테고리 chips
    const input = $('#newCatInput');
    const chips = $('#newCatChips');
    const hidden = $('#newCategoryNames');
    const bag = new Set();
    function renderChips(){
        chips.innerHTML=''; hidden.value = Array.from(bag).join(',');
        bag.forEach(nm=>{
            const span = document.createElement('span');
            span.className='chip'; span.textContent = nm;
            span.addEventListener('click', ()=>{ bag.delete(nm); renderChips(); });
            chips.appendChild(span);
        });
    }
    input.addEventListener('keydown', (e)=>{
        if(e.key==='Enter'){ e.preventDefault();
            const v=input.value.trim(); if(v){ bag.add(v); input.value=''; renderChips(); }
        }
    });

    $('#cancelCreate').addEventListener('click', fetchAndRender);

    form.addEventListener('submit', async (e)=>{
        e.preventDefault();
        const fd = new FormData(form);
        // hidden newCategoryNames → 배열로 변환
        const namesCSV = fd.get('newCategoryNames');
        if(namesCSV){ String(namesCSV).split(',').filter(Boolean).forEach(v=>fd.append('newCategoryNames', v)); }
        try{
            const res = await fetch('/api/clubs', {method:'POST', body:fd});
            if(!res.ok){ throw new Error('생성 실패'); }
            await fetchAndRender(); // 생성 후 목록으로 복귀
        }catch(err){
            const msg = $('#createMsg'); msg.textContent = err.message; setTimeout(()=>msg.textContent='',3000);
        }
    });
}

/* ----- 부트스트랩 ----- */
(async function boot(){
    await refreshMe();
    await initTopBar();
    await initSearchBar();
    await initSidebar();
    await fetchAndRender();
})();
