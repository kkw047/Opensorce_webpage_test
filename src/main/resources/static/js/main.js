(function () {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    // ===== 모달 =====
    function openModal(id) { $(id).hidden = false; }
    function closeModal(el) {
        const modal = el.closest('.modal');
        if (modal) modal.hidden = true;
    }

    $('#btnLoginOpen')?.addEventListener('click', () => openModal('#loginModal'));
    $('#btnRegOpen')?.addEventListener('click', () => openModal('#regModal'));
    $$('[data-close]').forEach(b => b.addEventListener('click', e => closeModal(e.target)));

    // ===== 로그인 AJAX =====
    $('#loginForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        const msg = $('#loginMsg');
        msg.textContent = '';

        const body = new URLSearchParams(new FormData(form));
        try {
            const res = await fetch('/auth/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body
            });
            const data = await res.json();
            if (!res.ok || !data.success) {
                msg.textContent = data.message || '로그인 실패';
                return;
            }
            msg.textContent = '로그인 성공';
            setTimeout(() => closeModal(form), 500);
        } catch (err) {
            msg.textContent = '네트워크 오류';
        }
    });

    // ===== 회원가입 AJAX =====
    $('#regForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        const msg = $('#regMsg');
        msg.textContent = '';

        // 체크박스 다중 name=categoryIds
        const fd = new FormData(form);
        const body = new URLSearchParams();
        for (const [k, v] of fd.entries()) body.append(k, v);

        try {
            const res = await fetch('/auth/register', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body
            });
            const data = await res.json();
            if (!res.ok || !data.success) {
                msg.textContent = data.message || '가입 실패';
                return;
            }
            msg.textContent = '가입 완료';
            setTimeout(() => closeModal(form), 500);
        } catch (err) {
            msg.textContent = '네트워크 오류';
        }
    });

    // ===== 모임 검색/필터 =====
    async function loadClubs() {
        const regionDo = $('#searchDo')?.value || '';
        const regionSi = $('#searchSi')?.value || '';
        const keyword = $('#searchKeyword')?.value?.trim() || '';
        const cats = $$('input[name="cat"]:checked').map(i => i.value);

        const params = new URLSearchParams();
        if (regionDo) params.set('regionDo', regionDo);
        if (regionSi) params.set('regionSi', regionSi);
        if (keyword) params.set('keyword', keyword);
        cats.forEach(c => params.append('cat', c));

        const res = await fetch('/api/clubs?' + params.toString());
        const list = await res.json();

        const wrap = $('#clubCards');
        wrap.innerHTML = '';
        list.forEach(c => {
            const el = document.createElement('article');
            el.className = 'card';
            el.innerHTML = `
                <div class="thumb"><img src="${c.imageUrl}" alt="club image"/></div>
                <div class="info">
                    <h3>${c.name ?? ''}</h3>
                    <p class="desc">${c.description ?? ''}</p>
                    <p class="region"><span>${c.regionDo ?? ''}</span> <span>${c.regionSi ?? ''}</span></p>
                    <div class="chips">
                        ${(c.categories || []).map(x => `<span class="chip">${x}</span>`).join('')}
                    </div>
                </div>`;
            wrap.appendChild(el);
        });
    }

    // 트리거
    $('#btnSearch')?.addEventListener('click', loadClubs);
    $('#btnReset')?.addEventListener('click', () => {
        $('#searchKeyword').value = '';
        $$('input[name="cat"]').forEach(i => (i.checked = false));
        loadClubs();
    });
    $$('input[name="cat"]').forEach(i => i.addEventListener('change', loadClubs));
    $('#searchDo')?.addEventListener('change', () => setTimeout(loadClubs, 100));
    $('#searchSi')?.addEventListener('change', loadClubs);

    // 초기 한번 로딩 (서버 렌더링과 동일하게 보이도록)
    window.addEventListener('load', () => {
        // 첫 로딩은 서버 렌더링이 있으니, 사용자 조작 시점부터 최신화
    });
})();
